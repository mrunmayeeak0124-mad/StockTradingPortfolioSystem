package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Detailed personal financial profile used by the rule-based Financial Advisor.
 * Stores income, expenses, risk tolerance, debts, and planning goals.
 */
public class FinancialProfile {

    private final String traderId;
    private RiskPreference riskPreference;
    private String primaryGoal;
    private BigDecimal targetAmount;
    private int timeHorizonYears;
    private BigDecimal availableSavings;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal existingDebt;
    private boolean needLiquidityNearTerm;
    private LocalDateTime lastUpdated;

    public FinancialProfile(String traderId) {
        this(traderId, RiskPreference.BALANCED, "Wealth Accumulation",
                new BigDecimal("500000.00"), 5, new BigDecimal("50000.00"),
                new BigDecimal("60000.00"), new BigDecimal("35000.00"),
                BigDecimal.ZERO, false, LocalDateTime.now());
    }

    public FinancialProfile(String traderId, RiskPreference riskPreference, String primaryGoal,
                            BigDecimal targetAmount, int timeHorizonYears, BigDecimal availableSavings,
                            BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal existingDebt,
                            boolean needLiquidityNearTerm, LocalDateTime lastUpdated) {
        if (traderId == null || traderId.isBlank()) {
            throw new InvalidInputException("Trader ID cannot be empty.");
        }
        this.traderId = traderId.trim();
        this.riskPreference = riskPreference != null ? riskPreference : RiskPreference.BALANCED;
        this.primaryGoal = primaryGoal != null && !primaryGoal.isBlank() ? primaryGoal.trim() : "Wealth Building";
        this.targetAmount = targetAmount != null ? MoneyUtil.round(targetAmount) : BigDecimal.ZERO.setScale(2);
        this.timeHorizonYears = Math.max(1, timeHorizonYears);
        this.availableSavings = availableSavings != null ? MoneyUtil.round(availableSavings) : BigDecimal.ZERO.setScale(2);
        this.monthlyIncome = monthlyIncome != null ? MoneyUtil.round(monthlyIncome) : BigDecimal.ZERO.setScale(2);
        this.monthlyExpenses = monthlyExpenses != null ? MoneyUtil.round(monthlyExpenses) : BigDecimal.ZERO.setScale(2);
        this.existingDebt = existingDebt != null ? MoneyUtil.round(existingDebt) : BigDecimal.ZERO.setScale(2);
        this.needLiquidityNearTerm = needLiquidityNearTerm;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    public String getTraderId() { return traderId; }
    public RiskPreference getRiskPreference() { return riskPreference; }
    public String getPrimaryGoal() { return primaryGoal; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public int getTimeHorizonYears() { return timeHorizonYears; }
    public BigDecimal getAvailableSavings() { return availableSavings; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public BigDecimal getExistingDebt() { return existingDebt; }
    public boolean isNeedLiquidityNearTerm() { return needLiquidityNearTerm; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }

    public void setRiskPreference(RiskPreference riskPreference) {
        if (riskPreference == null) throw new InvalidInputException("Risk preference required.");
        this.riskPreference = riskPreference;
        touch();
    }

    public void setPrimaryGoal(String primaryGoal) {
        this.primaryGoal = primaryGoal != null ? primaryGoal.trim() : "";
        touch();
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount != null ? MoneyUtil.round(targetAmount) : BigDecimal.ZERO;
        touch();
    }

    public void setTimeHorizonYears(int timeHorizonYears) {
        this.timeHorizonYears = Math.max(1, timeHorizonYears);
        touch();
    }

    public void setAvailableSavings(BigDecimal availableSavings) {
        this.availableSavings = availableSavings != null ? MoneyUtil.round(availableSavings) : BigDecimal.ZERO;
        touch();
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome != null ? MoneyUtil.round(monthlyIncome) : BigDecimal.ZERO;
        touch();
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses != null ? MoneyUtil.round(monthlyExpenses) : BigDecimal.ZERO;
        touch();
    }

    public void setExistingDebt(BigDecimal existingDebt) {
        this.existingDebt = existingDebt != null ? MoneyUtil.round(existingDebt) : BigDecimal.ZERO;
        touch();
    }

    public void setNeedLiquidityNearTerm(boolean needLiquidityNearTerm) {
        this.needLiquidityNearTerm = needLiquidityNearTerm;
        touch();
    }

    private void touch() {
        this.lastUpdated = LocalDateTime.now();
    }

    // ----------------- Financial Health Metrics -----------------

    /** Net monthly investable surplus (Income - Expenses). Can be negative if overspending. */
    public BigDecimal getMonthlySurplus() {
        return monthlyIncome.subtract(monthlyExpenses).setScale(2, RoundingMode.HALF_UP);
    }

    /** Savings rate percentage: (Monthly Surplus / Monthly Income) * 100. */
    public BigDecimal getSavingsRate() {
        if (monthlyIncome.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return getMonthlySurplus().multiply(new BigDecimal("100"))
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP);
    }

    /** Number of months of basic living expenses covered by available liquid savings. */
    public BigDecimal getEmergencyFundMonthsCovered() {
        if (monthlyExpenses.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return availableSavings.divide(monthlyExpenses, 2, RoundingMode.HALF_UP);
    }

    /** Debt-to-Income ratio (DTI) comparing existing debt to annual income. */
    public BigDecimal getDebtToIncomeRatio() {
        BigDecimal annualIncome = monthlyIncome.multiply(new BigDecimal("12"));
        if (annualIncome.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return existingDebt.multiply(new BigDecimal("100"))
                .divide(annualIncome, 2, RoundingMode.HALF_UP);
    }
}
