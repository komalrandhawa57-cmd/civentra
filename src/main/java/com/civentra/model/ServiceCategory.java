package com.civentra.model;

import java.io.Serializable;
import java.util.Objects;

public class ServiceCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String icon;
    private boolean active = true;

    public ServiceCategory() {
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

    public int getCategoryIdAsInt() {
        return categoryId == null ? 0 : categoryId.intValue();
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : name;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        this.name = categoryName;
    }

    public String getName() {
        return name != null ? name : categoryName;
    }

    public void setName(String name) {
        this.name = name;
        this.categoryName = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ServiceCategory that))
            return false;
        return Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId);
    }
}