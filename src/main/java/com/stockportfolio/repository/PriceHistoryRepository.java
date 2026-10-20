package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.PricePoint;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads the (simulated) daily price history of assets.
 */
public class PriceHistoryRepository {

    public void save(Connection connection, String assetId, LocalDate date, BigDecimal price) {
        String sql = """
                INSERT INTO price_history (asset_id, price_date, price) VALUES (?, ?, ?)
                ON CONFLICT(asset_id, price_date) DO UPDATE SET price = excluded.price
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, assetId);
            ps.setString(2, date.toString());
            ps.setString(3, price.toPlainString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save price history: " + e.getMessage(), e);
        }
    }

    /** Oldest first. Returns an empty list if there is no history. */
    public List<PricePoint> findByAsset(String assetId) {
        List<PricePoint> history = new ArrayList<>();
        String sql = "SELECT price_date, price FROM price_history "
                + "WHERE asset_id = ? ORDER BY price_date";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new PricePoint(LocalDate.parse(rs.getString("price_date")),
                            new BigDecimal(rs.getString("price"))));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load price history: " + e.getMessage(), e);
        }
        return history;
    }
}