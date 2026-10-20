package com.stockportfolio;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.database.DataSeeder;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Trader;
import com.stockportfolio.provider.MarketDataProvider;
import com.stockportfolio.provider.MockExchangeRateProvider;
import com.stockportfolio.provider.MockMarketDataProvider;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.service.AccountService;
import com.stockportfolio.service.CurrencyService;
import com.stockportfolio.service.MarketAnalyticsService;
import com.stockportfolio.service.MarketService;
import com.stockportfolio.service.MarketSimulationScheduler;
import com.stockportfolio.service.PortfolioService;
import com.stockportfolio.service.TradingService;
import com.stockportfolio.ui.AssetDetailsView;
import com.stockportfolio.ui.DashboardView;
import com.stockportfolio.ui.DataExportView;
import com.stockportfolio.ui.EducationView;
import com.stockportfolio.ui.FinancialAdvisorView;
import com.stockportfolio.ui.GoalsView;
import com.stockportfolio.ui.HomePlannerView;
import com.stockportfolio.ui.MarketExplorerView;
import com.stockportfolio.ui.MarketInsightsView;
import com.stockportfolio.ui.OrdersView;
import com.stockportfolio.ui.PortfolioView;
import com.stockportfolio.ui.TradingView;
import com.stockportfolio.ui.TransactionsView;
import com.stockportfolio.ui.UIContext;
import com.stockportfolio.ui.WalletView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main JavaFX Application: builds the modern fintech layout, manages application state,
 * handles screen transitions, initializes database and mock providers, and coordinates
 * multithreaded background price tickers and user switching.
 */
public class MainApp extends Application {

    private UIContext context;
    private BorderPane rootLayout;
    private StackPane contentArea;
    private Label currentSectionTitle;

    // Header metrics
    private Label headerCashLabel;
    private Label headerHoldingsLabel;
    private Label headerTotalLabel;
    private Button autoTickerBtn;

    // Sidebar Trader Info
    private Label sidebarTraderName;
    private Label sidebarTraderAcc;

    // View controllers
    private final Map<String, Node> views = new HashMap<>();
    private final Map<String, Button> navButtons = new HashMap<>();
    private String activeViewKey = "DASHBOARD";

    private AssetDetailsView assetDetailsView;
    private TradingView tradingView;

    private Map<String, Asset> assets;
    private MarketSimulationScheduler simulationScheduler;

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize SQLite Database & Demo Data
        DatabaseManager.initialize();
        DataSeeder.seedDemoTraderIfEmpty();

        // 2. Load Assets and Trader Profile
        assets = new AssetRepository().findAllAsMap();
        Trader trader = new AccountService().loadTrader(assets)
                .orElseGet(() -> new AccountService().createTrader(
                        "Alex Morgan", "alex.morgan@college.edu", new BigDecimal("100000.00")));

        // 3. Initialize Core Business Services
        TradingService tradingService = new TradingService(trader, assets);
        MarketDataProvider dataProvider = new MockMarketDataProvider();
        MarketService marketService = new MarketService(dataProvider, tradingService);
        MarketAnalyticsService analyticsService = new MarketAnalyticsService();
        PortfolioService portfolioService = new PortfolioService();
        CurrencyService currencyService = new CurrencyService(new MockExchangeRateProvider());

        // 4. Initialize Shared UI Context
        context = new UIContext(trader, tradingService, marketService, analyticsService, portfolioService, currencyService);
        context.setNavigationHandler(this::navigateTo);

        // 5. Initialize Multithreaded Background Simulation Scheduler
        simulationScheduler = new MarketSimulationScheduler(context);

        // 6. Initialize Views
        views.put("DASHBOARD", new DashboardView(context));
        views.put("MARKET", new MarketExplorerView(context));

        assetDetailsView = new AssetDetailsView(context);
        views.put("ASSET_DETAILS", assetDetailsView);

        tradingView = new TradingView(context);
        views.put("TRADING", tradingView);

        views.put("ORDERS", new OrdersView(context));
        views.put("PORTFOLIO", new PortfolioView(context));
        views.put("WALLET", new WalletView(context));
        views.put("TRANSACTIONS", new TransactionsView(context));
        views.put("TRENDS", new MarketInsightsView(context));
        views.put("ADVISOR", new FinancialAdvisorView(context));
        views.put("GOALS", new GoalsView(context));
        views.put("HOME_PLANNER", new HomePlannerView(context));
        views.put("EDUCATION", new EducationView(context));
        views.put("EXPORT", new DataExportView(context));

        // 7. Build Layout Structure
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root");

        rootLayout.setLeft(buildSidebar());
        rootLayout.setTop(buildTopBar());

        contentArea = new StackPane();
        contentArea.getChildren().add(views.get("DASHBOARD"));
        rootLayout.setCenter(contentArea);

        // Subscribe to data changes for header stats and sidebar profile
        context.addDataChangeListener(this::refreshHeaderMetrics);
        refreshHeaderMetrics();

        // 8. Show Stage
        Scene scene = new Scene(rootLayout, 1260, 780);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        primaryStage.setTitle("InvestIQ — Stock Trading & Portfolio Management System (Virtual Investment Simulator)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1080);
        primaryStage.setMinHeight(650);

        // Maximize window to fit the user's screen resolution without cutoffs
        primaryStage.setMaximized(true);

        // Cleanly terminate background scheduler threads on close
        primaryStage.setOnCloseRequest(e -> simulationScheduler.stop());

        primaryStage.show();
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(270);
        sidebar.setMinWidth(260);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(16, 14, 16, 14));

        // Brand Logo — Renamed to InvestIQ
        VBox brandBox = new VBox(2);
        brandBox.setPadding(new Insets(4, 8, 12, 8));
        Label brandTitle = new Label("InvestIQ");
        brandTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #38bdf8; -fx-letter-spacing: 1px;");

        Label brandSub = new Label("Virtual Simulator & Advisor");
        brandSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        brandBox.getChildren().addAll(brandTitle, brandSub);

        // Trader Badge Card
        VBox traderCard = new VBox(6);
        traderCard.setStyle("-fx-background-color: #131c2e; -fx-background-radius: 8px; -fx-padding: 10 12 10 12; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

        HBox traderPill = new HBox(10);
        traderPill.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 18px;");

        VBox tInfo = new VBox(2);
        sidebarTraderName = new Label(context.getTrader().getName());
        sidebarTraderName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        sidebarTraderAcc = new Label(context.getTrader().getUserId() + " • " + context.getTrader().getRiskPreference());
        sidebarTraderAcc.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8;");
        tInfo.getChildren().addAll(sidebarTraderName, sidebarTraderAcc);
        HBox.setHgrow(tInfo, Priority.ALWAYS);
        traderPill.getChildren().addAll(avatar, tInfo);

        Button switchUserBtn = new Button("👥 Switch / + Add User");
        switchUserBtn.setMaxWidth(Double.MAX_VALUE);
        switchUserBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6 10 6 10;");
        switchUserBtn.setOnAction(e -> showSwitchUserDialog());

        traderCard.getChildren().addAll(traderPill, switchUserBtn);

        // Nav groups inside a ScrollPane so all options are always visible on any display size
        VBox navList = new VBox(3);
        navList.setPadding(new Insets(4, 2, 8, 2));

        addNavHeader(navList, "TRADING & PORTFOLIO");
        addNavBtn(navList, "DASHBOARD", "📊 Dashboard");
        addNavBtn(navList, "MARKET", "🔍 Market Explorer");
        addNavBtn(navList, "TRADING", "⚡ Trading Desk");
        addNavBtn(navList, "ORDERS", "📋 Orders Book");
        addNavBtn(navList, "PORTFOLIO", "💼 Open Holdings");
        addNavBtn(navList, "WALLET", "💳 Virtual Wallet");
        addNavBtn(navList, "TRANSACTIONS", "📜 Audit Ledger");

        addNavHeader(navList, "MARKET INTEL");
        addNavBtn(navList, "TRENDS", "📈 Trends & FX Rates");

        addNavHeader(navList, "FINANCIAL PLANNING");
        addNavBtn(navList, "ADVISOR", "🤖 AI Financial Advisor");
        addNavBtn(navList, "GOALS", "🎯 Financial Goals");
        addNavBtn(navList, "HOME_PLANNER", "🏠 Home Loan Planner");
        addNavBtn(navList, "EDUCATION", "📚 Investor Education");
        addNavBtn(navList, "EXPORT", "📥 Export Reports");

        ScrollPane scrollNav = new ScrollPane(navList);
        scrollNav.setFitToWidth(true);
        scrollNav.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollNav.getStyleClass().add("view-scroll-pane");
        VBox.setVgrow(scrollNav, Priority.ALWAYS);

        // Bottom simulation pill
        Label simNotice = new Label("● VIRTUAL SIMULATION ONLY\nZero Real Money • Educational");
        simNotice.setStyle("-fx-font-size: 10px; -fx-text-fill: #10b981; -fx-alignment: CENTER; -fx-padding: 8 6 4 6; -fx-font-weight: bold;");

        sidebar.getChildren().addAll(brandBox, traderCard, scrollNav, simNotice);
        return sidebar;
    }

    private void addNavHeader(VBox list, String text) {
        Label header = new Label(text);
        header.getStyleClass().add("nav-category-header");
        header.setPadding(new Insets(10, 8, 4, 8));
        list.getChildren().add(header);
    }

    private void addNavBtn(VBox list, String key, String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("nav-button");
        if (key.equals("DASHBOARD")) {
            btn.getStyleClass().add("nav-button-active");
        }
        btn.setOnAction(e -> navigateTo(key, null));
        navButtons.put(key, btn);
        list.getChildren().add(btn);
    }

    private Node buildTopBar() {
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 24, 12, 24));
        topBar.setStyle("-fx-background-color: #0d1321; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        currentSectionTitle = new Label("Dashboard & Portfolio Overview");
        currentSectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        HBox.setHgrow(currentSectionTitle, Priority.ALWAYS);

        // Quick Stats Pills in Top Bar
        HBox statsPill = new HBox(16);
        statsPill.setAlignment(Pos.CENTER_LEFT);
        statsPill.setStyle("-fx-background-color: #131c2e; -fx-padding: 6 16 6 16; -fx-background-radius: 6px; -fx-border-color: #1e293b; -fx-border-radius: 6px;");

        headerCashLabel = new Label("Cash: ₹0.00");
        headerCashLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #38bdf8; -fx-font-weight: 600;");

        headerHoldingsLabel = new Label("Holdings: ₹0.00");
        headerHoldingsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c084fc; -fx-font-weight: 600;");

        headerTotalLabel = new Label("Total: ₹0.00");
        headerTotalLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34d399; -fx-font-weight: bold;");

        statsPill.getChildren().addAll(headerCashLabel, new Label("•"), headerHoldingsLabel, new Label("•"), headerTotalLabel);

        // Multithreaded Live Auto-Ticker Button
        autoTickerBtn = new Button("⚡ Auto-Ticker: OFF");
        autoTickerBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6 12 6 12;");
        autoTickerBtn.setOnAction(e -> {
            simulationScheduler.toggle(5);
            if (simulationScheduler.isRunning()) {
                autoTickerBtn.setText("⚡ Auto-Ticker: ON (5s)");
                autoTickerBtn.setStyle("-fx-background-color: #065f46; -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6 12 6 12;");
            } else {
                autoTickerBtn.setText("⚡ Auto-Ticker: OFF");
                autoTickerBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6 12 6 12;");
            }
        });

        // Manual Refresh Button
        Button refreshMarketBtn = new Button("🔄 Refresh Market");
        refreshMarketBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshMarketBtn.setOnAction(e -> {
            try {
                context.getMarketService().refreshPrices();
                context.getTradingService().processPendingOrders();
                context.notifyDataChanged();
            } catch (Exception ex) {
                UIContext.showError("Market Refresh Error", ex.getMessage());
            }
        });

        topBar.getChildren().addAll(currentSectionTitle, statsPill, autoTickerBtn, refreshMarketBtn);
        return topBar;
    }

    private void refreshHeaderMetrics() {
        Trader trader = context.getTrader();
        PortfolioSummary summary = context.getPortfolioService().getPortfolioSummary(trader);

        headerCashLabel.setText("Cash: " + UIContext.formatMoney(trader.getCashBalance()));
        headerHoldingsLabel.setText("Holdings: " + UIContext.formatMoney(summary.holdingsValue()));
        headerTotalLabel.setText("Total: " + UIContext.formatMoney(trader.getTotalAccountValue()));

        if (sidebarTraderName != null) {
            sidebarTraderName.setText(trader.getName());
        }
        if (sidebarTraderAcc != null) {
            sidebarTraderAcc.setText(trader.getUserId() + " • " + trader.getRiskPreference());
        }
    }

    private void navigateTo(String key, Object param) {
        Node targetView = views.get(key);
        if (targetView == null) return;

        // Pass parameter if applicable
        if (param instanceof Asset asset) {
            if ("ASSET_DETAILS".equals(key)) {
                assetDetailsView.setAsset(asset);
            } else if ("TRADING".equals(key)) {
                tradingView.selectAsset(asset);
            }
        }

        // Update active nav button styling
        Button prev = navButtons.get(activeViewKey);
        if (prev != null) prev.getStyleClass().remove("nav-button-active");

        Button next = navButtons.get(key);
        if (next != null) next.getStyleClass().add("nav-button-active");

        activeViewKey = key;
        currentSectionTitle.setText(formatSectionTitle(key));

        contentArea.getChildren().clear();
        contentArea.getChildren().add(targetView);
    }

    private String formatSectionTitle(String key) {
        return switch (key) {
            case "DASHBOARD" -> "Dashboard & Portfolio Overview";
            case "MARKET" -> "Market Explorer";
            case "ASSET_DETAILS" -> "Asset Technical Details & History";
            case "TRADING" -> "Trading Execution Desk";
            case "ORDERS" -> "Order Management Book";
            case "PORTFOLIO" -> "Open Portfolio Holdings";
            case "WALLET" -> "Virtual Wallet & Balances";
            case "TRANSACTIONS" -> "Transaction Audit Ledger";
            case "TRENDS" -> "Market Insights & FX Rates";
            case "ADVISOR" -> "Rule-Based Financial Advisor";
            case "GOALS" -> "Financial Goal Planning";
            case "HOME_PLANNER" -> "Home Purchase & Loan Planner";
            case "EDUCATION" -> "Investor Education Center";
            case "EXPORT" -> "Export Reports & Backups";
            default -> "InvestIQ Platform";
        };
    }

    /**
     * Interactive Dialog to Switch Current Trader Profile or Create a New Trader.
     * Fulfills Module A: User & Trading Account Management requirements.
     */
    private void showSwitchUserDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Switch Trader Profile or Add New Trader");
        dialog.setHeaderText("User Profile & Trading Account Management");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(16));
        layout.setPrefWidth(440);

        // 1. Existing traders section
        Label selectLabel = new Label("Switch to Existing Trader Profile:");
        selectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        AccountService accountService = new AccountService();
        List<Trader> allTraders = accountService.listAllTraders(assets);

        ComboBox<Trader> traderCombo = new ComboBox<>();
        traderCombo.setMaxWidth(Double.MAX_VALUE);
        traderCombo.getItems().addAll(allTraders);
        traderCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Trader t) {
                if (t == null) return "";
                return t.getName() + " (" + t.getUserId() + ") — Cash: " + UIContext.formatMoney(t.getCashBalance());
            }

            @Override
            public Trader fromString(String string) {
                return null;
            }
        });

        // Pre-select active trader
        for (Trader t : allTraders) {
            if (t.getUserId().equals(context.getTrader().getUserId())) {
                traderCombo.setValue(t);
                break;
            }
        }

        Button switchBtn = new Button("Switch to Selected Profile");
        switchBtn.getStyleClass().addAll("btn", "btn-secondary");
        switchBtn.setMaxWidth(Double.MAX_VALUE);
        switchBtn.setOnAction(e -> {
            Trader sel = traderCombo.getValue();
            if (sel != null) {
                context.setTrader(sel);
                dialog.close();
            }
        });

        // 2. Add New Trader Section
        Separator sep = new Separator();

        Label addLabel = new Label("Create a New Trader Profile:");
        addLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Maya Sharma");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g. maya.sharma@college.edu");
        TextField balanceField = new TextField("100000.00");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Starting Virtual Cash:"), 0, 2);
        grid.add(balanceField, 1, 2);

        Button createBtn = new Button("+ Create Trader Profile & Login");
        createBtn.getStyleClass().addAll("btn", "btn-primary");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            if (name.isEmpty()) {
                UIContext.showError("Validation Error", "Please enter a valid trader name.");
                return;
            }
            try {
                BigDecimal startBal = new BigDecimal(balanceField.getText().trim());
                Trader newTrader = accountService.createTrader(name, email, startBal);
                context.setTrader(newTrader);
                dialog.close();
            } catch (Exception ex) {
                UIContext.showError("Validation Error", "Could not create trader: " + ex.getMessage());
            }
        });

        layout.getChildren().addAll(selectLabel, traderCombo, switchBtn, sep, addLabel, grid, createBtn);
        dialog.getDialogPane().setContent(layout);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}