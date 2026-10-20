package com.stockportfolio.exception;

/**
 * Base class for problems that happen while trading.
 * It is a CHECKED exception: Java forces us to handle it.
 */
public class TradingException extends Exception {

    public TradingException(String message) {
        super(message);
    }
}