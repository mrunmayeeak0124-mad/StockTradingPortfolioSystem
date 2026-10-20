package com.stockportfolio;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.*;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.repository.OrderRepository;
import com.stockportfolio.repository.PriceHistoryRepository;
import com.stockportfolio.repository.TraderRepository;
import com.stockportfolio.repository.TransactionRepository;
import com.stockportfolio.service.PersistenceService;
import com.stockportfolio.util.FeeCalculator;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Temporary demo: run it TWICE. The second run proves the data survived the restart.
 * All assets and prices are simulated demonstration data.
 */
public class PersistenceDemo {

    public static void main(String[] args) throws TradingException {
        DatabaseManager.initialize();
        System.out.println("Database file: "
                + Paths.get(DatabaseManager.getDatabasePath()).toAbsolutePath());

        AssetRepository assetRepository = new AssetRepository();
        TraderRepository traderRepository = new TraderRepository();
        OrderRepository orderRepository = new OrderRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        PriceHistoryRepository historyRepository = new PriceHistoryRepository();
        PersistenceService persistence = new PersistenceService();

        Map<String, Asset> assets = assetRepository.findAllAsMap();
        System.out.println("Assets loaded: " + assets.size() + " (simulated demonstration data)");

        if (traderRepository.findFirst(assets).isEmpty()) {
            System.out.println("\nFIRST RUN: creating a trader and some activity...");
            firstRun(assets, persistence);
        } else {
            System.out.println("\nLATER RUN: trader found in the database, nothing re-created.");
        }

        Trader trader = traderRepository.findFirst(assets).orElseThrow();
        printSaved(trader, orderRepository, transactionRepository, historyRepository, assets);
        rollbackTest(trader, traderRepository, persistence, assets);
    }

    private static void firstRun(Map<String, Asset> assets, PersistenceService persistence)
            throws TradingException {
        Trader trader = new Trader("T001", "Mrunmayee", "", new BigDecimal("100000"));
        persistence.createTrader(trader);

        Asset reliance = assets.get("A001");

        // A market buy of 10 units, saved all-or-nothing
        BuyOrder buy = new BuyOrder(reliance, 10);
        BigDecimal gross = MoneyUtil.round(
                reliance.getCurrentPrice().multiply(BigDecimal.valueOf(10)));
        BigDecimal fee = FeeCalculator.calculateFee(gross);
        Transaction buyTransaction = buy.execute(trader, reliance.getCurrentPrice(), fee);
        persistence.saveTrade(trader, buy, buyTransaction);

        // A deposit
        trader.deposit(new BigDecimal("5000"));
        persistence.saveDeposit(trader,
                Transaction.forDeposit(trader.getUserId(), new BigDecimal("5000")));

        // A pending limit order (no trade yet)
        BuyOrder limit = new BuyOrder(reliance, 5, OrderType.LIMIT, new BigDecimal("2400.00"));
        persistence.saveOrder(trader, limit);
    }

    private static void printSaved(Trader trader, OrderRepository orderRepository,
                                   TransactionRepository transactionRepository,
                                   PriceHistoryRepository historyRepository,
                                   Map<String, Asset> assets) {
        System.out.println("\n--- Loaded from the database ---");
        System.out.println(trader);
        System.out.println("Cash: " + trader.getCashBalance());

        Portfolio portfolio = trader.getPortfolio();
        for (Position position : portfolio.getPositions()) {
            System.out.println("Position: " + position.getAsset().getSymbol()
                    + " qty " + position.getQuantity()
                    + " | cost basis " + position.getTotalCostBasis()
                    + " | avg cost " + position.getAverageCost());
        }
        System.out.println("Realized P/L: " + portfolio.getRealizedProfitLoss());

        System.out.println("Orders:");
        for (Order order : orderRepository.findByTrader(trader.getUserId(), assets)) {
            System.out.println("  " + order);
        }
        System.out.println("Transactions (newest first):");
        for (Transaction transaction : transactionRepository.findByTrader(trader.getUserId(), assets)) {
            System.out.println("  " + transaction);
        }

        List<PricePoint> history = historyRepository.findByAsset("A001");
        PricePoint last = history.get(history.size() - 1);
        System.out.println("Price history for RELIANCE: " + history.size()
                + " points, latest " + last.date() + " = " + last.price() + " (simulated)");
    }

    /** Proves a failed save leaves the database unchanged. */
    private static void rollbackTest(Trader trader, TraderRepository traderRepository,
                                     PersistenceService persistence, Map<String, Asset> assets) {
        System.out.println("\n--- Rollback test ---");
        BigDecimal before = trader.getCashBalance();

        trader.deposit(new BigDecimal("999"));                   // changes MEMORY only
        Transaction badRecord =
                Transaction.forDeposit("NO-SUCH-TRADER", new BigDecimal("999"));
        try {
            persistence.saveDeposit(trader, badRecord);          // trader row saves, record fails
            System.out.println("Unexpected: the save succeeded.");
        } catch (DatabaseException e) {
            System.out.println("Save failed as expected: " + e.getMessage());
        }

        Trader reloaded = traderRepository.findById(trader.getUserId(), assets).orElseThrow();
        System.out.println("Cash in memory (not saved): " + trader.getCashBalance());
        System.out.println("Cash reloaded from database: " + reloaded.getCashBalance());
        System.out.println("Database unchanged after failure: "
                + (reloaded.getCashBalance().compareTo(before) == 0));
    }
}