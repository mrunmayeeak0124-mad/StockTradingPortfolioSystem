package com.stockportfolio.model;

import java.math.BigDecimal;

/**
 * Overall performance. netProfitLoss = accountValue - totalDeposited, and it equals
 * realizedProfitLoss + unrealizedProfitLoss. Past performance does not guarantee future results.
 */
public record PerformanceSummary(BigDecimal totalDeposited, BigDecimal accountValue,
                                 BigDecimal netProfitLoss, BigDecimal returnOnDepositsPercent,
                                 BigDecimal realizedProfitLoss, BigDecimal unrealizedProfitLoss,
                                 BigDecimal totalFeesPaid, int tradeCount) { }