package com.civentra.model;

import java.io.Serializable;
import java.util.Objects;

public class IssueCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String categoryName;
    private String description;
    private Integer deptId;
    private String iconName;
    private boolean active = true;

    public IssueCategory() {}

    public IssueCategory(Long categoryId, String categoryName, String description) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = (long) categoryId; }
    public int getCategoryIdAsInt() { return categoryId == null ? 0 : categoryId.intValue(); }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDeptId() { return deptId; }
    public void setDeptId(Integer deptId) { this.deptId = deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueCategory that)) return false;
        return Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId);
    }
}