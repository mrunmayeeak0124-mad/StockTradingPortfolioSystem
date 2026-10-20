package com.stockportfolio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One place for money rounding, so every class rounds the same way.
 */
public final class MoneyUtil {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MoneyUtil() { }

    /** Round to 2 decimal places (HALF_UP). */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** part / whole x 100, safe when whole is zero. */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return round(part.multiply(HUNDRED).divide(whole, 4, RoundingMode.HALF_UP));
    }
}