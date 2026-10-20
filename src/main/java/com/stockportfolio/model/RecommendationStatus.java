package com.stockportfolio.model;

/**
 * Status tracking for generated financial recommendations.
 */
public enum RecommendationStatus {
    NEW("New"),
    REVIEWED("Reviewed"),
    ACCEPTED("Accepted"),
    DISMISSED("Dismissed"),
    COMPLETED("Completed");

    private final String displayName;

    RecommendationStatus(String displayName) {
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
