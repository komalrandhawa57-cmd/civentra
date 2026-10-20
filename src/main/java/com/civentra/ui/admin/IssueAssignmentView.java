package com.civentra.ui.admin;

import com.civentra.dao.IssueDAO;
import com.civentra.dao.ProviderDAO;
import com.civentra.model.CommunityIssue;
import com.civentra.model.ServiceProvider;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "admin/assignments", layout = com.civentra.ui.admin.AdminLayout.class)
@PageTitle("Issue Assignments | CIVENTRA")
@RolesAllowed({"ADMIN", "ROLE_4", "MUNICIPAL_OFFICER", "ROLE_2"})
public class IssueAssignmentView extends VerticalLayout {

    private final IssueDAO issueDAO;
    private final ProviderDAO providerDAO;
    private final Grid<CommunityIssue> grid = new Grid<>();

    public IssueAssignmentView(IssueDAO issueDAO, ProviderDAO providerDAO) {
        this.issueDAO = issueDAO;
        this.providerDAO = providerDAO;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Unassigned Issues");
        Paragraph subtitle = new Paragraph("Review open community complaints and assign them to verified service providers.");

        configureGrid();
        add(header, subtitle, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(issue -> issue.getIssueId() != null ? issue.getIssueId() : 0)
                .setHeader("ID").setAutoWidth(true);
        grid.addColumn(issue -> issue.getTitle() != null ? issue.getTitle() : "Untitled Issue")
                .setHeader("Title").setSortable(true).setAutoWidth(true);
        grid.addColumn(issue -> issue.getCategoryName() != null ? issue.getCategoryName() : "General")
                .setHeader("Category").setAutoWidth(true);
        grid.addColumn(issue -> issue.getPriority() != null ? issue.getPriority() : "MEDIUM")
                .setHeader("Priority").setAutoWidth(true);
        grid.addColumn(issue -> issue.getStatus() != null ? issue.getStatus() : "OPEN")
                .setHeader("Status").setSortable(true).setAutoWidth(true);

        grid.addComponentColumn(issue -> {
            Button assignBtn = new Button("Assign", e -> openAssignDialog(issue));
            assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            return assignBtn;
        }).setHeader("Action").setAutoWidth(true);
    }

    private void openAssignDialog(CommunityIssue issue) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Assign Complaint #" + issue.getIssueId());

        List<ServiceProvider> providers = providerDAO.findAll();
        ComboBox<ServiceProvider> providerSelect = new ComboBox<>("Select Service Provider");
        providerSelect.setItems(providers);
        providerSelect.setItemLabelGenerator(p -> {
            String bName = p.getBusinessName() != null ? p.getBusinessName() : "Provider";
            String phone = p.getPhone() != null ? p.getPhone() : "N/A";
            return bName + " (" + phone + ")";
        });
        providerSelect.setRequired(true);

        Button confirmBtn = new Button("Confirm Assignment", e -> {
            ServiceProvider selected = providerSelect.getValue();
            if (selected == null) {
                Notification.show("Please select a service provider.", 3000, Notification.Position.TOP_CENTER);
                return;
            }

            try {
                long providerId = selected.getProviderId() != null ? selected.getProviderId() : 0L;
                String providerName = selected.getBusinessName() != null ? selected.getBusinessName() : "Service Provider";
                
                issueDAO.assignIssue(issue.getIssueId(), 1L, providerId, "Assigned via Admin Portal: " + providerName, 1L);

                Notification.show("Issue #" + issue.getIssueId() + " assigned to " + providerName, 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Failed to assign issue: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        VerticalLayout dialogLayout = new VerticalLayout(providerSelect);
        dialog.add(dialogLayout);
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void refreshGrid() {
        try {
            List<CommunityIssue> list = issueDAO.findUnassigned();
            if (list == null || list.isEmpty()) {
                list = issueDAO.findAll();
            }
            grid.setItems(list != null ? list : List.of());
        } catch (Exception e) {
            grid.setItems(List.of());
        }
    }
}