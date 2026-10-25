package com.civentra.security;

import com.civentra.model.User;
import com.civentra.ui.LoginView;
import com.civentra.ui.RegisterView;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
public class SecurityServiceInitListener implements VaadinServiceInitListener {

    private final AuthenticatedUser authenticatedUser;

    @Autowired
    public SecurityServiceInitListener(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent ->
                uiEvent.getUI().addBeforeEnterListener(this::beforeEnter)
        );
    }

    private void beforeEnter(BeforeEnterEvent event) {
        Class<?> target = event.getNavigationTarget();
        boolean isAnonymous = target.isAnnotationPresent(AnonymousAllowed.class)
                || LoginView.class.equals(target)
                || RegisterView.class.equals(target);

        User currentUser = authenticatedUser != null ? authenticatedUser.getUser() : null;
        boolean loggedIn = currentUser != null && currentUser.isActive();

        if (isAnonymous) {
            // If already authenticated and trying to access login/register, route to user's dashboard
            if (loggedIn && (LoginView.class.equals(target) || RegisterView.class.equals(target))) {
                String dashboard = LoginView.getDashboardRoute(currentUser.getRoleName());
                if ("login".equals(dashboard)) {
                    authenticatedUser.logout();
                } else {
                    event.forwardTo(dashboard);
                }
            }
            return;
        }

        // Protected view: require authentication
        if (!loggedIn) {
            event.forwardTo(LoginView.class);
            return;
        }

        // Authenticated: check role-based authorization
        String userRole = currentUser.getRoleName() != null ? currentUser.getRoleName().trim().toUpperCase(Locale.ROOT) : "UNKNOWN";

        if (!Arrays.asList("CITIZEN", "ADMIN", "OFFICER", "PROVIDER").contains(userRole)) {
            event.forwardTo(LoginView.class);
            return;
        }

        if (!isAccessAllowed(target, userRole)) {
            String home = LoginView.getDashboardRoute(userRole);
            event.forwardTo(home);
            Notification notification = Notification.show("Access denied: You do not have permission to view that page.", 4000, Notification.Position.BOTTOM_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean isAccessAllowed(Class<?> target, String userRole) {
        // 1. Check @RolesAllowed annotation if present
        if (target.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAllowed = target.getAnnotation(RolesAllowed.class);
            boolean match = Arrays.stream(rolesAllowed.value()).anyMatch(allowed -> roleMatches(allowed, userRole));
            if (!match) {
                return false;
            }
        }

        // 2. Package-based defense-in-depth enforcement
        String packageName = target.getPackageName();
        if (packageName.startsWith("com.civentra.ui.admin")) {
            return "ADMIN".equals(userRole);
        } else if (packageName.startsWith("com.civentra.ui.officer")) {
            return "OFFICER".equals(userRole) || "ADMIN".equals(userRole);
        } else if (packageName.startsWith("com.civentra.ui.provider")) {
            return "PROVIDER".equals(userRole) || "ADMIN".equals(userRole);
        } else if (packageName.startsWith("com.civentra.ui.citizen")) {
            return "CITIZEN".equals(userRole) || "ADMIN".equals(userRole);
        }

        return true;
    }

    private boolean roleMatches(String allowedRole, String userRole) {
        if (allowedRole == null || userRole == null) return false;
        String a = allowedRole.trim().toUpperCase(Locale.ROOT);
        String u = userRole.trim().toUpperCase(Locale.ROOT);
        if (a.equals(u)) return true;

        // Support database role aliases
        if (("ROLE_4".equals(a) || "ADMINISTRATOR".equals(a)) && "ADMIN".equals(u)) return true;
        if (("ROLE_2".equals(a) || "MUNICIPAL_OFFICER".equals(a) || "MUNICIPAL OFFICER".equals(a)) && "OFFICER".equals(u)) return true;
        if (("ROLE_3".equals(a) || "SERVICE_PROVIDER".equals(a) || "SERVICE PROVIDER".equals(a)) && "PROVIDER".equals(u)) return true;
        if ("ROLE_1".equals(a) && "CITIZEN".equals(u)) return true;

        return false;
    }
}
