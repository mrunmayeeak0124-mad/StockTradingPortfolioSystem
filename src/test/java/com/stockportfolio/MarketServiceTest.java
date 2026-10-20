package com.stockportfolio;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.MarketQuote;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.OrderStatus;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.TransactionType;
import com.stockportfolio.model.Watchlist;
import com.stockportfolio.provider.MarketDataProvider;
import com.stockportfolio.provider.MockMarketDataProvider;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.repository.PriceHistoryRepository;
import com.stockportfolio.repository.WatchlistRepository;
import com.stockportfolio.service.AccountService;
import com.stockportfolio.service.MarketService;
import com.stockportfolio.service.TradingService;
import com.stockportfolio.util.FeeCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the market service. Each test uses its own temporary database.
 * Setup: cash 10000.00, asset A001 priced at 100.00.
 */
class MarketServiceTest {

    /** A tiny fake provider: proves MarketService works with ANY MarketDataProvider (interface). */
    private static class FixedPriceProvider implements MarketDataProvider {
        private final String assetId;
        private final BigDecimal price;
        private final boolean failing;

        FixedPriceProvider(String assetId, BigDecimal price, boolean failing) {
            this.assetId = assetId;
            this.price = price;
            this.failing = failing;
        }

        @Override
        public MarketQuote getQuote(Asset asset) throws MarketDataException {
            throw new MarketDataException("Not needed in this test.");
        }

        @Override
        public List<PricePoint> getPriceHistory(String id) throws MarketDataException {
            throw new MarketDataException("Not needed in this test.");
        }

        @Override
        public Map<String, BigDecimal> fetchLatestPrices(Collection<Asset> assets) throws MarketDataException {
            if (failing) {
                throw new MarketDataException("Market data is unavailable.");
            }
            return Map.of(assetId, price);
        }

        @Override
        public boolean isSimulated() {
            return true;
        }

        @Override
        public String getSourceLabel() {
            return "TEST DATA";
        }
    }

    private Map<String, Asset> assets;
    private TradingService trading;
    private Trader trader;
    private Asset stock;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        DatabaseManager.setDatabasePath(tempDir.resolve("test.db").toString());
        DatabaseManager.initialize();
        FeeCalculator.setFeeRatePercent(new BigDecimal("0.10"));

        assets = new AssetRepository().findAllAsMap();
        trader = new AccountService().createTrader("Test Trader", "", new BigDecimal("10000.00"));
        trading = new TradingService(trader, assets);
        stock = assets.get("A001");
        trading.updateAssetPrice(stock, new BigDecimal("100.00"));
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.setDatabasePath("database/stocktrading.db");
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }

    // 1
    @Test
    void refreshAppliesPricesAndExecutesPendingLimitOrder() throws TradingException, MarketDataException {
        Order order = trading.placeLimitOrder(TransactionType.BUY, stock, 5, new BigDecimal("95.00"));
        assertEquals(OrderStatus.PENDING, order.getStatus());

        MarketService market = new MarketService(
                new FixedPriceProvider("A001", new BigDecimal("94.00"), false), trading);
        List<Order> changed = market.refreshPrices();

        assertEquals(1, changed.size());
        assertEquals(OrderStatus.EXECUTED, order.getStatus());
        assertMoney("94.00", stock.getCurrentPrice());
        assertEquals(5, trading.getTrader().getPortfolio().getQuantityHeld("A001"));

        List<PricePoint> history = new PriceHistoryRepository().findByAsset("A001");
        assertMoney("94.00", history.get(history.size() - 1).price());   // today's history point was updated
        assertNotNull(market.getLastUpdated());
        assertEquals("TEST DATA", market.getSourceLabel());
    }

    // 2
    @Test
    void providerFailureChangesNothing() throws TradingException {
        Order order = trading.placeLimitOrder(TransactionType.BUY, stock, 5, new BigDecimal("95.00"));
        MarketService market = new MarketService(
                new FixedPriceProvider("A001", new BigDecimal("94.00"), true), trading);

        assertThrows(MarketDataException.class, market::refreshPrices);

        assertMoney("100.00", stock.getCurrentPrice());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    // 3
    @Test
    void watchlistAddRemoveAndPersist() {
        MarketService market = new MarketService(new MockMarketDataProvider(), trading);
        String traderId = trader.getUserId();

        market.addToWatchlist(traderId, assets.get("A001"));
        market.addToWatchlist(traderId, assets.get("A002"));
        market.addToWatchlist(traderId, assets.get("A001"));           // duplicate: ignored
        assertEquals(2, market.getWatchlistAssets(traderId).size());

        market.removeFromWatchlist(traderId, assets.get("A001"));

        Watchlist saved = new WatchlistRepository().findByTrader(traderId);   // read straight from the database
        assertEquals(1, saved.size());
        assertTrue(saved.contains("A002"));
        assertFalse(saved.contains("A001"));
    }

    // 4
    @Test
    void searchFiltersByTextAndCategory() {
        MarketService market = new MarketService(new MockMarketDataProvider(), trading);

        List<Asset> byName = market.searchAssets("rel", null);
        assertEquals(1, byName.size());
        assertEquals("A001", byName.get(0).getAssetId());

        assertEquals("A002", market.searchAssets("tcs", null).get(0).getAssetId());   // ticker, any capitals
        assertEquals(3, market.searchAssets("", AssetCategory.ETF).size());
        assertEquals(2, market.searchAssets("govt", null).size());
        assertTrue(market.searchAssets("nifty", AssetCategory.STOCK).isEmpty());
        assertEquals(assets.size(), market.searchAssets(null, null).size());
    }

    // 5
    @Test
    void mockPriceMovesStayWithinLimits() throws MarketDataException {
        MockMarketDataProvider provider = new MockMarketDataProvider(new Random(42));

        Map<String, BigDecimal> latest = provider.fetchLatestPrices(assets.values());

        assertEquals(assets.size(), latest.size());
        for (Asset asset : assets.values()) {
            BigDecimal newPrice = latest.get(asset.getAssetId());
            assertTrue(newPrice.signum() > 0);
            assertEquals(2, newPrice.scale());
            double move = Math.abs(newPrice.doubleValue() / asset.getCurrentPrice().doubleValue() - 1);
            double limit = asset.getCategory() == AssetCategory.BOND ? 0.0031 : 0.0201;   // 0.3% / 2% plus rounding
            assertTrue(move <= limit, asset.getSymbol() + " moved " + move);
        }
    }

    // 6
    @Test
    void mockQuoteIsLabelledAsSimulated() throws MarketDataException {
        MockMarketDataProvider provider = new MockMarketDataProvider();

        MarketQuote quote = provider.getQuote(stock);

        assertTrue(quote.simulated());
        assertTrue(quote.sourceLabel().contains("SIMULATED"));
        assertMoney("100.00", quote.price());
        assertTrue(quote.previousClose().signum() > 0);
        assertEquals(0, quote.price().subtract(quote.previousClose()).compareTo(quote.change()));
        assertTrue(provider.isSimulated());
    }
}