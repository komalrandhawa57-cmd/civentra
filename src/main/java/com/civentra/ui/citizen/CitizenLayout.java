package com.civentra.ui.citizen;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;

public class CitizenLayout extends AppLayout implements BeforeEnterObserver {
    private final AuthenticatedUser authenticatedUser;

    public CitizenLayout(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        addClassName("civ-layout");

        // Force sidebar drawer z-index above map tiles & canvas overlays
        getElement().getStyle().set("--vaadin-app-layout-drawer-z-index", "10000");

        H1 title = new H1("CIVENTRA | Citizen Portal");
        title.getStyle().set("font-size", "18px").set("margin", "0").set("color", "#FFE7B1");

        Button logout = new Button("Logout", VaadinIcon.SIGN_OUT.create(), event -> {
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                session.setAttribute("user", null);
                session.getSession().removeAttribute("civentra.user");
                session.close();
            }
            getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
        });
        logout.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        header.setWidthFull();
        header.expand(title);
        header.addClassName("civ-navbar");

        addToNavbar(header);
        SideNav nav = createSideNav();
        nav.addClassName("civ-drawer");
        addToDrawer(nav);
    }

    private SideNav createSideNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", "citizen", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Report Issue", "citizen/report-issue", VaadinIcon.PLUS_CIRCLE.create()));
        nav.addItem(new SideNavItem("My Issues", "citizen/my-issues", VaadinIcon.LIST.create()));
        nav.addItem(new SideNavItem("Find Services", "citizen/find-services", VaadinIcon.SEARCH.create()));
        nav.addItem(new SideNavItem("Service Requests", "citizen/service-requests", VaadinIcon.HANDSHAKE.create()));
        nav.addItem(new SideNavItem("My Bills", "citizen/my-bills", VaadinIcon.WALLET.create()));
        nav.addItem(new SideNavItem("Community Map", "citizen/community-map", VaadinIcon.MAP_MARKER.create()));
        nav.addItem(new SideNavItem("Notifications", "citizen/notifications", VaadinIcon.BELL.create()));
        nav.addItem(new SideNavItem("AI Assistant", "citizen/ai-assistant", VaadinIcon.COMMENT_ELLIPSIS.create()));
        nav.addItem(new SideNavItem("Profile & Settings", "profile", VaadinIcon.USER.create()));
        return nav;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User user = authenticatedUser.getUser();
        if (user == null) {
            event.getUI().getPage().setLocation("/login");
            event.rerouteTo("login");
            return;
        }
        String role = user.getRoleName() == null ? "" : user.getRoleName().toUpperCase(java.util.Locale.ROOT);
        String path = event.getLocation().getPath().toLowerCase(java.util.Locale.ROOT);
        boolean allowed = ("CITIZEN".equals(role) || "ADMIN".equals(role))
                && !("citizen/my-issues".equals(path) && !"CITIZEN".equals(role));
        if (!allowed) {
            String home = switch (role) {
                case "ADMIN" -> "admin";
                case "OFFICER" -> "officer";
                case "PROVIDER" -> "provider";
                default -> "login";
            };
            event.getUI().getPage().setLocation("/" + home);
            event.rerouteTo(home);
        }
    }
}