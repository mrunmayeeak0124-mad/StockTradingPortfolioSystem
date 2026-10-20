package com.stockportfolio;

import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.*;
import com.stockportfolio.util.FeeCalculator;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;

/**
 * Temporary console demo of buying, selling, P/L and limit orders.
 * All prices are simulated demonstration values.
 */
public class TradingDemo {

    public static void main(String[] args) throws TradingException {
        Asset reliance = new Stock("A001", "Reliance Industries", "RELIANCE", "NSE",
                "INR", "Energy", new BigDecimal("2450.50"));
        Trader trader = new Trader("T001", "Mrunmayee", "", new BigDecimal("100000"));
        Portfolio portfolio = trader.getPortfolio();
        System.out.println("Start cash: " + trader.getCashBalance());

        // 1. First purchase
        Transaction t1 = run(trader, new BuyOrder(reliance, 10));
        System.out.println("\nBuy 10 @ " + reliance.getCurrentPrice()
                + " | fee " + t1.getFee() + " | net cash effect " + t1.getNetAmount());
        printState(trader, reliance);

        // 2. Price changes, second purchase (average cost changes)
        reliance.setCurrentPrice(new BigDecimal("2500.00"));
        Transaction t2 = run(trader, new BuyOrder(reliance, 5));
        System.out.println("\nBuy 5 @ " + reliance.getCurrentPrice()
                + " | fee " + t2.getFee() + " | net cash effect " + t2.getNetAmount());
        printState(trader, reliance);
        Position p = portfolio.getPosition("A001");
        System.out.println("Market value: " + p.getMarketValue()
                + " | Unrealized P/L: " + p.getUnrealizedProfitLoss()
                + " | Return %: " + p.getReturnPercent());

        // 3. Partial sale
        Transaction t3 = run(trader, new SellOrder(reliance, 6));
        System.out.println("\nSell 6 @ " + reliance.getCurrentPrice()
                + " | fee " + t3.getFee() + " | net cash effect " + t3.getNetAmount()
                + " | realized P/L " + t3.getRealizedProfitLoss());
        printState(trader, reliance);
        System.out.println("Realized P/L: " + portfolio.getRealizedProfitLoss()
                + " | Unrealized P/L: " + portfolio.getUnrealizedProfitLoss()
                + " | Account value: " + trader.getTotalAccountValue());
        System.out.println("Allocation: " + portfolio.getAllocationByCategory());

        // 4. Failures must change nothing
        System.out.println();
        try {
            run(trader, new BuyOrder(reliance, 1000));
        } catch (TradingException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        try {
            run(trader, new SellOrder(reliance, 100));
        } catch (TradingException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        System.out.println("Cash after failed trades: " + trader.getCashBalance());

        // 5. Limit order and cancellation
        BuyOrder limit = new BuyOrder(reliance, 2, OrderType.LIMIT, new BigDecimal("2400.00"));
        System.out.println("\nLimit buy @2400.00 executable at 2450.50? "
                + limit.canExecuteAt(new BigDecimal("2450.50")));
        System.out.println("Limit buy @2400.00 executable at 2390.00? "
                + limit.canExecuteAt(new BigDecimal("2390.00")));
        limit.cancel();
        System.out.println("Status after cancel: " + limit.getStatus());
        try {
            limit.execute(trader, new BigDecimal("2390.00"), new BigDecimal("4.78"));
        } catch (TradingException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }

    /** Executes an order at the asset's current price with the simulated fee. */
    private static Transaction run(Trader trader, Order order) throws TradingException {
        BigDecimal price = order.getAsset().getCurrentPrice();
        BigDecimal gross = MoneyUtil.round(price.multiply(BigDecimal.valueOf(order.getQuantity())));
        BigDecimal fee = FeeCalculator.calculateFee(gross);
        return order.execute(trader, price, fee);
    }

    private static void printState(Trader trader, Asset asset) {
        Position p = trader.getPortfolio().getPosition(asset.getAssetId());
        System.out.println("Cash: " + trader.getCashBalance() + " | Qty: " + p.getQuantity()
                + " | Avg cost: " + p.getAverageCost() + " | Cost basis: " + p.getTotalCostBasis());
    }
}