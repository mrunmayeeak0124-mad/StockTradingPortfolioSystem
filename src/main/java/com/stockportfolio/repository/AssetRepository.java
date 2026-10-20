package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.Bond;
import com.stockportfolio.model.ETF;
import com.stockportfolio.model.Stock;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves and loads assets (Stock, ETF, Bond). All three kinds share one table;
 * columns that do not apply to a kind stay empty (NULL).
 */
public class AssetRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO assets (asset_id, category, name, symbol, currency, current_price,
                                exchange, sector, fund_category, tracking_index,
                                issuer, face_value, coupon_rate, maturity_date, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(asset_id) DO UPDATE SET
                name = excluded.name,
                current_price = excluded.current_price,
                last_updated = excluded.last_updated
            """;

    /** Insert a new asset or update its name and price. Uses the caller's connection. */
    public void save(Connection connection, Asset asset) {
        String exchange = null;
        String sector = null;
        String fundCategory = null;
        String trackingIndex = null;
        String issuer = null;
        String faceValue = null;
        String couponRate = null;
        String maturityDate = null;

        // Persistence needs to know each kind's extra fields, so we check the type here.
        if (asset instanceof Stock stock) {
            exchange = stock.getExchange();
            sector = stock.getSector();
        } else if (asset instanceof ETF etf) {
            fundCategory = etf.getFundCategory();
            trackingIndex = etf.getTrackingIndex();
        } else if (asset instanceof Bond bond) {
            issuer = bond.getIssuer();
            faceValue = bond.getFaceValue().toPlainString();
            couponRate = bond.getCouponRate().toPlainString();
            maturityDate = bond.getMaturityDate().toString();
        }

        try (PreparedStatement ps = connection.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, asset.getAssetId());
            ps.setString(2, asset.getCategory().name());
            ps.setString(3, asset.getName());
            ps.setString(4, asset.getSymbol());
            ps.setString(5, asset.getCurrency());
            ps.setString(6, asset.getCurrentPrice().toPlainString());
            ps.setString(7, exchange);
            ps.setString(8, sector);
            ps.setString(9, fundCategory);
            ps.setString(10, trackingIndex);
            ps.setString(11, issuer);
            ps.setString(12, faceValue);
            ps.setString(13, couponRate);
            ps.setString(14, maturityDate);
            ps.setString(15, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not save asset " + asset.getAssetId() + ": " + e.getMessage(), e);
        }
    }

    /** Update only the stored price of one asset. */
    public void updatePrice(String assetId, BigDecimal newPrice) {
        String sql = "UPDATE assets SET current_price = ?, last_updated = ? WHERE asset_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPrice.toPlainString());
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, assetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not update price: " + e.getMessage(), e);
        }
    }

    public List<Asset> findAll() {
        List<Asset> assets = new ArrayList<>();
        String sql = "SELECT * FROM assets ORDER BY asset_id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                assets.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load assets: " + e.getMessage(), e);
        }
        return assets;
    }

    /**
     * Assets keyed by ID. Everything that loads later (positions, orders, transactions)
     * uses THIS map, so all of them point to the same Asset objects and see the same prices.
     */
    public Map<String, Asset> findAllAsMap() {
        Map<String, Asset> map = new LinkedHashMap<>();
        for (Asset asset : findAll()) {
            map.put(asset.getAssetId(), asset);
        }
        return map;
    }

    private Asset mapRow(ResultSet rs) throws SQLException {
        AssetCategory category = AssetCategory.valueOf(rs.getString("category"));
        String id = rs.getString("asset_id");
        String name = rs.getString("name");
        String symbol = rs.getString("symbol");
        String currency = rs.getString("currency");
        BigDecimal price = new BigDecimal(rs.getString("current_price"));

        return switch (category) {
            case STOCK -> new Stock(id, name, symbol, rs.getString("exchange"),
                    currency, rs.getString("sector"), price);
            case ETF -> new ETF(id, name, symbol, currency,
                    rs.getString("fund_category"), rs.getString("tracking_index"), price);
            case BOND -> new Bond(id, name, rs.getString("issuer"), currency,
                    new BigDecimal(rs.getString("face_value")),
                    new BigDecimal(rs.getString("coupon_rate")),
                    LocalDate.parse(rs.getString("maturity_date")), price);
        };
    }
}