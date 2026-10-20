package com.civentra.ui.admin;

import com.civentra.model.ServiceRequest;
import com.civentra.model.ServiceRequestStatusHistory;
import com.civentra.security.AuthenticatedUser;
import com.civentra.service.ProviderService;
import com.civentra.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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
import java.time.format.DateTimeFormatter;

@Route(value = "admin/service-requests", layout = MainLayout.class)
@PageTitle("Service Completion Reviews | CIVENTRA")
@RolesAllowed("ADMIN")
public class ServiceRequestReviewView extends VerticalLayout {
    private final ProviderService providerService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<ServiceRequest> grid = new Grid<>(ServiceRequest.class, false);
    private final Paragraph emptyState = new Paragraph("No service completions are waiting for review.");

    public ServiceRequestReviewView(ProviderService providerService, AuthenticatedUser authenticatedUser) {
        this.providerService = providerService;
        this.authenticatedUser = authenticatedUser;
        setSizeFull();
        setPadding(true);

        H2 title = new H2("Service Completion Reviews");
        title.getStyle().set("color", "#3B0764");

        grid.addColumn(ServiceRequest::getRequestId).setHeader("Request #").setAutoWidth(true);
        grid.addColumn(req -> req.getRequestDetails() != null ? req.getRequestDetails() : (req.getTitle() != null ? req.getTitle() : "Service Request #" + req.getRequestId())).setHeader("Request").setFlexGrow(1);
        grid.addColumn(req -> req.getCompletionNotes() != null ? req.getCompletionNotes() : "N/A").setHeader("Provider completion report").setFlexGrow(2);
        grid.addColumn(ServiceRequest::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("Review").setAutoWidth(true);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);

        add(title, grid, emptyState);
        refresh();
    }

    private HorizontalLayout actions(ServiceRequest request) {
        Button history = new Button("History", event -> showHistory(request));
        history.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button approve = new Button("Approve completion", event -> review(request, true));
        approve.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

        Button returnWork = new Button("Return to provider", event -> review(request, false));
        returnWork.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        return new HorizontalLayout(history, approve, returnWork);
    }

    private void showHistory(ServiceRequest request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Request #" + request.getRequestId() + " history");
        VerticalLayout timeline = new VerticalLayout();
        timeline.setPadding(false);

        try {
            List<ServiceRequestStatusHistory> history = providerService.getRequestHistoryForAdmin(request.getRequestId());
            if (history == null || history.isEmpty()) {
                timeline.add(new Paragraph("No request history has been recorded yet."));
            } else {
                DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
                for (ServiceRequestStatusHistory item : history) {
                    String transition = item.getFromStatus() == null ? "Created as " + item.getToStatus()
                            : item.getFromStatus() + " → " + item.getToStatus();
                    String when = item.getChangedAt() == null ? "" : " · " + item.getChangedAt().format(format);
                    String actor = item.getChangedByName() == null ? "" : " · by " + item.getChangedByName();
                    String note = item.getRemarks() == null || item.getRemarks().isBlank() ? "" : " — " + item.getRemarks();
                    timeline.add(new Paragraph(transition + when + actor + note));
                }
            }
        } catch (Exception ex) {
            timeline.add(new Paragraph("Request history could not be loaded. Check the database connection and try again."));
        }

        dialog.add(timeline);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private void review(ServiceRequest request, boolean approve) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(approve ? "Approve completed work" : "Return work for revision");

        TextArea remarks = new TextArea(approve ? "Review remarks (optional)" : "Reason for return");
        remarks.setMaxLength(2000);
        remarks.setWidthFull();

        Button cancel = new Button("Cancel", event -> dialog.close());
        Button confirm = new Button(approve ? "Approve" : "Return to provider");
        confirm.addThemeVariants(approve ? ButtonVariant.LUMO_SUCCESS : ButtonVariant.LUMO_ERROR);

        confirm.addClickListener(event -> {
            if (!approve && remarks.isEmpty()) {
                Notification.show("Add a reason before returning work to the provider.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            confirm.setEnabled(false);
            try {
                providerService.verifyRequestCompletion(request.getRequestId(), authenticatedUser.getUserId(),
                        approve, remarks.getValue());
                dialog.close();
                Notification.show(approve ? "Completion approved." : "Work returned to the provider.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refresh();
            } catch (Exception ex) {
                confirm.setEnabled(true);
                Notification.show("This completion could not be reviewed. Refresh and try again.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(remarks);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void refresh() {
        try {
            List<ServiceRequest> requests = providerService.getRequestsPendingVerification();
            if (requests == null) requests = new ArrayList<>();

            grid.setItems(requests);
            emptyState.setText("No service completions are waiting for review.");
            emptyState.setVisible(requests.isEmpty());
        } catch (Exception ex) {
            grid.setItems(List.of());
            emptyState.setText("Service requests could not be loaded. Check the database connection.");
            emptyState.setVisible(true);
            Notification.show("Service requests could not be loaded. Check the database connection.")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}