package com.stockportfolio.exception;

/**
 * Thrown when the trader tries to sell more units than they own.
 */
public class InsufficientHoldingsException extends TradingException {

    public InsufficientHoldingsException(String message) {
        super(message);
    }
}