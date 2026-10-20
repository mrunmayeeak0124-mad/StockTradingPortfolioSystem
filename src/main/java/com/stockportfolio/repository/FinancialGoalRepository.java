package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.GoalCategory;
import com.stockportfolio.model.GoalStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for Financial Goals.
 */
public class FinancialGoalRepository {

    public List<FinancialGoal> findByTraderId(String traderId) {
        String sql = """
            SELECT goal_id, name, category, target_amount, current_amount, target_date, status
            FROM financial_goals
            WHERE trader_id = ?
            ORDER BY target_date ASC
            """;
        List<FinancialGoal> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FinancialGoal(
                            rs.getString("goal_id"),
                            traderId,
                            rs.getString("name"),
                            GoalCategory.valueOf(rs.getString("category")),
                            new BigDecimal(rs.getString("target_amount")),
                            new BigDecimal(rs.getString("current_amount")),
                            LocalDate.parse(rs.getString("target_date")),
                            GoalStatus.valueOf(rs.getString("status"))
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load financial goals: " + e.getMessage(), e);
        }
    }

    public void save(FinancialGoal goal) {
        try (Connection conn = DatabaseManager.getConnection()) {
            save(conn, goal);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save financial goal: " + e.getMessage(), e);
        }
    }

    public void save(Connection conn, FinancialGoal goal) throws SQLException {
        String sql = """
            INSERT INTO financial_goals (
                goal_id, trader_id, name, category, target_amount, current_amount, target_date, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(goal_id) DO UPDATE SET
                name = excluded.name,
                category = excluded.category,
                target_amount = excluded.target_amount,
                current_amount = excluded.current_amount,
                target_date = excluded.target_date,
                status = excluded.status
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.getGoalId());
            ps.setString(2, goal.getTraderId());
            ps.setString(3, goal.getName());
            ps.setString(4, goal.getCategory().name());
            ps.setString(5, goal.getTargetAmount().toPlainString());
            ps.setString(6, goal.getCurrentAmount().toPlainString());
            ps.setString(7, goal.getTargetDate().toString());
            ps.setString(8, goal.getStatus().name());
            ps.executeUpdate();
        }
    }

    public void delete(String goalId) {
        String sql = "DELETE FROM financial_goals WHERE goal_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goalId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete financial goal: " + e.getMessage(), e);
        }
    }
}
