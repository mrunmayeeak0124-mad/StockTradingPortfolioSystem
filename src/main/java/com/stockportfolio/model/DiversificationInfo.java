package com.stockportfolio.model;

import java.math.BigDecimal;

/**
 * A plain-language diversification check of the portfolio.
 */
public record DiversificationInfo(Level level, int positionCount, int categoryCount,
                                  String largestHoldingSymbol, BigDecimal largestHoldingPercent,
                                  String message) {

    public enum Level {
        NO_HOLDINGS,
        CONCENTRATED,
        MODERATE,
        DIVERSIFIED
    }
}