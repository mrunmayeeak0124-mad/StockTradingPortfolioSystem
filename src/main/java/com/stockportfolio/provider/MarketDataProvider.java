package com.stockportfolio.provider;

import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.MarketQuote;
import com.stockportfolio.model.PricePoint;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Where market prices come from. The rest of the application only knows THIS interface,
 * so the simulated provider can later be replaced by a real one without other changes.
 */
public interface MarketDataProvider {

    /** Current quote with the previous close. */
    MarketQuote getQuote(Asset asset) throws MarketDataException;

    /** Daily prices, oldest first. */
    List<PricePoint> getPriceHistory(String assetId) throws MarketDataException;

    /** The newest price for each asset (key = asset ID). Changes nothing by itself. */
    Map<String, BigDecimal> fetchLatestPrices(Collection<Asset> assets) throws MarketDataException;

    /** True if the data is made up for demonstration. */
    boolean isSimulated();

    /** Text shown next to prices, such as "SIMULATED - ...". */
    String getSourceLabel();
}