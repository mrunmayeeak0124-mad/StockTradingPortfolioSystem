package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.BuyOrder;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.OrderStatus;
import com.stockportfolio.model.OrderType;
import com.stockportfolio.model.SellOrder;

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
 * Saves and loads orders. The list returned holds BuyOrder and SellOrder objects
 * behind the common Order type (polymorphism).
 */
public class OrderRepository {

    /** Insert a new order, or update the status of an existing one. */
    public void save(Connection connection, String traderId, Order order) {
        String sql = """
                INSERT INTO orders (order_id, trader_id, asset_id, side, quantity,
                                    order_type, limit_price, submitted_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(order_id) DO UPDATE SET status = excluded.status
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, order.getOrderId());
            ps.setString(2, traderId);
            ps.setString(3, order.getAsset().getAssetId());
            ps.setString(4, order.getSide().name());
            ps.setInt(5, order.getQuantity());
            ps.setString(6, order.getOrderType().name());
            ps.setString(7, order.getLimitPrice() == null
                    ? null : order.getLimitPrice().toPlainString());
            ps.setString(8, order.getSubmittedAt().toString());
            ps.setString(9, order.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not save order " + order.getOrderId() + ": " + e.getMessage(), e);
        }
    }

    /** Oldest first. */
    public List<Order> findByTrader(String traderId, Map<String, Asset> assets) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE trader_id = ? ORDER BY rowid";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs, assets));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load orders: " + e.getMessage(), e);
        }
        return orders;
    }

    private Order mapRow(ResultSet rs, Map<String, Asset> assets) throws SQLException {
        Asset asset = assets.get(rs.getString("asset_id"));
        if (asset == null) {
            throw new DatabaseException("A saved order refers to an unknown asset: "
                    + rs.getString("asset_id"));
        }
        String orderId = rs.getString("order_id");
        int quantity = rs.getInt("quantity");
        OrderType type = OrderType.valueOf(rs.getString("order_type"));
        String limitText = rs.getString("limit_price");
        BigDecimal limitPrice = limitText == null ? null : new BigDecimal(limitText);
        LocalDateTime submittedAt = LocalDateTime.parse(rs.getString("submitted_at"));
        OrderStatus status = OrderStatus.valueOf(rs.getString("status"));

        if ("BUY".equals(rs.getString("side"))) {
            return new BuyOrder(orderId, asset, quantity, type, limitPrice, submittedAt, status);
        }
        return new SellOrder(orderId, asset, quantity, type, limitPrice, submittedAt, status);
    }
}