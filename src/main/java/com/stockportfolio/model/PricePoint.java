package com.stockportfolio.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One historical price: a date and the (simulated) price on that date.
 * A record is a short way to write a small, unchangeable data class.
 */
public record PricePoint(LocalDate date, BigDecimal price) { }