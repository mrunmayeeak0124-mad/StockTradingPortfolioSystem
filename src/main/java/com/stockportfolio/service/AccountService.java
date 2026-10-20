package com.stockportfolio.service;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.Trader;
import com.stockportfolio.repository.TraderRepository;
import com.stockportfolio.util.IdGenerator;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Creates and loads the local trader profile. VIRTUAL MONEY ONLY:
 * the application never asks for bank, card or broker details.
 */
public class AccountService {

    /** Largest single starting balance or deposit (stops typing mistakes). */
    public static final BigDecimal MAX_VIRTUAL_AMOUNT = new BigDecimal("10000000.00");

    private static BigDecimal defaultStartingBalance = new BigDecimal("100000.00");

    private final TraderRepository traderRepository = new TraderRepository();
    private final PersistenceService persistence = new PersistenceService();

    public static BigDecimal getDefaultStartingBalance() {
        return defaultStartingBalance;
    }

    /** The starting balance is configurable (a Settings screen will call this later). */
    public static void setDefaultStartingBalance(BigDecimal amount) {
        defaultStartingBalance = validateAmount(amount, "Starting balance");
    }

    /** The saved trader, or empty if no profile has been created yet. */
    public Optional<Trader> loadTrader(Map<String, Asset> assets) {
        return traderRepository.findFirst(assets);
    }

    public java.util.List<Trader> listAllTraders(Map<String, Asset> assets) {
        return traderRepository.findAll(assets);
    }

    public Trader createTrader(String name, String email) {
        return createTrader(name, email, defaultStartingBalance);
    }

    /**
     * Creates and saves a trader. The starting balance is recorded as a DEPOSIT
     * transaction so the balance can always be audited.
     */
    public Trader createTrader(String name, String email, BigDecimal startingBalance) {
        BigDecimal start = validateAmount(startingBalance, "Starting balance");
        Trader trader = new Trader(IdGenerator.generate("TRD"), name, email, start);  // validates name/email
        persistence.createTrader(trader);
        return trader;
    }

    private static BigDecimal validateAmount(BigDecimal amount, String label) {
        if (amount == null) {
            throw new InvalidInputException(label + " is required.");
        }
        BigDecimal rounded = MoneyUtil.round(amount);
        if (rounded.signum() <= 0) {
            throw new InvalidInputException(label + " must be greater than zero.");
        }
        if (rounded.compareTo(MAX_VIRTUAL_AMOUNT) > 0) {
            throw new InvalidInputException(label + " cannot be more than " + MAX_VIRTUAL_AMOUNT + ".");
        }
        return rounded;
    }
}