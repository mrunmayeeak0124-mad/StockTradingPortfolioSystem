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
    private final String marketCap;
    private final BigDecimal bookValue;
    private final BigDecimal oneYearRoi;
    private final long totalVolume;
    private final BigDecimal peRatio;
    private final BigDecimal fiftyTwoWeekHigh;
    private final BigDecimal fiftyTwoWeekLow;

    public Stock(String assetId, String name, String ticker, String exchange,
                 String currency, String sector, BigDecimal currentPrice) {
        this(assetId, name, ticker, exchange, currency, sector, currentPrice,
                null, null, null, 0, null, null, null);
    }

    public Stock(String assetId, String name, String ticker, String exchange,
                 String currency, String sector, BigDecimal currentPrice,
                 String marketCap, BigDecimal bookValue, BigDecimal oneYearRoi,
                 long totalVolume, BigDecimal peRatio, BigDecimal fiftyTwoWeekHigh, BigDecimal fiftyTwoWeekLow) {
        super(assetId, name, AssetCategory.STOCK, currency, currentPrice);
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidInputException("Ticker cannot be empty.");
        }
        this.ticker = ticker.trim().toUpperCase();
        this.exchange = exchange == null ? "NSE" : exchange.trim();
        this.sector = sector == null ? "General" : sector.trim();

        // Calculate or assign realistic institutional fundamentals
        double priceVal = currentPrice.doubleValue();
        this.marketCap = marketCap != null ? marketCap : computeDefaultMarketCap(this.ticker, priceVal);
        this.bookValue = bookValue != null ? bookValue : BigDecimal.valueOf(priceVal * 0.28).setScale(2, java.math.RoundingMode.HALF_UP);
        this.oneYearRoi = oneYearRoi != null ? oneYearRoi : BigDecimal.valueOf(14.50).setScale(2, java.math.RoundingMode.HALF_UP);
        this.totalVolume = totalVolume > 0 ? totalVolume : (long) (priceVal * 1250L + 850000L);
        this.peRatio = peRatio != null ? peRatio : BigDecimal.valueOf(25.40).setScale(1, java.math.RoundingMode.HALF_UP);
        this.fiftyTwoWeekHigh = fiftyTwoWeekHigh != null ? fiftyTwoWeekHigh : currentPrice.multiply(BigDecimal.valueOf(1.18)).setScale(2, java.math.RoundingMode.HALF_UP);
        this.fiftyTwoWeekLow = fiftyTwoWeekLow != null ? fiftyTwoWeekLow : currentPrice.multiply(BigDecimal.valueOf(0.82)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String computeDefaultMarketCap(String sym, double price) {
        if ("RELIANCE".equalsIgnoreCase(sym)) return "₹16.85 Lakh Cr";
        if ("TCS".equalsIgnoreCase(sym)) return "₹14.15 Lakh Cr";
        if ("HDFCBANK".equalsIgnoreCase(sym)) return "₹12.40 Lakh Cr";
        if ("INFY".equalsIgnoreCase(sym)) return "₹6.35 Lakh Cr";
        if ("ITC".equalsIgnoreCase(sym)) return "₹5.80 Lakh Cr";
        if ("TATAMOTORS".equalsIgnoreCase(sym)) return "₹3.60 Lakh Cr";
        double cr = (price * 850000000.0) / 10000000.0;
        return String.format("₹%.2f Cr", cr);
    }

    public String getTicker() { return ticker; }
    public String getExchange() { return exchange; }
    public String getSector() { return sector; }
    public String getMarketCap() { return marketCap; }
    public BigDecimal getBookValue() { return bookValue; }
    public BigDecimal getOneYearRoi() { return oneYearRoi; }
    public long getTotalVolume() { return totalVolume; }
    public BigDecimal getPeRatio() { return peRatio; }
    public BigDecimal getFiftyTwoWeekHigh() { return fiftyTwoWeekHigh; }
    public BigDecimal getFiftyTwoWeekLow() { return fiftyTwoWeekLow; }

    @Override
    public String getSymbol() {
        return ticker;
    }

    @Override
    public String getDetails() {
        return "Stock | Exchange: " + exchange + " | Sector: " + sector + " | MCap: " + marketCap;
    }
}