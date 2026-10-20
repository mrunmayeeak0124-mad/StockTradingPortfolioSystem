package com.stockportfolio.database;

import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.Bond;
import com.stockportfolio.model.ETF;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.GoalCategory;
import com.stockportfolio.model.GoalStatus;
import com.stockportfolio.model.RiskPreference;
import com.stockportfolio.model.Stock;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.repository.FinancialGoalRepository;
import com.stockportfolio.repository.FinancialProfileRepository;
import com.stockportfolio.repository.PriceHistoryRepository;
import com.stockportfolio.repository.TraderRepository;
import com.stockportfolio.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fills an EMPTY database with SIMULATED demonstration assets, historical price data,
 * a default trader profile, initial holdings, and planning goals.
 * None of these prices are real or live. They only exist so every screen has rich data.
 */
public final class DataSeeder {

    private static final int HISTORY_DAYS = 30;

    private DataSeeder() { }

    public static void seedIfEmpty(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM assets")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Ensure AAPL is seeded even if database was previously initialized
                try (Statement st2 = connection.createStatement();
                     ResultSet rs2 = st2.executeQuery("SELECT COUNT(*) FROM assets WHERE symbol = 'AAPL'")) {
                    if (rs2.next() && rs2.getInt(1) == 0) {
                        Asset aapl = new Stock("A013", "Apple Inc.", "AAPL", "NASDAQ", "USD",
                                "Consumer Technology", new BigDecimal("339.45"),
                                "$3.55T (₹295L Cr)", new BigDecimal("4.85"), new BigDecimal("38.60"),
                                48520000L, new BigDecimal("34.20"), new BigDecimal("343.46"), new BigDecimal("164.08"));
                        new AssetRepository().save(connection, aapl);
                    }
                } catch (Exception ignored) {}
                return;     // already seeded
            }
        }

        AssetRepository assetRepository = new AssetRepository();
        PriceHistoryRepository historyRepository = new PriceHistoryRepository();
        List<Asset> assets = buildSampleAssets();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            assetRepository.save(connection, asset);
            seedHistory(connection, historyRepository, asset, i);
        }
    }

    /**
     * Seeds a demonstration trader profile, sample positions, profile, and goals
     * if no traders exist. Called when launching the desktop UI.
     */
    public static void seedDemoTraderIfEmpty() {
        try (Connection connection = DatabaseManager.getConnection()) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM traders")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
            List<Asset> assets = new AssetRepository().findAll();
            seedDefaultTrader(connection, assets);
        } catch (SQLException e) {
            // Non-fatal if already initialized
        }
    }

    private static void seedDefaultTrader(Connection connection, List<Asset> assets) {
        String traderId = "TRD-DEMO-001";
        Trader trader = new Trader(traderId, "Alex Morgan", "alex.morgan@college.edu",
                new BigDecimal("65000.00"));
        trader.setRiskPreference(RiskPreference.BALANCED);

        // Map assets by symbol for easy holding generation
        Asset reliance = findAsset(assets, "RELIANCE");
        Asset tcs = findAsset(assets, "TCS");
        Asset niftyBees = findAsset(assets, "NIFTYBEES");
        Asset govtBond = findAsset(assets, "Govt Bond 2034");

        // Open initial sample positions
        if (reliance != null) {
            trader.getPortfolio().recordBuy(reliance, 10, new BigDecimal("24100.00")); // avg 2410.00
        }
        if (tcs != null) {
            trader.getPortfolio().recordBuy(tcs, 5, new BigDecimal("19050.00"));     // avg 3810.00
        }
        if (niftyBees != null) {
            trader.getPortfolio().recordBuy(niftyBees, 50, new BigDecimal("12400.00")); // avg 248.00
        }
        if (govtBond != null) {
            trader.getPortfolio().recordBuy(govtBond, 15, new BigDecimal("14700.00")); // avg 980.00
        }

        // Save trader and positions
        TraderRepository traderRepository = new TraderRepository();
        traderRepository.save(connection, trader);

        // Record initial cash deposit
        TransactionRepository txRepository = new TransactionRepository();
        Transaction initialDeposit = Transaction.forDeposit(traderId, new BigDecimal("100000.00"));
        txRepository.save(connection, initialDeposit);

        // Seed initial financial profile
        FinancialProfile profile = new FinancialProfile(
                traderId,
                RiskPreference.BALANCED,
                "Home Down Payment & Wealth",
                new BigDecimal("1500000.00"),
                5,
                new BigDecimal("85000.00"),
                new BigDecimal("75000.00"),
                new BigDecimal("42000.00"),
                new BigDecimal("25000.00"),
                false,
                LocalDateTime.now()
        );
        try {
            new FinancialProfileRepository().save(connection, profile);

            // Seed sample financial goals
            FinancialGoalRepository goalRepository = new FinancialGoalRepository();
            goalRepository.save(connection, new FinancialGoal(
                    "GOL-001", traderId, "Emergency Fund Reserve",
                    GoalCategory.EMERGENCY_FUND,
                    new BigDecimal("250000.00"),
                    new BigDecimal("85000.00"),
                    LocalDate.now().plusMonths(12),
                    GoalStatus.IN_PROGRESS
            ));
            goalRepository.save(connection, new FinancialGoal(
                    "GOL-002", traderId, "Home Down Payment",
                    GoalCategory.HOME_PURCHASE,
                    new BigDecimal("1200000.00"),
                    new BigDecimal("350000.00"),
                    LocalDate.now().plusYears(3),
                    GoalStatus.IN_PROGRESS
            ));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed default profile or goals: " + e.getMessage(), e);
        }
    }

    private static Asset findAsset(List<Asset> assets, String symbol) {
        for (Asset a : assets) {
            if (a.getSymbol().equalsIgnoreCase(symbol) || a.getName().equalsIgnoreCase(symbol)) {
                return a;
            }
        }
        return null;
    }

    public static List<Asset> buildSampleAssets() {
        List<Asset> assets = new ArrayList<>();
        assets.add(new Stock("A001", "Reliance Industries", "RELIANCE", "NSE", "INR",
                "Energy", new BigDecimal("2450.50")));
        assets.add(new Stock("A002", "Tata Consultancy Services", "TCS", "NSE", "INR",
                "IT Services", new BigDecimal("3850.00")));
        assets.add(new Stock("A003", "Infosys", "INFY", "NSE", "INR",
                "IT Services", new BigDecimal("1520.75")));
        assets.add(new Stock("A004", "HDFC Bank", "HDFCBANK", "NSE", "INR",
                "Banking", new BigDecimal("1680.25")));
        assets.add(new Stock("A005", "ITC", "ITC", "NSE", "INR",
                "Consumer Goods", new BigDecimal("465.40")));
        assets.add(new Stock("A006", "Tata Motors", "TATAMOTORS", "NSE", "INR",
                "Automobile", new BigDecimal("980.60")));
        assets.add(new Stock("A013", "Apple Inc.", "AAPL", "NASDAQ", "USD",
                "Consumer Technology", new BigDecimal("339.45"),
                "$3.55T (₹295L Cr)", new BigDecimal("4.85"), new BigDecimal("38.60"),
                48520000L, new BigDecimal("34.20"), new BigDecimal("343.46"), new BigDecimal("164.08")));

        assets.add(new ETF("A007", "Nifty 50 Index Fund", "NIFTYBEES", "INR",
                "Large Cap", "Nifty 50", new BigDecimal("250.75")));
        assets.add(new ETF("A008", "Gold Exchange Traded Fund", "GOLDBEES", "INR",
                "Commodity", "Domestic gold price", new BigDecimal("62.30")));
        assets.add(new ETF("A009", "Bank Index Fund", "BANKBEES", "INR",
                "Sectoral", "Nifty Bank", new BigDecimal("520.10")));

        assets.add(new Bond("A010", "Govt Bond 2034", "Govt Bond 2034", "INR",
                new BigDecimal("1000"), new BigDecimal("7.10"),
                LocalDate.of(2034, 6, 15), new BigDecimal("985.00")));
        assets.add(new Bond("A011", "Govt Bond 2030", "Govt Bond 2030", "INR",
                new BigDecimal("1000"), new BigDecimal("6.80"),
                LocalDate.of(2030, 3, 20), new BigDecimal("1002.50")));
        assets.add(new Bond("A012", "Sample Corporate Bond 2028", "Corporate Bond 2028", "INR",
                new BigDecimal("1000"), new BigDecimal("7.60"),
                LocalDate.of(2028, 9, 30), new BigDecimal("995.00")));
        return assets;
    }

    /**
     * Creates 30 days of smooth, made-up prices that END exactly at the asset's current price.
     */
    private static void seedHistory(Connection connection, PriceHistoryRepository repository,
                                    Asset asset, int index) {
        boolean isBond = asset.getCategory() == AssetCategory.BOND;
        double amplitude = isBond ? 0.005 : 0.03;
        double trendPerDay = isBond ? 0.0002 : 0.0015;
        double phase = index * 0.7;
        double lastWave = Math.sin((HISTORY_DAYS - 1) * 0.5 + phase);
        BigDecimal base = asset.getCurrentPrice();
        LocalDate today = LocalDate.now();

        for (int day = 0; day < HISTORY_DAYS; day++) {
            BigDecimal price;
            if (day == HISTORY_DAYS - 1) {
                price = base;
            } else {
                double wave = Math.sin(day * 0.5 + phase) - lastWave;
                double trend = trendPerDay * (HISTORY_DAYS - 1 - day);
                double factor = 1 + amplitude * wave - trend;
                price = BigDecimal.valueOf(base.doubleValue() * factor)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            repository.save(connection, asset.getAssetId(),
                    today.minusDays(HISTORY_DAYS - 1 - day), price);
        }
    }
}