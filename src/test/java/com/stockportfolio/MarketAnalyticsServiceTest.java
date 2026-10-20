package com.stockportfolio;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.model.ConversionResult;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.provider.MockExchangeRateProvider;
import com.stockportfolio.service.CurrencyService;
import com.stockportfolio.service.MarketAnalyticsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the price statistics and the currency converter. No database is needed.
 * Sample prices: 100, 102, 101, 105, 103, 110.
 */
class MarketAnalyticsServiceTest {

    private final MarketAnalyticsService analytics = new MarketAnalyticsService();
    private final CurrencyService currency = new CurrencyService(new MockExchangeRateProvider());

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }

    private static List<PricePoint> sampleHistory() {
        String[] prices = {"100.00", "102.00", "101.00", "105.00", "103.00", "110.00"};
        LocalDate start = LocalDate.of(2026, 10, 1);
        List<PricePoint> history = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            history.add(new PricePoint(start.plusDays(i), new BigDecimal(prices[i])));
        }
        return history;
    }

    // 1
    @Test
    void priceChangeAndPercentChange() {
        List<PricePoint> history = sampleHistory();

        assertMoney("7.00", analytics.priceChange(history).orElseThrow());               // 110 - 103
        assertMoney("6.80", analytics.percentChange(history, 1).orElseThrow());          // 7 / 103 x 100
        assertMoney("10.00", analytics.percentChange(history, 5).orElseThrow());         // (110 - 100) / 100 x 100
        assertTrue(analytics.percentChange(history, 6).isEmpty());                       // needs 7 observations
    }

    // 2
    @Test
    void simpleMovingAverageNeedsEnoughObservations() {
        List<PricePoint> history = sampleHistory();

        List<PricePoint> sma = analytics.simpleMovingAverage(history, 3);

        assertEquals(4, sma.size());                       // 6 - 3 + 1
        assertMoney("101.00", sma.get(0).price());         // (100 + 102 + 101) / 3
        assertMoney("102.67", sma.get(1).price());         // (102 + 101 + 105) / 3
        assertMoney("103.00", sma.get(2).price());         // (101 + 105 + 103) / 3
        assertMoney("106.00", sma.get(3).price());         // (105 + 103 + 110) / 3
        assertEquals(history.get(5).date(), sma.get(3).date());

        assertTrue(analytics.simpleMovingAverage(history, 7).isEmpty());   // not enough data
        assertTrue(analytics.simpleMovingAverage(history, 0).isEmpty());
    }

    // 3
    @Test
    void volatilityIsTheSpreadOfDailyChanges() {
        double volatility = analytics.volatilityPercent(sampleHistory()).orElseThrow().doubleValue();

        assertTrue(Math.abs(volatility - 3.57) < 0.05, "Volatility was " + volatility);   // worked out by hand: about 3.57%

        List<PricePoint> tooShort = sampleHistory().subList(0, 2);
        assertTrue(analytics.volatilityPercent(tooShort).isEmpty());
    }

    // 4
    @Test
    void emptyOrMissingHistoryIsHandledSafely() {
        assertTrue(analytics.priceChange(List.of()).isEmpty());
        assertTrue(analytics.percentChange(List.of(), 1).isEmpty());
        assertTrue(analytics.simpleMovingAverage(List.of(), 3).isEmpty());
        assertTrue(analytics.volatilityPercent(List.of()).isEmpty());
        assertTrue(analytics.priceChange(null).isEmpty());
        assertTrue(analytics.simpleMovingAverage(null, 3).isEmpty());
    }

    // 5
    @Test
    void currencyConversionShowsTheRateUsed() throws MarketDataException {
        ConversionResult usdToInr = currency.convert(new BigDecimal("100"), "USD", "INR");
        assertMoney("88.500000", usdToInr.rate().rate());
        assertMoney("8850.00", usdToInr.convertedAmount());                  // 100 x 88.50
        assertTrue(usdToInr.rate().simulated());
        assertTrue(usdToInr.rate().sourceLabel().contains("SIMULATED"));
        assertNotNull(usdToInr.rate().lastUpdated());

        ConversionResult inrToUsd = currency.convert(new BigDecimal("1000"), "inr", "usd");   // lower case is accepted
        assertMoney("0.011299", inrToUsd.rate().rate());                     // 1 / 88.50
        assertMoney("11.30", inrToUsd.convertedAmount());                    // 1000 x 0.011299 = 11.299

        ConversionResult same = currency.convert(new BigDecimal("250.00"), "INR", "INR");
        assertMoney("1.000000", same.rate().rate());
        assertMoney("250.00", same.convertedAmount());

        assertThrows(InvalidInputException.class, () -> currency.convert(BigDecimal.ZERO, "USD", "INR"));
        assertThrows(InvalidInputException.class, () -> currency.convert(new BigDecimal("-5"), "USD", "INR"));
        assertThrows(InvalidInputException.class, () -> currency.convert(null, "USD", "INR"));
        assertThrows(MarketDataException.class, () -> currency.convert(new BigDecimal("10"), "XYZ", "INR"));
    }
}