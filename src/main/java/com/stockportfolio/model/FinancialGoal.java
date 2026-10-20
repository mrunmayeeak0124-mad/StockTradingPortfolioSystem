package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.util.IdGenerator;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * A specific financial milestone tracked by the trader.
 */
public class FinancialGoal {

    private final String goalId;
    private final String traderId;
    private String name;
    private GoalCategory category;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private GoalStatus status;

    public FinancialGoal(String traderId, String name, GoalCategory category,
                         BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate) {
        this(IdGenerator.generate("GOL"), traderId, name, category, targetAmount, currentAmount,
                targetDate, GoalStatus.IN_PROGRESS);
    }

    public FinancialGoal(String goalId, String traderId, String name, GoalCategory category,
                         BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate) {
        this(goalId, traderId, name, category, targetAmount, currentAmount, targetDate, GoalStatus.IN_PROGRESS);
    }

    public FinancialGoal(String goalId, String traderId, String name, GoalCategory category,
                         BigDecimal targetAmount, BigDecimal currentAmount,
                         LocalDate targetDate, GoalStatus status) {
        if (goalId == null || goalId.isBlank()) {
            throw new InvalidInputException("Goal ID cannot be empty.");
        }
        if (traderId == null || traderId.isBlank()) {
            throw new InvalidInputException("Trader ID cannot be empty.");
        }
        this.goalId = goalId.trim();
        this.traderId = traderId.trim();
        setName(name);
        setCategory(category);
        setTargetAmount(targetAmount);
        setCurrentAmount(currentAmount);
        setTargetDate(targetDate);
        setStatus(status);
    }

    public String getGoalId() { return goalId; }
    public String getTraderId() { return traderId; }
    public String getName() { return name; }
    public GoalCategory getCategory() { return category; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public LocalDate getTargetDate() { return targetDate; }
    public GoalStatus getStatus() { return status; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Goal name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setCategory(GoalCategory category) {
        if (category == null) {
            throw new InvalidInputException("Goal category is required.");
        }
        this.category = category;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new InvalidInputException("Target amount must be greater than zero.");
        }
        this.targetAmount = MoneyUtil.round(targetAmount);
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        if (currentAmount == null || currentAmount.signum() < 0) {
            throw new InvalidInputException("Current amount cannot be negative.");
        }
        this.currentAmount = MoneyUtil.round(currentAmount);
        if (this.targetAmount != null && this.currentAmount.compareTo(this.targetAmount) >= 0) {
            this.status = GoalStatus.ACHIEVED;
        }
    }

    public void setTargetDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new InvalidInputException("Target date is required.");
        }
        this.targetDate = targetDate;
    }

    public void setStatus(GoalStatus status) {
        if (status == null) {
            throw new InvalidInputException("Goal status is required.");
        }
        this.status = status;
    }

    /** Add savings towards this goal. */
    public void addSavings(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Savings amount must be positive.");
        }
        setCurrentAmount(this.currentAmount.add(amount));
    }

    /** Amount left to reach the target amount. Zero if already achieved. */
    public BigDecimal getRemainingAmount() {
        BigDecimal diff = targetAmount.subtract(currentAmount);
        return diff.signum() > 0 ? MoneyUtil.round(diff) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /** Completion percentage between 0.00% and 100.00%+. Safe against division by zero. */
    public BigDecimal getProgressPercentage() {
        if (targetAmount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return currentAmount.multiply(new BigDecimal("100"))
                .divide(targetAmount, 2, RoundingMode.HALF_UP);
    }

    /** Months remaining until target date from today. Negative if overdue. */
    public long getMonthsRemaining() {
        LocalDate today = LocalDate.now();
        return ChronoUnit.MONTHS.between(today.withDayOfMonth(1), targetDate.withDayOfMonth(1));
    }

    public boolean isAchieved() {
        return status == GoalStatus.ACHIEVED || currentAmount.compareTo(targetAmount) >= 0;
    }

    @Override
    public String toString() {
        return name + " (" + getProgressPercentage() + "% of " + targetAmount + ")";
    }
}
