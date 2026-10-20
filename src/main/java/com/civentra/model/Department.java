package com.civentra.model;

import java.time.LocalDateTime;

public class Department {

    private int deptId;
    private String deptName;
    private String description;
    private String headName;
    private String contact;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Department() {}

    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getHeadName() { return headName; }
    public void setHeadName(String headName) { this.headName = headName; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return deptName; }
}