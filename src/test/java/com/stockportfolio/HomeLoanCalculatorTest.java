package com.stockportfolio;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.model.HomeLoanPlan;
import com.stockportfolio.util.HomeLoanCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Home Loan & EMI Calculator Tests")
class HomeLoanCalculatorTest {

    @Test
    @DisplayName("1. Standard loan EMI calculation with realistic numbers")
    void testStandardEmi() {
        // Principal: 24,00,000, Rate: 9.0%, Tenure: 20 years (240 months)
        BigDecimal principal = new BigDecimal("2400000.00");
        BigDecimal rate = new BigDecimal("9.00");
        int tenureYears = 20;

        BigDecimal emi = HomeLoanCalculator.calculateEmi(principal, rate, tenureYears);

        assertNotNull(emi);
        // Formula gives ~21,593.41
        assertTrue(emi.compareTo(new BigDecimal("21500.00")) > 0);
        assertTrue(emi.compareTo(new BigDecimal("21700.00")) < 0);
    }

    @Test
    @DisplayName("2. Zero interest edge case calculates P / n correctly")
    void testZeroInterestLoan() {
        BigDecimal principal = new BigDecimal("120000.00");
        BigDecimal zeroRate = BigDecimal.ZERO;
        int tenureYears = 1; // 12 months

        BigDecimal emi = HomeLoanCalculator.calculateEmi(principal, zeroRate, tenureYears);

        // 120,000 / 12 = 10,000.00 exactly
        assertEquals(new BigDecimal("10000.00"), emi);
    }

    @Test
    @DisplayName("3. Zero principal returns zero EMI safely")
    void testZeroPrincipal() {
        BigDecimal zeroPrincipal = BigDecimal.ZERO;
        BigDecimal rate = new BigDecimal("8.50");
        BigDecimal emi = HomeLoanCalculator.calculateEmi(zeroPrincipal, rate, 15);

        assertEquals(new BigDecimal("0.00"), emi);
    }

    @Test
    @DisplayName("4. Complete HomeLoanPlan computes total repayment, interest, and upfront cash")
    void testCompleteHomeLoanPlan() {
        BigDecimal housePrice = new BigDecimal("5000000.00");
        BigDecimal downPayment = new BigDecimal("1000000.00"); // 20% down
        BigDecimal rate = new BigDecimal("8.50");
        int tenure = 20;
        BigDecimal stampDutyRegistration = new BigDecimal("350000.00");

        HomeLoanPlan plan = HomeLoanCalculator.calculatePlan(
                housePrice, downPayment, rate, tenure, stampDutyRegistration);

        assertNotNull(plan);
        assertEquals(new BigDecimal("4000000.00"), plan.getLoanAmount());
        assertEquals(new BigDecimal("1350000.00"), plan.getTotalUpfrontCashRequired());
        assertEquals(new BigDecimal("80.00"), plan.getLoanToValueRatio());
        assertEquals(new BigDecimal("20.00"), plan.getDownPaymentPercentage());

        // Total repayment must exceed principal due to interest
        assertTrue(plan.getTotalRepayment().compareTo(plan.getLoanAmount()) > 0);
        assertTrue(plan.getTotalInterest().signum() > 0);
    }

    @Test
    @DisplayName("5. Invalid inputs throw InvalidInputException safely")
    void testInvalidInputs() {
        // Negative house price
        assertThrows(InvalidInputException.class, () ->
                HomeLoanCalculator.calculatePlan(new BigDecimal("-1000"), BigDecimal.ZERO,
                        new BigDecimal("8.0"), 10, BigDecimal.ZERO));

        // Down payment exceeding house price
        assertThrows(InvalidInputException.class, () ->
                HomeLoanCalculator.calculatePlan(new BigDecimal("500000"), new BigDecimal("600000"),
                        new BigDecimal("8.0"), 10, BigDecimal.ZERO));

        // Zero tenure
        assertThrows(InvalidInputException.class, () ->
                HomeLoanCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("8.0"), 0));

        // Negative interest rate
        assertThrows(InvalidInputException.class, () ->
                HomeLoanCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("-5.0"), 10));
    }
}
