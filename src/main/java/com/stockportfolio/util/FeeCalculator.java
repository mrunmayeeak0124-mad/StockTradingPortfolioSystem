package com.stockportfolio.util;

import com.stockportfolio.exception.InvalidInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates the SIMULATED transaction fee (not a real brokerage charge).
 * The rate can be changed, so it is configurable.
 */
public final class FeeCalculator {

    private static BigDecimal feeRatePercent = new BigDecimal("0.10");

    private FeeCalculator() { }

    public static BigDecimal getFeeRatePercent() {
        return feeRatePercent;
    }

    public static void setFeeRatePercent(BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidInputException("Fee rate must be between 0 and 100 percent.");
        }
        feeRatePercent = rate;
    }

    /** fee = grossValue x rate / 100, rounded to 2 decimals. */
    public static BigDecimal calculateFee(BigDecimal grossValue) {
        if (grossValue == null || grossValue.signum() < 0) {
            throw new InvalidInputException("Gross value cannot be negative.");
        }
        return grossValue.multiply(feeRatePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}