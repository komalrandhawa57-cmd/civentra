package com.civentra.service;

import com.civentra.model.Invoice;
import com.civentra.model.InvoiceLineItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Renders an escaped, self-contained HTML invoice that can be opened, printed, or saved. */
public final class InvoiceDocumentRenderer {
    private InvoiceDocumentRenderer() {}

    public static String render(Invoice invoice, List<InvoiceLineItem> items) {
        if (invoice == null) throw new IllegalArgumentException("An invoice is required.");
        StringBuilder html = new StringBuilder(1200);
        html.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>CIVENTRA invoice ").append(escape(invoice.getInvoiceNumber()))
                .append("</title><style>")
                .append("body{font:16px Arial,sans-serif;max-width:850px;margin:40px auto;padding:0 24px;color:#21152b}")
                .append("header{border-bottom:3px solid #7c3aed;padding-bottom:18px;margin-bottom:24px}")
                .append("h1{color:#581c87;margin:0}table{width:100%;border-collapse:collapse;margin:28px 0}")
                .append("th,td{padding:12px;border-bottom:1px solid #ddd;text-align:left}th:last-child,td.num{text-align:right}")
                .append(".total{font-size:1.25rem;font-weight:bold;text-align:right}.muted{color:#665b6c}")
                .append("button{padding:10px 16px;background:#7c3aed;color:#fff;border:0;border-radius:8px;cursor:pointer}")
                .append("@media print{body{margin:0;max-width:none}button{display:none}}")
                .append("</style></head><body><header><h1>CIVENTRA</h1><p class=\"muted\">Personal services invoice</p></header>")
                .append("<p><strong>Invoice number:</strong> ").append(escape(invoice.getInvoiceNumber())).append("</p>")
                .append("<p><strong>Type:</strong> ").append(escape(invoice.getInvoiceType())).append("</p>")
                .append("<p><strong>Status:</strong> ").append(escape(invoice.getStatus())).append("</p>")
                .append("<p><strong>Citizen:</strong> ").append(escape(invoice.getCitizenName())).append("</p>")
                .append("<p><strong>Provider:</strong> ").append(escape(invoice.getProviderName())).append("</p>");
        if (invoice.getServiceRequestId() != null) {
            html.append("<p><strong>Service request:</strong> #").append(invoice.getServiceRequestId()).append("</p>");
        }
        html
                .append("<table><thead><tr><th>Description</th><th>Quantity</th><th>Unit price</th><th>Total</th></tr></thead><tbody>");
        if (items != null) {
            for (InvoiceLineItem item : items) {
                if (item == null) continue;
                html.append("<tr><td>").append(escape(item.getItemDescription())).append("</td><td class=\"num\">")
                        .append(item.getQuantity()).append("</td><td class=\"num\">").append(money(item.getUnitPrice()))
                        .append("</td><td class=\"num\">").append(money(item.getTotalPrice())).append("</td></tr>");
            }
        }
        html.append("</tbody></table><p>Subtotal: ").append(money(invoice.getSubtotal())).append("</p>")
                .append("<p>Tax: ").append(money(invoice.getTax())).append("</p>")
                .append("<p class=\"total\">Total: ").append(money(invoice.getTotalAmount())).append("</p>")
                .append("<p>Amount paid: ").append(money(invoice.getAmountPaid())).append("</p>")
                .append("<p>Balance due: ").append(money(invoice.getBalanceDue())).append("</p>")
                .append("<p class=\"muted\">Payment status reflects the saved invoice record. This document does not process payment.</p>")
                .append("<button type=\"button\" onclick=\"window.print()\">Print invoice</button>")
                .append("</body></html>");
        return html.toString();
    }

    private static String money(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return "₹" + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
