package com.stockportfolio.model;

import com.stockportfolio.exception.TradingException;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An order to buy. Total cost = quantity x price + fee.
 */
public class BuyOrder extends Order {

    /** Market order (constructor overloading). */
    public BuyOrder(Asset asset, int quantity) {
        this(asset, quantity, OrderType.MARKET, null);
    }

    public BuyOrder(Asset asset, int quantity, OrderType orderType, BigDecimal limitPrice) {
        super(asset, quantity, orderType, limitPrice);
    }

    /** Used when loading a saved order from the database. */
    public BuyOrder(String orderId, Asset asset, int quantity, OrderType orderType,
                    BigDecimal limitPrice, LocalDateTime submittedAt, OrderStatus status) {
        super(orderId, asset, quantity, orderType, limitPrice, submittedAt, status);
    }

    @Override
    public TransactionType getSide() {
        return TransactionType.BUY;
    }

    /** A buy limit triggers when the market price is at or BELOW the limit. */
    @Override
    protected boolean isLimitConditionMet(BigDecimal marketPrice) {
        return marketPrice.compareTo(getLimitPrice()) <= 0;
    }

    @Override
    public Transaction execute(Trader trader, BigDecimal executionPrice, BigDecimal fee)
            throws TradingException {
        checkExecutable(executionPrice, fee);

        BigDecimal gross = MoneyUtil.round(
                executionPrice.multiply(BigDecimal.valueOf(getQuantity())));
        BigDecimal totalCost = gross.add(fee);

        // The step that can fail comes FIRST. If cash is too low, nothing has changed yet.
        trader.withdraw(totalCost);
        trader.getPortfolio().recordBuy(getAsset(), getQuantity(), totalCost);
        markExecuted();

        return Transaction.forTrade(trader.getUserId(), this, executionPrice, fee,
                gross, totalCost.negate(), BigDecimal.ZERO.setScale(2));
    }
}