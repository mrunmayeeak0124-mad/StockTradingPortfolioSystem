package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Saves and loads transaction records. Transactions are only ever added, never changed.
 */
public class TransactionRepository {

    public void save(Connection connection, Transaction t) {
        String sql = """
                INSERT INTO transactions (transaction_id, trader_id, order_id, asset_id, type,
                                          quantity, execution_price, fee, gross_value,
                                          net_amount, realized_pl, created_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getTransactionId());
            ps.setString(2, t.getTraderId());
            ps.setString(3, t.getOrderId());
            ps.setString(4, t.getAsset() == null ? null : t.getAsset().getAssetId());
            ps.setString(5, t.getType().name());
            ps.setInt(6, t.getQuantity());
            ps.setString(7, t.getExecutionPrice().toPlainString());
            ps.setString(8, t.getFee().toPlainString());
            ps.setString(9, t.getGrossValue().toPlainString());
            ps.setString(10, t.getNetAmount().toPlainString());
            ps.setString(11, t.getRealizedProfitLoss().toPlainString());
            ps.setString(12, t.getTimestamp().toString());
            ps.setString(13, t.getStatus());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not save transaction " + t.getTransactionId() + ": " + e.getMessage(), e);
        }
    }

    /** Newest first. */
    public List<Transaction> findByTrader(String traderId, Map<String, Asset> assets) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE trader_id = ? ORDER BY rowid DESC";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs, assets));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load transactions: " + e.getMessage(), e);
        }
        return list;
    }

    private Transaction mapRow(ResultSet rs, Map<String, Asset> assets) throws SQLException {
        String assetId = rs.getString("asset_id");           // NULL for deposits
        Asset asset = assetId == null ? null : assets.get(assetId);
        return Transaction.restore(
                rs.getString("transaction_id"),
                rs.getString("trader_id"),
                rs.getString("order_id"),
                asset,
                TransactionType.valueOf(rs.getString("type")),
                rs.getInt("quantity"),
                new BigDecimal(rs.getString("execution_price")),
                new BigDecimal(rs.getString("fee")),
                new BigDecimal(rs.getString("gross_value")),
                new BigDecimal(rs.getString("net_amount")),
                new BigDecimal(rs.getString("realized_pl")),
                LocalDateTime.parse(rs.getString("created_at")),
                rs.getString("status"));
    }
}