package com.stockportfolio.model;

/**
 * The three kinds of assets we support.
 */
public enum AssetCategory {
    STOCK("Stock"),
    ETF("ETF"),
    BOND("Bond");

    private final String displayName;

    AssetCategory(String displayName) {
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