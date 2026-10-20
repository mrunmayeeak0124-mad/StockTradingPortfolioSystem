package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.RiskPreference;
import com.stockportfolio.model.Trader;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Saves and loads a Trader together with its open positions (portfolio).
 */
public class TraderRepository {

    /**
     * Saves the trader row and replaces the saved positions with the current ones.
     * Uses the caller's connection so it can be part of a bigger database transaction.
     */
    public void save(Connection connection, Trader trader) {
        String upsert = """
                INSERT INTO traders (trader_id, name, email, cash_balance, risk_preference, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(trader_id) DO UPDATE SET
                    name = excluded.name,
                    email = excluded.email,
                    cash_balance = excluded.cash_balance,
                    risk_preference = excluded.risk_preference
                """;
        String insertPosition = "INSERT INTO positions "
                + "(trader_id, asset_id, quantity, total_cost_basis) VALUES (?, ?, ?, ?)";
        try {
            try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                ps.setString(1, trader.getUserId());
                ps.setString(2, trader.getName());
                ps.setString(3, trader.getEmail());
                ps.setString(4, trader.getCashBalance().toPlainString());
                ps.setString(5, trader.getRiskPreference().name());
                ps.setString(6, LocalDateTime.now().toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM positions WHERE trader_id = ?")) {
                ps.setString(1, trader.getUserId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(insertPosition)) {
                for (Position position : trader.getPortfolio().getPositions()) {
                    ps.setString(1, trader.getUserId());
                    ps.setString(2, position.getAsset().getAssetId());
                    ps.setInt(3, position.getQuantity());
                    ps.setString(4, position.getTotalCostBasis().toPlainString());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save trader: " + e.getMessage(), e);
        }
    }

    /** The first (oldest) saved trader, or empty if nobody has created a profile yet. */
    public Optional<Trader> findFirst(Map<String, Asset> assets) {
        String sql = "SELECT * FROM traders ORDER BY rowid LIMIT 1";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(buildTrader(connection, rs, assets));
        } catch (SQLException e) {
            throw new DatabaseException("Could not load trader: " + e.getMessage(), e);
        }
    }

    public Optional<Trader> findById(String traderId, Map<String, Asset> assets) {
        String sql = "SELECT * FROM traders WHERE trader_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(buildTrader(connection, rs, assets));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load trader: " + e.getMessage(), e);
        }
    }

    public List<Trader> findAll(Map<String, Asset> assets) {
        String sql = "SELECT * FROM traders ORDER BY created_at DESC";
        List<Trader> list = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(buildTrader(connection, rs, assets));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load traders: " + e.getMessage(), e);
        }
    }

    private Trader buildTrader(Connection connection, ResultSet row, Map<String, Asset> assets)
            throws SQLException {
        String traderId = row.getString("trader_id");
        Trader trader = new Trader(traderId, row.getString("name"), row.getString("email"),
                new BigDecimal(row.getString("cash_balance")));
        trader.setRiskPreference(RiskPreference.valueOf(row.getString("risk_preference")));
        loadPositions(connection, trader, assets);
        trader.getPortfolio().restoreRealizedProfitLoss(
                loadRealizedProfitLoss(connection, traderId));
        return trader;
    }

    private void loadPositions(Connection connection, Trader trader, Map<String, Asset> assets)
            throws SQLException {
        String sql = "SELECT asset_id, quantity, total_cost_basis FROM positions "
                + "WHERE trader_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, trader.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asset asset = assets.get(rs.getString("asset_id"));
                    if (asset == null) {
                        throw new DatabaseException("A saved holding refers to an unknown asset: "
                                + rs.getString("asset_id"));
                    }
                    trader.getPortfolio().recordBuy(asset, rs.getInt("quantity"),
                            new BigDecimal(rs.getString("total_cost_basis")));
                }
            }
        }
    }

    /** Realized P/L = sum of the realized P/L of all saved SELL transactions. */
    private BigDecimal loadRealizedProfitLoss(Connection connection, String traderId)
            throws SQLException {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        String sql = "SELECT realized_pl FROM transactions WHERE trader_id = ? AND type = 'SELL'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total = total.add(new BigDecimal(rs.getString("realized_pl")));
                }
            }
        }
        return total;
    }
}