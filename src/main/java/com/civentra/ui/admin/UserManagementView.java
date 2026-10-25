package com.civentra.ui.admin;

import com.civentra.model.User;
import com.civentra.service.UserService;
import com.civentra.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("Users — CIVENTRA")
@RolesAllowed({ "ADMIN", "OFFICER" })
public class UserManagementView extends VerticalLayout {
    private final UserService userService;
    private final List<User> users = new ArrayList<>();
    private final Div userList = new Div();
    private final TextField search = new TextField();
    private final ComboBox<String> roleFilter = new ComboBox<>();
    private final ComboBox<String> activeFilter = new ComboBox<>();

    public UserManagementView(UserService userService) {
        this.userService = userService;
        addClassNames("civ-page", "civ-role-page", "civ-users-page");
        setPadding(false);
        setSpacing(false);
        H1 title = new H1("User Management");
        title.addClassName("civ-title");
        Paragraph subtitle = new Paragraph("Search accounts and manage access across the platform.");
        subtitle.addClassName("civ-subtitle");
        Button addStaff = new Button("Add Officer / Provider", e -> openStaffDialog());
        addStaff.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        search.setPlaceholder("Search name, email, or ward...");
        search.setWidthFull();
        roleFilter.setItems("All Roles", "CITIZEN", "OFFICER", "PROVIDER", "ADMIN");
        roleFilter.setValue("All Roles");
        roleFilter.setPlaceholder("Role");
        activeFilter.setItems("All Accounts", "Active", "Inactive");
        activeFilter.setValue("All Accounts");
        HorizontalLayout filters = new HorizontalLayout(search, roleFilter, activeFilter);
        filters.addClassName("civ-user-filters");
        filters.setWidthFull();
        filters.setFlexGrow(1, search);
        userList.addClassName("civ-user-list");
        search.addValueChangeListener(e -> renderUsers());
        roleFilter.addValueChangeListener(e -> renderUsers());
        activeFilter.addValueChangeListener(e -> renderUsers());
        add(title, subtitle, addStaff, filters, userList);
        loadUsers();
    }

    private void openStaffDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create staff account");
        dialog.setWidth("min(540px, 96vw)");
        TextField name = new TextField("Full name");
        name.setWidthFull();
        EmailField email = new EmailField("Email address");
        email.setWidthFull();
        TextField phone = new TextField("Mobile number");
        phone.setMaxLength(10);
        phone.setPlaceholder("10-digit Indian mobile number");
        phone.setWidthFull();
        ComboBox<String> role = new ComboBox<>("Account role");
        role.setItems("OFFICER", "PROVIDER", "ADMIN");
        role.setValue("OFFICER");
        role.setWidthFull();
        PasswordField password = new PasswordField("Temporary password");
        password.setMinLength(8);
        password.setWidthFull();
        Paragraph note = new Paragraph(
                "Use a real staff member’s details. The temporary password is stored as a BCrypt hash; CIVENTRA cannot show it later.");
        Button cancel = new Button("Cancel", e -> dialog.close());
        Button create = new Button("Create account");
        create.addClickListener(e -> {
            create.setEnabled(false);
            try {
                userService.createStaffAccount(name.getValue(), email.getValue(), phone.getValue(), password.getValue(),
                        role.getValue());
                Notification.show(role.getValue() + " account created and activated.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                loadUsers();
            } catch (Exception ex) {
                create.setEnabled(true);
                String message = ex instanceof IllegalArgumentException && ex.getMessage() != null
                        ? ex.getMessage()
                        : "Could not create the staff account right now.";
                Notification.show(message)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(name, email, phone, role, password, note);
        dialog.getFooter().add(cancel, create);
        dialog.open();
    }

    private void loadUsers() {
        try {
            users.clear();
            List<User> loaded = userService.getAllUsers();
            if (loaded != null)
                users.addAll(loaded);
            renderUsers();
        } catch (Exception ex) {
            userList.removeAll();
            userList.add(new Paragraph("Users could not be loaded. Check the database connection."));
        }
    }

    private void renderUsers() {
        userList.removeAll();
        String query = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        String role = roleFilter.getValue();
        String active = activeFilter.getValue();
        List<User> filtered = users.stream().filter(user -> {
            String text = (safe(user.getFullName()) + " " + safe(user.getEmail()) + " " + safe(user.getWard()))
                    .toLowerCase(Locale.ROOT);
            return (query.isBlank() || text.contains(query))
                    && (role == null || "All Roles".equals(role) || role.equalsIgnoreCase(user.getRoleName()))
                    && (active == null || "All Accounts".equals(active)
                            || ("Active".equals(active) == user.isActive()));
        }).toList();
        if (filtered.isEmpty()) {
            userList.add(
                    new Paragraph(users.isEmpty() ? "No user accounts found." : "No accounts match these filters."));
            return;
        }
        filtered.forEach(user -> userList.add(userRow(user)));
    }

    private Component userRow(User user) {
        Div row = new Div();
        row.addClassName("civ-user-row");
        Div avatar = new Div();
        avatar.addClassName("civ-user-avatar");
        avatar.setText(user.getFullName() == null || user.getFullName().isBlank()
                ? "?"
                : user.getFullName().substring(0, 1).toUpperCase(Locale.ROOT));
        Div copy = new Div();
        H3 name = new H3(safe(user.getFullName()));
        Span email = new Span(safe(user.getEmail()));
        Span details = new Span(safe(user.getRoleName(), "USER") + " · " + safe(user.getWard(), "No ward"));
        copy.add(name, email, details);
        Span state = new Span(user.isActive() ? "ACTIVE" : "INACTIVE");
        state.addClassNames("civ-user-state", user.isActive() ? "is-active" : "is-inactive");
        Button toggle = new Button(user.isActive() ? "Deactivate" : "Activate");
        toggle.addThemeVariants(user.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_PRIMARY);
        User current = (User) VaadinSession.getCurrent().getAttribute("user");
        toggle.setEnabled(current == null || current.getUserId() != user.getUserId());
        toggle.addClickListener(e -> {
            try {
                if (userService.setUserActive(user.getUserId(), !user.isActive())) {
                    Notification.show(user.isActive() ? "Account deactivated." : "Account activated.")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadUsers();
                } else
                    Notification.show("Account status was not changed.");
            } catch (Exception ex) {
                Notification.show("Could not update account status. Check the database connection and try again.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        row.add(avatar, copy, state, toggle);
        return row;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
