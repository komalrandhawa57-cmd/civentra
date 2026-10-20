package com.civentra.model;

/** Persisted status transition for a complaint, ordered by history sequence. */
public record IssueStatusChange(int historyId, String oldStatus, String newStatus,
                                String changedByName, String remarks) {
}
