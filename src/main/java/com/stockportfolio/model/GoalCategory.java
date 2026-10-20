package com.stockportfolio.model;

/**
 * Categories for trader financial goals.
 */
public enum GoalCategory {
    RETIREMENT("Retirement Planning"),
    HOME_PURCHASE("Home Purchase & Down Payment"),
    EDUCATION("Higher Education"),
    EMERGENCY_FUND("Emergency Safety Buffer"),
    WEALTH_BUILDING("Long-Term Wealth Building"),
    VACATION("Vacation & Travel"),
    VEHICLE("Vehicle Purchase"),
    OTHER("General Goal");

    private final String displayName;

    GoalCategory(String displayName) {
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
