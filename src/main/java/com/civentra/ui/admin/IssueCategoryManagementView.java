package com.civentra.ui.admin;

import com.civentra.model.IssueCategory;
import com.civentra.service.IssueService;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/categories", layout = AdminLayout.class)
@PageTitle("Issue Categories | CIVENTRA")
@RolesAllowed("ADMIN")
public class IssueCategoryManagementView extends VerticalLayout {
    private final IssueService issueService;
    private final Grid<IssueCategory> grid = new Grid<>(IssueCategory.class, false);

    public IssueCategoryManagementView(IssueService issueService) {
        this.issueService = issueService;
        setSizeFull();
        setPadding(true);
        H2 title = new H2("Issue Categories");
        title.getStyle().set("color", "#3B0764");
        Button addButton = new Button("Add Category", event -> openCreateDialog());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.addColumn(IssueCategory::getCategoryId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(IssueCategory::getCategoryName).setHeader("Category Name").setAutoWidth(true);
        grid.addColumn(IssueCategory::getDescription).setHeader("Description").setFlexGrow(1);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        add(title, addButton, grid);
        refreshGrid();
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create issue category");
        TextField name = new TextField("Category name");
        name.setMaxLength(100);
        name.setWidthFull();
        TextArea description = new TextArea("Description");
        description.setMaxLength(255);
        description.setWidthFull();
        Button cancel = new Button("Cancel", event -> dialog.close());
        Button save = new Button("Save category");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> {
            if (name.isEmpty()) {
                Notification.show("Enter a category name.").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            save.setEnabled(false);
            try {
                long id = issueService.createIssueCategory(name.getValue().trim(), description.getValue());
                if (id <= 0) throw new IllegalStateException("The database did not return a category ID.");
                dialog.close();
                Notification.show("Category created.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            } catch (Exception ex) {
                save.setEnabled(true);
                Notification.show("The category could not be saved. Check for duplicate names and try again.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.add(name, description);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshGrid() {
        try {
            grid.setItems(issueService.getIssueCategories());
        } catch (Exception ex) {
            grid.setItems(java.util.List.of());
            add(new Paragraph("Issue categories could not be loaded. Check the database connection."));
        }
    }
}
