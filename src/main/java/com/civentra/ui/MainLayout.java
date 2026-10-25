package com.civentra.ui;

import com.civentra.model.Notification;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import com.civentra.service.NotificationService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;
    private final NotificationService notificationService;

    public MainLayout(NotificationService notificationService, AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        this.notificationService = notificationService;

        addClassName("civ-layout");
        setPrimarySection(Section.DRAWER);
        setDrawerOpened(true);

        createHeader();
        createSidebar();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle().set("color", "#FFD166");

        User user = getCurrentUser();
        String name = (user != null && user.getFullName() != null) ? user.getFullName() : "User";
        String role = (user != null && user.getRoleName() != null) ? user.getRoleName() : "Citizen";
        String initial = name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase();

        Button bell = new Button(VaadinIcon.BELL.create());
        bell.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        bell.addClassName("civ-header-icon");
        // Header bell opens dialog for quick-view
        bell.addClickListener(e -> openNotificationsDialog());

        HorizontalLayout notifications = new HorizontalLayout(bell);
        notifications.addClassName("civ-notification-control");
        notifications.setPadding(false);
        notifications.setSpacing(false);

        if (user != null && user.getUserId() > 0) {
            try {
                int unread = notificationService.getUnreadCount(user.getUserId());
                if (unread > 0) {
                    Span badge = new Span(unread > 99 ? "99+" : String.valueOf(unread));
                    badge.addClassName("civ-notification-badge");
                    notifications.add(badge);
                }
            } catch (Exception ex) {
                Span badge = new Span("1");
                badge.addClassName("civ-notification-badge");
                notifications.add(badge);
            }
        } else {
            bell.setVisible(false);
        }

        TextField search = new TextField();
        boolean citizen = user != null && "CITIZEN".equalsIgnoreCase(user.getRoleName());
        search.setVisible(citizen);
        search.setPlaceholder("Search your issues...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addClassName("civ-global-search");
        search.setClearButtonVisible(true);
        search.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> {
            if (getCurrentUser() != null && "CITIZEN".equalsIgnoreCase(getCurrentUser().getRoleName())) {
                VaadinSession.getCurrent().setAttribute("issueSearchQuery", search.getValue());
                getUI().ifPresent(ui -> ui.navigate("citizen/my-issues"));
            }
        });

        Div avatar = new Div();
        avatar.setText(initial);
        avatar.getStyle().set("width", "40px").set("height", "40px").set("border-radius", "50%")
                .set("background", "#6D28D9").set("color", "#FFFFFF")
                .set("font-weight", "700").set("display", "flex").set("align-items", "center")
                .set("justify-content", "center");

        Div info = new Div();
        Span nameEl = new Span(name);
        nameEl.getStyle().set("font-weight", "600").set("display", "block").set("font-size", "0.9rem");
        Span roleEl = new Span(role);
        roleEl.getStyle().set("font-size", "0.75rem").set("color", "#9CA3AF");
        info.add(nameEl, roleEl);
        info.addClassName("civ-user-info");

        Button logout = new Button(VaadinIcon.SIGN_OUT.create(), e -> logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        logout.addClassName("civ-header-icon");

        HorizontalLayout profile = new HorizontalLayout(avatar, info, logout);
        profile.addClassName("civ-profile-pill");
        profile.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout right = new HorizontalLayout(notifications, profile);
        right.setAlignItems(FlexComponent.Alignment.CENTER);
        right.getStyle().set("margin-left", "auto").set("padding-right", "24px");

        HorizontalLayout header = new HorizontalLayout(toggle, search, right);
        header.addClassName("civ-navbar");
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(search);

        addToNavbar(header);
    }

    private void createSidebar() {
        VerticalLayout drawer = new VerticalLayout();
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.addClassName("civ-drawer");
        drawer.getStyle().set("background", "#310A4D").set("color", "#FFFFFF").set("padding", "20px 12px");

        HorizontalLayout brand = new HorizontalLayout();
        brand.setAlignItems(FlexComponent.Alignment.CENTER);
        brand.addClassName("civ-brand");
        brand.getStyle().set("padding", "0 12px 24px").set("border-bottom", "1px solid rgba(255,255,255,0.1)").set("margin-bottom", "20px");

        Div logoBox = new Div();
        logoBox.getStyle().set("width", "42px").set("height", "42px")
                .set("background", "linear-gradient(135deg, #6D28D9, #FFD166)")
                .set("border-radius", "12px").set("display", "flex")
                .set("align-items", "center").set("justify-content", "center").set("font-size", "20px");
        Icon building = VaadinIcon.BUILDING.create();
        building.setColor("white");
        logoBox.add(building);

        VerticalLayout titleBox = new VerticalLayout();
        titleBox.setPadding(false);
        titleBox.setSpacing(false);
        H2 h2 = new H2("CIVENTRA");
        h2.getStyle().set("color", "#FFFFFF").set("margin", "0").set("font-size", "1.2rem").set("letter-spacing", "1.5px");
        Span tag = new Span("Issue Resolution");
        tag.getStyle().set("color", "rgba(255,255,255,0.6)").set("font-size", "0.7rem");
        titleBox.add(h2, tag);
        brand.add(logoBox, titleBox);

        User user = getCurrentUser();
        String role = (user != null && user.getRoleName() != null) ? user.getRoleName().toUpperCase() : "CITIZEN";

        VerticalLayout nav = new VerticalLayout();
        nav.setPadding(false);
        nav.setSpacing(false);

        // FIXED: Replaced "citizen/map" with "citizen/community-map"
        // FIXED: Replaced actionItem (Dialogs) with navItem (Pages)
        switch (role) {
            case "ADMIN" -> {
                nav.add(navItem("Dashboard", VaadinIcon.DASHBOARD, "admin"));
                nav.add(navItem("Complaints", VaadinIcon.CLIPBOARD, "admin/assignments"));
                nav.add(navItem("Service Request Reviews", VaadinIcon.HANDSHAKE, "admin/service-requests"));
                nav.add(navItem("Issue Categories", VaadinIcon.LIST, "admin/categories"));
                nav.add(navItem("Users", VaadinIcon.USERS, "admin/users"));
                nav.add(navItem("Service Providers", VaadinIcon.TOOLS, "admin/providers"));
                nav.add(navItem("Analytics", VaadinIcon.CHART, "admin/analytics"));
                nav.add(navItem("Notifications", VaadinIcon.BELL, "citizen/notifications"));
                nav.add(navItem("Profile & Settings", VaadinIcon.USER, "profile"));
                nav.add(logoutItem());
            }
            case "OFFICER" -> {
                nav.add(navItem("Workspace", VaadinIcon.BRIEFCASE, "officer"));
                nav.add(navItem("Notifications", VaadinIcon.BELL, "citizen/notifications"));
                nav.add(navItem("Profile & Settings", VaadinIcon.USER, "profile"));
                nav.add(logoutItem());
            }
            case "PROVIDER" -> {
                nav.add(navItem("Workspace", VaadinIcon.INBOX, "provider"));
                nav.add(navItem("Notifications", VaadinIcon.BELL, "citizen/notifications"));
                nav.add(navItem("Profile & Settings", VaadinIcon.USER, "profile"));
                nav.add(logoutItem());
            }
            default -> {
                nav.add(navItem("Dashboard", VaadinIcon.HOME, "citizen"));
                nav.add(navItem("Report Issue", VaadinIcon.PLUS_CIRCLE, "citizen/report-issue"));
                nav.add(navItem("My Issues", VaadinIcon.FILE_TEXT, "citizen/my-issues"));
                nav.add(navItem("Find Services", VaadinIcon.SEARCH, "citizen/find-services"));
                nav.add(navItem("Service Requests", VaadinIcon.HANDSHAKE, "citizen/service-requests"));
                nav.add(navItem("Community Map", VaadinIcon.MAP_MARKER, "citizen/community-map"));
                nav.add(navItem("Notifications", VaadinIcon.BELL, "citizen/notifications"));
                nav.add(navItem("Profile & Settings", VaadinIcon.USER, "profile"));
                nav.add(logoutItem());
            }
        }

        Div footer = new Div();
        footer.setText("Stronger Communities");
        footer.getStyle().set("margin-top", "auto").set("padding", "20px 12px").set("color", "rgba(255,255,255,0.5)").set("font-size", "0.75rem").set("border-top", "1px solid rgba(255,255,255,0.1)");
        drawer.add(brand, nav, footer);
        addToDrawer(drawer);
    }

    private HorizontalLayout navItem(String text, VaadinIcon icon, String route) {
        HorizontalLayout row = baseNavRow(text, icon);
        row.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));
        return row;
    }

    private HorizontalLayout baseNavRow(String text, VaadinIcon icon) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.addClassName("civ-nav-item");
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle().set("padding", "12px 14px").set("border-radius", "10px").set("color", "rgba(255,255,255,0.75)").set("cursor", "pointer").set("margin-bottom", "4px").set("transition", "all 0.2s");
        row.getElement().addEventListener("mouseover", e -> row.getStyle().set("background", "rgba(255,255,255,0.08)").set("color", "#FFFFFF"));
        row.getElement().addEventListener("mouseout", e -> row.getStyle().set("background", "transparent").set("color", "rgba(255,255,255,0.75)"));

        Icon i = icon.create();
        i.getStyle().set("width", "18px");
        Span label = new Span(text);
        label.getStyle().set("font-size", "0.9rem").set("font-weight", "600").set("margin-left", "12px");

        row.add(i, label);
        return row;
    }

    private HorizontalLayout logoutItem() {
        HorizontalLayout row = baseNavRow("Logout", VaadinIcon.SIGN_OUT);
        row.addClickListener(e -> logout());
        return row;
    }

    // Bell Quick-view Dialog
    private void openNotificationsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Notifications");
        dialog.setWidth("420px");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        list.setMaxHeight("420px");
        list.getStyle().set("overflow-y", "auto");

        User user = getCurrentUser();
        if (user == null || user.getUserId() <= 0) {
            list.add(new Paragraph("Please log in to view notifications."));
        } else {
            try {
                List<Notification> notes = notificationService.getNotificationsForUser(user.getUserId());
                if (notes == null || notes.isEmpty()) {
                    list.add(buildNoticeCard("Welcome to CIVENTRA", "Your workspace is ready. No new alerts right now.", null));
                } else {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
                    for (Notification n : notes) {
                        String when = null;
                        try {
                            if (n.getCreatedAt() != null) when = n.getCreatedAt().format(fmt);
                        } catch (Exception ignored) {}
                        list.add(buildNoticeCard(n.getTitle() != null ? n.getTitle() : "Update", n.getMessage() != null ? n.getMessage() : "", when));
                    }
                }
            } catch (Exception ex) {
                list.add(buildNoticeCard("System notice", "Showing demo notifications.", null));
            }
        }

        dialog.add(list);
        Button fullView = new Button("View Full Page", e -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.navigate("citizen/notifications"));
        });
        Button close = new Button("Close", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        dialog.getFooter().add(fullView, close);
        dialog.open();
    }

    private VerticalLayout buildNoticeCard(String title, String message, String when) {
        Span t = new Span(title);
        t.getStyle().set("font-weight", "700").set("color", "#FFD166").set("display", "block");
        Paragraph m = new Paragraph(message);
        m.getStyle().set("margin", "4px 0 0 0").set("font-size", "0.9em").set("color", "rgba(255,255,255,0.9)");
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("border-bottom", "1px solid rgba(255,255,255,0.1)");
        card.add(t, m);
        if (when != null && !when.isBlank()) {
            Span time = new Span(when);
            time.getStyle().set("font-size", "0.75em").set("color", "rgba(255,255,255,0.5)");
            card.add(time);
        }
        return card;
    }

    private void logout() {
        try {
            authenticatedUser.logout();
        } catch (Exception ignored) {
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                session.setAttribute("user", null);
                session.close();
            }
        }
        getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
    }

    private User getCurrentUser() {
        try { return authenticatedUser.getUser(); } catch (Exception e) { return null; }
    }

}
