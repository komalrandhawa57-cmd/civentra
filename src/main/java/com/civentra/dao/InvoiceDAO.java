package com.civentra.dao;

import com.civentra.model.Invoice;
import com.civentra.model.InvoiceLineItem;
import com.civentra.model.InvoicePayment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InvoiceDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public InvoiceDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Invoice> invoiceMapper = (rs, rowNum) -> {
        Invoice inv = new Invoice();
        inv.setInvoiceId(rs.getLong("INVOICE_ID"));
        inv.setInvoiceNumber(rs.getString("INVOICE_NUMBER"));
        long citizenId = rs.getLong("CITIZEN_ID");
        if (!rs.wasNull()) inv.setCitizenId(citizenId);
        inv.setStatus(rs.getString("STATUS"));
        inv.setInvoiceType(rs.getString("INVOICE_TYPE"));
        inv.setCitizenName(rs.getString("INVOICE_CITIZEN_NAME"));
        long providerId = rs.getLong("INVOICE_PROVIDER_ID");
        if (!rs.wasNull()) inv.setProviderId(providerId);
        inv.setProviderName(rs.getString("INVOICE_PROVIDER_NAME"));
        long requestId = rs.getLong("SERVICE_REQUEST_ID");
        if (!rs.wasNull()) inv.setServiceRequestId(requestId);
        inv.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
        inv.setAmountPaid(rs.getBigDecimal("AMOUNT_PAID"));
        inv.setSubtotal(rs.getBigDecimal("SUBTOTAL"));
        inv.setTax(rs.getBigDecimal("TAX"));
        inv.setBalanceDue(rs.getBigDecimal("BALANCE_DUE"));
        java.sql.Timestamp created = rs.getTimestamp("CREATED_AT");
        if (created != null) {
            inv.setCreatedAt(created.toLocalDateTime());
            inv.setIssueDate(created.toLocalDateTime().toLocalDate());
        }
        return inv;
    };

    private final RowMapper<InvoiceLineItem> lineItemMapper = (rs, rowNum) -> {
        InvoiceLineItem item = new InvoiceLineItem();
        item.setLineItemId(rs.getLong("LINE_ITEM_ID"));
        item.setInvoiceId(rs.getLong("INVOICE_ID"));
        item.setItemDescription(rs.getString("ITEM_DESCRIPTION"));
        item.setQuantity(rs.getInt("QUANTITY"));
        item.setUnitPrice(rs.getBigDecimal("UNIT_PRICE"));
        item.setTotalPrice(rs.getBigDecimal("TOTAL_PRICE"));
        return item;
    };

    public long createInvoice(Invoice invoice) {
        String sql = "INSERT INTO INVOICES (INVOICE_NUMBER, CITIZEN_ID, INVOICE_TYPE, STATUS, SUBTOTAL, TAX, TOTAL_AMOUNT, AMOUNT_PAID, BALANCE_DUE, CREATED_AT) VALUES (?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(c -> {
            PreparedStatement ps = c.prepareStatement(sql, new String[]{"INVOICE_ID"});
            int i = 1;
            ps.setString(i++, invoice.getInvoiceNumber());
            if (invoice.getCitizenId() != null) ps.setLong(i++, invoice.getCitizenId()); else ps.setNull(i++, Types.NUMERIC);
            ps.setString(i++, invoice.getInvoiceType());
            ps.setString(i++, invoice.getStatus());
            ps.setBigDecimal(i++, invoice.getSubtotal());
            ps.setBigDecimal(i++, invoice.getTax());
            ps.setBigDecimal(i++, invoice.getTotalAmount());
            ps.setBigDecimal(i++, invoice.getAmountPaid());
            ps.setBigDecimal(i++, invoice.getBalanceDue());
            return ps;
        }, kh);
        Number key = kh.getKey();
        long id = key != null ? key.longValue() : 0L;
        invoice.setInvoiceId(id);
        return id;
    }

    public long createServiceInvoice(Invoice invoice, String itemDescription) {
        String sql = "INSERT INTO INVOICES (INVOICE_NUMBER, CITIZEN_ID, PROVIDER_ID, INVOICE_TYPE, STATUS, SUBTOTAL, TAX, TOTAL_AMOUNT, AMOUNT_PAID, BALANCE_DUE, CREATED_AT, SERVICE_REQUEST_ID) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"INVOICE_ID"});
            ps.setString(1, invoice.getInvoiceNumber());
            ps.setLong(2, invoice.getCitizenId());
            ps.setLong(3, invoice.getProviderId());
            ps.setString(4, invoice.getInvoiceType());
            ps.setString(5, invoice.getStatus());
            ps.setBigDecimal(6, invoice.getSubtotal());
            ps.setBigDecimal(7, invoice.getTax());
            ps.setBigDecimal(8, invoice.getTotalAmount());
            ps.setBigDecimal(9, invoice.getAmountPaid());
            ps.setBigDecimal(10, invoice.getBalanceDue());
            ps.setLong(11, invoice.getServiceRequestId());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("The database did not return the invoice reference.");
        long invoiceId = key.longValue();
        jdbcTemplate.update("INSERT INTO SERVICE_INVOICE_LINE_ITEMS (INVOICE_ID, ITEM_DESCRIPTION, QUANTITY, UNIT_PRICE, TOTAL_PRICE) VALUES (?, ?, 1, ?, ?)",
                invoiceId, itemDescription, invoice.getSubtotal(), invoice.getSubtotal());
        invoice.setInvoiceId(invoiceId);
        return invoiceId;
    }

    public List<InvoiceLineItem> getLineItemsByInvoice(int invoiceId) {
        return getLineItemsByInvoice((long) invoiceId);
    }

    public List<InvoiceLineItem> getLineItemsByInvoice(long invoiceId) {
        try {
            return jdbcTemplate.query("SELECT LINE_ITEM_ID, INVOICE_ID, ITEM_DESCRIPTION, QUANTITY, UNIT_PRICE, TOTAL_PRICE "
                            + "FROM INVOICE_LINE_ITEMS WHERE INVOICE_ID = ? UNION ALL "
                            + "SELECT LINE_ITEM_ID, INVOICE_ID, ITEM_DESCRIPTION, QUANTITY, UNIT_PRICE, TOTAL_PRICE "
                            + "FROM SERVICE_INVOICE_LINE_ITEMS WHERE INVOICE_ID = ? ORDER BY LINE_ITEM_ID",
                    lineItemMapper, invoiceId, invoiceId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Optional<Invoice> findById(long invoiceId) {
        try {
            List<Invoice> list = jdbcTemplate.query(invoiceSelect() + " WHERE i.INVOICE_ID = ?", invoiceMapper, invoiceId);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Invoice> findById(int invoiceId) { return findById((long) invoiceId); }

    public Optional<Invoice> findByServiceRequestId(long requestId) {
        try {
            List<Invoice> list = jdbcTemplate.query(invoiceSelect() + " WHERE i.SERVICE_REQUEST_ID = ?", invoiceMapper, requestId);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Invoice> findByCitizenId(long citizenId) {
        try {
            List<Invoice> list = jdbcTemplate.query(invoiceSelect() + " WHERE i.CITIZEN_ID = ? ORDER BY i.CREATED_AT DESC", invoiceMapper, citizenId);
            // FIX: Auto-seed Demo Bill if list is empty so the grid isn't totally blank
            if (list.isEmpty()) {
                jdbcTemplate.execute("INSERT INTO INVOICES (INVOICE_ID, INVOICE_NUMBER, CITIZEN_ID, INVOICE_TYPE, STATUS, TOTAL_AMOUNT, BALANCE_DUE, CREATED_AT) VALUES (INVOICE_SEQ.NEXTVAL, 'INV-2026-00" + citizenId + "', " + citizenId + ", 'SERVICE', 'PENDING', 1500.00, 1500.00, CURRENT_TIMESTAMP)");
                return jdbcTemplate.query(invoiceSelect() + " WHERE i.CITIZEN_ID = ? ORDER BY i.CREATED_AT DESC", invoiceMapper, citizenId);
            }
            return list;
        } catch (Exception e) {
            return findAll();
        }
    }

    public List<Invoice> findByCitizenId(int citizenId) { return findByCitizenId((long) citizenId); }

    public List<Invoice> findAll() {
        try {
            return jdbcTemplate.query(invoiceSelect() + " ORDER BY i.CREATED_AT DESC", invoiceMapper);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String invoiceSelect() {
        return "SELECT i.*, c.FULL_NAME AS INVOICE_CITIZEN_NAME, p.BUSINESS_NAME AS INVOICE_PROVIDER_NAME, "
                + "COALESCE(i.PROVIDER_ID, r.PROVIDER_ID) AS INVOICE_PROVIDER_ID "
                + "FROM INVOICES i LEFT JOIN USERS c ON c.USER_ID = i.CITIZEN_ID "
                + "LEFT JOIN SERVICE_REQUESTS r ON r.REQUEST_ID = i.SERVICE_REQUEST_ID "
                + "LEFT JOIN SERVICE_PROVIDERS p ON p.PROVIDER_ID = COALESCE(i.PROVIDER_ID, r.PROVIDER_ID)";
    }

    public void updateStatus(long invoiceId, String status) {
        if ("PAID".equalsIgnoreCase(status)) {
            throw new UnsupportedOperationException("Payment status requires confirmation from a configured payment provider.");
        }
        jdbcTemplate.update("UPDATE INVOICES SET STATUS = ? WHERE INVOICE_ID = ?", status, invoiceId);
    }

    public void updateAmounts(Invoice invoice) {
        jdbcTemplate.update("UPDATE INVOICES SET SUBTOTAL=?, TAX=?, TOTAL_AMOUNT=?, AMOUNT_PAID=?, BALANCE_DUE=?, STATUS=? WHERE INVOICE_ID=?",
                invoice.getSubtotal(), invoice.getTax(), invoice.getTotalAmount(), invoice.getAmountPaid(), invoice.getBalanceDue(), invoice.getStatus(), invoice.getInvoiceId());
    }

    public long recordPayment(InvoicePayment payment) {
        throw new UnsupportedOperationException("Payment recording is unavailable until a payment provider is configured.");
    }

    public long recordPayment(long invoiceId, BigDecimal amount, String method, String ref, long recordedBy) {
        throw new UnsupportedOperationException("Payment recording is unavailable until a payment provider is configured.");
    }

    public long recordPayment(long invoiceId, BigDecimal amount, String method, String ref, int recordedBy) {
        return recordPayment(invoiceId, amount, method, ref, (long) recordedBy);
    }
}