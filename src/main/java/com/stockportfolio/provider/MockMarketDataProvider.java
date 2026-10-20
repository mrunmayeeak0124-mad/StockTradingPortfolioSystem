package com.stockportfolio.provider;

import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.MarketQuote;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.repository.PriceHistoryRepository;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * SIMULATED market data for offline demonstrations. Nothing here is a live price.
 * fetchLatestPrices makes each price move randomly by at most 2% (bonds: 0.3%).
 */
public class MockMarketDataProvider implements MarketDataProvider {

    public static final String SOURCE_LABEL = "SIMULATED - mock market data (not live prices)";

    private static final double MAX_MOVE_PERCENT = 2.0;
    private static final double MAX_BOND_MOVE_PERCENT = 0.3;

    private final PriceHistoryRepository historyRepository = new PriceHistoryRepository();
    private final Random random;

    public MockMarketDataProvider() {
        this(new Random());
    }

    /** A seeded Random gives repeatable moves (useful for tests). */
    public MockMarketDataProvider(Random random) {
        this.random = random;
    }

    @Override
    public List<PricePoint> getPriceHistory(String assetId) throws MarketDataException {
        List<PricePoint> history = historyRepository.findByAsset(assetId);
        if (history.isEmpty()) {
            throw new MarketDataException("No price history is available for asset " + assetId + ".");
        }
        return history;
    }

    @Override
    public MarketQuote getQuote(Asset asset) throws MarketDataException {
        if (asset == null || asset.getCurrentPrice() == null || asset.getCurrentPrice().signum() <= 0) {
            throw new MarketDataException("A price is not available for this asset.");
        }
        BigDecimal price = asset.getCurrentPrice();
        BigDecimal previousClose = price;
        LocalDate today = LocalDate.now();
        for (PricePoint point : historyRepository.findByAsset(asset.getAssetId())) {
            if (point.date().isBefore(today)) {
                previousClose = point.price();      // history is oldest first, so the last match wins
            }
        }
        BigDecimal change = price.subtract(previousClose);
        return new MarketQuote(asset.getAssetId(), price, previousClose, change,
                MoneyUtil.percent(change, previousClose), LocalDateTime.now(), SOURCE_LABEL, true);
    }

    @Override
    public Map<String, BigDecimal> fetchLatestPrices(Collection<Asset> assets) throws MarketDataException {
        Map<String, BigDecimal> latest = new LinkedHashMap<>();
        for (Asset asset : assets) {
            double maxMove = asset.getCategory() == AssetCategory.BOND
                    ? MAX_BOND_MOVE_PERCENT : MAX_MOVE_PERCENT;
            double movePercent = (random.nextDouble() * 2 - 1) * maxMove;      // between -max and +max
            BigDecimal newPrice = asset.getCurrentPrice()
                    .multiply(BigDecimal.valueOf(1 + movePercent / 100))
                    .setScale(2, RoundingMode.HALF_UP);
            if (newPrice.signum() <= 0) {
                newPrice = new BigDecimal("0.01");
            }
            latest.put(asset.getAssetId(), newPrice);
        }
        return latest;
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public String getSourceLabel() {
        return SOURCE_LABEL;
    }
}