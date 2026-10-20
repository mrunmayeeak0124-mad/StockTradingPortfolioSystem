package com.stockportfolio.model;

import com.stockportfolio.exception.InsufficientFundsException;
import com.stockportfolio.exception.InvalidInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A Trader is a User who trades assets and manages a portfolio.
 * It owns a Portfolio (composition: a Trader HAS-A Portfolio).
 */
public class Trader extends User {

    private BigDecimal cashBalance;
    private RiskPreference riskPreference;
    private final Portfolio portfolio = new Portfolio();

    public Trader(String userId, String name, String email, BigDecimal startingCash) {
        super(userId, name, email);
        if (startingCash == null || startingCash.signum() < 0) {
            throw new InvalidInputException("Starting cash cannot be negative.");
        }
        this.cashBalance = startingCash.setScale(2, RoundingMode.HALF_UP);
        this.riskPreference = RiskPreference.BALANCED;
    }

    public BigDecimal getCashBalance() { return cashBalance; }
    public RiskPreference getRiskPreference() { return riskPreference; }
    public Portfolio getPortfolio() { return portfolio; }

    public void setRiskPreference(RiskPreference riskPreference) {
        if (riskPreference == null) {
            throw new InvalidInputException("Risk preference is required.");
        }
        this.riskPreference = riskPreference;
    }

    /** Add funds to wallet. Only positive amounts are allowed. */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Deposit must be greater than zero.");
        }
        cashBalance = cashBalance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /** Withdraw funds. Fails (and changes nothing) if cash is not enough. */
    public void withdraw(BigDecimal amount) throws InsufficientFundsException {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Amount must be greater than zero.");
        }
        if (cashBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(amount, cashBalance);
        }
        cashBalance = cashBalance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /** Total account value = cash + market value of open holdings. */
    public BigDecimal getTotalAccountValue() {
        return cashBalance.add(portfolio.getTotalMarketValue());
    }

    @Override
    public String getRole() {
        return "Trader";
    }
}