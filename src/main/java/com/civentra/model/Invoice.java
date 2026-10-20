 package com.civentra.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Invoice implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long invoiceId;
    private String invoiceNumber;
    private Long citizenId;
    private String citizenName;
    private Long issueId;
    private Long providerId;
    private String providerName;
    private Long serviceRequestId;
    private String invoiceType = "COMMUNITY";
    private String status = "DRAFT";
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private BigDecimal balanceDue = BigDecimal.ZERO;
    private String currency = "INR";
    private String notes;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    public Invoice() {
        this.createdAt = LocalDateTime.now();
        this.issueDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(15);
        this.status = "DRAFT";
    }

    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        if (lineItems != null) {
            for (InvoiceLineItem li : lineItems) {
                if (li.getTotalPrice() != null) {
                    sum = sum.add(li.getTotalPrice());
                }
            }
        }
        this.subtotal = sum.setScale(2, RoundingMode.HALF_UP);
        BigDecimal t = tax == null ? BigDecimal.ZERO : tax;
        BigDecimal d = discount == null ? BigDecimal.ZERO : discount;
        this.totalAmount = subtotal.add(t).subtract(d).setScale(2, RoundingMode.HALF_UP);
        BigDecimal paid = amountPaid == null ? BigDecimal.ZERO : amountPaid;
        this.balanceDue = totalAmount.subtract(paid).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public void setInvoiceId(long invoiceId) { this.invoiceId = invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = (long) invoiceId; }
    public int getInvoiceIdAsInt() { return invoiceId == null ? 0 : invoiceId.intValue(); }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Long getCitizenId() { return citizenId; }
    public void setCitizenId(Long citizenId) { this.citizenId = citizenId; }
    public void setCitizenId(long citizenId) { this.citizenId = citizenId; }
    public void setCitizenId(int citizenId) { this.citizenId = (long) citizenId; }
    public int getCitizenIdAsInt() { return citizenId == null ? 0 : citizenId.intValue(); }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public void setIssueId(long issueId) { this.issueId = issueId; }
    public void setIssueId(int issueId) { this.issueId = (long) issueId; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public void setProviderId(long providerId) { this.providerId = providerId; }
    public void setProviderId(int providerId) { this.providerId = (long) providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public Long getServiceRequestId() { return serviceRequestId; }
    public void setServiceRequestId(Long serviceRequestId) { this.serviceRequestId = serviceRequestId; }

    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getSubtotal() { return subtotal == null ? BigDecimal.ZERO : subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = BigDecimal.valueOf(subtotal); }
    public double getSubtotalAsDouble() { return getSubtotal().doubleValue(); }

    public BigDecimal getTax() { return tax == null ? BigDecimal.ZERO : tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public void setTax(double tax) { this.tax = BigDecimal.valueOf(tax); }
    public double getTaxAsDouble() { return getTax().doubleValue(); }

    public BigDecimal getDiscount() { return discount == null ? BigDecimal.ZERO : discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotalAmount() { return totalAmount == null ? BigDecimal.ZERO : totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = BigDecimal.valueOf(totalAmount); }
    public double getTotalAmountAsDouble() { return getTotalAmount().doubleValue(); }
    public double getTotalAsDouble() { return getTotalAmountAsDouble(); }

    public BigDecimal getAmountPaid() { return amountPaid == null ? BigDecimal.ZERO : amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = BigDecimal.valueOf(amountPaid); }
    public void setPaidAmount(double paidAmount) { setAmountPaid(paidAmount); }
    public void setPaidAmount(BigDecimal paidAmount) { setAmountPaid(paidAmount); }
    public double getAmountPaidAsDouble() { return getAmountPaid().doubleValue(); }

    public BigDecimal getBalanceDue() { return balanceDue == null ? BigDecimal.ZERO : balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public List<InvoiceLineItem> getLineItems() {
        if (lineItems == null) lineItems = new ArrayList<>();
        return lineItems;
    }
    public void setLineItems(List<InvoiceLineItem> lineItems) {
        this.lineItems = lineItems == null ? new ArrayList<>() : lineItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice that)) return false;
        return Objects.equals(invoiceId, that.invoiceId);
    }

    @Override
    public int hashCode() { return Objects.hash(invoiceId); }
}