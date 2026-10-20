package com.stockportfolio.model;

import java.math.BigDecimal;

/**
 * The result of a currency conversion, including the exact rate that was used.
 */
public record ConversionResult(BigDecimal amount, ExchangeRate rate, BigDecimal convertedAmount) { }