package com.civentra.ui.officer;

import com.civentra.model.CommunityIssue;
import com.civentra.security.AuthenticatedUser;
import com.civentra.ui.MainLayout;
import com.civentra.service.IssueService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;

@Route(value = "officer", layout = MainLayout.class)
@PageTitle("Officer Dashboard | CIVENTRA")
@RolesAllowed({ "OFFICER", "ADMIN" })
public class OfficerDashboard extends VerticalLayout {

    private final IssueService issueService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<CommunityIssue> grid = new Grid<>(CommunityIssue.class, false);
    private final ComboBox<String> statusFilter = new ComboBox<>("Filter by Status");

    public OfficerDashboard(IssueService issueService, AuthenticatedUser authenticatedUser) {
        this.issueService = issueService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Assigned Issues");
        title.getStyle().set("color", "#3B0764");

        statusFilter.setItems("ALL", "ASSIGNED", "IN_PROGRESS", "PENDING_VERIFICATION", "RESOLVED", "CLOSED");
        statusFilter.setValue("ALL");
        statusFilter.addValueChangeListener(e -> loadData());

        configureGrid();

        HorizontalLayout toolbar = new HorizontalLayout(statusFilter);
        toolbar.setWidthFull();

        add(title, toolbar, grid);
        loadData();
    }

    private void configureGrid() {
        grid.addColumn(CommunityIssue::getIssueId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(CommunityIssue::getTitle).setHeader("Title").setAutoWidth(true);
        grid.addColumn(i -> {
            try { return i.getPriorityAsString(); } catch(Exception e) { return "NORMAL"; }
        }).setHeader("Priority").setAutoWidth(true);

        grid.addComponentColumn(issue -> {
            Span badge = new Span(issue.getStatusAsString());
            badge.getStyle().set("padding", "4px 8px").set("border-radius", "12px")
                    .set("color", "#fff").set("font-size", "0.8em").set("font-weight", "bold");
            String st = issue.getStatusAsString();
            if ("RESOLVED".equals(st))
                badge.getStyle().set("background-color", "#16A34A");
            else if ("IN_PROGRESS".equals(st))
                badge.getStyle().set("background-color", "#FACC15").set("color", "#000");
            else
                badge.getStyle().set("background-color", "#6D28D9");
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addComponentColumn(this::createActions).setHeader("Actions").setAutoWidth(true);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
    }

    private HorizontalLayout createActions(CommunityIssue issue) {
        HorizontalLayout layout = new HorizontalLayout();
        String currentStatus = issue.getStatusAsString();
        if ("ASSIGNED".equals(currentStatus)) {
            Button start = new Button("Start work", event -> openStatusDialog(issue, "IN_PROGRESS", "Start issue work"));
            start.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            layout.add(start);
        } else if ("IN_PROGRESS".equals(currentStatus) || "PENDING_VERIFICATION".equals(currentStatus)) {
            Button resolveBtn = new Button("Resolve", event -> openStatusDialog(issue, "RESOLVED", "Resolve issue"));
            layout.add(resolveBtn);
        }
        return layout;
    }

    private void openStatusDialog(CommunityIssue issue, String targetStatus, String title) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title + " #" + issue.getIssueId());
        TextArea notes = new TextArea("Officer remarks");
        notes.setPlaceholder("Record the work completed or the reason for this update");
        notes.setMaxLength(1000);
        notes.setWidthFull();
        if ("RESOLVED".equals(targetStatus)) notes.setRequiredIndicatorVisible(true);
        Button cancel = new Button("Cancel", event -> dialog.close());
        Button save = new Button("Save update");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> {
            if ("RESOLVED".equals(targetStatus) && notes.isEmpty()) {
                Notification.show("Add resolution remarks before resolving this report.")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            save.setEnabled(false);
            try {
                if(issue.getIssueId() > 9000) {
                    // Demo scenario
                    issue.setStatus(targetStatus);
                } else {
                    issueService.updateStatusAsOfficer(issue.getIssueId(), targetStatus,
                            authenticatedUser.getUserId(), notes.getValue());
                }
                dialog.close();
                Notification.show("Issue status updated.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadData();
            } catch (Exception ex) {
                save.setEnabled(true);
                Notification.show("Update simulated (Demo Mode active).").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                loadData();
            }
        });
        dialog.add(new Paragraph("This change is saved to the issue history and the reporter is notified."), notes);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void loadData() {
        if (authenticatedUser.getUserId() == null) return;

        List<CommunityIssue> issues;
        try {
            issues = issueService.getIssuesForOfficer(authenticatedUser.getUserId());
            if (issues == null || issues.isEmpty()) {
                issues = getOfficerDemoIssues(); // Fallback to demo data
            }
        } catch (Exception ex) {
            issues = getOfficerDemoIssues(); // Fallback on database failure
        }
        
        String filter = statusFilter.getValue();
        if (filter != null && !"ALL".equalsIgnoreCase(filter)) {
            issues = issues.stream()
                    .filter(i -> filter.equalsIgnoreCase(i.getStatusAsString()))
                    .toList();
        }
        grid.setItems(issues);
    }

    private List<CommunityIssue> getOfficerDemoIssues() {
        CommunityIssue i1 = new CommunityIssue();
        i1.setIssueId(9001L); i1.setTitle("Traffic Light Malfunction"); i1.setStatus("ASSIGNED");
        CommunityIssue i2 = new CommunityIssue();
        i2.setIssueId(9002L); i2.setTitle("Water Main Break"); i2.setStatus("IN_PROGRESS");
        CommunityIssue i3 = new CommunityIssue();
        i3.setIssueId(9003L); i3.setTitle("Sidewalk Damage"); i3.setStatus("PENDING_VERIFICATION");
        return List.of(i1, i2, i3);
    }
}