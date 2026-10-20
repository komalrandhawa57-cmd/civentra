package com.civentra.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class InvoiceLineItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long lineItemId;
    private Long invoiceId;
    private String itemDescription;
    private Integer quantity = 1;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private String unit;
    private Integer sortOrder = 0;

    public InvoiceLineItem() {
        this.quantity = 1;
    }

    public InvoiceLineItem(String itemDescription, int quantity, BigDecimal unitPrice) {
        this();
        this.itemDescription = itemDescription;
        this.quantity = quantity;
        this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        recalculate();
    }

    public InvoiceLineItem(String itemDescription, int quantity, double unitPrice) {
        this(itemDescription, quantity, BigDecimal.valueOf(unitPrice));
    }

    public InvoiceLineItem(long lineItemId, long invoiceId, String itemDescription, int quantity, double unitPrice) {
        this(itemDescription, quantity, unitPrice);
        this.lineItemId = lineItemId;
        this.invoiceId = invoiceId;
    }

    public void recalculate() {
        BigDecimal q = BigDecimal.valueOf(quantity == null ? 0 : quantity);
        BigDecimal up = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        this.totalPrice = up.multiply(q).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getLineItemId() { return lineItemId; }
    public void setLineItemId(Long lineItemId) { this.lineItemId = lineItemId; }
    public void setLineItemId(long lineItemId) { this.lineItemId = lineItemId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public void setInvoiceId(long invoiceId) { this.invoiceId = invoiceId; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public Integer getQuantity() { return quantity == null ? 0 : quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        recalculate();
    }

    public BigDecimal getUnitPrice() { return unitPrice == null ? BigDecimal.ZERO : unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        recalculate();
    }
    public double getUnitPriceAsDouble() { return getUnitPrice().doubleValue(); }

    public BigDecimal getTotalPrice() { return totalPrice == null ? BigDecimal.ZERO : totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = BigDecimal.valueOf(totalPrice); }
    public double getTotalPriceAsDouble() { return getTotalPrice().doubleValue(); }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getSortOrder() { return sortOrder == null ? 0 : sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceLineItem that)) return false;
        return Objects.equals(lineItemId, that.lineItemId);
    }

    @Override
    public int hashCode() { return Objects.hash(lineItemId); }
}