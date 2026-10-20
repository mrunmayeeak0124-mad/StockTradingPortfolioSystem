package com.stockportfolio.model;

import com.stockportfolio.exception.InsufficientHoldingsException;
import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All open holdings of one trader. Positions are stored in a Map keyed by asset ID.
 * Cash is NOT part of the portfolio: it stays in the Trader.
 */
public class Portfolio {

    private final Map<String, Position> positions = new LinkedHashMap<>();
    private BigDecimal realizedProfitLoss = BigDecimal.ZERO.setScale(2);

    /** Add units to an existing position or open a new one. totalCost includes the fee. */
    public void recordBuy(Asset asset, int quantity, BigDecimal totalCost) {
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero.");
        }
        Position existing = positions.get(asset.getAssetId());
        if (existing == null) {
            positions.put(asset.getAssetId(), new Position(asset, quantity, totalCost));
        } else {
            existing.addUnits(quantity, totalCost);
        }
    }

    /**
     * Remove units and return the cost basis of the sold units.
     * Throws BEFORE changing anything if the sale is not possible.
     */
    public BigDecimal recordSell(Asset asset, int quantity) throws InsufficientHoldingsException {
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero.");
        }
        Position position = positions.get(asset.getAssetId());
        if (position == null) {
            throw new InsufficientHoldingsException(
                    "You do not own any units of " + asset.getSymbol() + ".");
        }
        if (quantity > position.getQuantity()) {
            throw new InsufficientHoldingsException("You hold only " + position.getQuantity()
                    + " unit(s) of " + asset.getSymbol() + " but tried to sell " + quantity + ".");
        }
        BigDecimal removedCost = position.removeUnits(quantity);
        if (position.getQuantity() == 0) {
            positions.remove(asset.getAssetId());    // closed position is removed
        }
        return removedCost;
    }

    void addRealizedProfitLoss(BigDecimal amount) {
        realizedProfitLoss = MoneyUtil.round(realizedProfitLoss.add(amount));
    }

    /**
     * Used ONLY when loading saved data: sets the realized P/L total that the
     * repository calculated from the saved SELL records.
     */
    public void restoreRealizedProfitLoss(BigDecimal total) {
        realizedProfitLoss = MoneyUtil.round(total);
    }

    public Position getPosition(String assetId) {
        return positions.get(assetId);
    }

    public int getQuantityHeld(String assetId) {
        Position p = positions.get(assetId);
        return p == null ? 0 : p.getQuantity();
    }

    /** A copy of the list, so outside code cannot change our map. */
    public List<Position> getPositions() {
        return new ArrayList<>(positions.values());
    }

    public int getOpenPositionCount() { return positions.size(); }

    public boolean isEmpty() { return positions.isEmpty(); }

    public BigDecimal getTotalMarketValue() {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Position p : positions.values()) {
            total = total.add(p.getMarketValue());
        }
        return total;
    }

    public BigDecimal getTotalCostBasis() {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Position p : positions.values()) {
            total = total.add(p.getTotalCostBasis());
        }
        return total;
    }

    public BigDecimal getUnrealizedProfitLoss() {
        return getTotalMarketValue().subtract(getTotalCostBasis());
    }

    public BigDecimal getRealizedProfitLoss() { return realizedProfitLoss; }

    public BigDecimal getTotalReturnPercent() {
        return MoneyUtil.percent(getUnrealizedProfitLoss(), getTotalCostBasis());
    }

    /** Percentage of market value in each category. Empty map for an empty portfolio. */
    public Map<AssetCategory, BigDecimal> getAllocationByCategory() {
        Map<AssetCategory, BigDecimal> values = new EnumMap<>(AssetCategory.class);
        for (Position p : positions.values()) {
            values.merge(p.getAsset().getCategory(), p.getMarketValue(), BigDecimal::add);
        }
        BigDecimal total = getTotalMarketValue();
        Map<AssetCategory, BigDecimal> allocation = new EnumMap<>(AssetCategory.class);
        for (Map.Entry<AssetCategory, BigDecimal> entry : values.entrySet()) {
            allocation.put(entry.getKey(), MoneyUtil.percent(entry.getValue(), total));
        }
        return allocation;
    }
}