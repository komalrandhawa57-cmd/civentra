package com.civentra.ui.citizen;

import com.civentra.security.AuthenticatedUser;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "citizen/my-bills", layout = CitizenLayout.class)
@PageTitle("CIVENTRA | My Civic Bills & Invoices")
@RolesAllowed({"CITIZEN", "ADMIN"})
public class MyBillsView extends VerticalLayout {

    public static class CivicBill {
        private String billId;
        private String serviceName;
        private String providerName;
        private String period;
        private double amount;
        private LocalDate dueDate;
        private String status;

        public CivicBill(String billId, String serviceName, String providerName, String period, double amount, LocalDate dueDate, String status) {
            this.billId = billId;
            this.serviceName = serviceName;
            this.providerName = providerName;
            this.period = period;
            this.amount = amount;
            this.dueDate = dueDate;
            this.status = status;
        }

        public String getBillId() { return billId; }
        public String getServiceName() { return serviceName; }
        public String getProviderName() { return providerName; }
        public String getPeriod() { return period; }
        public double getAmount() { return amount; }
        public LocalDate getDueDate() { return dueDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    private final AuthenticatedUser authenticatedUser;
    private final List<CivicBill> billsList = new ArrayList<>();
    private final Grid<CivicBill> billGrid = new Grid<>(CivicBill.class, false);

    public MyBillsView(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        setWidthFull();
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");
        getStyle().set("padding", "20px");
        setSpacing(true);

        initDemoData();

        add(buildHeader());
        add(buildSummaryMetrics());
        add(buildBillsGridPanel());
    }

    private void initDemoData() {
        billsList.add(new CivicBill("INV-2026-081", "Civic Plumbing & Drain Cleaning", "Apex Municipal Repairs Ltd.", "October 2026", 1500.00, LocalDate.now().plusDays(5), "PENDING"));
        billsList.add(new CivicBill("INV-2026-072", "Waste Management & Sanitation", "Urban Sanitation Dept.", "October 2026", 500.00, LocalDate.now().plusDays(10), "PENDING"));
        billsList.add(new CivicBill("INV-2026-045", "Municipal Water Meter Charges", "City Water Authority", "Q3 2026", 1200.00, LocalDate.now().plusDays(25), "PENDING"));
        billsList.add(new CivicBill("INV-2026-012", "Streetlight Repair Assessment", "ElectroCivic Services", "September 2026", 850.00, LocalDate.now().minusDays(20), "PAID"));
    }

    private Component buildHeader() {
        Div header = new Div();
        header.getStyle().set("margin-bottom", "10px");

        H1 title = new H1("Civic Utility Bills & Invoices");
        title.getStyle().set("color", "#FFD166").set("margin", "0").set("font-size", "1.8rem");

        Paragraph sub = new Paragraph("Preview, pay, and print official invoices for civic services and municipal charges.");
        sub.getStyle().set("color", "rgba(255,255,255,0.8)").set("margin", "4px 0 0 0");

        header.add(title, sub);
        return header;
    }

    private Component buildSummaryMetrics() {
        Div grid = new Div();
        grid.getStyle()
                .set("width", "100%")
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(220px, 1fr))")
                .set("gap", "16px")
                .set("box-sizing", "border-box");

        double totalDue = 0.0;
        long pendingCount = 0;
        long paidCount = 0;

        for (CivicBill b : billsList) {
            if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                totalDue += b.getAmount();
                pendingCount++;
            } else if ("PAID".equalsIgnoreCase(b.getStatus())) {
                paidCount++;
            }
        }

        grid.add(summaryCard("Total Outstanding", "₹" + String.format("%.2f", totalDue), VaadinIcon.WALLET, "#EF4444"));
        grid.add(summaryCard("Unpaid Invoices", pendingCount + " Pending", VaadinIcon.CLOCK, "#F59E0B"));
        grid.add(summaryCard("Paid Receipts", paidCount + " Completed", VaadinIcon.CHECK_CIRCLE, "#10B981"));
        grid.add(summaryCard("Meter Status", "Active & Verified", VaadinIcon.SHIELD, "#8B5CF6"));

        return grid;
    }

    private Div summaryCard(String label, String val, VaadinIcon icon, String accent) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#1E1B4B")
                .set("border", "1px solid rgba(255,255,255,0.12)")
                .set("border-left", "5px solid " + accent)
                .set("border-radius", "12px")
                .set("padding", "16px 20px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "14px");

        Div iconBox = new Div(icon.create());
        iconBox.getStyle().set("color", accent).set("font-size", "24px");

        Div copy = new Div();
        Span l = new Span(label);
        l.getStyle().set("display", "block").set("color", "rgba(255,255,255,0.7)").set("font-size", "0.85rem");

        Span v = new Span(val);
        v.getStyle().set("display", "block").set("color", "#FFFFFF").set("font-size", "1.3rem").set("font-weight", "800");

        copy.add(l, v);
        card.add(iconBox, copy);
        return card;
    }

    private Component buildBillsGridPanel() {
        Div panel = new Div();
        panel.getStyle()
                .set("width", "100%")
                .set("background", "#1E1B4B")
                .set("border", "1px solid rgba(255, 255, 255, 0.12)")
                .set("border-radius", "14px")
                .set("padding", "20px")
                .set("box-sizing", "border-box");

        H2 title = new H2("Billing Statements");
        title.getStyle().set("color", "#FFD166").set("margin", "0 0 16px 0").set("font-size", "1.2rem");

        billGrid.getStyle().set("background", "transparent").set("color", "#FFFFFF");
        billGrid.setItems(billsList);

        billGrid.addColumn(CivicBill::getBillId).setHeader("Ref No.").setAutoWidth(true);
        billGrid.addColumn(CivicBill::getServiceName).setHeader("Service Description").setAutoWidth(true);
        billGrid.addColumn(CivicBill::getProviderName).setHeader("Provider / Agency").setAutoWidth(true);
        billGrid.addColumn(b -> "₹" + String.format("%.2f", b.getAmount())).setHeader("Amount").setAutoWidth(true);
        billGrid.addColumn(b -> b.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))).setHeader("Due Date").setAutoWidth(true);

        billGrid.addComponentColumn(b -> {
            Span chip = new Span(b.getStatus());
            chip.getStyle().set("padding", "4px 10px").set("border-radius", "20px").set("font-weight", "700").set("font-size", "0.75rem");
            if ("PAID".equalsIgnoreCase(b.getStatus())) {
                chip.getStyle().set("background", "rgba(16, 185, 129, 0.2)").set("color", "#34D399");
            } else {
                chip.getStyle().set("background", "rgba(239, 68, 68, 0.2)").set("color", "#F87171");
            }
            return chip;
        }).setHeader("Status").setAutoWidth(true);

        billGrid.addComponentColumn(b -> {
            Div actions = new Div();
            actions.getStyle().set("display", "flex").set("gap", "8px");

            Button preview = new Button("Preview Invoice", VaadinIcon.EYE.create(), e -> showInvoicePreviewModal(b));
            preview.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            preview.getStyle().set("color", "#818CF8");

            if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                Button pay = new Button("Pay", VaadinIcon.CREDIT_CARD.create(), e -> openPaymentModal(b));
                pay.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                pay.getStyle().set("background", "#F59E0B").set("color", "#0F172A").set("font-weight", "700");
                actions.add(preview, pay);
            } else {
                actions.add(preview);
            }

            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        panel.add(title, billGrid);
        return panel;
    }

    private void showInvoicePreviewModal(CivicBill bill) {
        Dialog dialog = new Dialog();
        dialog.setWidth("650px");
        dialog.setHeaderTitle("Invoice Preview - " + bill.getBillId());

        String citizenName = authenticatedUser.getFullName();
        if (citizenName == null || citizenName.isBlank()) {
            citizenName = "Gopika Randhawa";
        }

        double paidAmount = "PAID".equalsIgnoreCase(bill.getStatus()) ? bill.getAmount() : 0.0;
        double balanceDue = bill.getAmount() - paidAmount;

        VerticalLayout invoiceBody = new VerticalLayout();
        invoiceBody.getStyle()
                .set("background", "#FFFFFF")
                .set("color", "#1E293B")
                .set("padding", "24px")
                .set("border-radius", "8px")
                .set("font-family", "Arial, sans-serif");

        H2 header = new H2("CIVENTRA OFFICIAL INVOICE");
        header.getStyle().set("margin", "0 0 16px 0").set("color", "#312E81").set("font-size", "1.4rem");

        Div meta = new Div();
        meta.getStyle().set("line-height", "1.8").set("margin-bottom", "16px");
        meta.add(new Paragraph("Type: SERVICE"));
        meta.add(new Paragraph("Status: " + bill.getStatus()));
        meta.add(new Paragraph("Citizen: " + citizenName));
        meta.add(new Paragraph("Provider: " + bill.getProviderName()));
        meta.getChildren().forEach(c -> ((Paragraph) c).getStyle().set("margin", "2px 0").set("font-weight", "600"));

        // Table with Line Items
        Div table = new Div();
        table.getStyle().set("width", "100%").set("margin", "16px 0").set("border-bottom", "2px solid #E2E8F0");

        Div tHead = new Div(new Span("Description"), new Span("Quantity"), new Span("Unit price"), new Span("Total"));
        tHead.getStyle().set("display", "grid").set("grid-template-columns", "2fr 1fr 1fr 1fr")
                .set("font-weight", "700").set("padding", "8px 0").set("border-bottom", "2px solid #CBD5E1");

        Div tRow = new Div(new Span(bill.getServiceName()), new Span("1"), new Span("₹" + String.format("%.2f", bill.getAmount())), new Span("₹" + String.format("%.2f", bill.getAmount())));
        tRow.getStyle().set("display", "grid").set("grid-template-columns", "2fr 1fr 1fr 1fr").set("padding", "10px 0");

        table.add(tHead, tRow);

        Div summary = new Div();
        summary.getStyle().set("text-align", "right").set("line-height", "1.8").set("margin-top", "12px");
        summary.add(new Paragraph("Subtotal: ₹" + String.format("%.2f", bill.getAmount())));
        summary.add(new Paragraph("Tax: ₹0.00"));

        H3 total = new H3("Total: ₹" + String.format("%.2f", bill.getAmount()));
        total.getStyle().set("color", "#1E1B4B").set("margin", "8px 0");

        summary.add(total);
        summary.add(new Paragraph("Amount paid: ₹" + String.format("%.2f", paidAmount)));
        summary.add(new Paragraph("Balance due: ₹" + String.format("%.2f", balanceDue)));
        summary.getChildren().forEach(c -> {
            if (c instanceof Paragraph) ((Paragraph) c).getStyle().set("margin", "2px 0");
        });

        Paragraph note = new Paragraph("Payment status reflects the saved invoice record. This document does not process payment.");
        note.getStyle().set("font-size", "0.8rem").set("color", "#64748B").set("margin-top", "20px");

        invoiceBody.add(header, meta, table, summary, note);
        dialog.add(invoiceBody);

        String finalCitizen = citizenName;
        Button printBtn = new Button("Print invoice", VaadinIcon.PRINT.create(), e -> printInvoiceHtml(bill, finalCitizen));
        printBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        printBtn.getStyle().set("background", "#7C3AED").set("color", "#FFFFFF");

        Button closeBtn = new Button("Close", e -> dialog.close());

        dialog.getFooter().add(closeBtn, printBtn);
        dialog.open();
    }

    private void printInvoiceHtml(CivicBill bill, String citizenName) {
        double paidAmount = "PAID".equalsIgnoreCase(bill.getStatus()) ? bill.getAmount() : 0.0;
        double balanceDue = bill.getAmount() - paidAmount;

        String html = "<html><head><title>Invoice - " + bill.getBillId() + "</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; padding: 30px; color: #1E293B; }" +
                "h2 { color: #312E81; }" +
                ".meta p { margin: 4px 0; font-weight: bold; }" +
                "table { width: 100%; border-collapse: collapse; margin: 20px 0; }" +
                "th, td { text-align: left; padding: 10px; border-bottom: 1px solid #E2E8F0; }" +
                "th { border-bottom: 2px solid #CBD5E1; }" +
                ".summary { text-align: right; margin-top: 20px; }" +
                ".note { font-size: 12px; color: #64748B; margin-top: 30px; }" +
                ".btn { background: #7C3AED; color: white; border: none; padding: 10px 20px; font-weight: bold; border-radius: 6px; cursor: pointer; }" +
                "@media print { .no-print { display: none; } }" +
                "</style></head><body>" +
                "<h2>CIVENTRA OFFICIAL INVOICE</h2>" +
                "<div class='meta'>" +
                "<p>Type: SERVICE</p>" +
                "<p>Status: " + bill.getStatus() + "</p>" +
                "<p>Citizen: " + citizenName + "</p>" +
                "<p>Provider: " + bill.getProviderName() + "</p>" +
                "</div>" +
                "<table>" +
                "<tr><th>Description</th><th>Quantity</th><th>Unit price</th><th>Total</th></tr>" +
                "<tr><td>" + bill.getServiceName() + "</td><td>1</td><td>₹" + String.format("%.2f", bill.getAmount()) + "</td><td>₹" + String.format("%.2f", bill.getAmount()) + "</td></tr>" +
                "</table>" +
                "<div class='summary'>" +
                "<p>Subtotal: ₹" + String.format("%.2f", bill.getAmount()) + "</p>" +
                "<p>Tax: ₹0.00</p>" +
                "<h3>Total: ₹" + String.format("%.2f", bill.getAmount()) + "</h3>" +
                "<p>Amount paid: ₹" + String.format("%.2f", paidAmount) + "</p>" +
                "<p>Balance due: ₹" + String.format("%.2f", balanceDue) + "</p>" +
                "</div>" +
                "<p class='note'>Payment status reflects the saved invoice record. This document does not process payment.</p>" +
                "<button class='btn no-print' onclick='window.print()'>Print invoice</button>" +
                "</body></html>";

        UI.getCurrent().getPage().executeJs(
                "var win = window.open('', '_blank');" +
                        "win.document.write($0);" +
                        "win.document.close();",
                html
        );
    }

    private void openPaymentModal(CivicBill bill) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pay Invoice - " + bill.getBillId());

        VerticalLayout layout = new VerticalLayout();
        layout.getStyle().set("color", "#FFFFFF");

        Paragraph details = new Paragraph("Service: " + bill.getServiceName() + "\nTotal Amount: ₹" + String.format("%.2f", bill.getAmount()));
        details.getStyle().set("white-space", "pre-line");

        ComboBox<String> method = new ComboBox<>("Payment Method");
        method.setItems("Credit / Debit Card", "UPI / Digital Wallet", "Net Banking");
        method.setValue("UPI / Digital Wallet");
        method.setWidthFull();

        layout.add(details, method);
        dialog.add(layout);

        Button cancel = new Button("Cancel", e -> dialog.close());
        Button payConfirm = new Button("Confirm Payment (₹" + String.format("%.2f", bill.getAmount()) + ")", e -> {
            bill.setStatus("PAID");
            billGrid.getDataProvider().refreshItem(bill);
            dialog.close();
            Notification n = Notification.show("Payment of ₹" + String.format("%.2f", bill.getAmount()) + " processed successfully!", 3000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            removeAll();
            add(buildHeader());
            add(buildSummaryMetrics());
            add(buildBillsGridPanel());
        });
        payConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        payConfirm.getStyle().set("background", "#10B981").set("color", "#FFFFFF");

        dialog.getFooter().add(cancel, payConfirm);
        dialog.open();
    }
}