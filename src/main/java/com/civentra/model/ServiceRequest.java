package com.civentra.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ServiceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long requestId;
    private Long citizenId;
    private Long providerId;
    private Long serviceCategoryId;
    private String citizenName;
    private String providerName;
    private String categoryName;
    private String requestDetails;
    private String title;
    private String status = "PENDING";
    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime preferredDate;
    private String preferredTime = "Morning (9 AM - 12 PM)";
    private String address;
    private String notes;
    private String completionNotes;
    private LocalDateTime updatedAt;
    private Long estimateId;
    private String estimateDescription;
    private java.math.BigDecimal estimateSubtotal;
    private java.math.BigDecimal estimateTax;
    private java.math.BigDecimal estimateTotal;
    private String estimateStatus;

    public ServiceRequest() {
    }

    public Long getRequestId() {
        return requestId;
    }

    public int getRequestIdAsInt() {
        return requestId != null ? requestId.intValue() : 0;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = (long) requestId;
    }

    public Long getCitizenId() {
        return citizenId;
    }

    public int getCitizenIdAsInt() {
        return citizenId != null ? citizenId.intValue() : 0;
    }

    public void setCitizenId(Long citizenId) {
        this.citizenId = citizenId;
    }

    public void setCitizenId(int citizenId) {
        this.citizenId = (long) citizenId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public int getProviderIdAsInt() {
        return providerId != null ? providerId.intValue() : 0;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public void setProviderId(int providerId) {
        this.providerId = (long) providerId;
    }

    public Long getServiceCategoryId() {
        return serviceCategoryId;
    }

    public int getServiceCategoryIdAsInt() {
        return serviceCategoryId != null ? serviceCategoryId.intValue() : 0;
    }

    public void setServiceCategoryId(Long serviceCategoryId) {
        this.serviceCategoryId = serviceCategoryId;
    }

    public void setServiceCategoryId(int serviceCategoryId) {
        this.serviceCategoryId = (long) serviceCategoryId;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getRequestDetails() {
        return requestDetails;
    }

    public void setRequestDetails(String requestDetails) {
        this.requestDetails = requestDetails;
    }

    public String getTitle() {
        return title != null ? title : requestDetails;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return requestDetails;
    }

    public void setDescription(String description) {
        this.requestDetails = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDateTime preferredDate) {
        this.preferredDate = preferredDate;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCompletionNotes() { return completionNotes; }
    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getEstimateId() { return estimateId; }
    public void setEstimateId(Long estimateId) { this.estimateId = estimateId; }
    public String getEstimateDescription() { return estimateDescription; }
    public void setEstimateDescription(String estimateDescription) { this.estimateDescription = estimateDescription; }
    public java.math.BigDecimal getEstimateSubtotal() { return estimateSubtotal == null ? java.math.BigDecimal.ZERO : estimateSubtotal; }
    public void setEstimateSubtotal(java.math.BigDecimal estimateSubtotal) { this.estimateSubtotal = estimateSubtotal; }
    public java.math.BigDecimal getEstimateTax() { return estimateTax == null ? java.math.BigDecimal.ZERO : estimateTax; }
    public void setEstimateTax(java.math.BigDecimal estimateTax) { this.estimateTax = estimateTax; }
    public java.math.BigDecimal getEstimateTotal() { return estimateTotal == null ? java.math.BigDecimal.ZERO : estimateTotal; }
    public void setEstimateTotal(java.math.BigDecimal estimateTotal) { this.estimateTotal = estimateTotal; }
    public String getEstimateStatus() { return estimateStatus; }
    public void setEstimateStatus(String estimateStatus) { this.estimateStatus = estimateStatus; }
}
