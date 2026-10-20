package com.stockportfolio.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Whole-account figures. Cash and invested assets are shown separately.
 * totalAccountValue = cash + holdingsValue.
 */
public record PortfolioSummary(BigDecimal cash, BigDecimal holdingsValue,
                               BigDecimal totalAccountValue,
                               BigDecimal cashPercent, BigDecimal investedPercent,
                               BigDecimal totalCostBasis,
                               BigDecimal unrealizedProfitLoss, BigDecimal realizedProfitLoss,
                               BigDecimal returnPercent, int openPositions,
                               Map<AssetCategory, BigDecimal> allocationByCategory) { }