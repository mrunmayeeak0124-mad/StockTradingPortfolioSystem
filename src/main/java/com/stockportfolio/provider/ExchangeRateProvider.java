package com.stockportfolio.provider;

import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.model.ExchangeRate;

import java.util.List;

/**
 * Where exchange rates come from (mock now, possibly a live source later).
 */
public interface ExchangeRateProvider {

    ExchangeRate getRate(String fromCurrency, String toCurrency) throws MarketDataException;

    List<String> getSupportedCurrencies();

    String getSourceLabel();

    boolean isSimulated();
}