package com.civentra.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class CommunityIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long issueId;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long reportedBy;
    private String reporterName;
    private String reporterEmail;
    private Long assignedTo;
    private String assigneeName;
    private Long providerId;
    private String providerName;
    private IssueStatus status = IssueStatus.REPORTED;
    private IssuePriority priority = IssuePriority.MEDIUM;
    private String verificationStatus;
    private Long locationId;
    private String locationAddress;
    private String addressDetail;
    private String landmark;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String ward;
    private String zone;
    private String city;
    private String stateCode;
    private String pincode;
    private String imageUrl;
    private String evidencePath;
    private Integer deptId;
    private String resolutionNotes;
    private String officerNotes;
    private Integer upvoteCount = 0;
    private LocalDateTime reportedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime citizenConfirmedAt;

    public CommunityIssue() {
        this.status = IssueStatus.REPORTED;
        this.priority = IssuePriority.MEDIUM;
        this.createdAt = LocalDateTime.now();
        this.reportedAt = LocalDateTime.now();
        this.upvoteCount = 0;
    }

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public void setIssueId(long issueId) { this.issueId = issueId; }
    public void setIssueId(int issueId) { this.issueId = (long) issueId; }
    public int getIssueIdAsInt() { return issueId == null ? 0 : issueId.intValue(); }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = (long) categoryId; }
    public int getCategoryIdAsInt() { return categoryId == null ? 0 : categoryId.intValue(); }

    public Long getReportedBy() { return reportedBy; }
    public void setReportedBy(Long reportedBy) { this.reportedBy = reportedBy; }
    public void setReportedBy(long reportedBy) { this.reportedBy = reportedBy; }
    public void setReportedBy(int reportedBy) { this.reportedBy = (long) reportedBy; }
    public int getReportedByAsInt() { return reportedBy == null ? 0 : reportedBy.intValue(); }
    public int getUserIdAsInt() { return getReportedByAsInt(); }

    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public void setAssignedTo(long assignedTo) { this.assignedTo = assignedTo; }
    public void setAssignedTo(int assignedTo) { this.assignedTo = (long) assignedTo; }
    public int getAssignedToAsInt() { return assignedTo == null ? 0 : assignedTo.intValue(); }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public void setProviderId(long providerId) { this.providerId = providerId; }
    public void setProviderId(int providerId) { this.providerId = (long) providerId; }
    public int getProviderIdAsInt() { return providerId == null ? 0 : providerId.intValue(); }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public void setLocationId(long locationId) { this.locationId = locationId; }
    public void setLocationId(int locationId) { this.locationId = (long) locationId; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    // Alias methods to support locationText callers
    public String getLocationText() { return locationAddress; }
    public void setLocationText(String locationText) { this.locationAddress = locationText; }

    public String getAddressDetail() { return addressDetail != null ? addressDetail : locationAddress; }
    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
        if (this.locationAddress == null) this.locationAddress = addressDetail;
    }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getEvidencePath() { return evidencePath != null ? evidencePath : imageUrl; }
    public void setEvidencePath(String evidencePath) {
        this.evidencePath = evidencePath;
        if (this.imageUrl == null) this.imageUrl = evidencePath;
    }

    public Integer getDeptId() { return deptId; }
    public void setDeptId(Integer deptId) { this.deptId = deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getOfficerNotes() { return officerNotes; }
    public void setOfficerNotes(String officerNotes) { this.officerNotes = officerNotes; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status == null ? IssueStatus.REPORTED : status; }
    public void setStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            this.status = IssueStatus.REPORTED;
            return;
        }
        this.status = IssueStatus.fromString(statusStr);
    }
    public String getStatusAsString() { return status == null ? "REPORTED" : status.name(); }

    public IssuePriority getPriority() { return priority; }
    public void setPriority(IssuePriority priority) { this.priority = priority == null ? IssuePriority.MEDIUM : priority; }
    public void setPriority(String priorityStr) {
        if (priorityStr == null || priorityStr.isBlank()) {
            this.priority = IssuePriority.MEDIUM;
            return;
        }
        try {
            this.priority = IssuePriority.valueOf(priorityStr.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            this.priority = IssuePriority.MEDIUM;
        }
    }
    public String getPriorityAsString() { return priority == null ? "MEDIUM" : priority.name(); }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude == null ? null : BigDecimal.valueOf(latitude); }
    public void setLatitude(double latitude) { this.latitude = BigDecimal.valueOf(latitude); }
    public double getLatitudeAsDouble() { return latitude == null ? 0.0d : latitude.doubleValue(); }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude == null ? null : BigDecimal.valueOf(longitude); }
    public void setLongitude(double longitude) { this.longitude = BigDecimal.valueOf(longitude); }
    public double getLongitudeAsDouble() { return longitude == null ? 0.0d : longitude.doubleValue(); }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public Integer getUpvoteCount() { return upvoteCount == null ? 0 : upvoteCount; }
    public void setUpvoteCount(Integer upvoteCount) { this.upvoteCount = upvoteCount == null ? 0 : upvoteCount; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getCitizenConfirmedAt() { return citizenConfirmedAt; }
    public void setCitizenConfirmedAt(LocalDateTime citizenConfirmedAt) { this.citizenConfirmedAt = citizenConfirmedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommunityIssue that)) return false;
        return Objects.equals(issueId, that.issueId);
    }

    @Override
    public int hashCode() { return Objects.hash(issueId); }
}