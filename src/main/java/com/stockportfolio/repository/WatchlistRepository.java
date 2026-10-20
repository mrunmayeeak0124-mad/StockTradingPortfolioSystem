package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.Watchlist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Saves and loads each trader's watchlist.
 */
public class WatchlistRepository {

    public Watchlist findByTrader(String traderId) {
        Watchlist watchlist = new Watchlist();
        String sql = "SELECT asset_id FROM watchlist WHERE trader_id = ? ORDER BY rowid";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    watchlist.add(rs.getString("asset_id"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the watchlist: " + e.getMessage(), e);
        }
        return watchlist;
    }

    /** Adding the same asset twice does nothing (INSERT OR IGNORE). */
    public void add(String traderId, String assetId) {
        run("INSERT OR IGNORE INTO watchlist (trader_id, asset_id) VALUES (?, ?)", traderId, assetId);
    }

    public void remove(String traderId, String assetId) {
        run("DELETE FROM watchlist WHERE trader_id = ? AND asset_id = ?", traderId, assetId);
    }

    private void run(String sql, String traderId, String assetId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, traderId);
            ps.setString(2, assetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not update the watchlist: " + e.getMessage(), e);
        }
    }
}