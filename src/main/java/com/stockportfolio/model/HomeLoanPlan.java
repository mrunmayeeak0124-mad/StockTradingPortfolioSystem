package com.stockportfolio.model;

import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable value object holding the financial evaluation of a home purchase and mortgage.
 */
public class HomeLoanPlan {

    private final BigDecimal housePrice;
    private final BigDecimal downPayment;
    private final BigDecimal loanAmount;
    private final BigDecimal annualInterestRate;
    private final int tenureYears;
    private final BigDecimal additionalCosts;
    private final BigDecimal monthlyEmi;
    private final BigDecimal totalRepayment;
    private final BigDecimal totalInterest;
    private final BigDecimal totalUpfrontCashRequired;

    public HomeLoanPlan(BigDecimal housePrice, BigDecimal downPayment, BigDecimal loanAmount,
                        BigDecimal annualInterestRate, int tenureYears, BigDecimal additionalCosts,
                        BigDecimal monthlyEmi, BigDecimal totalRepayment, BigDecimal totalInterest,
                        BigDecimal totalUpfrontCashRequired) {
        this.housePrice = MoneyUtil.round(housePrice);
        this.downPayment = MoneyUtil.round(downPayment);
        this.loanAmount = MoneyUtil.round(loanAmount);
        this.annualInterestRate = annualInterestRate.setScale(2, RoundingMode.HALF_UP);
        this.tenureYears = tenureYears;
        this.additionalCosts = MoneyUtil.round(additionalCosts);
        this.monthlyEmi = MoneyUtil.round(monthlyEmi);
        this.totalRepayment = MoneyUtil.round(totalRepayment);
        this.totalInterest = MoneyUtil.round(totalInterest);
        this.totalUpfrontCashRequired = MoneyUtil.round(totalUpfrontCashRequired);
    }

    public BigDecimal getHousePrice() { return housePrice; }
    public BigDecimal getDownPayment() { return downPayment; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public int getTenureYears() { return tenureYears; }
    public BigDecimal getAdditionalCosts() { return additionalCosts; }
    public BigDecimal getMonthlyEmi() { return monthlyEmi; }
    public BigDecimal getTotalRepayment() { return totalRepayment; }
    public BigDecimal getTotalInterest() { return totalInterest; }
    public BigDecimal getTotalUpfrontCashRequired() { return totalUpfrontCashRequired; }

    /** Loan-to-Value (LTV) percentage: (Loan Amount / House Price) * 100. */
    public BigDecimal getLoanToValueRatio() {
        if (housePrice.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return loanAmount.multiply(new BigDecimal("100"))
                .divide(housePrice, 2, RoundingMode.HALF_UP);
    }

    /** Down payment percentage: (Down Payment / House Price) * 100. */
    public BigDecimal getDownPaymentPercentage() {
        if (housePrice.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return downPayment.multiply(new BigDecimal("100"))
                .divide(housePrice, 2, RoundingMode.HALF_UP);
    }
}
