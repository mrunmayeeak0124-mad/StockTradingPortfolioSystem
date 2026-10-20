package com.stockportfolio.service;

import com.stockportfolio.model.AdvisorRecommendation;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.RecommendationCategory;
import com.stockportfolio.model.RecommendationStatus;
import com.stockportfolio.model.RiskPreference;
import com.stockportfolio.model.Trader;
import com.stockportfolio.repository.AdvisorRecommendationRepository;
import com.stockportfolio.repository.FinancialProfileRepository;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Transparent, rule-based educational financial advisor service.
 * Translates personal profile data, cash, and holdings into explainable recommendations.
 */
public class FinancialAdvisorService implements AdvisorService {

    private final FinancialProfileRepository profileRepository;
    private final AdvisorRecommendationRepository recommendationRepository;

    public FinancialAdvisorService() {
        this(new FinancialProfileRepository(), new AdvisorRecommendationRepository());
    }

    public FinancialAdvisorService(FinancialProfileRepository profileRepository,
                                  AdvisorRecommendationRepository recommendationRepository) {
        this.profileRepository = profileRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Override
    public FinancialProfile getProfile(String traderId) {
        return profileRepository.findByTraderId(traderId)
                .orElseGet(() -> {
                    FinancialProfile defaultProfile = new FinancialProfile(traderId);
                    profileRepository.save(defaultProfile);
                    return defaultProfile;
                });
    }

    @Override
    public void saveProfile(FinancialProfile profile) {
        profileRepository.save(profile);
    }

    @Override
    public List<AdvisorRecommendation> getRecommendations(String traderId) {
        return recommendationRepository.findByTraderId(traderId);
    }

    @Override
    public void updateRecommendationStatus(String recommendationId, RecommendationStatus newStatus) {
        recommendationRepository.updateStatus(recommendationId, newStatus);
    }

    @Override
    public Map<AssetCategory, BigDecimal> getTargetAllocation(RiskPreference riskPreference) {
        Map<AssetCategory, BigDecimal> targets = new EnumMap<>(AssetCategory.class);
        if (riskPreference == null) {
            riskPreference = RiskPreference.BALANCED;
        }
        switch (riskPreference) {
            case CONSERVATIVE -> {
                targets.put(AssetCategory.STOCK, new BigDecimal("20.00"));
                targets.put(AssetCategory.ETF, new BigDecimal("20.00"));
                targets.put(AssetCategory.BOND, new BigDecimal("60.00"));
            }
            case BALANCED -> {
                targets.put(AssetCategory.STOCK, new BigDecimal("45.00"));
                targets.put(AssetCategory.ETF, new BigDecimal("25.00"));
                targets.put(AssetCategory.BOND, new BigDecimal("30.00"));
            }
            case GROWTH_ORIENTED -> {
                targets.put(AssetCategory.STOCK, new BigDecimal("65.00"));
                targets.put(AssetCategory.ETF, new BigDecimal("25.00"));
                targets.put(AssetCategory.BOND, new BigDecimal("10.00"));
            }
        }
        return targets;
    }

    @Override
    public String explainAllocationDifference(Map<AssetCategory, BigDecimal> actual,
                                             Map<AssetCategory, BigDecimal> target) {
        StringBuilder sb = new StringBuilder();
        for (AssetCategory cat : AssetCategory.values()) {
            BigDecimal act = actual.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal tgt = target.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal diff = act.subtract(tgt);

            if (diff.abs().compareTo(new BigDecimal("10.00")) > 0) {
                if (diff.signum() > 0) {
                    sb.append("• ").append(cat).append(" is over-weighted by ").append(diff.abs()).append("% ")
                      .append("compared to your target. Consider trimming to restore balance.\n");
                } else {
                    sb.append("• ").append(cat).append(" is under-weighted by ").append(diff.abs()).append("% ")
                      .append("compared to your target. Future savings could prioritize this asset class.\n");
                }
            }
        }
        if (sb.isEmpty()) {
            return "Your portfolio allocation is closely aligned with your target risk profile within normal drift thresholds.";
        }
        return sb.toString().trim();
    }

    @Override
    public List<AdvisorRecommendation> generateRecommendations(Trader trader, FinancialProfile profile,
                                                               List<FinancialGoal> goals) {
        List<AdvisorRecommendation> list = new ArrayList<>();
        String traderId = trader.getUserId();

        // ---------------- Rule 1: Emergency Buffer ----------------
        BigDecimal monthsCovered = profile.getEmergencyFundMonthsCovered();
        if (monthsCovered.compareTo(new BigDecimal("3.00")) < 0) {
            list.add(new AdvisorRecommendation(
                    traderId, null, RecommendationCategory.EMERGENCY_FUND,
                    "Build Liquid Emergency Buffer",
                    "Your liquid savings currently cover only " + monthsCovered + " months of living expenses.",
                    "Financial prudence requires keeping 3 to 6 months of expenses in safe, instantly accessible instruments (such as bank deposits) so you are never forced to sell investments during a market downturn.",
                    "Assumes declared monthly expenses of ₹" + profile.getMonthlyExpenses() + " and available cash of ₹" + profile.getAvailableSavings() + "."
            ));
        }

        // ---------------- Rule 2: Debt Burden & Cashflow ----------------
        if (profile.getDebtToIncomeRatio().compareTo(new BigDecimal("35.00")) > 0) {
            list.add(new AdvisorRecommendation(
                    traderId, null, RecommendationCategory.DEBT_MANAGEMENT,
                    "High Debt-to-Income Exposure",
                    "Your existing liabilities represent " + profile.getDebtToIncomeRatio() + "% of your annual income.",
                    "Carrying high debt obligations restricts cashflow resilience. High-interest loan interest usually outweighs realistic long-term stock market returns.",
                    "Based on total debt of ₹" + profile.getExistingDebt() + " against annual income."
            ));
        }

        // ---------------- Rule 3: Single Holding Concentration ----------------
        BigDecimal totalPortfolioValue = trader.getPortfolio().getTotalMarketValue();
        if (totalPortfolioValue.signum() > 0) {
            for (Position p : trader.getPortfolio().getPositions()) {
                BigDecimal weight = p.getMarketValue().multiply(new BigDecimal("100"))
                        .divide(totalPortfolioValue, 2, RoundingMode.HALF_UP);
                if (weight.compareTo(new BigDecimal("25.00")) > 0) {
                    list.add(new AdvisorRecommendation(
                            traderId, null, RecommendationCategory.DIVERSIFICATION,
                            "High Concentration in " + p.getAsset().getSymbol(),
                            p.getAsset().getName() + " comprises " + weight + "% of your total investment holdings.",
                            "When an individual asset exceeds 25% of your portfolio, company-specific risks (unsystematic risk) can heavily depress your overall portfolio value.",
                            "Calculated from current position market value of ₹" + p.getMarketValue() + " against total portfolio of ₹" + totalPortfolioValue + "."
                    ));
                }
            }
        }

        // ---------------- Rule 4: Asset Allocation Drift ----------------
        if (totalPortfolioValue.signum() > 0) {
            Map<AssetCategory, BigDecimal> actual = trader.getPortfolio().getAllocationByCategory();
            Map<AssetCategory, BigDecimal> target = getTargetAllocation(profile.getRiskPreference());

            BigDecimal actualEquity = actual.getOrDefault(AssetCategory.STOCK, BigDecimal.ZERO)
                    .add(actual.getOrDefault(AssetCategory.ETF, BigDecimal.ZERO));
            BigDecimal targetEquity = target.getOrDefault(AssetCategory.STOCK, BigDecimal.ZERO)
                    .add(target.getOrDefault(AssetCategory.ETF, BigDecimal.ZERO));

            BigDecimal equityDrift = actualEquity.subtract(targetEquity);
            if (equityDrift.abs().compareTo(new BigDecimal("15.00")) > 0) {
                if (equityDrift.signum() > 0) {
                    list.add(new AdvisorRecommendation(
                            traderId, null, RecommendationCategory.ASSET_ALLOCATION,
                            "Equity Over-Exposure Warning",
                            "Your equity exposure (" + actualEquity + "%) exceeds your " + profile.getRiskPreference() + " benchmark (" + targetEquity + "%).",
                            "Higher equity weighting exposes your wealth to sharp market corrections. Consider allocating future contributions to fixed-income bonds or cash equivalents.",
                            "Benchmark based on illustrative " + profile.getRiskPreference() + " risk profile."
                    ));
                } else {
                    list.add(new AdvisorRecommendation(
                            traderId, null, RecommendationCategory.ASSET_ALLOCATION,
                            "Conservative Allocation Drag",
                            "Your equity exposure (" + actualEquity + "%) is significantly below target (" + targetEquity + "%).",
                            "Holding disproportionate cash and bonds over long horizons risks wealth erosion from inflation.",
                            "Benchmark target equity is " + targetEquity + "%."
                    ));
                }
            }
        }

        // ---------------- Rule 5: Goal Savings Feasibility ----------------
        if (goals != null && !goals.isEmpty()) {
            BigDecimal monthlySurplus = profile.getMonthlySurplus();
            for (FinancialGoal goal : goals) {
                if (!goal.isAchieved()) {
                    long monthsLeft = Math.max(1, goal.getMonthsRemaining());
                    BigDecimal monthlyRequired = goal.getRemainingAmount().divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.HALF_UP);

                    if (monthlySurplus.signum() <= 0 || monthlyRequired.compareTo(monthlySurplus) > 0) {
                        list.add(new AdvisorRecommendation(
                                traderId, goal.getGoalId(), RecommendationCategory.GOAL_SAVINGS,
                                "Goal Deficit: " + goal.getName(),
                                "Reaching '" + goal.getName() + "' requires saving ₹" + monthlyRequired + "/month, exceeding current surplus of ₹" + monthlySurplus + "/month.",
                                "To meet this goal on time, either increase your monthly savings, trim discretionary spending, or adjust the target date further out.",
                                "Assumes linear monthly contributions across " + monthsLeft + " remaining months."
                        ));
                    }
                }
            }
        }

        // ---------------- Rule 6: Near-Term Liquidity Needs ----------------
        if (profile.isNeedLiquidityNearTerm() && trader.getCashBalance().compareTo(new BigDecimal("20000.00")) < 0) {
            list.add(new AdvisorRecommendation(
                    traderId, null, RecommendationCategory.RISK_ALIGNMENT,
                    "Near-Term Cash Requirement Flag",
                    "You flagged an upcoming liquidity requirement, but available cash is under ₹20,000.",
                    "When cash will be needed within 12 months, avoid deploying funds into volatile equities. Keep required sums in liquid cash or short-term debt instruments.",
                    "Based on profile flag for near-term cash needs."
            ));
        }

        // Persist newly generated recommendations
        for (AdvisorRecommendation rec : list) {
            recommendationRepository.save(rec);
        }

        return recommendationRepository.findByTraderId(traderId);
    }
}
