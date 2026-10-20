package com.stockportfolio.service;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.exception.MarketDataException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.MarketQuote;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.model.Watchlist;
import com.stockportfolio.provider.MarketDataProvider;
import com.stockportfolio.repository.PriceHistoryRepository;
import com.stockportfolio.repository.WatchlistRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Market Explorer's logic: search and filter, history and quotes, price refresh,
 * and the watchlist. It talks to the provider only through the MarketDataProvider interface.
 */
public class MarketService {

    private final MarketDataProvider provider;
    private final TradingService tradingService;
    private final PriceHistoryRepository historyRepository = new PriceHistoryRepository();
    private final WatchlistRepository watchlistRepository = new WatchlistRepository();
    private LocalDateTime lastUpdated = LocalDateTime.now();    // when prices were loaded or last refreshed

    public MarketService(MarketDataProvider provider, TradingService tradingService) {
        if (provider == null || tradingService == null) {
            throw new InvalidInputException("A market-data provider and a trading service are required.");
        }
        this.provider = provider;
        this.tradingService = tradingService;
    }

    // ------------------------------------------------------------------ explorer

    /**
     * Filters by name or ticker text (ignoring capital letters) and optionally by category.
     * Empty text means "no text filter"; a null category means "all categories".
     */
    public List<Asset> searchAssets(String text, AssetCategory category) {
        String query = text == null ? "" : text.trim().toLowerCase();
        List<Asset> result = new ArrayList<>();
        for (Asset asset : tradingService.getAssets().values()) {
            if (category != null && asset.getCategory() != category) {
                continue;
            }
            if (query.isEmpty()
                    || asset.getName().toLowerCase().contains(query)
                    || asset.getSymbol().toLowerCase().contains(query)) {
                result.add(asset);
            }
        }
        return result;
    }

    public MarketQuote getQuote(Asset asset) throws MarketDataException {
        return provider.getQuote(asset);
    }

    public List<PricePoint> getPriceHistory(Asset asset) {
        if (asset == null) {
            return List.of();
        }
        try {
            return provider.getPriceHistory(asset.getAssetId());
        } catch (MarketDataException e) {
            return List.of();
        }
    }

    public List<PricePoint> getPriceHistory(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return List.of();
        }
        try {
            return provider.getPriceHistory(assetId);
        } catch (MarketDataException e) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------ refresh

    /**
     * Gets the latest prices from the provider, applies them (saving each one and today's
     * history point) and re-checks pending orders. If the provider fails, NOTHING changes.
     * Returns the orders whose status changed.
     */
    public List<Order> refreshPrices() throws MarketDataException {
        Map<String, BigDecimal> latest = provider.fetchLatestPrices(tradingService.getAssets().values());

        List<Order> changed = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : latest.entrySet()) {
            Asset asset = tradingService.getAssets().get(entry.getKey());
            if (asset == null) {
                continue;
            }
            changed.addAll(tradingService.updateAssetPrice(asset, entry.getValue()));
            recordTodayPrice(asset);
        }
        lastUpdated = LocalDateTime.now();
        return changed;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getSourceLabel() {
        return provider.getSourceLabel();
    }

    public boolean isSimulated() {
        return provider.isSimulated();
    }

    // ------------------------------------------------------------------ watchlist

    public Watchlist getWatchlist(String traderId) {
        return watchlistRepository.findByTrader(traderId);
    }

    public boolean isInWatchlist(String traderId, String assetId) {
        return getWatchlist(traderId).contains(assetId);
    }

    public boolean toggleWatchlist(String traderId, String assetId) {
        if (isInWatchlist(traderId, assetId)) {
            watchlistRepository.remove(traderId, assetId);
            return false;
        } else {
            watchlistRepository.add(traderId, assetId);
            return true;
        }
    }

    public void addToWatchlist(String traderId, Asset asset) {
        if (asset == null) {
            throw new InvalidInputException("Please select an asset.");
        }
        watchlistRepository.add(traderId, asset.getAssetId());
    }

    public void removeFromWatchlist(String traderId, Asset asset) {
        if (asset == null) {
            throw new InvalidInputException("Please select an asset.");
        }
        watchlistRepository.remove(traderId, asset.getAssetId());
    }

    public List<Asset> getWatchlistAssets(String traderId) {
        List<Asset> assets = new ArrayList<>();
        for (String assetId : getWatchlist(traderId).getAssetIds()) {
            Asset asset = tradingService.getAssets().get(assetId);
            if (asset != null) {
                assets.add(asset);
            }
        }
        return assets;
    }

    // ------------------------------------------------------------------ helper

    /** Saves today's price in the history so charts include the refreshed price. */
    private void recordTodayPrice(Asset asset) {
        try (Connection connection = DatabaseManager.getConnection()) {
            historyRepository.save(connection, asset.getAssetId(), LocalDate.now(),
                    asset.getCurrentPrice());
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the price history: " + e.getMessage(), e);
        }
    }
}