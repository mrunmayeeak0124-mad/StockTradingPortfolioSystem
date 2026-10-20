package com.stockportfolio.exception;

/**
 * Thrown when market data or an exchange rate is not available
 * (no history, unsupported currency, provider failure).
 * It is checked, so the screens must show a message instead of crashing.
 */
public class MarketDataException extends Exception {

    public MarketDataException(String message) {
        super(message);
    }
}