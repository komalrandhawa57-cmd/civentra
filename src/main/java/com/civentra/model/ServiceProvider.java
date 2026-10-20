package com.civentra.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class ServiceProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long providerId;
    private Long userId;
    private String businessName;
    private String name;
    private String email;
    private String phone;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String address;
    private String city;
    private String pincode;
    private String status = "PENDING";
    private String verificationStatus = "UNVERIFIED";
    private double avgRating = 0.0;
    private int totalJobs = 0;
    private LocalDateTime createdAt;

    public ServiceProvider() {
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public void setProviderId(int providerId) {
        this.providerId = (long) providerId;
    }

    public int getProviderIdAsInt() {
        return providerId == null ? 0 : providerId.intValue();
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

    public String getBusinessName() {
        return businessName != null ? businessName : name;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
        if (this.name == null)
            this.name = businessName;
    }

    public String getName() {
        return name != null ? name : businessName;
    }

    public void setName(String name) {
        this.name = name;
        if (this.businessName == null)
            this.businessName = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = (long) categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating == null ? 0.0 : avgRating;
    }

    public int getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public void setTotalJobs(Integer totalJobs) {
        this.totalJobs = totalJobs == null ? 0 : totalJobs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ServiceProvider that))
            return false;
        return Objects.equals(providerId, that.providerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId);
    }
}