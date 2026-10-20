package com.stockportfolio.exception;

import java.math.BigDecimal;

/**
 * Thrown when the trader does not have enough virtual cash.
 */
public class InsufficientFundsException extends TradingException {

    public InsufficientFundsException(BigDecimal needed, BigDecimal available) {
        super("Insufficient virtual cash. Needed " + needed + " but available " + available + ".");
    }
}