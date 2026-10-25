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
import com.vaadin.flow.server.VaadinSession;
import com.civentra.security.AuthenticatedUser;

public class CitizenLayout extends AppLayout {
    private final AuthenticatedUser authenticatedUser;

    public CitizenLayout(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        addClassName("civ-layout");

        // Force sidebar drawer z-index above map tiles & canvas overlays
        getElement().getStyle().set("--vaadin-app-layout-drawer-z-index", "10000");

        H1 title = new H1("CIVENTRA | Citizen Portal");
        title.getStyle().set("font-size", "18px").set("margin", "0").set("color", "#FFE7B1");

        Button logout = new Button("Logout", VaadinIcon.SIGN_OUT.create(), event -> {
            authenticatedUser.logout();
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

}
