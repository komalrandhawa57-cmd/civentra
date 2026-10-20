package com.civentra.ui.citizen;

import com.civentra.model.ServiceCategory;
import com.civentra.model.ServiceProvider;
import com.civentra.model.ServiceRequest;
import com.civentra.security.AuthenticatedUser;
import com.civentra.service.ProviderService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "citizen/find-services", layout = CitizenLayout.class)
@PageTitle("Find Verified Local Services | CIVENTRA")
public class FindServiceView extends VerticalLayout {

    private final ProviderService providerService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<ServiceProvider> providerGrid = new Grid<>(ServiceProvider.class, false);
    private ComboBox<ServiceCategory> categoryFilter;
    private TextField searchField;

    @Autowired
    public FindServiceView(ProviderService providerService, AuthenticatedUser authenticatedUser) {
        this.providerService = providerService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(createHeaderSection());
        add(createFilterSection());
        add(createGridSection());

        refreshGrid();
    }

    private Component createHeaderSection() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H1 title = new H1("Verified Local Service Providers");
        title.getStyle().set("margin", "0").set("color", "#3B0764").set("font-size", "24px").set("font-weight", "700");

        Paragraph sub = new Paragraph(
                "Connect with municipal-approved electricians, plumbers, carpenters, and waste specialists.");
        sub.getStyle().set("margin", "0").set("color", "#6B7280").set("font-size", "14px");

        VerticalLayout v = new VerticalLayout(title, sub);
        v.setSpacing(false);
        v.setPadding(false);

        header.add(v);
        return header;
    }

    private Component createFilterSection() {
        HorizontalLayout filter = new HorizontalLayout();
        filter.setWidthFull();
        filter.setAlignItems(FlexComponent.Alignment.CENTER);

        searchField = new TextField();
        searchField.setPlaceholder("Search service, provider, area...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addValueChangeListener(e -> refreshGrid());

        categoryFilter = new ComboBox<>();
        categoryFilter.setPlaceholder("Select Category");
        List<ServiceCategory> categories = providerService.getActiveServiceCategories();
        categoryFilter.setItems(categories);
        categoryFilter.setItemLabelGenerator(ServiceCategory::getCategoryName);
        categoryFilter.addValueChangeListener(e -> refreshGrid());

        filter.add(searchField, categoryFilter);
        return filter;
    }

    private Component createGridSection() {
        providerGrid.setSizeFull();

        providerGrid.addColumn(ServiceProvider::getBusinessName).setHeader("Provider").setAutoWidth(true);
        providerGrid.addColumn(ServiceProvider::getCategoryName).setHeader("Category").setAutoWidth(true);
        providerGrid.addColumn(p -> p.getCity() != null ? p.getCity() : p.getAddress()).setHeader("Service Area").setAutoWidth(true);
        providerGrid.addColumn(ServiceProvider::getPhone).setHeader("Contact Phone").setAutoWidth(true);
        providerGrid.addColumn(p -> "Ask provider for an estimate").setHeader("Pricing").setAutoWidth(true);
        providerGrid.addColumn(p -> String.format("%.1f (%d jobs)", p.getAvgRating(), p.getTotalJobs()))
                .setHeader("Rating").setAutoWidth(true);

        providerGrid.addComponentColumn(this::createActionButtons).setHeader("Book / Request").setAutoWidth(true);

        return providerGrid;
    }

    private Component createActionButtons(ServiceProvider provider) {
        Button requestBtn = new Button("Book Service", VaadinIcon.CALENDAR.create());
        requestBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        requestBtn.getStyle().set("background-color", "#7C3AED");
        requestBtn.addClickListener(e -> openServiceRequestDialog(provider));

        return requestBtn;
    }

    private void openServiceRequestDialog(ServiceProvider provider) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Request Service: " + provider.getBusinessName());
        dialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);

        content.add(new Paragraph("Category: " + provider.getCategoryName() + " | Area: " + provider.getCity()));

        TextArea detailsFld = new TextArea("Issue / Service Requirement Details");
        detailsFld.setPlaceholder("Describe work required (e.g. Broken pipe in kitchen sink)...");
        detailsFld.setWidthFull();

        TextField addrFld = new TextField("Service Location Address");
        addrFld.setPlaceholder("Flat 302, Green Avenue, Ward 5");
        addrFld.setWidthFull();

        content.add(detailsFld, addrFld);

        Button submitBtn = new Button("Submit Service Request");
        submitBtn.addClickListener(e -> {
            ServiceRequest req = new ServiceRequest();
            if (authenticatedUser.getUserId() == null) {
                Notification.show("Your session has expired. Please sign in again.");
                return;
            }
            req.setCitizenId(authenticatedUser.getUserId());
            req.setProviderId(provider.getProviderId());
            req.setServiceCategoryId(provider.getCategoryId());
            req.setRequestDetails(detailsFld.getValue());
            req.setAddress(addrFld.getValue());
            req.setStatus("REQUESTED");
            submitBtn.setEnabled(false);

            try {
                long requestId = providerService.createServiceRequest(req);
                if (requestId <= 0) throw new IllegalStateException("The database did not return a request reference.");

                Notification n = Notification.show("Service request #" + requestId + " sent to " + provider.getBusinessName() + ".",
                        4000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                dialog.close();
            } catch (Exception ex) {
                submitBtn.setEnabled(true);
                Notification.show("The service request could not be saved. Please check the details and try again.",
                        4000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.getStyle().set("background-color", "#16A34A");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, submitBtn);
        dialog.add(content);
        dialog.open();
    }

    private void refreshGrid() {
        List<ServiceProvider> providers = providerService.getAllVerifiedProviders();

        ServiceCategory cat = categoryFilter != null ? categoryFilter.getValue() : null;
        String query = searchField != null ? searchField.getValue().toLowerCase() : "";

        if (cat != null) {
            providers = providers.stream()
                    .filter(p -> cat.getCategoryName().equalsIgnoreCase(p.getCategoryName()))
                    .toList();
        }

        if (!query.isBlank()) {
            providers = providers.stream()
                    .filter(p -> (p.getBusinessName() != null && p.getBusinessName().toLowerCase().contains(query)) ||
                            (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query)) ||
                            (p.getCity() != null && p.getCity().toLowerCase().contains(query)) ||
                            (p.getAddress() != null && p.getAddress().toLowerCase().contains(query)))
                    .toList();
        }

        providerGrid.setItems(providers);
    }
}
