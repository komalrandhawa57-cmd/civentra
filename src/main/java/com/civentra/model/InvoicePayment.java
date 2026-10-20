package com.civentra.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoicePayment {
    private Long paymentId;
    private Long invoiceId;
    private BigDecimal amount = BigDecimal.ZERO;
    private String paymentMethod;
    private String transactionRef;
    private Long recordedBy;
    private LocalDateTime paymentDate = LocalDateTime.now();

    public InvoicePayment() {
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public void setPaymentId(long paymentId) { this.paymentId = paymentId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public void setInvoiceId(long invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public double getAmountAsDouble() { return amount != null ? amount.doubleValue() : 0.0; }
    public void setAmount(double amount) { this.amount = BigDecimal.valueOf(amount); }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public void setRecordedBy(long recordedBy) { this.recordedBy = recordedBy; }
    public void setRecordedBy(int recordedBy) { this.recordedBy = (long) recordedBy; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}