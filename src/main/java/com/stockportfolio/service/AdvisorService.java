package com.stockportfolio.service;

import com.stockportfolio.model.AdvisorRecommendation;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.RecommendationStatus;
import com.stockportfolio.model.RiskPreference;
import com.stockportfolio.model.Trader;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Interface defining the educational, rule-based financial advisor capabilities.
 */
public interface AdvisorService {

    /** Retrieves the active profile for a trader, or creates a sensible default if none exists. */
    FinancialProfile getProfile(String traderId);

    /** Persists updates to the trader's financial profile. */
    void saveProfile(FinancialProfile profile);

    /**
     * Executes the explainable rule-based engine against the trader's cash, open portfolio,
     * goals, and financial profile to produce fresh recommendations.
     */
    List<AdvisorRecommendation> generateRecommendations(Trader trader, FinancialProfile profile,
                                                        List<FinancialGoal> goals);

    /** Loads previously generated recommendations from the database. */
    List<AdvisorRecommendation> getRecommendations(String traderId);

    /** Updates the status of an existing recommendation (e.g., REVIEWED, ACCEPTED, COMPLETED). */
    void updateRecommendationStatus(String recommendationId, RecommendationStatus newStatus);

    /** Returns illustrative benchmark target asset allocation percentages for a risk preference. */
    Map<AssetCategory, BigDecimal> getTargetAllocation(RiskPreference riskPreference);

    /** Generates an educational explanation comparing actual portfolio allocations with benchmark targets. */
    String explainAllocationDifference(Map<AssetCategory, BigDecimal> actual,
                                       Map<AssetCategory, BigDecimal> target);
}
