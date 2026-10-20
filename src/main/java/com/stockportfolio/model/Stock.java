package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;

import java.math.BigDecimal;

/**
 * A company share. Adds ticker, exchange and sector to the common Asset fields.
 */
public class Stock extends Asset {

    private final String ticker;
    private final String exchange;
    private final String sector;

    public Stock(String assetId, String name, String ticker, String exchange,
                 String currency, String sector, BigDecimal currentPrice) {
        super(assetId, name, AssetCategory.STOCK, currency, currentPrice);
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidInputException("Ticker cannot be empty.");
        }
        this.ticker = ticker.trim().toUpperCase();
        this.exchange = exchange == null ? "N/A" : exchange.trim();
        this.sector = sector == null ? "N/A" : sector.trim();
    }

    public String getTicker() { return ticker; }
    public String getExchange() { return exchange; }
    public String getSector() { return sector; }

    @Override
    public String getSymbol() {
        return ticker;
    }

    @Override
    public String getDetails() {
        return "Stock | Exchange: " + exchange + " | Sector: " + sector;
    }
}