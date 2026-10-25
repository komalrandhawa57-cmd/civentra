package com.civentra.model;

public enum IssueStatus {
    REPORTED("Reported"),
    SUBMITTED("Submitted"),
    PENDING("Pending"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    SOLVED("Solved"),
    CLOSED("Closed"),
    REJECTED("Rejected");

    private final String displayName;

    IssueStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static IssueStatus fromString(Object input) {
        if (input == null) {
            return REPORTED;
        }
        String text = input.toString().trim();
        if (text.isEmpty()) {
            return REPORTED;
        }

        String clean = text.toUpperCase().replace(" ", "_");

        for (IssueStatus status : IssueStatus.values()) {
            if (status.name().equalsIgnoreCase(clean) || status.getDisplayName().equalsIgnoreCase(text)) {
                return status;
            }
        }

        if (clean.contains("SOLV") || clean.contains("RESOLV")) {
            return SOLVED;
        }
        if (clean.contains("ASSIGN") || clean.contains("PROGRESS")) {
            return ASSIGNED;
        }
        if (clean.contains("PEND") || clean.contains("SUBMIT")) {
            return PENDING;
        }

        throw new IllegalArgumentException("Unknown issue status: " + input);
    }

    public static String toDisplayName(Object input) {
        if (input == null) {
            return "Reported";
        }
        if (input instanceof IssueStatus) {
            return ((IssueStatus) input).getDisplayName();
        }
        return fromString(input.toString()).getDisplayName();
    }
}