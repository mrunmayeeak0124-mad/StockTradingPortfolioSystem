package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.exception.TradingException;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An order to sell. Net proceeds = quantity x price - fee.
 */
public class SellOrder extends Order {

    /** Market order (constructor overloading). */
    public SellOrder(Asset asset, int quantity) {
        this(asset, quantity, OrderType.MARKET, null);
    }

    public SellOrder(Asset asset, int quantity, OrderType orderType, BigDecimal limitPrice) {
        super(asset, quantity, orderType, limitPrice);
    }

    /** Used when loading a saved order from the database. */
    public SellOrder(String orderId, Asset asset, int quantity, OrderType orderType,
                     BigDecimal limitPrice, LocalDateTime submittedAt, OrderStatus status) {
        super(orderId, asset, quantity, orderType, limitPrice, submittedAt, status);
    }

    @Override
    public TransactionType getSide() {
        return TransactionType.SELL;
    }

    /** A sell limit triggers when the market price is at or ABOVE the limit. */
    @Override
    protected boolean isLimitConditionMet(BigDecimal marketPrice) {
        return marketPrice.compareTo(getLimitPrice()) >= 0;
    }

    @Override
    public Transaction execute(Trader trader, BigDecimal executionPrice, BigDecimal fee)
            throws TradingException {
        checkExecutable(executionPrice, fee);

        BigDecimal gross = MoneyUtil.round(
                executionPrice.multiply(BigDecimal.valueOf(getQuantity())));
        BigDecimal netProceeds = gross.subtract(fee);
        if (netProceeds.signum() <= 0) {
            throw new InvalidInputException("The fee is larger than the sale value.");
        }

        // Fails here (before any change) if the trader does not hold enough units.
        BigDecimal costOfSoldUnits = trader.getPortfolio().recordSell(getAsset(), getQuantity());
        trader.deposit(netProceeds);

        BigDecimal realized = netProceeds.subtract(costOfSoldUnits);
        trader.getPortfolio().addRealizedProfitLoss(realized);
        markExecuted();

        return Transaction.forTrade(trader.getUserId(), this, executionPrice, fee,
                gross, netProceeds, realized);
    }
}