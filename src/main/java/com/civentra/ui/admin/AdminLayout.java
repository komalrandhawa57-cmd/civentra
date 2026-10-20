package com.civentra.ui.admin;

import com.civentra.security.AuthenticatedUser;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminLayout extends AppLayout implements com.vaadin.flow.router.BeforeEnterObserver {
    private static final Logger log = LoggerFactory.getLogger(AdminLayout.class);

    private final AuthenticatedUser authenticatedUser;
    private final H1 viewTitle = new H1();

    public AdminLayout(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;

        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createHeader());
        addToDrawer(createDrawer());
        getElement().getStyle().set("--vaadin-app-layout-drawer-width", "280px");
    }

    private HorizontalLayout createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle().set("color", "#FACC15");

        viewTitle.setText("Admin Console");
        viewTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("color", "#FFFFFF")
                .set("font-weight", "700");

        Span brand = new Span("CIVENTRA");
        brand.getStyle()
                .set("font-weight", "800")
                .set("color", "#FACC15")
                .set("font-size", "1.1rem")
                .set("margin-left", "0.5rem")
                .set("margin-right", "1.5rem");

        String displayName = "Admin";
        if (authenticatedUser != null) {
            try {
                displayName = authenticatedUser.getFullName() != null ? authenticatedUser.getFullName() : "Admin";
            } catch (Exception ex) {
                log.warn("Could not resolve the signed-in administrator name for the header.", ex);
            }
        }

        Avatar avatar = new Avatar(displayName);
        avatar.setColorIndex(3);

        Span nameSpan = new Span(displayName);
        nameSpan.getStyle().set("color", "#E9D5FF").set("font-size", "0.875rem");

        Button logoutBtn = new Button("Logout", new Icon(VaadinIcon.SIGN_OUT), e -> logout());
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        logoutBtn.getStyle().set("color", "#FACC15");

        HorizontalLayout userBox = new HorizontalLayout(avatar, nameSpan, logoutBtn);
        userBox.setAlignItems(FlexComponent.Alignment.CENTER);
        userBox.setSpacing(true);
        userBox.getStyle().set("margin-left", "auto").set("padding-right", "1rem");

        HorizontalLayout header = new HorizontalLayout(toggle, brand, viewTitle, userBox);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.setPadding(true);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #3B0764 0%, #2E1065 50%, #0B0514 100%)")
                .set("min-height", "56px");
        header.expand(viewTitle);
        return header;
    }

    private VerticalLayout createDrawer() {
        Div logoArea = new Div();
        logoArea.getStyle()
                .set("padding", "1.25rem 1rem 0.75rem")
                .set("background", "linear-gradient(160deg, #2E1065, #0F0C1B)");

        Span logo = new Span("CIVENTRA");
        logo.getStyle().set("font-size", "1.35rem").set("font-weight", "900").set("color", "#FACC15").set("display", "block");

        Span tag = new Span("Admin Control Center");
        tag.getStyle().set("font-size", "0.75rem").set("color", "#C4B5FD").set("display", "block");

        logoArea.add(logo, tag);

        SideNav nav = new SideNav();
        nav.getStyle().set("padding", "0.5rem");

        nav.addItem(new SideNavItem("Dashboard", "admin", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Providers", "admin/providers", VaadinIcon.HANDSHAKE.create()));
        nav.addItem(new SideNavItem("Issue Categories", "admin/categories", VaadinIcon.TAGS.create()));
        nav.addItem(new SideNavItem("Assignments", "admin/assignments", VaadinIcon.CONNECT.create()));
        nav.addItem(new SideNavItem("Service Request Reviews", "admin/service-requests", VaadinIcon.HANDSHAKE.create()));

        VerticalLayout drawer = new VerticalLayout(logoArea, nav);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.getStyle().set("background", "#0F0C1B").set("color", "#E9D5FF");
        drawer.expand(nav);
        return drawer;
    }

    private void logout() {
        clearSession();
    }

    private void clearSession() {
        com.vaadin.flow.server.VaadinSession session = com.vaadin.flow.server.VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute("user", null);
            session.getSession().removeAttribute("civentra.user");
            session.close();
        }
        UI.getCurrent().getPage().setLocation("/login");
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        String title = "";
        if (getContent() != null) {
            PageTitle pt = getContent().getClass().getAnnotation(PageTitle.class);
            if (pt != null) title = pt.value();
        }
        viewTitle.setText(title == null || title.isBlank() ? "Admin Console" : title);
    }

    @Override
    public void beforeEnter(com.vaadin.flow.router.BeforeEnterEvent event) {
        com.civentra.model.User user = authenticatedUser.getUser();
        if (user == null) {
            event.getUI().getPage().setLocation("/login");
            event.rerouteTo("login");
        } else if (!"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            String home = switch (user.getRoleName() == null ? "" : user.getRoleName().toUpperCase(java.util.Locale.ROOT)) {
                case "OFFICER" -> "officer";
                case "PROVIDER" -> "provider";
                case "CITIZEN" -> "citizen";
                default -> "login";
            };
            event.getUI().getPage().setLocation("/" + home);
            event.rerouteTo(home);
        }
    }
}
