package com.stockportfolio.repository;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.RiskPreference;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles database persistence for the trader's financial profile.
 */
public class FinancialProfileRepository {

    public Optional<FinancialProfile> findByTraderId(String traderId) {
        String sql = """
            SELECT risk_preference, primary_goal, target_amount, time_horizon_years,
                   available_savings, monthly_income, monthly_expenses, existing_debt,
                   need_liquidity_near_term, last_updated
            FROM financial_profiles
            WHERE trader_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, traderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RiskPreference risk = RiskPreference.valueOf(rs.getString("risk_preference"));
                    String primaryGoal = rs.getString("primary_goal");
                    BigDecimal target = new BigDecimal(rs.getString("target_amount"));
                    int horizon = rs.getInt("time_horizon_years");
                    BigDecimal savings = new BigDecimal(rs.getString("available_savings"));
                    BigDecimal income = new BigDecimal(rs.getString("monthly_income"));
                    BigDecimal expenses = new BigDecimal(rs.getString("monthly_expenses"));
                    BigDecimal debt = new BigDecimal(rs.getString("existing_debt"));
                    boolean liquidity = rs.getInt("need_liquidity_near_term") == 1;
                    LocalDateTime updated = LocalDateTime.parse(rs.getString("last_updated"));

                    return Optional.of(new FinancialProfile(
                            traderId, risk, primaryGoal, target, horizon, savings,
                            income, expenses, debt, liquidity, updated));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load financial profile: " + e.getMessage(), e);
        }
    }

    public void save(FinancialProfile profile) {
        try (Connection conn = DatabaseManager.getConnection()) {
            save(conn, profile);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save financial profile: " + e.getMessage(), e);
        }
    }

    public void save(Connection conn, FinancialProfile profile) throws SQLException {
        String sql = """
            INSERT INTO financial_profiles (
                trader_id, risk_preference, primary_goal, target_amount, time_horizon_years,
                available_savings, monthly_income, monthly_expenses, existing_debt,
                need_liquidity_near_term, last_updated
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(trader_id) DO UPDATE SET
                risk_preference = excluded.risk_preference,
                primary_goal = excluded.primary_goal,
                target_amount = excluded.target_amount,
                time_horizon_years = excluded.time_horizon_years,
                available_savings = excluded.available_savings,
                monthly_income = excluded.monthly_income,
                monthly_expenses = excluded.monthly_expenses,
                existing_debt = excluded.existing_debt,
                need_liquidity_near_term = excluded.need_liquidity_near_term,
                last_updated = excluded.last_updated
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getTraderId());
            ps.setString(2, profile.getRiskPreference().name());
            ps.setString(3, profile.getPrimaryGoal());
            ps.setString(4, profile.getTargetAmount().toPlainString());
            ps.setInt(5, profile.getTimeHorizonYears());
            ps.setString(6, profile.getAvailableSavings().toPlainString());
            ps.setString(7, profile.getMonthlyIncome().toPlainString());
            ps.setString(8, profile.getMonthlyExpenses().toPlainString());
            ps.setString(9, profile.getExistingDebt().toPlainString());
            ps.setInt(10, profile.isNeedLiquidityNearTerm() ? 1 : 0);
            ps.setString(11, profile.getLastUpdated().toString());
            ps.executeUpdate();
        }
    }
}
