package com.civentra.ui.admin;

import com.civentra.dao.AnalyticsDAO;
import com.civentra.service.UserService;
import com.civentra.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Map;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin — CIVENTRA")
public class AdminDashboard extends VerticalLayout {
    public AdminDashboard(UserService userService, AnalyticsDAO analyticsDAO) {
        addClassNames("civ-page", "civ-role-page");
        setPadding(false);
        setSpacing(false);
        H1 title = new H1("System Administration");
        title.addClassName("civ-title");
        Paragraph subtitle = new Paragraph("Manage civic services and monitor citywide activity.");
        subtitle.addClassName("civ-subtitle");
        add(title, subtitle);

        Object totalUsers = "Unavailable", citizens = "Unavailable", officers = "Unavailable";
        Object providers = "Unavailable", issues = "Unavailable", requests = "Unavailable";
        try {
            totalUsers = userService.getTotalUsers();
            citizens = userService.getCitizenCount();
            officers = userService.getOfficerCount();
            providers = userService.getProviderCount();
            Map<String, Integer> stats = analyticsDAO.getSummaryStats();
            issues = stats.getOrDefault("totalIssues", 0);
            requests = stats.getOrDefault("totalRequests", 0);
        } catch (Exception ex) {
            Notification.show("Some dashboard totals could not be loaded.");
        }

        Div metrics = new Div(
                metric("Active users", totalUsers, VaadinIcon.USERS),
                metric("Community issues", issues, VaadinIcon.CLIPBOARD_TEXT),
                metric("Service requests", requests, VaadinIcon.WRENCH),
                metric("Citizens · Officers · Providers", citizens + " · " + officers + " · " + providers, VaadinIcon.GROUP));
        metrics.addClassName("civ-role-metrics");
        Div links = new Div();
        links.addClassName("civ-role-actions");
        Button users = new Button("Manage Users", VaadinIcon.USERS.create(), e -> navigate("admin/users"));
        Button complaints = new Button("Assign & Track Complaints", VaadinIcon.CLIPBOARD.create(), e -> navigate("admin/assignments"));
        Button serviceReviews = new Button("Review Service Completions", VaadinIcon.HANDSHAKE.create(), e -> navigate("admin/service-requests"));
        Button providerManagement = new Button("Manage Service Providers", VaadinIcon.TOOLS.create(), e -> navigate("admin/providers"));
        Button analytics = new Button("View Analytics", VaadinIcon.CHART.create(), e -> navigate("admin/analytics"));
        users.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        complaints.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        serviceReviews.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        providerManagement.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        links.add(users, complaints, serviceReviews, providerManagement, analytics);
        add(metrics, links);
    }

    private Div metric(String label, Object value, VaadinIcon icon) {
        Div item = new Div(icon.create(), new Span(label), new H2(String.valueOf(value)));
        item.addClassName("civ-role-metric");
        return item;
    }

    private void navigate(String route) { getUI().ifPresent(ui -> ui.navigate(route)); }
}
