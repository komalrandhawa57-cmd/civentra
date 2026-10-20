package com.civentra.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private int roleId;
    private String roleName;
    private String fullName;
    private String address;
    private String city;
    private String ward;
    private String status = "ACTIVE";
    private boolean active = true;

    public User() {
    }

    public User(int userId, String username, String email, String phone, String passwordHash, int roleId, String roleName, String fullName, String address, String city, String ward, String status) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.roleName = roleName;
        this.fullName = fullName;
        this.address = address;
        this.city = city;
        this.ward = ward;
        this.status = status;
        this.active = "ACTIVE".equalsIgnoreCase(status);
    }

    // User ID getters and setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId != null ? userId.intValue() : 0;
    }

    public void setUserId(Number userId) {
        this.userId = userId != null ? userId.intValue() : 0;
    }

    public int getId() {
        return userId;
    }

    public void setId(int id) {
        this.userId = id;
    }

    // Username getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserName() {
        return username;
    }

    public void setUserName(String userName) {
        this.username = userName;
    }

    // Email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Phone
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneNumber() {
        return phone;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
    }

    // Password Hash
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    // Role ID
    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId != null ? roleId.intValue() : 1;
    }

    public void setRoleId(Number roleId) {
        this.roleId = roleId != null ? roleId.intValue() : 1;
    }

    // Role Name
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRole() {
        return roleName;
    }

    public void setRole(String role) {
        this.roleName = role;
    }

    // Full Name
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    // Location
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    // Active Status (Boolean)
    public boolean isActive() {
        return active || "ACTIVE".equalsIgnoreCase(status);
    }

    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? "ACTIVE" : "INACTIVE";
    }

    // Account Status (String)
    public String getStatus() {
        return status != null ? status : (active ? "ACTIVE" : "INACTIVE");
    }

    public void setStatus(String status) {
        this.status = status;
        this.active = "ACTIVE".equalsIgnoreCase(status);
    }

    public String getUserStatus() {
        return getStatus();
    }

    public void setUserStatus(String userStatus) {
        setStatus(userStatus);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", roleName='" + roleName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}