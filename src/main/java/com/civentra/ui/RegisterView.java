package com.civentra.ui;

import com.civentra.service.UserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.civentra.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;

@Route("register")
@PageTitle("Create account — CIVENTRA")
@AnonymousAllowed
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegisterView.class);
    private Notification activeNotification;
    private boolean registrationInProgress;

    @Autowired(required = false)
    private AuthenticatedUser authenticatedUser;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticatedUser != null && authenticatedUser.isLoggedIn()) {
            String route = LoginView.getDashboardRoute(authenticatedUser.getRoleName());
            event.forwardTo(route);
        }
    }

    public RegisterView(UserService userService) {
        addClassName("civ-login-page");
        setSizeFull();
        setPadding(true);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Div card = new Div();
        card.addClassName("civ-login-card");

        H1 title = new H1("Create your account");
        title.addClassName("civ-login-title");
        Paragraph subtitle = new Paragraph("Join your community on CIVENTRA");
        subtitle.addClassName("civ-login-subtitle");

        TextField fullName = new TextField("Full name");
        fullName.setPlaceholder("Your name");
        fullName.setMaxLength(100);
        fullName.setWidthFull();

        EmailField email = new EmailField("Email address");
        email.setPlaceholder("you@example.com");
        email.setClearButtonVisible(true);
        email.setWidthFull();

        TextField phone = new TextField("Mobile number");
        phone.setPlaceholder("10-digit Indian mobile number");
        phone.setMaxLength(10);
        phone.setWidthFull();

        PasswordField password = new PasswordField("Password");
        password.setPlaceholder("At least 8 characters");
        password.setMinLength(8);
        password.setWidthFull();

        PasswordField confirm = new PasswordField("Confirm password");
        confirm.setPlaceholder("Enter the password again");
        confirm.setWidthFull();

        Button create = new Button("Create citizen account");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.setWidthFull();
        create.getStyle().set("margin-top", "12px");
        create.addClickShortcut(Key.ENTER);
        create.addClickListener(event -> {
            if (registrationInProgress) return;
            registrationInProgress = true;
            create.setEnabled(false);
            try {
                if (!password.getValue().equals(confirm.getValue())) {
                    throw new IllegalArgumentException("The passwords do not match.");
                }
                userService.registerCitizen(fullName.getValue(), email.getValue(), phone.getValue(), password.getValue());
                showFeedback("Account created. Sign in with your new email and password.", true);
                getUI().ifPresent(ui -> ui.navigate("login"));
            } catch (Exception ex) {
                String message = ex instanceof IllegalArgumentException || ex instanceof IllegalStateException
                        ? ex.getMessage() : null;
                if ("Email is already registered.".equals(message)) {
                    message = "This email already has an account. Sign in or use a different email address.";
                }
                if (message == null || message.isBlank()) {
                    message = "Account creation is temporarily unavailable. Check CIVENTRA's database connection and try again.";
                }
                if (!(ex instanceof IllegalArgumentException)) log.error("Registration failed due to a service error", ex);
                showFeedback(message, false);
            } finally {
                registrationInProgress = false;
                create.setEnabled(true);
            }
        });

        Anchor signIn = new Anchor("login", "Sign in");
        Div footer = new Div(new Span("Already have an account? "), signIn);
        footer.getStyle().set("text-align", "center").set("margin-top", "16px");
        card.add(title, subtitle, fullName, email, phone, password, confirm, create, footer);
        add(card);
    }

    private void showFeedback(String message, boolean success) {
        if (activeNotification != null) activeNotification.close();
        activeNotification = Notification.show(message, 4500, Notification.Position.TOP_CENTER);
        activeNotification.addThemeVariants(success ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
    }
}
