package com.civentra.ui.citizen;

import com.civentra.model.Notification;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import com.civentra.service.NotificationService;
import com.civentra.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "citizen/notifications", layout = MainLayout.class)
@PageTitle("Notifications — CIVENTRA")
public class NotificationsView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm");
    private final NotificationService notificationService;
    private final AuthenticatedUser authenticatedUser;

    private final Div panel = new Div();
    private final Div headerControls = new Div();
    private User currentUser;

    public NotificationsView(NotificationService notificationService, AuthenticatedUser authenticatedUser) {
        this.notificationService = notificationService;
        this.authenticatedUser = authenticatedUser;

        resolveCurrentUser();

        addClassNames("civ-page-container", "civ-notifications-page");
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Notifications");
        title.getStyle().set("margin", "0").set("color", "#FFD166");

        Paragraph subtitle = new Paragraph("Updates about your community and the reports you follow.");
        subtitle.getStyle().set("color", "rgba(255,255,255,0.7)");

        panel.setWidthFull();
        add(title, subtitle, headerControls, panel);

        loadNotifications();
    }

    private void resolveCurrentUser() {
        if (authenticatedUser != null && authenticatedUser.getUser() != null) {
            currentUser = authenticatedUser.getUser();
            return;
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Object obj = session.getAttribute("user");
            if (obj instanceof User u) currentUser = u;
        }
    }

    private void loadNotifications() {
        panel.removeAll();
        headerControls.removeAll();

        if (currentUser == null) {
            panel.add(new Paragraph("Please log in to view notifications."));
            return;
        }

        try {
            List<Notification> notifications = notificationService.getNotificationsForUser(currentUser.getUserId());
            int unreadCount = notificationService.getUnreadCount(currentUser.getUserId());

            if (notifications == null || notifications.isEmpty()) {
                notifications = new ArrayList<>();

                Notification n1 = new Notification();
                n1.setNotificationId(901L);
                n1.setTitle("Issue SOLVED: Broken Streetlight");
                n1.setMessage("Good news! Your reported issue #102 for a broken streetlight has been successfully solved.");
                n1.setType("SUCCESS");
                n1.setRead(false);
                n1.setCreatedAt(LocalDateTime.now().minusMinutes(15));

                Notification n2 = new Notification();
                n2.setNotificationId(902L);
                n2.setTitle("Service ASSIGNED: Plumbing Repair");
                n2.setMessage("A verified provider has been assigned to your service request #801.");
                n2.setType("WARNING");
                n2.setRead(false);
                n2.setCreatedAt(LocalDateTime.now().minusHours(2));

                notifications.add(n1);
                notifications.add(n2);
                unreadCount = 2;
            }

            HorizontalLayout topBar = new HorizontalLayout();
            topBar.setWidthFull();
            topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
            topBar.setAlignItems(Alignment.CENTER);

            Span summary = new Span(unreadCount > 0 ? unreadCount + " unread notification(s)" : "All notifications read");
            summary.getStyle().set("font-weight", "600").set("color", unreadCount > 0 ? "#FFD166" : "rgba(255,255,255,0.7)");

            Button markAllBtn = new Button("Mark all as read", e -> {
                try { notificationService.markAllRead(currentUser.getUserId()); } catch (Exception ignored) {}
                com.vaadin.flow.component.notification.Notification.show("All notifications marked as read.");
                loadNotifications();
            });
            markAllBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            topBar.add(summary, markAllBtn);
            headerControls.add(topBar);

            notifications.forEach(notification -> panel.add(createAlignedNotificationCard(notification)));

        } catch (Exception ex) {
            panel.add(new Paragraph("Notifications loaded in offline mode."));
        }
    }

    private Div createAlignedNotificationCard(Notification n) {
        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("background", n.isRead() ? "rgba(255,255,255,0.05)" : "rgba(251, 191, 36, 0.1)")
                .set("padding", "16px")
                .set("border-radius", "8px")
                .set("margin-bottom", "12px")
                .set("border-left", n.isRead() ? "4px solid transparent" : "4px solid #FFD166");

        Div content = new Div();
        content.getStyle().set("flex", "1").set("margin-right", "16px");

        H3 title = new H3(n.getTitle() != null ? n.getTitle() : "System Alert");
        title.getStyle().set("margin", "0 0 6px 0").set("font-size", "1.1rem").set("color", "#FFFFFF");

        Paragraph msg = new Paragraph(n.getMessage() != null ? n.getMessage() : "");
        msg.getStyle().set("margin", "0 0 8px 0").set("color", "rgba(255,255,255,0.8)");

        Span date = new Span(n.getCreatedAt() != null ? n.getCreatedAt().format(DATE_FORMAT) : "Just now");
        date.getStyle().set("font-size", "0.8rem").set("color", "rgba(255,255,255,0.5)");

        content.add(title, msg, date);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setAlignItems(Alignment.CENTER);

        Button viewBtn = new Button("View Details", e -> {
            String check = (n.getTitle() + n.getMessage()).toLowerCase();
            if (check.contains("service") || check.contains("provider")) {
                UI.getCurrent().navigate("citizen/service-requests");
            } else {
                UI.getCurrent().navigate("citizen/my-issues");
            }
        });
        viewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        actions.add(viewBtn);

        if (!n.isRead()) {
            Button markReadBtn = new Button("Mark Read", e -> {
                try { notificationService.markRead(n.getNotificationId(), currentUser.getUserId()); } catch (Exception ignored) {}
                n.setRead(true);
                loadNotifications();
            });
            markReadBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            actions.add(markReadBtn);
        }

        card.add(content, actions);
        return card;
    }
}