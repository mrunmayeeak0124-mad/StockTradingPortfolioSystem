package com.stockportfolio.provider;

import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.model.ExchangeRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SIMULATED exchange rates that work offline. They are NOT real market rates.
 * Every currency is stored as "how many INR is one unit worth", and any pair is worked out from that.
 */
public class MockExchangeRateProvider implements ExchangeRateProvider {

    public static final String SOURCE_LABEL = "SIMULATED - mock exchange rates (not live rates)";

    private static final Map<String, BigDecimal> INR_PER_UNIT = new LinkedHashMap<>();

    static {
        INR_PER_UNIT.put("INR", new BigDecimal("1.00"));
        INR_PER_UNIT.put("USD", new BigDecimal("88.50"));
        INR_PER_UNIT.put("EUR", new BigDecimal("102.40"));
        INR_PER_UNIT.put("GBP", new BigDecimal("118.90"));
        INR_PER_UNIT.put("JPY", new BigDecimal("0.59"));
    }

    private final LocalDateTime lastUpdated = LocalDateTime.now();

    @Override
    public ExchangeRate getRate(String fromCurrency, String toCurrency) throws MarketDataException {
        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);
        BigDecimal inrFrom = INR_PER_UNIT.get(from);
        BigDecimal inrTo = INR_PER_UNIT.get(to);
        if (inrFrom == null) {
            throw new MarketDataException("Currency not supported: " + from);
        }
        if (inrTo == null) {
            throw new MarketDataException("Currency not supported: " + to);
        }
        BigDecimal rate = inrFrom.divide(inrTo, 6, RoundingMode.HALF_UP);
        return new ExchangeRate(from, to, rate, SOURCE_LABEL, true, lastUpdated);
    }

    @Override
    public List<String> getSupportedCurrencies() {
        return new ArrayList<>(INR_PER_UNIT.keySet());
    }

    @Override
    public String getSourceLabel() {
        return SOURCE_LABEL;
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}