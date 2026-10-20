package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;

import java.math.BigDecimal;

/**
 * An Exchange Traded Fund: a basket of many investments traded like one share.
 */
public class ETF extends Asset {

    private final String ticker;
    private final String fundCategory;
    private final String trackingIndex;

    public ETF(String assetId, String name, String ticker, String currency,
               String fundCategory, String trackingIndex, BigDecimal currentPrice) {
        super(assetId, name, AssetCategory.ETF, currency, currentPrice);
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidInputException("Ticker cannot be empty.");
        }
        this.ticker = ticker.trim().toUpperCase();
        this.fundCategory = fundCategory == null ? "N/A" : fundCategory.trim();
        this.trackingIndex = trackingIndex == null ? "N/A" : trackingIndex.trim();
    }

    public String getTicker() { return ticker; }
    public String getFundCategory() { return fundCategory; }
    public String getTrackingIndex() { return trackingIndex; }

    @Override
    public String getSymbol() {
        return ticker;
    }

    @Override
    public String getDetails() {
        return "ETF | Category: " + fundCategory + " | Tracks: " + trackingIndex;
    }
}