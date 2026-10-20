package com.civentra.ui.admin;

import com.civentra.model.ServiceProvider;
import com.civentra.service.ProviderService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

@Route(value = "admin/providers", layout = com.civentra.ui.admin.AdminLayout.class)
@PageTitle("Provider Management | CIVENTRA")
@RolesAllowed({"ADMIN", "ROLE_4"})
public class ProviderManagementView extends VerticalLayout {

    private final ProviderService providerService;
    private final Grid<ServiceProvider> grid = new Grid<>();

    public ProviderManagementView(ProviderService providerService) {
        this.providerService = providerService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Manage Service Providers");
        Paragraph subtitle = new Paragraph("Review credentials, verify contractors, or update active status.");

        configureGrid();
        add(header, subtitle, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(provider -> provider.getBusinessName() != null ? provider.getBusinessName() : "N/A")
                .setHeader("Business Name").setSortable(true).setAutoWidth(true);
        grid.addColumn(provider -> provider.getEmail() != null ? provider.getEmail() : "N/A")
                .setHeader("Email").setAutoWidth(true);
        grid.addColumn(provider -> provider.getPhone() != null ? provider.getPhone() : "N/A")
                .setHeader("Phone").setAutoWidth(true);
        grid.addColumn(provider -> provider.getStatus() != null ? provider.getStatus() : "ACTIVE")
                .setHeader("Status").setSortable(true).setAutoWidth(true);
        grid.addColumn(provider -> provider.getVerificationStatus() != null ? provider.getVerificationStatus() : "PENDING")
                .setHeader("Verification").setSortable(true).setAutoWidth(true);

        grid.addComponentColumn(provider -> {
            HorizontalLayout actions = new HorizontalLayout();

            boolean isVerified = "VERIFIED".equalsIgnoreCase(provider.getVerificationStatus());
            Button verifyBtn = new Button(isVerified ? "Revoke Verify" : "Verify", e -> toggleVerification(provider));
            verifyBtn.addThemeVariants(isVerified ? ButtonVariant.LUMO_TERTIARY : ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

            boolean isSuspended = "SUSPENDED".equalsIgnoreCase(provider.getStatus());
            Button suspendBtn = new Button(isSuspended ? "Reactivate" : "Suspend", e -> toggleStatus(provider));
            suspendBtn.addThemeVariants(isSuspended ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            actions.add(verifyBtn, suspendBtn);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
    }

    private void toggleVerification(ServiceProvider provider) {
        try {
            String current = provider.getVerificationStatus();
            String next = "VERIFIED".equalsIgnoreCase(current) ? "PENDING" : "VERIFIED";

            boolean ok = providerService.updateVerificationStatus(provider.getProviderId(), provider.getUserId(), next);
            if (ok) {
                Notification.show("Verification updated to " + next, 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            } else {
                Notification.show("Failed to update verification status.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void toggleStatus(ServiceProvider provider) {
        try {
            String current = provider.getStatus();
            String next = "SUSPENDED".equalsIgnoreCase(current) ? "ACTIVE" : "SUSPENDED";

            boolean ok = providerService.updateStatus(provider.getProviderId(), provider.getUserId(), next);
            if (ok) {
                Notification.show("Account status updated to " + next, 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            } else {
                Notification.show("Failed to update status.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void refreshGrid() {
        try {
            List<ServiceProvider> providers = providerService.getAllProviders();
            grid.setItems(providers != null ? providers : List.of());
        } catch (Exception e) {
            grid.setItems(List.of());
        }
    }
}