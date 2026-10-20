package com.stockportfolio;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.*;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.service.AccountService;
import com.stockportfolio.service.TradingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Temporary console demo of the services. Run it on a FRESH database
 * (delete database/stocktrading.db first). All prices are simulated.
 */
public class TradingServiceDemo {

    public static void main(String[] args) throws TradingException {
        DatabaseManager.initialize();
        Map<String, Asset> assets = new AssetRepository().findAllAsMap();

        AccountService accountService = new AccountService();
        Trader trader = accountService.loadTrader(assets)
                .orElseGet(() -> accountService.createTrader("Mrunmayee", "", new BigDecimal("100000")));
        TradingService trading = new TradingService(trader, assets);
        Asset reliance = assets.get("A001");
        Asset tcs = assets.get("A002");

        System.out.println("Account ID: " + trading.getTrader().getUserId());

        System.out.println("\n1. Deposit 5000");
        trading.deposit(new BigDecimal("5000"));
        printStatus(trading);

        System.out.println("\n2. Quote for buying 10 RELIANCE");
        OrderQuote quote = trading.getQuote(TransactionType.BUY, reliance, 10);
        System.out.println("Price " + quote.price() + " | order value " + quote.grossValue()
                + " | fee " + quote.fee() + " | " + quote.netLabel() + " " + quote.netAmount());

        System.out.println("\n3. Market buy of 10 RELIANCE");
        trading.placeMarketOrder(TransactionType.BUY, reliance, 10);
        printStatus(trading);

        System.out.println("\n4. Failures change nothing");
        try {
            trading.placeMarketOrder(TransactionType.BUY, reliance, 1000);
        } catch (TradingException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        try {
            trading.placeMarketOrder(TransactionType.SELL, reliance, 50);
        } catch (TradingException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        try {
            trading.placeMarketOrder(TransactionType.SELL, tcs, 1);
        } catch (TradingException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println("\n5. Limit buy of 5 RELIANCE at 2400.00 (price is " + reliance.getCurrentPrice() + ")");
        Order limitBuy = trading.placeLimitOrder(TransactionType.BUY, reliance, 5, new BigDecimal("2400.00"));
        System.out.println(limitBuy);

        System.out.println("\n6. Simulated price falls to 2390.00");
        List<Order> changed = trading.updateAssetPrice(reliance, new BigDecimal("2390.00"));
        System.out.println("Orders changed: " + changed);
        printStatus(trading);

        System.out.println("\n7. Limit sell at 2500.00, then cancel it, then price rises to 2600.00");
        Order limitSell = trading.placeLimitOrder(TransactionType.SELL, reliance, 5, new BigDecimal("2500.00"));
        System.out.println(limitSell);
        trading.cancelOrder(limitSell.getOrderId());
        System.out.println(limitSell);
        changed = trading.updateAssetPrice(reliance, new BigDecimal("2600.00"));
        System.out.println("Orders changed by the price update: " + changed.size());
        printStatus(trading);

        System.out.println("\n8. Market sell of 5 RELIANCE at 2600.00");
        trading.placeMarketOrder(TransactionType.SELL, reliance, 5);
        printStatus(trading);

        System.out.println("\nOrders:");
        for (Order order : trading.getOrders()) {
            System.out.println("  " + order);
        }
        System.out.println("Transactions (newest first): " + trading.getTransactions().size());
        for (Transaction transaction : trading.getTransactions()) {
            System.out.println("  " + transaction);
        }
    }

    private static void printStatus(TradingService trading) {
        Trader t = trading.getTrader();
        Portfolio p = t.getPortfolio();
        System.out.println("Cash " + t.getCashBalance()
                + " | Holdings value " + p.getTotalMarketValue()
                + " | Account value " + t.getTotalAccountValue()
                + " | Realized P/L " + p.getRealizedProfitLoss()
                + " | Unrealized P/L " + p.getUnrealizedProfitLoss());
    }
}