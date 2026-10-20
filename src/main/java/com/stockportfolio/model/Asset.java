package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Abstract parent of Stock, ETF and Bond.
 * It holds the fields every investment has.
 * It cannot be created directly: new Asset(...) is a compile error.
 */
public abstract class Asset {

    private final String assetId;
    private String name;
    private final AssetCategory category;
    private final String currency;
    private BigDecimal currentPrice;

    protected Asset(String assetId, String name, AssetCategory category,
                    String currency, BigDecimal currentPrice) {
        if (assetId == null || assetId.isBlank()) {
            throw new InvalidInputException("Asset ID cannot be empty.");
        }
        if (category == null) {
            throw new InvalidInputException("Asset category is required.");
        }
        if (currency == null || currency.isBlank()) {
            throw new InvalidInputException("Currency is required.");
        }
        this.assetId = assetId.trim();
        this.category = category;
        this.currency = currency.trim().toUpperCase();
        setName(name);
        setCurrentPrice(currentPrice);
    }

    // ---------- getters ----------
    public String getAssetId() { return assetId; }
    public String getName() { return name; }
    public AssetCategory getCategory() { return category; }
    public String getCurrency() { return currency; }
    public BigDecimal getCurrentPrice() { return currentPrice; }

    // ---------- setters with validation (encapsulation) ----------
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Asset name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setCurrentPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new InvalidInputException("Price must be greater than zero.");
        }
        this.currentPrice = price.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- abstract methods: every subclass MUST implement these ----------

    /** Short identifier shown in tables (ticker for stocks and ETFs, name for bonds). */
    public abstract String getSymbol();

    /** One-line description with type-specific information. */
    public abstract String getDetails();

    @Override
    public String toString() {
        return getSymbol() + " - " + name;
    }
}