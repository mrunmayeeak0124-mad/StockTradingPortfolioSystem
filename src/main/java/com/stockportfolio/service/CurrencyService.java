package com.stockportfolio.service;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.model.ConversionResult;
import com.stockportfolio.model.ExchangeRate;
import com.stockportfolio.provider.ExchangeRateProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Converts amounts between currencies using any ExchangeRateProvider.
 * The result always carries the rate that was used and where it came from.
 */
public class CurrencyService {

    private final ExchangeRateProvider provider;

    public CurrencyService(ExchangeRateProvider provider) {
        if (provider == null) {
            throw new InvalidInputException("An exchange-rate provider is required.");
        }
        this.provider = provider;
    }

    public ConversionResult convert(BigDecimal amount, String from, String to)
            throws MarketDataException {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Enter an amount greater than zero.");
        }
        ExchangeRate rate = provider.getRate(from, to);
        // converted = amount x the DISPLAYED rate, so the numbers on screen can be checked by hand
        BigDecimal converted = amount.multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP);
        return new ConversionResult(amount, rate, converted);
    }

    public List<String> getSupportedCurrencies() {
        return provider.getSupportedCurrencies();
    }

    public String getSourceLabel() {
        return provider.getSourceLabel();
    }

    public boolean isSimulated() {
        return provider.isSimulated();
    }
}