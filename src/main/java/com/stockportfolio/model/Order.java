package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.exception.TradingException;
import com.stockportfolio.util.IdGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Abstract parent of BuyOrder and SellOrder.
 * It holds what every order has and the rules that are the same for both sides.
 * The parts that differ (side, limit condition, execution) are abstract.
 */
public abstract class Order {

    private final String orderId;
    private final Asset asset;
    private final int quantity;
    private final OrderType orderType;
    private final BigDecimal limitPrice;       // null for market orders
    private final LocalDateTime submittedAt;
    private OrderStatus status;

    /** Creates a NEW order: fresh ID, current time, status PENDING. */
    protected Order(Asset asset, int quantity, OrderType orderType, BigDecimal limitPrice) {
        this(IdGenerator.generate("ORD"), asset, quantity, orderType, limitPrice,
                LocalDateTime.now(), OrderStatus.PENDING);
    }

    /** Rebuilds an order that was saved earlier (used when loading from the database). */
    protected Order(String orderId, Asset asset, int quantity, OrderType orderType,
                    BigDecimal limitPrice, LocalDateTime submittedAt, OrderStatus status) {
        if (orderId == null || orderId.isBlank()) {
            throw new InvalidInputException("Order ID cannot be empty.");
        }
        if (asset == null) {
            throw new InvalidInputException("Please select an asset.");
        }
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero.");
        }
        if (orderType == null) {
            throw new InvalidInputException("Order type is required.");
        }
        if (submittedAt == null || status == null) {
            throw new InvalidInputException("Order time and status are required.");
        }
        if (orderType == OrderType.LIMIT) {
            if (limitPrice == null || limitPrice.signum() <= 0) {
                throw new InvalidInputException("A limit order needs a limit price above zero.");
            }
            this.limitPrice = limitPrice.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.limitPrice = null;
        }
        this.orderId = orderId;
        this.asset = asset;
        this.quantity = quantity;
        this.orderType = orderType;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public Asset getAsset() { return asset; }
    public int getQuantity() { return quantity; }
    public OrderType getOrderType() { return orderType; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public OrderStatus getStatus() { return status; }

    // ---------- abstract: each side decides ----------

    /** BUY or SELL. */
    public abstract TransactionType getSide();

    /** Is the limit condition satisfied at this market price? */
    protected abstract boolean isLimitConditionMet(BigDecimal marketPrice);

    /**
     * Carry out the trade. Changes the trader's cash and portfolio,
     * marks this order EXECUTED and returns the Transaction record.
     */
    public abstract Transaction execute(Trader trader, BigDecimal executionPrice, BigDecimal fee)
            throws TradingException;

    // ---------- shared rules ----------

    /** Market orders can always execute; limit orders only when their condition is met. */
    public boolean canExecuteAt(BigDecimal marketPrice) {
        if (orderType == OrderType.MARKET) {
            return true;
        }
        return isLimitConditionMet(marketPrice);
    }

    /** Only a PENDING order can be cancelled. */
    public void cancel() throws TradingException {
        if (status != OrderStatus.PENDING) {
            throw new TradingException("Only pending orders can be cancelled. Current status: "
                    + status);
        }
        status = OrderStatus.CANCELLED;
    }

    /** Mark a pending order as rejected (for example, validation failed). */
    public void reject() throws TradingException {
        if (status != OrderStatus.PENDING) {
            throw new TradingException("Only pending orders can be rejected. Current status: "
                    + status);
        }
        status = OrderStatus.REJECTED;
    }

    protected void markExecuted() {
        status = OrderStatus.EXECUTED;
    }

    /** Checks common to buy and sell. Also prevents executing an order twice. */
    protected void checkExecutable(BigDecimal executionPrice, BigDecimal fee)
            throws TradingException {
        if (status != OrderStatus.PENDING) {
            throw new TradingException("Order " + orderId + " is " + status
                    + " and cannot be executed.");
        }
        if (executionPrice == null || executionPrice.signum() <= 0) {
            throw new InvalidInputException("Execution price must be greater than zero.");
        }
        if (fee == null || fee.signum() < 0) {
            throw new InvalidInputException("Fee cannot be negative.");
        }
        if (!canExecuteAt(executionPrice)) {
            throw new TradingException("Limit condition not met for order " + orderId
                    + " at price " + executionPrice + ".");
        }
    }

    @Override
    public String toString() {
        return orderId + " " + getSide() + " " + quantity + " x " + asset.getSymbol()
                + " [" + orderType + ", " + status + "]";
    }
}