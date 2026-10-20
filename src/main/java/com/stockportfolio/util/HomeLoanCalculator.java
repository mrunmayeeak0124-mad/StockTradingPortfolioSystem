package com.stockportfolio.util;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.model.HomeLoanPlan;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Pure mathematical calculator for Home Purchase, Mortgage Loan, and EMI planning.
 */
public final class HomeLoanCalculator {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private HomeLoanCalculator() { }

    /**
     * Calculates the complete Home Purchase & Mortgage plan.
     *
     * @param housePrice           Agreed purchase price of the property
     * @param downPayment          Available cash down payment (must be <= housePrice)
     * @param annualInterestRate   Annual interest percentage (e.g. 8.50 for 8.5%)
     * @param tenureYears          Duration of the loan in years (e.g. 20)
     * @param additionalCosts      Stamp duty, registration fees, and legal charges
     * @return Fully populated HomeLoanPlan
     */
    public static HomeLoanPlan calculatePlan(BigDecimal housePrice, BigDecimal downPayment,
                                             BigDecimal annualInterestRate, int tenureYears,
                                             BigDecimal additionalCosts) {
        validateInputs(housePrice, downPayment, annualInterestRate, tenureYears, additionalCosts);

        BigDecimal principal = housePrice.subtract(downPayment).setScale(2, RoundingMode.HALF_UP);
        BigDecimal emi = calculateEmi(principal, annualInterestRate, tenureYears);

        int totalMonths = tenureYears * 12;
        BigDecimal totalRepayment = MoneyUtil.round(emi.multiply(BigDecimal.valueOf(totalMonths)));
        BigDecimal totalInterest = totalRepayment.subtract(principal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal upfrontCash = downPayment.add(additionalCosts).setScale(2, RoundingMode.HALF_UP);

        return new HomeLoanPlan(housePrice, downPayment, principal, annualInterestRate,
                tenureYears, additionalCosts, emi, totalRepayment, totalInterest, upfrontCash);
    }

    /**
     * Calculates the Monthly Equated Installment (EMI).
     * Handles zero-interest edge case safely without dividing by zero.
     */
    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, int tenureYears) {
        if (principal == null || principal.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (tenureYears <= 0) {
            throw new InvalidInputException("Tenure must be at least 1 year.");
        }
        int totalMonths = tenureYears * 12;

        if (annualInterestRate == null || annualInterestRate.signum() == 0) {
            // Zero-interest loan edge case: P / n
            return principal.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        }

        if (annualInterestRate.signum() < 0) {
            throw new InvalidInputException("Interest rate cannot be negative.");
        }

        // r = annualRate / (12 * 100)
        double monthlyRate = annualInterestRate.doubleValue() / 1200.0;
        double p = principal.doubleValue();
        double factor = Math.pow(1.0 + monthlyRate, totalMonths);

        // EMI = [P * r * (1 + r)^n] / [(1 + r)^n - 1]
        double emiAmount = (p * monthlyRate * factor) / (factor - 1.0);

        return new BigDecimal(Double.toString(emiAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, int tenureYears) {
        return calculateEmi(principal, annualInterestRate, tenureYears);
    }

    private static void validateInputs(BigDecimal housePrice, BigDecimal downPayment,
                                       BigDecimal annualInterestRate, int tenureYears,
                                       BigDecimal additionalCosts) {
        if (housePrice == null || housePrice.signum() <= 0) {
            throw new InvalidInputException("House price must be greater than zero.");
        }
        if (downPayment == null || downPayment.signum() < 0) {
            throw new InvalidInputException("Down payment cannot be negative.");
        }
        if (downPayment.compareTo(housePrice) > 0) {
            throw new InvalidInputException("Down payment cannot be greater than the house price.");
        }
        if (annualInterestRate == null || annualInterestRate.signum() < 0) {
            throw new InvalidInputException("Interest rate cannot be negative.");
        }
        if (tenureYears <= 0 || tenureYears > 40) {
            throw new InvalidInputException("Loan tenure must be between 1 and 40 years.");
        }
        if (additionalCosts == null || additionalCosts.signum() < 0) {
            throw new InvalidInputException("Additional costs cannot be negative.");
        }
    }
}
