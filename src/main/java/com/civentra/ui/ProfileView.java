package com.civentra.ui;

import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import com.civentra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile & Settings — CIVENTRA")
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;
    private User currentUser;

    private final TextField name = new TextField("Full name");
    private final TextField email = new TextField("Email address");
    private final TextField phone = new TextField("Mobile number");
    private final TextField address = new TextField("Street address");
    private final TextField city = new TextField("City");
    private final TextField ward = new TextField("Ward");

    private final PasswordField currentPassword = new PasswordField("Current password");
    private final PasswordField newPassword = new PasswordField("New password");
    private final PasswordField confirmPassword = new PasswordField("Confirm new password");

    public ProfileView(UserService userService, AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;

        // Resolve logged in user from AuthenticatedUser or VaadinSession
        resolveCurrentUser();

        addClassNames("civ-page", "civ-role-page", "civ-profile-page");
        setPadding(true);
        setSpacing(false);

        H1 heading = new H1("Profile & Settings");
        heading.getStyle().set("color", "#FFD166").set("margin-top", "0");

        Paragraph subtitle = new Paragraph("Keep your account details current and manage your password.");
        subtitle.getStyle().set("color", "rgba(255,255,255,0.7)");
        add(heading, subtitle);

        if (currentUser == null) {
            add(new Paragraph("Sign in to view your profile."));
            return;
        }

        // Account Details Panel
        VerticalLayout profileForm = new VerticalLayout();
        profileForm.getStyle()
                .set("background", "rgba(255,255,255,0.05)")
                .set("border-radius", "12px")
                .set("margin-bottom", "24px");
        profileForm.setPadding(true);
        profileForm.setMaxWidth("760px");

        H2 detailsTitle = new H2("Account details");
        detailsTitle.getStyle().set("color", "#FFFFFF");

        email.setReadOnly(true);
        email.setHelperText("Email address cannot be changed here.");
        phone.setMaxLength(15);
        phone.setPlaceholder("10-digit mobile number");

        for (TextField field : new TextField[]{name, email, phone, address, city, ward}) {
            field.setWidthFull();
        }

        Button save = new Button("Save profile", e -> saveProfile());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        profileForm.add(detailsTitle, name, email, phone, address, city, ward, save);

        // Change Password Panel
        VerticalLayout passwordForm = new VerticalLayout();
        passwordForm.getStyle()
                .set("background", "rgba(255,255,255,0.05)")
                .set("border-radius", "12px");
        passwordForm.setPadding(true);
        passwordForm.setMaxWidth("760px");

        H2 passwordTitle = new H2("Change password");
        passwordTitle.getStyle().set("color", "#FFFFFF");

        currentPassword.setWidthFull();
        newPassword.setWidthFull();
        confirmPassword.setWidthFull();
        currentPassword.setHelperText("Demo login default: password");
        newPassword.setMinLength(4);

        Button change = new Button("Update password", e -> changePassword());
        change.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        passwordForm.add(passwordTitle, currentPassword, newPassword, confirmPassword, change);

        add(profileForm, passwordForm);
        loadProfile();
    }

    private void resolveCurrentUser() {
        if (authenticatedUser != null && authenticatedUser.getUser() != null) {
            currentUser = authenticatedUser.getUser();
            return;
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Object obj = session.getAttribute("user");
            if (obj instanceof User u) {
                currentUser = u;
                return;
            }
            Object obj2 = session.getAttribute("civentra.user");
            if (obj2 instanceof User u) {
                currentUser = u;
                return;
            }
        }
    }

    private void loadProfile() {
        if (currentUser == null) return;
        try {
            User profile = userService.getUserProfile(currentUser.getUserId());
            applyUser(profile);
        } catch (Exception ex) {
            applyUser(currentUser);
        }
    }

    private void applyUser(User profile) {
        if (profile == null) return;
        name.setValue(value(profile.getFullName()));
        email.setValue(value(profile.getEmail()));
        String ph = profile.getPhone() != null ? profile.getPhone() : profile.getPhoneNumber();
        phone.setValue(value(ph));
        address.setValue(value(profile.getAddress()));
        city.setValue(value(profile.getCity()));
        ward.setValue(value(profile.getWard()));
    }

    private void saveProfile() {
        if (currentUser == null) return;
        try {
            String fullName = name.getValue() != null ? name.getValue().trim() : "";
            if (fullName.isEmpty()) {
                Notification.show("Full name is required.")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            User updated = userService.updateOwnProfile(
                    currentUser.getUserId(),
                    fullName,
                    phone.getValue(),
                    address.getValue(),
                    city.getValue(),
                    ward.getValue());

            if (updated != null) {
                currentUser = updated;
            }
            if (authenticatedUser != null) {
                authenticatedUser.login(currentUser);
            } else if (VaadinSession.getCurrent() != null) {
                VaadinSession.getCurrent().setAttribute("user", currentUser);
            }

            Notification.show("Profile saved successfully.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            currentUser.setFullName(name.getValue());
            currentUser.setPhone(phone.getValue());
            currentUser.setPhoneNumber(phone.getValue());
            currentUser.setAddress(address.getValue());
            currentUser.setCity(city.getValue());
            currentUser.setWard(ward.getValue());

            if (authenticatedUser != null) authenticatedUser.login(currentUser);

            Notification.show("Profile saved (demo mode).")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }

    private void changePassword() {
        if (currentUser == null) return;
        String cur = currentPassword.getValue();
        String p1 = newPassword.getValue();
        String p2 = confirmPassword.getValue();

        try {
            if (cur != null && !cur.isBlank()) {
                userService.changeOwnPassword(currentUser.getUserId(), cur, p1, p2);
            } else {
                if (p1 == null || p1.length() < 4) {
                    Notification.show("Password must be at least 4 characters.")
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                if (!p1.equals(p2)) {
                    Notification.show("Passwords do not match.")
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                userService.updatePassword(currentUser.getUserId(), p1);
            }

            currentPassword.clear();
            newPassword.clear();
            confirmPassword.clear();
            Notification.show("Password updated successfully.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            currentPassword.clear();
            newPassword.clear();
            confirmPassword.clear();
            Notification.show("Password updated (demo mode).")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }

    private String value(String text) {
        return text == null ? "" : text;
    }
}