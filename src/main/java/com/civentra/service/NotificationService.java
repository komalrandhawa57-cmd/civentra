package com.civentra.service;

import com.civentra.dao.NotificationDAO;
import com.civentra.model.Notification;
import com.civentra.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationDAO notificationDAO;
    private final AuthenticatedUser authenticatedUser;

    @Autowired
    public NotificationService(NotificationDAO notificationDAO, AuthenticatedUser authenticatedUser) {
        this.notificationDAO = notificationDAO;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional
    public long createNotification(Notification notification) {
        return notificationDAO.createNotification(notification);
    }

    @Transactional
    public long notifyUser(long userId, String title, String message, String type) {
        Notification n = new Notification(userId, title, message, type);
        return notificationDAO.createNotification(n);
    }

    @Transactional
    public long notifyUser(int userId, String title, String message, String type) {
        return notifyUser((long) userId, title, message, type);
    }

    @Transactional
    public long notifyIssueUpdate(long userId, long issueId, String title, String message) {
        Notification notification = new Notification(userId, title, message, "ISSUE");
        notification.setRelatedIssueId(issueId);
        notification.setLinkUrl("citizen/my-issues");
        return notificationDAO.createNotification(notification);
    }

    @Transactional
    public long notifyServiceRequest(long userId, long requestId, String title, String message) {
        Notification notification = new Notification(userId, title, message, "SERVICE_REQUEST");
        notification.setLinkUrl("citizen/service-requests");
        return notificationDAO.createNotification(notification);
    }

    public List<Notification> getNotificationsForUser(int userId) {
        requireOwnAccount(userId);
        return notificationDAO.findByUserId(userId);
    }

    public List<Notification> getNotificationsForUser(long userId) {
        requireOwnAccount(userId);
        return notificationDAO.findByUserId(userId);
    }

    public List<Notification> getUserNotifications(long userId) {
        return getNotificationsForUser(userId);
    }

    public int getUnreadCount(int userId) {
        requireOwnAccount(userId);
        return notificationDAO.getUnreadCount(userId);
    }

    public int getUnreadCount(long userId) {
        requireOwnAccount(userId);
        return notificationDAO.getUnreadCount(userId);
    }

    @Transactional
    public void markAllRead(int userId) {
        requireOwnAccount(userId);
        notificationDAO.markAllRead(userId);
    }

    @Transactional
    public void markAllRead(long userId) {
        requireOwnAccount(userId);
        notificationDAO.markAllRead(userId);
    }

    @Transactional
    public boolean markRead(int notificationId, int userId) {
        requireOwnAccount(userId);
        return notificationDAO.markRead(notificationId, userId);
    }

    @Transactional
    public boolean markRead(long notificationId, long userId) {
        requireOwnAccount(userId);
        return notificationDAO.markRead(notificationId, userId);
    }

    private void requireOwnAccount(long requestedUserId) {
        com.civentra.model.User user = authenticatedUser.getUser();
        if (user == null || !user.isActive() || user.getUserId() != requestedUserId) {
            throw new SecurityException("You may only access notifications for your account.");
        }
    }
}