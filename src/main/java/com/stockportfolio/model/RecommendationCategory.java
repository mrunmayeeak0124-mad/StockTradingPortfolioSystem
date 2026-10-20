package com.stockportfolio.model;

/**
 * Types of educational advisor recommendations.
 */
public enum RecommendationCategory {
    EMERGENCY_FUND("Emergency Safety Buffer"),
    DEBT_MANAGEMENT("Debt & Liability Management"),
    ASSET_ALLOCATION("Strategic Asset Allocation"),
    DIVERSIFICATION("Portfolio Diversification"),
    GOAL_SAVINGS("Goal Progress & Savings Rate"),
    RISK_ALIGNMENT("Risk Profile Realignment");

    private final String displayName;

    RecommendationCategory(String displayName) {
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
