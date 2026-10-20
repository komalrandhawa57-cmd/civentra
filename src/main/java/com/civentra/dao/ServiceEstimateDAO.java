package com.civentra.dao;

import com.civentra.model.ServiceEstimate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ServiceEstimateDAO {
    private final JdbcTemplate jdbc;

    public ServiceEstimateDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long submit(long requestId, long providerId, long providerUserId, String description,
                       BigDecimal subtotal, BigDecimal tax) {
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.isBlank() || cleanDescription.length() > 1000) {
            throw new IllegalArgumentException("Estimate description is required (up to 1,000 characters).");
        }
        BigDecimal cleanSubtotal = money(subtotal);
        BigDecimal cleanTax = money(tax);
        if (cleanSubtotal.signum() <= 0 || cleanTax.signum() < 0) {
            throw new IllegalArgumentException("Enter a positive estimate amount and a non-negative tax amount.");
        }
        Map<String, Object> request = lockRequest(requestId, providerId);
        String current = normalize(String.valueOf(request.get("STATUS")));
        if (!"ACCEPTED".equals(current)) {
            throw new IllegalStateException("A cost estimate can only be submitted after the request is accepted.");
        }
        long citizenId = ((Number) request.get("CITIZEN_ID")).longValue();
        BigDecimal total = cleanSubtotal.add(cleanTax).setScale(2, RoundingMode.HALF_UP);
        String sql = "INSERT INTO SERVICE_ESTIMATES (REQUEST_ID, PROVIDER_ID, CITIZEN_ID, DESCRIPTION, SUBTOTAL, TAX, TOTAL_AMOUNT, STATUS, CREATED_AT) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ESTIMATE_ID"});
            ps.setLong(1, requestId); ps.setLong(2, providerId); ps.setLong(3, citizenId);
            ps.setString(4, cleanDescription); ps.setBigDecimal(5, cleanSubtotal);
            ps.setBigDecimal(6, cleanTax); ps.setBigDecimal(7, total);
            return ps;
        }, key);
        Number estimateId = key.getKey();
        if (estimateId == null) throw new IllegalStateException("The database did not return an estimate reference.");
        jdbc.update("INSERT INTO SERVICE_ESTIMATE_HISTORY (ESTIMATE_ID, FROM_STATUS, TO_STATUS, CHANGED_BY, REMARKS, CHANGED_AT) "
                        + "VALUES (?, NULL, 'PENDING', ?, ?, CURRENT_TIMESTAMP)",
                estimateId.longValue(), providerUserId, cleanDescription);
        updateRequestStatus(requestId, "ACCEPTED", "AWAITING_ESTIMATE_APPROVAL", providerUserId,
                "Provider submitted estimate " + estimateId);
        return estimateId.longValue();
    }

    public ServiceEstimate review(long requestId, long citizenId, long changedBy, boolean approve, String remarks) {
        Map<String, Object> request = lockCitizenRequest(requestId, citizenId);
        String current = normalize(String.valueOf(request.get("STATUS")));
        if (!"AWAITING_ESTIMATE_APPROVAL".equals(current)) {
            throw new IllegalStateException("This request has no estimate awaiting your decision.");
        }
        ServiceEstimate estimate = latestForRequest(requestId)
                .orElseThrow(() -> new IllegalStateException("The pending estimate could not be found."));
        if (!"PENDING".equalsIgnoreCase(estimate.getStatus())) {
            throw new IllegalStateException("This estimate has already been reviewed.");
        }
        String status = approve ? "APPROVED" : "DECLINED";
        int changed = jdbc.update("UPDATE SERVICE_ESTIMATES SET STATUS = ?, REVIEWED_AT = CURRENT_TIMESTAMP, REVIEWED_BY = ?, REVIEW_REMARKS = ? WHERE ESTIMATE_ID = ? AND STATUS = 'PENDING'",
                status, changedBy, remarks, estimate.getEstimateId());
        if (changed != 1) throw new IllegalStateException("The estimate changed while your decision was being saved.");
        jdbc.update("INSERT INTO SERVICE_ESTIMATE_HISTORY (ESTIMATE_ID, FROM_STATUS, TO_STATUS, CHANGED_BY, REMARKS, CHANGED_AT) "
                        + "VALUES (?, 'PENDING', ?, ?, ?, CURRENT_TIMESTAMP)",
                estimate.getEstimateId(), status, changedBy, remarks);
        updateRequestStatus(requestId, current, "ACCEPTED", changedBy,
                "Citizen " + status.toLowerCase(java.util.Locale.ROOT) + " estimate " + estimate.getEstimateId());
        estimate.setStatus(status);
        estimate.setReviewRemarks(remarks);
        return estimate;
    }

    public Optional<ServiceEstimate> latestForRequest(long requestId) {
        List<ServiceEstimate> rows = jdbc.query("SELECT * FROM (SELECT * FROM SERVICE_ESTIMATES WHERE REQUEST_ID = ? ORDER BY ESTIMATE_ID DESC) WHERE ROWNUM = 1",
                (rs, rowNum) -> {
                    ServiceEstimate estimate = new ServiceEstimate();
                    estimate.setEstimateId(rs.getLong("ESTIMATE_ID")); estimate.setRequestId(rs.getLong("REQUEST_ID"));
                    estimate.setProviderId(rs.getLong("PROVIDER_ID")); estimate.setCitizenId(rs.getLong("CITIZEN_ID"));
                    estimate.setDescription(rs.getString("DESCRIPTION")); estimate.setSubtotal(rs.getBigDecimal("SUBTOTAL"));
                    estimate.setTax(rs.getBigDecimal("TAX")); estimate.setTotal(rs.getBigDecimal("TOTAL_AMOUNT"));
                    estimate.setStatus(rs.getString("STATUS")); estimate.setReviewRemarks(rs.getString("REVIEW_REMARKS"));
                    Timestamp created = rs.getTimestamp("CREATED_AT"); if (created != null) estimate.setCreatedAt(created.toLocalDateTime());
                    Timestamp reviewed = rs.getTimestamp("REVIEWED_AT"); if (reviewed != null) estimate.setReviewedAt(reviewed.toLocalDateTime());
                    return estimate;
                }, requestId);
        return rows.stream().findFirst();
    }

    public Optional<ServiceEstimate> approvedForCompletedRequest(long requestId) {
        return jdbc.query("SELECT e.* FROM SERVICE_ESTIMATES e JOIN SERVICE_REQUESTS r ON r.REQUEST_ID = e.REQUEST_ID "
                        + "WHERE e.REQUEST_ID = ? AND e.STATUS = 'APPROVED' AND r.STATUS = 'COMPLETED' "
                        + "AND e.ESTIMATE_ID = (SELECT MAX(e2.ESTIMATE_ID) FROM SERVICE_ESTIMATES e2 WHERE e2.REQUEST_ID = e.REQUEST_ID)",
                (rs, rowNum) -> {
                    ServiceEstimate estimate = new ServiceEstimate();
                    estimate.setEstimateId(rs.getLong("ESTIMATE_ID")); estimate.setRequestId(rs.getLong("REQUEST_ID"));
                    estimate.setProviderId(rs.getLong("PROVIDER_ID")); estimate.setCitizenId(rs.getLong("CITIZEN_ID"));
                    estimate.setDescription(rs.getString("DESCRIPTION")); estimate.setSubtotal(rs.getBigDecimal("SUBTOTAL"));
                    estimate.setTax(rs.getBigDecimal("TAX")); estimate.setTotal(rs.getBigDecimal("TOTAL_AMOUNT"));
                    estimate.setStatus(rs.getString("STATUS"));
                    return estimate;
                }, requestId).stream().findFirst();
    }

    private Map<String, Object> lockRequest(long requestId, long providerId) {
        try {
            return jdbc.queryForMap("SELECT STATUS, CITIZEN_ID FROM SERVICE_REQUESTS WHERE REQUEST_ID = ? AND PROVIDER_ID = ? FOR UPDATE",
                    requestId, providerId);
        } catch (EmptyResultDataAccessException ex) {
            throw new SecurityException("This request is not assigned to your provider account.");
        }
    }

    private Map<String, Object> lockCitizenRequest(long requestId, long citizenId) {
        try {
            return jdbc.queryForMap("SELECT STATUS, CITIZEN_ID FROM SERVICE_REQUESTS WHERE REQUEST_ID = ? AND CITIZEN_ID = ? FOR UPDATE",
                    requestId, citizenId);
        } catch (EmptyResultDataAccessException ex) {
            throw new SecurityException("This request does not belong to your account.");
        }
    }

    private void updateRequestStatus(long requestId, String current, String next, long changedBy, String remarks) {
        int count = jdbc.update("UPDATE SERVICE_REQUESTS SET STATUS = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE REQUEST_ID = ? AND UPPER(STATUS) = ?",
                next, requestId, current);
        if (count != 1) throw new IllegalStateException("The request changed while the estimate was being updated.");
        jdbc.update("INSERT INTO SERVICE_REQUEST_STATUS_HISTORY (HISTORY_ID, REQUEST_ID, FROM_STATUS, TO_STATUS, CHANGED_BY, REMARKS, CHANGED_AT) "
                        + "VALUES (SERVICE_REQUEST_HISTORY_SEQ.NEXTVAL, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                requestId, current, next, changedBy, remarks);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT); }
}
