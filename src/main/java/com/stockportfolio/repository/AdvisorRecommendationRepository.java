package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.AdvisorRecommendation;
import com.stockportfolio.model.RecommendationCategory;
import com.stockportfolio.model.RecommendationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database persistence for educational advisor recommendations.
 */
public class AdvisorRecommendationRepository {

    public List<AdvisorRecommendation> findByTraderId(String traderId) {
        String sql = """
            SELECT recommendation_id, goal_id, created_at, category, title, summary,
                   reasoning, assumptions, status
            FROM advisor_recommendations
            WHERE trader_id = ?
            ORDER BY created_at DESC
            """;
        List<AdvisorRecommendation> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AdvisorRecommendation(
                            rs.getString("recommendation_id"),
                            traderId,
                            rs.getString("goal_id"),
                            LocalDateTime.parse(rs.getString("created_at")),
                            RecommendationCategory.valueOf(rs.getString("category")),
                            rs.getString("title"),
                            rs.getString("summary"),
                            rs.getString("reasoning"),
                            rs.getString("assumptions"),
                            RecommendationStatus.valueOf(rs.getString("status"))
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load recommendations: " + e.getMessage(), e);
        }
    }

    public void save(AdvisorRecommendation recommendation) {
        String sql = """
            INSERT INTO advisor_recommendations (
                recommendation_id, trader_id, goal_id, created_at, category,
                title, summary, reasoning, assumptions, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(recommendation_id) DO UPDATE SET
                status = excluded.status
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recommendation.getRecommendationId());
            ps.setString(2, recommendation.getTraderId());
            ps.setString(3, recommendation.getGoalId());
            ps.setString(4, recommendation.getCreatedAt().toString());
            ps.setString(5, recommendation.getCategory().name());
            ps.setString(6, recommendation.getTitle());
            ps.setString(7, recommendation.getSummary());
            ps.setString(8, recommendation.getReasoning());
            ps.setString(9, recommendation.getAssumptions());
            ps.setString(10, recommendation.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save recommendation: " + e.getMessage(), e);
        }
    }

    public void updateStatus(String recommendationId, RecommendationStatus newStatus) {
        String sql = "UPDATE advisor_recommendations SET status = ? WHERE recommendation_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, recommendationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update recommendation status: " + e.getMessage(), e);
        }
    }
}
