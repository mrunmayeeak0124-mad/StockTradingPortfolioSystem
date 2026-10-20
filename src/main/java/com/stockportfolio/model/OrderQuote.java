package com.stockportfolio.model;

import java.math.BigDecimal;

/**
 * A preview of an order before the user confirms it.
 * netAmount is the TOTAL PAYABLE for a buy and the EXPECTED PROCEEDS for a sell.
 */
public record OrderQuote(TransactionType side, Asset asset, int quantity,
                         BigDecimal price, BigDecimal grossValue,
                         BigDecimal fee, BigDecimal netAmount) {

    /** Text for the screen label. */
    public String netLabel() {
        return side == TransactionType.BUY ? "Total payable" : "Expected proceeds";
    }
}