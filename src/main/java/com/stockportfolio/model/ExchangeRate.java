package com.stockportfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 unit of fromCurrency = rate units of toCurrency.
 */
public record ExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate,
                           String sourceLabel, boolean simulated, LocalDateTime lastUpdated) { }