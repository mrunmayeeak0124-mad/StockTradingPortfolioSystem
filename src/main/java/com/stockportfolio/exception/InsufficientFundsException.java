package com.stockportfolio.exception;

import java.math.BigDecimal;

/**
 * Thrown when the trader does not have enough cash balance.
 */
public class InsufficientFundsException extends TradingException {

    public InsufficientFundsException(BigDecimal needed, BigDecimal available) {
        super("Insufficient cash balance. Needed " + needed + " but available " + available + ".");
    }
}