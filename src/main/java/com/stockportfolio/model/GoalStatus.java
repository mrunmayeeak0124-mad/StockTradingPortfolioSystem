package com.stockportfolio.model;

/**
 * Lifecycle status of a financial goal.
 */
public enum GoalStatus {
    IN_PROGRESS("In Progress"),
    ACHIEVED("Achieved"),
    PAUSED("Paused"),
    CANCELLED("Cancelled");

    private final String displayName;

    GoalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
