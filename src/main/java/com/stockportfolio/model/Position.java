package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One holding: how many units of one asset the trader owns and what they paid for them.
 * Only Portfolio (same package) can change it, which protects the numbers.
 */
public class Position {

    private final Asset asset;
    private int quantity;
    private BigDecimal totalCostBasis;   // includes buy fees

    public Position(Asset asset, int quantity, BigDecimal totalCostBasis) {
        if (asset == null) {
            throw new InvalidInputException("Asset is required.");
        }
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero.");
        }
        if (totalCostBasis == null || totalCostBasis.signum() < 0) {
            throw new InvalidInputException("Cost basis cannot be negative.");
        }
        this.asset = asset;
        this.quantity = quantity;
        this.totalCostBasis = MoneyUtil.round(totalCostBasis);
    }

    public Asset getAsset() { return asset; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotalCostBasis() { return totalCostBasis; }

    /** Cost basis / quantity. */
    public BigDecimal getAverageCost() {
        return totalCostBasis.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCurrentPrice() {
        return asset.getCurrentPrice();
    }

    /** Market value = quantity x current price. */
    public BigDecimal getMarketValue() {
        return MoneyUtil.round(asset.getCurrentPrice().multiply(BigDecimal.valueOf(quantity)));
    }

    /** Unrealized P/L = market value - remaining cost basis. */
    public BigDecimal getUnrealizedProfitLoss() {
        return getMarketValue().subtract(totalCostBasis);
    }

    /** Percentage gain or loss on the cost basis (0 if cost basis is zero). */
    public BigDecimal getReturnPercent() {
        return MoneyUtil.percent(getUnrealizedProfitLoss(), totalCostBasis);
    }

    // ---------- package-private: only Portfolio may call these ----------

    void addUnits(int units, BigDecimal cost) {
        if (units <= 0) {
            throw new InvalidInputException("Units must be greater than zero.");
        }
        quantity += units;
        totalCostBasis = MoneyUtil.round(totalCostBasis.add(cost));
    }

    /** Removes units and returns the cost basis of the removed units. */
    BigDecimal removeUnits(int units) {
        if (units <= 0 || units > quantity) {
            throw new InvalidInputException("Invalid number of units to remove.");
        }
        BigDecimal removedCost;
        if (units == quantity) {
            removedCost = totalCostBasis;          // sell everything: no rounding leftovers
        } else {
            removedCost = totalCostBasis.multiply(BigDecimal.valueOf(units))
                    .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        }
        quantity -= units;
        totalCostBasis = totalCostBasis.subtract(removedCost);
        return removedCost;
    }
}