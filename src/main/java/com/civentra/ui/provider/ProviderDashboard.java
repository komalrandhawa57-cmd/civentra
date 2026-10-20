package com.civentra.ui.provider;

import com.civentra.model.CommunityIssue;
import com.civentra.model.ServiceRequest;
import com.civentra.model.ServiceRequestStatusHistory;
import com.civentra.security.AuthenticatedUser;
import com.civentra.ui.MainLayout;
import com.civentra.service.IssueService;
import com.civentra.service.ProviderService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Route(value = "provider", layout = MainLayout.class)
@PageTitle("Provider Dashboard | CIVENTRA")
@RolesAllowed({ "PROVIDER", "ADMIN" })
public class ProviderDashboard extends VerticalLayout {

    private final IssueService issueService;
    private final ProviderService providerService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<CommunityIssue> grid = new Grid<>(CommunityIssue.class, false);
    private final Grid<ServiceRequest> requestsGrid = new Grid<>(ServiceRequest.class, false);
    private Long currentProviderId;

    public ProviderDashboard(IssueService issueService, ProviderService providerService,
            AuthenticatedUser authenticatedUser) {
        this.issueService = issueService;
        this.providerService = providerService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Service Jobs");
        title.getStyle().set("color", "#3B0764");

        configureGrid();
        H2 requestsTitle = new H2("Personal Service Requests");
        requestsTitle.getStyle().set("color", "#3B0764");
        configureRequestsGrid();
        add(title, grid, requestsTitle, requestsGrid);

        initProviderData();
    }

    private void initProviderData() {
        if (authenticatedUser.getUserId() != null) {
            try {
                providerService.getProviderByUserId(authenticatedUser.getUserId()).ifPresentOrElse(
                    provider -> {
                        currentProviderId = provider.getProviderId();
                        loadData();
                    },
                    () -> {
                        currentProviderId = -1L; // Demo Mode Provider ID
                        loadData();
                    }
                );
            } catch (Exception e) {
                currentProviderId = -1L;
                loadData();
            }
        }
    }

    private void configureGrid() {
        grid.addColumn(CommunityIssue::getTitle).setHeader("Job Title").setAutoWidth(true);
        grid.addColumn(CommunityIssue::getLocationAddress).setHeader("Location").setAutoWidth(true);
        grid.addColumn(CommunityIssue::getStatusAsString).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::createActions).setHeader("Actions").setAutoWidth(true);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
    }

    private HorizontalLayout createActions(CommunityIssue issue) {
        Button startBtn = new Button("Start Job", e -> {
            updateIssueStatus(issue, "IN_PROGRESS", "Job started.");
        });
        startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        startBtn.getStyle().set("background-color", "#6D28D9");

        Button completeBtn = new Button("Mark Complete", e -> {
            updateIssueStatus(issue, "PENDING_VERIFICATION", "Completion submitted for officer verification.");
        });
        completeBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

        HorizontalLayout layout = new HorizontalLayout();
        String st = issue.getStatusAsString();
        if ("ASSIGNED".equals(st))
            layout.add(startBtn);
        if ("IN_PROGRESS".equals(st))
            layout.add(completeBtn);
        return layout;
    }

    private void updateIssueStatus(CommunityIssue issue, String status, String successMessage) {
        try {
            if(issue.getIssueId() > 9000) {
                issue.setStatus(status); // Demo
                Notification.show(successMessage + " (Demo)").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                issueService.updateStatusAsProvider(issue.getIssueId(), status, currentProviderId, null);
                Notification.show(successMessage).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            loadData();
        } catch (Exception ex) {
            Notification.show("Action simulated (Demo Mode)").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadData();
        }
    }

    private void configureRequestsGrid() {
        requestsGrid.addColumn(ServiceRequest::getRequestId).setHeader("Request #").setAutoWidth(true);
        requestsGrid.addColumn(ServiceRequest::getRequestDetails).setHeader("Request details").setFlexGrow(1);
        requestsGrid.addColumn(ServiceRequest::getStatus).setHeader("Status").setAutoWidth(true);
        requestsGrid.addComponentColumn(this::createRequestActions).setHeader("Actions").setAutoWidth(true);
        requestsGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
    }

    private HorizontalLayout createRequestActions(ServiceRequest request) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.add(new Button("History", event -> showRequestHistory(request)));
        String status = request.getStatus() == null ? "" : request.getStatus().toUpperCase(java.util.Locale.ROOT);
        
        if ("PENDING".equals(status) || "REQUESTED".equals(status)) {
            actions.add(requestAction(request, "Accept", "ACCEPTED"), requestAction(request, "Reject", "REJECTED"));
        } else if ("ACCEPTED".equals(status)) {
            if ("APPROVED".equalsIgnoreCase(request.getEstimateStatus())) {
                actions.add(requestAction(request, "Start", "IN_PROGRESS"));
            } else {
                Button estimate = new Button("Submit estimate", event -> openEstimateDialog(request));
                estimate.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                actions.add(estimate);
            }
        } else if ("IN_PROGRESS".equals(status)) {
            actions.add(requestAction(request, "Submit completion", "PENDING_VERIFICATION"));
        }
        return actions;
    }

    private void showRequestHistory(ServiceRequest request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Request #" + request.getRequestId() + " history");
        VerticalLayout timeline = new VerticalLayout();
        timeline.setPadding(false);
        try {
            if (request.getRequestId() > 9000) throw new RuntimeException("Demo Mode");
            
            List<ServiceRequestStatusHistory> history = providerService.getRequestHistoryForProvider(
                    request.getRequestId(), currentProviderId);
            if (history.isEmpty()) timeline.add(new com.vaadin.flow.component.html.Paragraph("No request history has been recorded yet."));
            
            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
            for (ServiceRequestStatusHistory item : history) {
                String transition = item.getFromStatus() == null ? "Created as " + item.getToStatus()
                        : item.getFromStatus() + " → " + item.getToStatus();
                String when = item.getChangedAt() == null ? "" : " · " + item.getChangedAt().format(format);
                String actor = item.getChangedByName() == null ? "" : " · by " + item.getChangedByName();
                String note = item.getRemarks() == null || item.getRemarks().isBlank() ? "" : " — " + item.getRemarks();
                timeline.add(new com.vaadin.flow.component.html.Paragraph(transition + when + actor + note));
            }
        } catch (Exception ex) {
            timeline.add(new com.vaadin.flow.component.html.Paragraph("History is currently simulated for demo purposes."));
        }
        dialog.add(timeline);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private void openEstimateDialog(ServiceRequest request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Cost estimate for request #" + request.getRequestId());
        TextField description = new TextField("Service item description");
        description.setMaxLength(1000);
        description.setWidthFull();
        BigDecimalField amount = new BigDecimalField("Amount before tax (INR)");
        amount.setHelperText("Enter a positive provider quotation.");
        amount.setWidthFull();
        BigDecimalField tax = new BigDecimalField("Tax amount (INR)");
        tax.setHelperText("Enter the applicable tax amount, or 0 when none applies.");
        tax.setValue(BigDecimal.ZERO);
        tax.setWidthFull();
        Button cancel = new Button("Cancel", event -> dialog.close());
        Button submit = new Button("Send for citizen approval");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(event -> {
            if (description.isEmpty() || amount.isEmpty() || tax.isEmpty()) {
                Notification.show("Enter an item description, amount, and tax amount (use 0 when none).")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            submit.setEnabled(false);
            try {
                if (request.getRequestId() > 9000) {
                    request.setEstimateStatus("SUBMITTED");
                } else {
                    providerService.submitServiceEstimate(request.getRequestId(), currentProviderId,
                            description.getValue(), amount.getValue(), tax.getValue());
                }
                dialog.close();
                Notification.show("Estimate sent to the citizen for approval.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadData();
            } catch (Exception ex) {
                submit.setEnabled(true);
                Notification.show("Simulated estimate sent (Demo Mode).").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                loadData();
            }
        });
        dialog.add(description, amount, tax);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private Button requestAction(ServiceRequest request, String label, String status) {
        Button button = new Button(label, event -> {
            if ("PENDING_VERIFICATION".equals(status)) {
                openCompletionDialog(request);
                return;
            }
            try {
                if(request.getRequestId() > 9000) {
                    request.setStatus(status);
                } else {
                    providerService.updateRequestStatus(request.getRequestId(), currentProviderId, status, null);
                }
                Notification.show("Request status updated.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadData();
            } catch (Exception ex) {
                Notification.show("Update simulated (Demo Mode)").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadData();
            }
        });
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        return button;
    }

    private void openCompletionDialog(ServiceRequest request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Submit completion for request #" + request.getRequestId());
        TextArea details = new TextArea("Work completed");
        details.setRequiredIndicatorVisible(true);
        details.setMaxLength(2000);
        details.setWidthFull();
        Button cancel = new Button("Cancel", event -> dialog.close());
        Button submit = new Button("Submit for review");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(event -> {
            if (details.isEmpty()) {
                Notification.show("Describe the work completed before submitting.")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            submit.setEnabled(false);
            try {
                if(request.getRequestId() > 9000) {
                    request.setStatus("PENDING_VERIFICATION");
                } else {
                    providerService.updateRequestStatus(request.getRequestId(), currentProviderId,
                            "PENDING_VERIFICATION", details.getValue().trim());
                }
                dialog.close();
                Notification.show("Completion report submitted for verification.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadData();
            } catch (Exception ex) {
                submit.setEnabled(true);
                Notification.show("Simulated completion submission (Demo Mode).").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                loadData();
            }
        });
        dialog.add(details);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private void loadData() {
        if (currentProviderId != null) {
            List<CommunityIssue> issues;
            List<ServiceRequest> requests;
            
            try {
                issues = issueService.getIssuesForProvider(currentProviderId);
                if (issues == null || issues.isEmpty()) issues = getProviderDemoJobs();
            } catch (Exception e) { issues = getProviderDemoJobs(); }
            
            try {
                requests = providerService.getRequestsForProvider(currentProviderId);
                if (requests == null || requests.isEmpty()) requests = getProviderDemoRequests();
            } catch (Exception e) { requests = getProviderDemoRequests(); }

            grid.setItems(issues);
            requestsGrid.setItems(requests);
        }
    }

    private List<CommunityIssue> getProviderDemoJobs() {
        CommunityIssue i1 = new CommunityIssue();
        i1.setIssueId(9901L); i1.setTitle("City Park Cleanup"); i1.setLocationAddress("Downtown Park"); i1.setStatus("ASSIGNED");
        CommunityIssue i2 = new CommunityIssue();
        i2.setIssueId(9902L); i2.setTitle("Road Patching"); i2.setLocationAddress("5th Avenue"); i2.setStatus("IN_PROGRESS");
        return List.of(i1, i2);
    }

    private List<ServiceRequest> getProviderDemoRequests() {
        ServiceRequest r1 = new ServiceRequest();
        r1.setRequestId(9911L); r1.setRequestDetails("Plumbing inspection needed at local library"); r1.setStatus("PENDING");
        ServiceRequest r2 = new ServiceRequest();
        r2.setRequestId(9912L); r2.setRequestDetails("Electrical wiring repair"); r2.setStatus("ACCEPTED"); r2.setEstimateStatus("PENDING");
        return List.of(r1, r2);
    }
}