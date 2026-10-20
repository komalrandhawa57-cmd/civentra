package com.civentra.service;

import com.civentra.model.Invoice;
import com.civentra.model.InvoiceLineItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceDocumentRendererTest {
    @Test
    void rendersPrintableInvoiceWithDecimalSafeTotals() {
        Invoice invoice = invoice("CIV-25", "UNPAID");
        invoice.setSubtotal(new BigDecimal("12.345"));
        invoice.setTax(new BigDecimal("1.00"));
        invoice.setTotalAmount(new BigDecimal("13.345"));
        invoice.setBalanceDue(new BigDecimal("13.345"));

        String html = InvoiceDocumentRenderer.render(invoice,
                List.of(new InvoiceLineItem("Roadside service", 1, new BigDecimal("12.345"))));

        assertTrue(html.contains("Print invoice"));
        assertTrue(html.contains("window.print()"));
        assertTrue(html.contains("₹13.35"));
        assertTrue(html.contains("Roadside service"));
    }

    @Test
    void escapesDatabaseSuppliedTextBeforeEmbeddingInHtml() {
        String html = InvoiceDocumentRenderer.render(invoice("<img src=x onerror=alert(1)>", "<script>bad</script>"),
                List.of(new InvoiceLineItem("<script>item()</script>", 1, BigDecimal.ONE)));

        assertFalse(html.contains("<img src=x"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
        assertTrue(html.contains("&lt;script&gt;item()&lt;/script&gt;"));
    }

    private Invoice invoice(String number, String status) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(number);
        invoice.setStatus(status);
        return invoice;
    }
}
