package com.stockportfolio.model;

import java.math.BigDecimal;

/**
 * One row of the holdings table (one open position, ready to display).
 */
public record HoldingSummary(String assetId, String name, String symbol, AssetCategory category,
                             int quantity, BigDecimal averageCost, BigDecimal currentPrice,
                             BigDecimal totalCost, BigDecimal marketValue,
                             BigDecimal unrealizedProfitLoss, BigDecimal returnPercent,
                             BigDecimal portfolioSharePercent) { }