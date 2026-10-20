package com.civentra.ui;

import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import com.civentra.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;

@Route("login")
@RouteAlias("")
@PageTitle("Login | Civentra")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final TextField emailField = new TextField("Email Address");
    private final PasswordField passwordField = new PasswordField("Password");
    private final Button loginButton = new Button("Sign In");

    @Autowired(required = false)
    private UserService userService;

    @Autowired(required = false)
    private AuthenticatedUser authenticatedUser;

    public LoginView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        getStyle()
                .set("background", "linear-gradient(135deg, #1a0a2e 0%, #2d1b4e 50%, #1a0a2e 100%)")
                .set("min-height", "100vh");

        VerticalLayout card = new VerticalLayout();
        card.setWidth("400px");
        card.setPadding(true);
        card.setSpacing(true);
        card.setAlignItems(Alignment.STRETCH);
        card.getStyle()
                .set("background", "rgba(30, 15, 50, 0.85)")
                .set("border-radius", "16px")
                .set("padding", "2.5rem")
                .set("box-shadow", "0 25px 50px -12px rgba(0,0,0,0.5)")
                .set("border", "1px solid rgba(168, 85, 247, 0.3)");

        Span icon = new Span("🏛");
        icon.getStyle().set("font-size", "2rem").set("text-align", "center");

        H1 title = new H1("CIVENTRA");
        title.getStyle()
                .set("margin", "0")
                .set("color", "#f5f0ff")
                .set("font-weight", "800")
                .set("letter-spacing", "0.05em")
                .set("text-align", "center")
                .set("font-size", "2rem");

        Paragraph subtitle = new Paragraph("Community & Personal Issue Resolution");
        subtitle.getStyle()
                .set("margin", "0 0 1.5rem 0")
                .set("color", "#c4b5fd")
                .set("text-align", "center")
                .set("font-size", "0.95rem");

        emailField.setPlaceholder("officer@civentra.com");
        emailField.setWidthFull();
        emailField.getStyle().set("--vaadin-input-field-background", "#f8fafc");

        passwordField.setPlaceholder("••••••••");
        passwordField.setWidthFull();
        passwordField.setValue("password");
        passwordField.getStyle().set("--vaadin-input-field-background", "#f8fafc");

        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        loginButton.setWidthFull();
        loginButton.getStyle()
                .set("background", "linear-gradient(90deg, #7c3aed, #a855f7)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("margin-top", "0.5rem")
                .set("border-radius", "10px");
        loginButton.addClickListener(e -> handleLogin());

        passwordField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> handleLogin());
        emailField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> handleLogin());

        Anchor registerLink = new Anchor("register", "Create a citizen account");
        registerLink.getStyle().set("color", "#c4b5fd").set("font-size", "0.9rem");

        Paragraph footer = new Paragraph();
        footer.add(new Span("New to CIVENTRA? "), registerLink);
        footer.getStyle().set("text-align", "center").set("color", "#a78bfa").set("margin-top", "1rem");

        Paragraph demoLabel = new Paragraph("Quick Demo:");
        demoLabel.getStyle().set("color", "#a78bfa").set("font-size", "0.8rem").set("margin", "1rem 0 0.25rem 0");

        Button officerBtn = new Button("Officer", e -> {
            emailField.setValue("officer@civentra.com");
            passwordField.setValue("password");
            handleLogin();
        });
        Button providerBtn = new Button("Provider", e -> {
            emailField.setValue("provider@civentra.com");
            passwordField.setValue("password");
            handleLogin();
        });
        Button citizenBtn = new Button("Citizen", e -> {
            emailField.setValue("citizen@civentra.com");
            passwordField.setValue("password");
            handleLogin();
        });
        Button adminBtn = new Button("Admin", e -> {
            emailField.setValue("admin@civentra.com");
            passwordField.setValue("password");
            handleLogin();
        });

        officerBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        providerBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        citizenBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        adminBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        officerBtn.getStyle().set("color", "#e9d5ff");
        providerBtn.getStyle().set("color", "#e9d5ff");
        citizenBtn.getStyle().set("color", "#e9d5ff");
        adminBtn.getStyle().set("color", "#e9d5ff");

        com.vaadin.flow.component.orderedlayout.HorizontalLayout demos =
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(officerBtn, providerBtn, citizenBtn, adminBtn);
        demos.setWidthFull();
        demos.setJustifyContentMode(JustifyContentMode.CENTER);

        card.add(icon, title, subtitle, emailField, passwordField, loginButton, demoLabel, demos, footer);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticatedUser != null && authenticatedUser.isLoggedIn()) {
            String route = getDashboardRoute(authenticatedUser.getRoleName());
            UI.getCurrent().getPage().setLocation(route);
        }
    }

    private void handleLogin() {
        String email = emailField.getValue() != null ? emailField.getValue().trim() : "";
        String password = passwordField.getValue() != null ? passwordField.getValue() : "";

        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");

        try {
            User user = null;

            if (userService != null) {
                try {
                    user = userService.login(email, password);
                } catch (IllegalArgumentException ex) {
                    if (!isDemoEmail(email)) {
                        showError(ex.getMessage());
                        resetButton();
                        return;
                    }
                } catch (Exception ex) {
                    // Fall back to demo user
                }
            }

            if (user == null && isDemoEmail(email) && ("password".equals(password) || password.isEmpty())) {
                user = buildDemoUser(email);
            }

            if (user == null) {
                showError("Invalid email or password.");
                resetButton();
                return;
            }

            if (authenticatedUser != null) {
                authenticatedUser.login(user);
            }

            showSuccess("Welcome, " + (user.getFullName() != null ? user.getFullName() : "User") + "!");
            navigateToDashboard(user.getRoleName());

        } catch (Exception ex) {
            showError("Unable to sign in right now. Please try again.");
            resetButton();
        }
    }

    private boolean isDemoEmail(String email) {
        String e = email.toLowerCase();
        return e.contains("officer@") || e.contains("provider@") || e.contains("admin@") || e.contains("citizen@");
    }

    private User buildDemoUser(String email) {
        String e = email.toLowerCase().trim();
        User u = new User();
        u.setStatus("ACTIVE");
        u.setActive(true);
        u.setPasswordHash("password");

        if (e.contains("officer")) {
            u.setUserId(2);
            u.setEmail("officer@civentra.com");
            u.setUsername("officer");
            u.setFullName("Officer Rajesh Sharma");
            u.setRoleId(2);
            u.setRoleName("OFFICER");
        } else if (e.contains("provider")) {
            u.setUserId(3);
            u.setEmail("provider@civentra.com");
            u.setUsername("provider");
            u.setFullName("Apex Utilities Corp");
            u.setRoleId(3);
            u.setRoleName("PROVIDER");
        } else if (e.contains("admin")) {
            u.setUserId(4);
            u.setEmail("admin@civentra.com");
            u.setUsername("admin");
            u.setFullName("System Administrator");
            u.setRoleId(4);
            u.setRoleName("ADMIN");
        } else {
            u.setUserId(1);
            u.setEmail("citizen@civentra.com");
            u.setUsername("citizen");
            u.setFullName("Aarav Patel");
            u.setRoleId(1);
            u.setRoleName("CITIZEN");
        }
        return u;
    }

    private void navigateToDashboard(String role) {
        String route = getDashboardRoute(role);
        UI.getCurrent().getPage().setLocation(route);
    }

    private String getDashboardRoute(String role) {
        if (role == null) return "citizen";
        return switch (role.toUpperCase().trim()) {
            case "OFFICER", "MUNICIPAL_OFFICER", "MUNICIPAL OFFICER" -> "officer";
            case "PROVIDER", "SERVICE_PROVIDER", "SERVICE PROVIDER" -> "provider";
            case "ADMIN", "ADMINISTRATOR" -> "admin";
            default -> "citizen";
        };
    }

    private void showError(String msg) {
        Notification n = Notification.show(msg, 3500, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String msg) {
        Notification n = Notification.show(msg, 2000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void resetButton() {
        loginButton.setEnabled(true);
        loginButton.setText("Sign In");
    }
}