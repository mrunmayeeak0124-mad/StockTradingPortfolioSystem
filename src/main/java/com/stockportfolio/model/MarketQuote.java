package com.stockportfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A price quote with its source. 'simulated' is true for demonstration data.
 */
public record MarketQuote(String assetId, BigDecimal price, BigDecimal previousClose,
                          BigDecimal change, BigDecimal changePercent,
                          LocalDateTime timestamp, String sourceLabel, boolean simulated) { }