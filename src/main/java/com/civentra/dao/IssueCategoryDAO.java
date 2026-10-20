package com.civentra.dao;

import com.civentra.model.IssueCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class IssueCategoryDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public IssueCategoryDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<IssueCategory> findAll() {
        try {
            String sql = "SELECT CATEGORY_ID, NVL(NAME, CATEGORY_NAME) AS CATEGORY_NAME, DESCRIPTION FROM ISSUE_CATEGORIES ORDER BY CATEGORY_ID";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                IssueCategory cat = new IssueCategory();
                cat.setCategoryId(rs.getLong("CATEGORY_ID"));
                try { 
                    cat.setCategoryName(rs.getString("CATEGORY_NAME")); 
                } catch (Exception ignored) {}
                try { 
                    cat.setDescription(rs.getString("DESCRIPTION")); 
                } catch (Exception ignored) {}
                return cat;
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean existsByName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ISSUE_CATEGORIES WHERE UPPER(NVL(NAME, CATEGORY_NAME)) = UPPER(?)",
                Integer.class, name.trim());
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public long save(IssueCategory category) {
        String name = category.getCategoryName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        if (category.getCategoryId() == null && existsByName(name)) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }

        Long nextId = category.getCategoryId() != null ? category.getCategoryId() : getNextCategoryId();
        
        if (category.getCategoryId() == null) {
            try {
                jdbcTemplate.update(
                    "INSERT INTO ISSUE_CATEGORIES (CATEGORY_ID, NAME, CATEGORY_NAME, DESCRIPTION) VALUES (?, ?, ?, ?)",
                    nextId, name.trim(), name.trim(), category.getDescription()
                );
            } catch (Exception e) {
                jdbcTemplate.update(
                    "INSERT INTO ISSUE_CATEGORIES (CATEGORY_ID, NAME, DESCRIPTION) VALUES (?, ?, ?)",
                    nextId, name.trim(), category.getDescription()
                );
            }
        } else {
            try {
                jdbcTemplate.update(
                    "UPDATE ISSUE_CATEGORIES SET NAME = ?, CATEGORY_NAME = ?, DESCRIPTION = ? WHERE CATEGORY_ID = ?",
                    name.trim(), name.trim(), category.getDescription(), category.getCategoryId()
                );
            } catch (Exception e) {
                jdbcTemplate.update(
                    "UPDATE ISSUE_CATEGORIES SET NAME = ?, DESCRIPTION = ? WHERE CATEGORY_ID = ?",
                    name.trim(), category.getDescription(), category.getCategoryId()
                );
            }
        }
        category.setCategoryId(nextId);
        return nextId;
    }

    public void deleteById(Long categoryId) {
        jdbcTemplate.update("DELETE FROM ISSUE_CATEGORIES WHERE CATEGORY_ID = ?", categoryId);
    }

    private Long getNextCategoryId() {
        try { 
            return jdbcTemplate.queryForObject("SELECT category_seq.NEXTVAL FROM DUAL", Long.class); 
        } catch (Exception ignored) {}
        try { 
            Long maxId = jdbcTemplate.queryForObject("SELECT NVL(MAX(CATEGORY_ID), 0) FROM ISSUE_CATEGORIES", Long.class);
            return (maxId == null ? 0L : maxId) + 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}