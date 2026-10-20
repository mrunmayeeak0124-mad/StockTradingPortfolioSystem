package com.stockportfolio.service;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.repository.FinancialGoalRepository;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service managing financial goals, milestone tracking, and monthly savings requirements.
 */
public class GoalPlanningService {

    private final FinancialGoalRepository repository;

    public GoalPlanningService() {
        this(new FinancialGoalRepository());
    }

    public GoalPlanningService(FinancialGoalRepository repository) {
        this.repository = repository;
    }

    public List<FinancialGoal> getGoals(String traderId) {
        return repository.findByTraderId(traderId);
    }

    public void addGoal(FinancialGoal goal) {
        if (goal == null) {
            throw new InvalidInputException("Financial goal cannot be null.");
        }
        repository.save(goal);
    }

    public void updateGoal(FinancialGoal goal) {
        if (goal == null) {
            throw new InvalidInputException("Financial goal cannot be null.");
        }
        repository.save(goal);
    }

    public void deleteGoal(String goalId) {
        repository.delete(goalId);
    }

    public void contributeToGoal(String goalId, String traderId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Contribution amount must be positive.");
        }
        List<FinancialGoal> goals = repository.findByTraderId(traderId);
        for (FinancialGoal g : goals) {
            if (g.getGoalId().equals(goalId)) {
                g.addSavings(amount);
                repository.save(g);
                return;
            }
        }
        throw new InvalidInputException("Goal not found: " + goalId);
    }

    /**
     * Calculates the monthly savings needed to reach the goal by target date.
     * Optionally incorporates a hypothetical annual investment return rate.
     *
     * @param goal                      The financial goal
     * @param hypotheticalAnnualReturn  Annual percentage return (e.g. 8.0 for 8%, or 0.0 for pure cash savings)
     * @return Required monthly contribution
     */
    public BigDecimal calculateRequiredMonthlySavings(FinancialGoal goal, BigDecimal hypotheticalAnnualReturn) {
        if (goal == null) {
            throw new InvalidInputException("Goal is required.");
        }
        BigDecimal remaining = goal.getRemainingAmount();
        if (remaining.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long months = Math.max(1, goal.getMonthsRemaining());

        if (hypotheticalAnnualReturn == null || hypotheticalAnnualReturn.signum() <= 0) {
            // Pure linear savings: Remaining / Months
            return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        // Future Value of Ordinary Annuity formula:
        // FV = PMT * [ ((1 + r)^n - 1) / r ]
        // PMT = FV * r / [ (1 + r)^n - 1 ]
        double r = hypotheticalAnnualReturn.doubleValue() / 1200.0;
        double fv = remaining.doubleValue();
        double factor = Math.pow(1.0 + r, months) - 1.0;

        if (factor <= 0) {
            return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        double pmt = (fv * r) / factor;
        return new BigDecimal(Double.toString(pmt)).setScale(2, RoundingMode.HALF_UP);
    }
}
