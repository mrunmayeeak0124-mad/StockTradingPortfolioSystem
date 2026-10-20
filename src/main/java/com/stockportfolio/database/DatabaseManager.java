package com.stockportfolio.database;

import com.stockportfolio.exception.DatabaseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections to the SQLite file and creates the tables on first run.
 * The database file is created automatically at database/stocktrading.db
 * (relative to the project folder).
 */
public final class DatabaseManager {

    private static String databasePath = "database/stocktrading.db";

    private static final String[] CREATE_STATEMENTS = {
            """
            CREATE TABLE IF NOT EXISTS assets (
                asset_id       TEXT PRIMARY KEY,
                category       TEXT NOT NULL CHECK (category IN ('STOCK', 'ETF', 'BOND')),
                name           TEXT NOT NULL,
                symbol         TEXT NOT NULL,
                currency       TEXT NOT NULL,
                current_price  TEXT NOT NULL,
                exchange       TEXT,
                sector         TEXT,
                fund_category  TEXT,
                tracking_index TEXT,
                issuer         TEXT,
                face_value     TEXT,
                coupon_rate    TEXT,
                maturity_date  TEXT,
                last_updated   TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS price_history (
                asset_id   TEXT NOT NULL,
                price_date TEXT NOT NULL,
                price      TEXT NOT NULL,
                PRIMARY KEY (asset_id, price_date),
                FOREIGN KEY (asset_id) REFERENCES assets (asset_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS traders (
                trader_id       TEXT PRIMARY KEY,
                name            TEXT NOT NULL,
                email           TEXT,
                cash_balance    TEXT NOT NULL,
                risk_preference TEXT NOT NULL,
                created_at      TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS positions (
                trader_id        TEXT NOT NULL,
                asset_id         TEXT NOT NULL,
                quantity         INTEGER NOT NULL CHECK (quantity > 0),
                total_cost_basis TEXT NOT NULL,
                PRIMARY KEY (trader_id, asset_id),
                FOREIGN KEY (trader_id) REFERENCES traders (trader_id),
                FOREIGN KEY (asset_id) REFERENCES assets (asset_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS orders (
                order_id     TEXT PRIMARY KEY,
                trader_id    TEXT NOT NULL,
                asset_id     TEXT NOT NULL,
                side         TEXT NOT NULL CHECK (side IN ('BUY', 'SELL')),
                quantity     INTEGER NOT NULL CHECK (quantity > 0),
                order_type   TEXT NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT')),
                limit_price  TEXT,
                submitted_at TEXT NOT NULL,
                status       TEXT NOT NULL
                    CHECK (status IN ('PENDING', 'EXECUTED', 'CANCELLED', 'REJECTED')),
                FOREIGN KEY (trader_id) REFERENCES traders (trader_id),
                FOREIGN KEY (asset_id) REFERENCES assets (asset_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id  TEXT PRIMARY KEY,
                trader_id       TEXT NOT NULL,
                order_id        TEXT,
                asset_id        TEXT,
                type            TEXT NOT NULL CHECK (type IN ('DEPOSIT', 'BUY', 'SELL')),
                quantity        INTEGER NOT NULL,
                execution_price TEXT NOT NULL,
                fee             TEXT NOT NULL,
                gross_value     TEXT NOT NULL,
                net_amount      TEXT NOT NULL,
                realized_pl     TEXT NOT NULL,
                created_at      TEXT NOT NULL,
                status          TEXT NOT NULL,
                FOREIGN KEY (trader_id) REFERENCES traders (trader_id),
                FOREIGN KEY (order_id) REFERENCES orders (order_id),
                FOREIGN KEY (asset_id) REFERENCES assets (asset_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS watchlist (
                trader_id TEXT NOT NULL,
                asset_id  TEXT NOT NULL,
                PRIMARY KEY (trader_id, asset_id),
                FOREIGN KEY (trader_id) REFERENCES traders (trader_id),
                FOREIGN KEY (asset_id) REFERENCES assets (asset_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS financial_profiles (
                trader_id                 TEXT PRIMARY KEY,
                risk_preference           TEXT NOT NULL,
                primary_goal              TEXT NOT NULL,
                target_amount             TEXT NOT NULL,
                time_horizon_years        INTEGER NOT NULL,
                available_savings         TEXT NOT NULL,
                monthly_income            TEXT NOT NULL,
                monthly_expenses          TEXT NOT NULL,
                existing_debt             TEXT NOT NULL,
                need_liquidity_near_term  INTEGER NOT NULL,
                last_updated              TEXT NOT NULL,
                FOREIGN KEY (trader_id) REFERENCES traders (trader_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS financial_goals (
                goal_id        TEXT PRIMARY KEY,
                trader_id      TEXT NOT NULL,
                name           TEXT NOT NULL,
                category       TEXT NOT NULL,
                target_amount  TEXT NOT NULL,
                current_amount TEXT NOT NULL,
                target_date    TEXT NOT NULL,
                status         TEXT NOT NULL,
                FOREIGN KEY (trader_id) REFERENCES traders (trader_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS advisor_recommendations (
                recommendation_id TEXT PRIMARY KEY,
                trader_id         TEXT NOT NULL,
                goal_id           TEXT,
                created_at        TEXT NOT NULL,
                category          TEXT NOT NULL,
                title             TEXT NOT NULL,
                summary           TEXT NOT NULL,
                reasoning         TEXT NOT NULL,
                assumptions       TEXT NOT NULL,
                status            TEXT NOT NULL,
                FOREIGN KEY (trader_id) REFERENCES traders (trader_id)
            )
            """
    };

    private DatabaseManager() { }

    public static String getDatabasePath() {
        return databasePath;
    }

    /** Lets unit tests use a separate temporary database file. */
    public static void setDatabasePath(String path) {
        databasePath = path;
    }

    /** Opens a new connection. The caller closes it (use try-with-resources). */
    public static Connection getConnection() {
        try {
            Path parent = Paths.get(databasePath).toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");   // SQLite needs this per connection
            } catch (SQLException e) {
                connection.close();
                throw e;
            }
            return connection;
        } catch (SQLException | IOException e) {
            throw new DatabaseException("Could not open the database: " + e.getMessage(), e);
        }
    }

    /** Creates missing tables and seeds demo data on the very first run. Safe to call every start. */
    public static void initialize() {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    for (String sql : CREATE_STATEMENTS) {
                        statement.execute(sql);
                    }
                }
                DataSeeder.seedIfEmpty(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not set up the database: " + e.getMessage(), e);
        }
    }
}