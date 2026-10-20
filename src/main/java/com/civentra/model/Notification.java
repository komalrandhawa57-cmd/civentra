package com.civentra.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long notificationId;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private String linkUrl;
    private boolean readFlag;
    private Long relatedIssueId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Notification() {
        this.readFlag = false;
        this.createdAt = LocalDateTime.now();
    }

    public Notification(Long userId, String title, String message, String type) {
        this();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = (long) notificationId;
    }

    public int getNotificationIdAsInt() {
        return notificationId == null ? 0 : notificationId.intValue();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setUserId(int userId) {
        this.userId = (long) userId;
    }

    public int getUserIdAsInt() {
        return userId == null ? 0 : userId.intValue();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public boolean isReadFlag() {
        return readFlag;
    }

    public boolean isRead() {
        return readFlag;
    }

    public void setReadFlag(boolean readFlag) {
        this.readFlag = readFlag;
    }

    public void setRead(boolean read) {
        this.readFlag = read;
    }

    public void setIsRead(int isRead) {
        this.readFlag = isRead == 1;
    }

    public void setIsRead(boolean isRead) {
        this.readFlag = isRead;
    }

    public Long getRelatedIssueId() {
        return relatedIssueId;
    }

    public void setRelatedIssueId(Long relatedIssueId) {
        this.relatedIssueId = relatedIssueId;
    }

    public void setRelatedIssueId(long relatedIssueId) {
        this.relatedIssueId = relatedIssueId;
    }

    public void setRelatedIssueId(int relatedIssueId) {
        this.relatedIssueId = (long) relatedIssueId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Notification that))
            return false;
        return Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }
}