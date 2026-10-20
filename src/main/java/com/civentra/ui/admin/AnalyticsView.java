
package com.civentra.ui.admin;

import com.civentra.dao.AnalyticsDAO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Route(value = "admin/analytics", layout = com.civentra.ui.admin.AdminLayout.class)
@PageTitle("Citywide Analytics | CIVENTRA")
@RolesAllowed({"ADMIN", "ROLE_4"})
public class AnalyticsView extends VerticalLayout {

    private final AnalyticsDAO analyticsDAO;

    private final DatePicker fromDatePicker = new DatePicker("From");
    private final DatePicker toDatePicker = new DatePicker("To");
    private final Paragraph summaryText = new Paragraph();

    private final H3 reportsInRangeValue = new H3("0");
    private final H3 awaitingResolutionValue = new H3("0");
    private final H3 resolvedReportsValue = new H3("0");
    private final H3 avgDaysValue = new H3("0.0 days");

    public AnalyticsView(AnalyticsDAO analyticsDAO) {
        this.analyticsDAO = analyticsDAO;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Citywide Analytics");
        Paragraph subtitle = new Paragraph("Explore reports and service activity using saved database records.");

        // Cover from 2024 to future so all historical issues render
        LocalDate now = LocalDate.now();
        fromDatePicker.setValue(LocalDate.of(2024, 1, 1));
        toDatePicker.setValue(now.plusYears(1));

        Button refreshBtn = new Button("Refresh analytics", e -> loadAnalytics());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout filterLayout = new HorizontalLayout(fromDatePicker, toDatePicker, refreshBtn);
        filterLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        VerticalLayout reportsCard = createMetricCard("Reports in range", reportsInRangeValue);
        VerticalLayout awaitingCard = createMetricCard("Awaiting resolution", awaitingResolutionValue);
        VerticalLayout resolvedCard = createMetricCard("Resolved reports", resolvedReportsValue);
        VerticalLayout avgDaysCard = createMetricCard("Avg. days to resolve", avgDaysValue);

        HorizontalLayout cardsLayout1 = new HorizontalLayout(reportsCard, awaitingCard);
        cardsLayout1.setWidthFull();
        HorizontalLayout cardsLayout2 = new HorizontalLayout(resolvedCard, avgDaysCard);
        cardsLayout2.setWidthFull();

        add(header, subtitle, filterLayout, summaryText, cardsLayout1, cardsLayout2);

        loadAnalytics();
    }

    private VerticalLayout createMetricCard(String title, H3 valueComponent) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle().set("background-color", "rgba(255, 255, 255, 0.05)")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("border", "1px solid rgba(255, 255, 255, 0.1)");

        Paragraph titleLabel = new Paragraph(title);
        titleLabel.getStyle().set("margin", "0").set("color", "rgba(255, 255, 255, 0.7)");
        valueComponent.getStyle().set("margin-top", "8px").set("margin-bottom", "0");

        card.add(titleLabel, valueComponent);
        return card;
    }

    private void loadAnalytics() {
        try {
            LocalDate from = fromDatePicker.getValue() != null ? fromDatePicker.getValue() : LocalDate.of(2024, 1, 1);
            LocalDate to = toDatePicker.getValue() != null ? toDatePicker.getValue() : LocalDate.now().plusYears(1);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
            long registeredUsers = analyticsDAO.getTotalRegisteredUsers();
            long totalIssuesInRange = analyticsDAO.getIssuesCountInRange(from, to);
            long awaiting = analyticsDAO.getAwaitingResolutionCountInRange(from, to);
            long resolved = analyticsDAO.getResolvedCountInRange(from, to);
            double avgDays = analyticsDAO.getAverageResolutionTimeDays(from, to);

            summaryText.setText(String.format("Showing %s through %s · %d registered users · %d reports in range",
                    from.format(dtf), to.format(dtf), registeredUsers, totalIssuesInRange));

            reportsInRangeValue.setText(String.valueOf(totalIssuesInRange));
            awaitingResolutionValue.setText(String.valueOf(awaiting));
            resolvedReportsValue.setText(String.valueOf(resolved));
            avgDaysValue.setText(String.format("%.1f days", avgDays));
        } catch (Exception e) {
            summaryText.setText("Error loading analytics: " + e.getMessage());
        }
    }
}