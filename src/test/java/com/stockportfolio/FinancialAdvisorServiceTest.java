package com.stockportfolio;

import com.stockportfolio.model.AdvisorRecommendation;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.GoalCategory;
import com.stockportfolio.model.RecommendationCategory;
import com.stockportfolio.model.RecommendationStatus;
import com.stockportfolio.model.RiskPreference;
import com.stockportfolio.model.Stock;
import com.stockportfolio.model.Trader;
import com.stockportfolio.repository.AdvisorRecommendationRepository;
import com.stockportfolio.repository.FinancialProfileRepository;
import com.stockportfolio.service.FinancialAdvisorService;
import com.stockportfolio.service.GoalPlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Financial Advisor Service Tests")
class FinancialAdvisorServiceTest {

    private FinancialAdvisorService advisorService;
    private GoalPlanningService goalService;

    @BeforeEach
    void setUp() {
        advisorService = new FinancialAdvisorService(
                new StubProfileRepository(),
                new StubRecommendationRepository()
        );
        goalService = new GoalPlanningService();
    }

    @Test
    @DisplayName("1. Benchmark target asset allocations for different risk preferences")
    void testTargetAllocations() {
        Map<AssetCategory, BigDecimal> conservative = advisorService.getTargetAllocation(RiskPreference.CONSERVATIVE);
        assertEquals(new BigDecimal("20.00"), conservative.get(AssetCategory.STOCK));
        assertEquals(new BigDecimal("60.00"), conservative.get(AssetCategory.BOND));

        Map<AssetCategory, BigDecimal> growth = advisorService.getTargetAllocation(RiskPreference.GROWTH_ORIENTED);
        assertEquals(new BigDecimal("65.00"), growth.get(AssetCategory.STOCK));
        assertEquals(new BigDecimal("10.00"), growth.get(AssetCategory.BOND));
    }

    @Test
    @DisplayName("2. Emergency fund gap triggers explainable safety recommendation")
    void testEmergencyFundRule() {
        Trader trader = new Trader("T1", "John", "john@example.com", new BigDecimal("10000.00"));
        FinancialProfile profile = new FinancialProfile(
                "T1", RiskPreference.BALANCED, "Wealth",
                new BigDecimal("500000"), 5,
                new BigDecimal("30000.00"), // available savings
                new BigDecimal("50000.00"), // monthly income
                new BigDecimal("40000.00"), // monthly expenses -> savings covers only 0.75 months!
                BigDecimal.ZERO, false, LocalDateTime.now()
        );

        List<AdvisorRecommendation> recs = advisorService.generateRecommendations(trader, profile, List.of());

        boolean hasEmergencyRec = recs.stream()
                .anyMatch(r -> r.getCategory() == RecommendationCategory.EMERGENCY_FUND);
        assertTrue(hasEmergencyRec, "Should trigger emergency fund recommendation when < 3 months covered.");
    }

    @Test
    @DisplayName("3. High concentration in a single asset triggers diversification warning")
    void testConcentrationRiskRule() {
        Trader trader = new Trader("T2", "Alice", "alice@example.com", new BigDecimal("5000.00"));
        Stock bigStock = new Stock("S1", "Mega Corp", "MEGA", "NSE", "INR", "Tech", new BigDecimal("1000.00"));
        trader.getPortfolio().recordBuy(bigStock, 80, new BigDecimal("80000.00")); // 80,000 / 80,000 = 100%

        FinancialProfile profile = new FinancialProfile("T2");

        List<AdvisorRecommendation> recs = advisorService.generateRecommendations(trader, profile, List.of());

        boolean hasDiversificationWarning = recs.stream()
                .anyMatch(r -> r.getCategory() == RecommendationCategory.DIVERSIFICATION);
        assertTrue(hasDiversificationWarning, "Should warn about concentration when an asset exceeds 25%.");
    }

    @Test
    @DisplayName("4. Goal savings requirement calculation with linear and return assumptions")
    void testGoalSavingsCalculation() {
        FinancialGoal goal = new FinancialGoal(
                "G1", "T1", "Retirement", GoalCategory.RETIREMENT,
                new BigDecimal("120000.00"), // target
                new BigDecimal("0.00"),      // current
                LocalDate.now().plusMonths(12) // 12 months left
        );

        // Linear: 120,000 / 12 = 10,000.00 / month
        BigDecimal linearMonthly = goalService.calculateRequiredMonthlySavings(goal, BigDecimal.ZERO);
        assertEquals(new BigDecimal("10000.00"), linearMonthly);

        // With 12% annual return, monthly required should be less than linear
        BigDecimal compoundingMonthly = goalService.calculateRequiredMonthlySavings(goal, new BigDecimal("12.00"));
        assertTrue(compoundingMonthly.compareTo(linearMonthly) < 0,
                "Compounding returns reduce the required monthly cash contribution.");
    }

    @Test
    @DisplayName("5. Recommendation status tracking lifecycle")
    void testRecommendationLifecycle() {
        AdvisorRecommendation rec = new AdvisorRecommendation(
                "T1", null, RecommendationCategory.ASSET_ALLOCATION,
                "Title", "Summary", "Reasoning", "Assumptions"
        );
        assertEquals(RecommendationStatus.NEW, rec.getStatus());

        rec.markReviewed();
        assertEquals(RecommendationStatus.REVIEWED, rec.getStatus());

        rec.markAccepted();
        assertEquals(RecommendationStatus.ACCEPTED, rec.getStatus());

        rec.markCompleted();
        assertEquals(RecommendationStatus.COMPLETED, rec.getStatus());
    }

    @Test
    @DisplayName("6. 50/30/20 and 70/15/15 framework allocation calculations")
    void testBudgetingFrameworkCalculations() {
        FinancialProfile profile = new FinancialProfile("T1");
        profile.setMonthlyIncome(new BigDecimal("100000.00")); // 1 Lakh monthly income
        profile.setMonthlyExpenses(new BigDecimal("60000.00"));
        profile.setExistingDebt(new BigDecimal("50000.00"));

        // 50/30/20 rule:
        assertEquals(new BigDecimal("50000.00"), profile.getRecommendedNeedsFifty());
        assertEquals(new BigDecimal("30000.00"), profile.getRecommendedWantsThirty());
        assertEquals(new BigDecimal("20000.00"), profile.getRecommendedSavingsTwenty());

        // 70/15/15 rule:
        assertEquals(new BigDecimal("70000.00"), profile.getRecommendedLivingExpensesSeventy());
        assertEquals(new BigDecimal("15000.00"), profile.getRecommendedInvestingFifteen());
        assertEquals(new BigDecimal("15000.00"), profile.getRecommendedDebtOrEmergencyFifteen());
        assertEquals(new BigDecimal("30000.00"), profile.getAcceleratedPostDebtInvestingThirty());
    }

    @Test
    @DisplayName("7. 50/30/20 savings gap and 70/15/15 debt rules trigger advisory recommendations")
    void testBudgetingRulesRecommendationTriggers() {
        Trader trader = new Trader("T3", "Rohan", "rohan@example.com", new BigDecimal("5000.00"));
        FinancialProfile profile = new FinancialProfile(
                "T3", RiskPreference.BALANCED, "Wealth",
                new BigDecimal("500000"), 5,
                new BigDecimal("200000.00"), // available savings (healthy buffer)
                new BigDecimal("50000.00"),  // income
                new BigDecimal("45000.00"),  // expenses = 90% of income! Surplus = 5000 (only 10%, below 20%)
                new BigDecimal("40000.00"),  // existing debt
                false, LocalDateTime.now()
        );

        List<AdvisorRecommendation> recs = advisorService.generateRecommendations(trader, profile, List.of());

        boolean hasBudgetingRec = recs.stream()
                .anyMatch(r -> r.getCategory() == RecommendationCategory.BUDGETING_FRAMEWORK);
        assertTrue(hasBudgetingRec, "Should generate recommendation when savings rate is under 20% or debt exists.");
    }

    // ---------------- Stub repositories for isolated unit testing ----------------

    private static class StubProfileRepository extends FinancialProfileRepository {
        private FinancialProfile saved;

        @Override
        public void save(FinancialProfile profile) { this.saved = profile; }
    }

    private static class StubRecommendationRepository extends AdvisorRecommendationRepository {
        private final List<AdvisorRecommendation> list = new ArrayList<>();

        @Override
        public void save(AdvisorRecommendation recommendation) {
            list.removeIf(r -> r.getRecommendationId().equals(recommendation.getRecommendationId()));
            list.add(recommendation);
        }

        @Override
        public List<AdvisorRecommendation> findByTraderId(String traderId) {
            return new ArrayList<>(list);
        }

        @Override
        public void updateStatus(String recommendationId, RecommendationStatus newStatus) {
            for (AdvisorRecommendation r : list) {
                if (r.getRecommendationId().equals(recommendationId)) {
                    r.setStatus(newStatus);
                    break;
                }
            }
        }
    }
}
