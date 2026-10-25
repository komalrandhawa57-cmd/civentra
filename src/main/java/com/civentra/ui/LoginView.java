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

        emailField.setPlaceholder("name@example.com");
        emailField.setWidthFull();
        emailField.getStyle().set("--vaadin-input-field-background", "#f8fafc");

        passwordField.setPlaceholder("••••••••");
        passwordField.setWidthFull();
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

        card.add(icon, title, subtitle, emailField, passwordField, loginButton, footer);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticatedUser != null && authenticatedUser.isLoggedIn()) {
            String route = getDashboardRoute(authenticatedUser.getRoleName());
            if (!"login".equals(route)) {
                event.forwardTo(route);
            }
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
                    showError(ex.getMessage());
                    resetButton();
                    return;
                } catch (Exception ex) {
                    showError("Unable to sign in right now. Please try again.");
                    resetButton();
                    return;
                }
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

    private void navigateToDashboard(String role) {
        String route = getDashboardRoute(role);
        if ("login".equals(route)) {
            showError("Your account has an unsupported role. Please contact an administrator.");
            resetButton();
            return;
        }
        UI.getCurrent().navigate(route);
    }

    public static String getDashboardRoute(String role) {
        if (role == null) return "login";
        return switch (role.toUpperCase(java.util.Locale.ROOT).trim()) {
            case "OFFICER", "MUNICIPAL_OFFICER", "MUNICIPAL OFFICER" -> "officer";
            case "PROVIDER", "SERVICE_PROVIDER", "SERVICE PROVIDER" -> "provider";
            case "ROLE_2" -> "officer";
            case "ROLE_3" -> "provider";
            case "ADMIN", "ADMINISTRATOR", "ROLE_4" -> "admin";
            case "CITIZEN", "ROLE_1" -> "citizen";
            default -> "login";
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
