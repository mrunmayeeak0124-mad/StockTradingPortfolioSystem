package com.stockportfolio.model;

import com.stockportfolio.util.IdGenerator;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A permanent record of one cash-affecting event (deposit, buy or sell).
 * It has no setters: a record of the past should never change.
 * netAmount is the SIGNED cash effect: buy = negative, sell/deposit = positive.
 */
public class Transaction {

    private final String transactionId;
    private final String traderId;
    private final String orderId;            // null for deposits
    private final Asset asset;               // null for deposits
    private final TransactionType type;
    private final int quantity;
    private final BigDecimal executionPrice;
    private final BigDecimal fee;
    private final BigDecimal grossValue;
    private final BigDecimal netAmount;
    private final BigDecimal realizedProfitLoss;   // only meaningful for SELL
    private final LocalDateTime timestamp;
    private final String status;

    private Transaction(String transactionId, String traderId, String orderId, Asset asset,
                        TransactionType type, int quantity, BigDecimal executionPrice,
                        BigDecimal fee, BigDecimal grossValue, BigDecimal netAmount,
                        BigDecimal realizedProfitLoss, LocalDateTime timestamp, String status) {
        this.transactionId = transactionId;
        this.traderId = traderId;
        this.orderId = orderId;
        this.asset = asset;
        this.type = type;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.fee = fee;
        this.grossValue = grossValue;
        this.netAmount = netAmount;
        this.realizedProfitLoss = realizedProfitLoss;
        this.timestamp = timestamp;
        this.status = status;
    }

    /** Factory method for a deposit record. */
    public static Transaction forDeposit(String traderId, BigDecimal amount) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        BigDecimal value = MoneyUtil.round(amount);
        return new Transaction(IdGenerator.generate("TXN"), traderId, null, null,
                TransactionType.DEPOSIT, 0, zero, zero, value, value, zero,
                LocalDateTime.now(), "COMPLETED");
    }

    /** Factory method for a completed buy or sell. */
    public static Transaction forTrade(String traderId, Order order, BigDecimal executionPrice,
                                       BigDecimal fee, BigDecimal grossValue,
                                       BigDecimal netAmount, BigDecimal realizedProfitLoss) {
        return new Transaction(IdGenerator.generate("TXN"), traderId, order.getOrderId(),
                order.getAsset(), order.getSide(), order.getQuantity(), executionPrice, fee,
                grossValue, netAmount, realizedProfitLoss, LocalDateTime.now(), "COMPLETED");
    }

    /** Factory method that rebuilds a saved transaction (used when loading from the database). */
    public static Transaction restore(String transactionId, String traderId, String orderId,
                                      Asset asset, TransactionType type, int quantity,
                                      BigDecimal executionPrice, BigDecimal fee,
                                      BigDecimal grossValue, BigDecimal netAmount,
                                      BigDecimal realizedProfitLoss, LocalDateTime timestamp,
                                      String status) {
        return new Transaction(transactionId, traderId, orderId, asset, type, quantity,
                executionPrice, fee, grossValue, netAmount, realizedProfitLoss,
                timestamp, status);
    }

    public String getTransactionId() { return transactionId; }
    public String getTraderId() { return traderId; }
    public String getOrderId() { return orderId; }
    public Asset getAsset() { return asset; }
    public TransactionType getType() { return type; }
    public int getQuantity() { return quantity; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public BigDecimal getFee() { return fee; }
    public BigDecimal getGrossValue() { return grossValue; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getRealizedProfitLoss() { return realizedProfitLoss; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        String assetText = asset == null ? "-" : asset.getSymbol();
        return transactionId + " | " + type + " | " + assetText + " | qty " + quantity
                + " | net " + netAmount;
    }
}