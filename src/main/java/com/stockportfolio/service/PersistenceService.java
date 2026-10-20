package com.stockportfolio.service;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.repository.OrderRepository;
import com.stockportfolio.repository.TraderRepository;
import com.stockportfolio.repository.TransactionRepository;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Saves related records TOGETHER in one database transaction.
 * Either every write succeeds (commit) or none is kept (rollback).
 */
public class PersistenceService {

    private final TraderRepository traderRepository = new TraderRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();

    /** A piece of database work that runs inside one transaction. */
    @FunctionalInterface
    private interface DatabaseWork {
        void run(Connection connection) throws SQLException;
    }

    /** Saves a brand-new trader and records the starting balance as a DEPOSIT. */
    public void createTrader(Trader trader) {
        Transaction initialDeposit =
                Transaction.forDeposit(trader.getUserId(), trader.getCashBalance());
        runInTransaction(connection -> {
            traderRepository.save(connection, trader);
            transactionRepository.save(connection, initialDeposit);
        });
    }

    /** Saves the new cash balance together with the deposit record. */
    public void saveDeposit(Trader trader, Transaction deposit) {
        runInTransaction(connection -> {
            traderRepository.save(connection, trader);
            transactionRepository.save(connection, deposit);
        });
    }

    /** Saves an executed order, its transaction, and the updated cash and holdings. */
    public void saveTrade(Trader trader, Order order, Transaction transaction) {
        runInTransaction(connection -> {
            orderRepository.save(connection, trader.getUserId(), order);   // first: the transaction refers to it
            transactionRepository.save(connection, transaction);
            traderRepository.save(connection, trader);
        });
    }

    /** Saves an order that has no trade yet (pending) or changed status (cancelled/rejected). */
    public void saveOrder(Trader trader, Order order) {
        runInTransaction(connection ->
                orderRepository.save(connection, trader.getUserId(), order));
    }

    private void runInTransaction(DatabaseWork work) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);             // start a database transaction
            try {
                work.run(connection);
                connection.commit();                     // all writes become permanent together
            } catch (RuntimeException e) {
                rollbackQuietly(connection);             // undo every write of this transaction
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new DatabaseException("Database error: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // the original error is more important than a failed rollback
        }
    }
}