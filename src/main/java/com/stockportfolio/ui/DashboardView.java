package com.stockportfolio.ui;

import com.stockportfolio.model.AdvisorRecommendation;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Modern fintech dashboard providing a bird's-eye view of account metrics,
 * portfolio allocation chart, recent trades, financial goal progress, and advisor insights.
 */
public class DashboardView extends ScrollPane {

    private final UIContext context;
    private final VBox contentBox = new VBox(24);

    // Dynamic metrics
    private Label cashLabel;
    private Label holdingsLabel;
    private Label accountValueLabel;
    private Label unrealizedPlLabel;
    private Label realizedPlLabel;
    private Label positionsCountLabel;

    private PieChart allocationChart;
    private TableView<Transaction> recentTransactionsTable;
    private VBox goalsContainer;
    private VBox advisorTeaserContainer;

    public DashboardView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        contentBox.setPadding(new Insets(28));
        contentBox.getStyleClass().add("view-container");

        buildHeader();
        buildMetricCards();
        buildChartsAndActivitySection();
        buildGoalsAndAdvisorSection();

        setContent(contentBox);
        refreshData();

        // Register for data refresh events
        context.addDataChangeListener(this::refreshData);
    }

    private void buildHeader() {
        Trader trader = context.getTrader();
        VBox titleBox = new VBox(4);
        Label title = new Label("Trading & Investment Dashboard");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Welcome back, " + trader.getName() + " • Account ID: " + trader.getUserId()
                + " • Risk Profile: " + trader.getRiskPreference());
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox topBar = new HBox(titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button quickDepositBtn = new Button("+ Deposit Funds");
        quickDepositBtn.getStyleClass().addAll("btn", "btn-secondary");
        quickDepositBtn.setOnAction(e -> context.navigateTo("WALLET"));

        Button tradeBtn = new Button("⚡ New Trade");
        tradeBtn.getStyleClass().addAll("btn", "btn-primary");
        tradeBtn.setOnAction(e -> context.navigateTo("TRADING"));

        HBox actionBox = new HBox(12, quickDepositBtn, tradeBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(topBar, actionBox);
        header.setAlignment(Pos.CENTER_LEFT);
        contentBox.getChildren().add(header);
    }

    private void buildMetricCards() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        cashLabel = new Label("₹0.00");
        VBox cashCard = createCard("Available Virtual Cash", cashLabel, "Virtual funds ready for deployment", "card-accent-blue");

        holdingsLabel = new Label("₹0.00");
        VBox holdingsCard = createCard("Invested Portfolio Value", holdingsLabel, "Current market value of holdings", "card-accent-purple");

        accountValueLabel = new Label("₹0.00");
        VBox accountCard = createCard("Total Account Value", accountValueLabel, "Cash balance + portfolio value", "card-accent-teal");

        unrealizedPlLabel = new Label("₹0.00");
        VBox unrealizedCard = createCard("Unrealized Profit / Loss", unrealizedPlLabel, "Gain/loss on open holdings", "card-accent-gold");

        realizedPlLabel = new Label("₹0.00");
        VBox realizedCard = createCard("Realized Net P/L", realizedPlLabel, "Profit/loss from executed sales", "card-accent-green");

        positionsCountLabel = new Label("0");
        VBox positionsCard = createCard("Open Holdings", positionsCountLabel, "Distinct assets currently held", "card-accent-cyan");

        grid.add(cashCard, 0, 0);
        grid.add(holdingsCard, 1, 0);
        grid.add(accountCard, 2, 0);
        grid.add(unrealizedCard, 0, 1);
        grid.add(realizedCard, 1, 1);
        grid.add(positionsCard, 2, 1);

        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            grid.getColumnConstraints().add(cc);
        }

        contentBox.getChildren().add(grid);
    }

    private VBox createCard(String title, Label valueLabel, String subtitle, String accentClass) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("metric-card", accentClass);
        card.setPadding(new Insets(16, 20, 16, 20));

        Label tLabel = new Label(title);
        tLabel.getStyleClass().add("card-title");

        valueLabel.getStyleClass().add("card-value");

        Label sLabel = new Label(subtitle);
        sLabel.getStyleClass().add("card-subtitle");

        card.getChildren().addAll(tLabel, valueLabel, sLabel);
        return card;
    }

    private void buildChartsAndActivitySection() {
        HBox row = new HBox(20);

        // Chart Card
        VBox chartCard = new VBox(12);
        chartCard.getStyleClass().add("content-card");
        chartCard.setPadding(new Insets(18));
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        Label chartTitle = new Label("Asset Allocation Breakdown");
        chartTitle.getStyleClass().add("section-title");

        allocationChart = new PieChart();
        allocationChart.setPrefHeight(260);
        allocationChart.setLabelsVisible(true);
        allocationChart.setLegendVisible(true);

        chartCard.getChildren().addAll(chartTitle, allocationChart);

        // Recent Activity Card
        VBox activityCard = new VBox(12);
        activityCard.getStyleClass().add("content-card");
        activityCard.setPadding(new Insets(18));
        HBox.setHgrow(activityCard, Priority.ALWAYS);

        Label activityTitle = new Label("Recent Transactions");
        activityTitle.getStyleClass().add("section-title");

        recentTransactionsTable = new TableView<>();
        recentTransactionsTable.setPrefHeight(260);
        recentTransactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().name()));

        TableColumn<Transaction, String> assetCol = new TableColumn<>("Asset");
        assetCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAsset() != null ? data.getValue().getAsset().getSymbol() : "Cash Deposit"));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Net Amount");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                UIContext.formatMoney(data.getValue().getNetAmount())));

        TableColumn<Transaction, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                UIContext.formatDateTime(data.getValue().getTimestamp())));

        recentTransactionsTable.getColumns().addAll(typeCol, assetCol, amountCol, timeCol);

        activityCard.getChildren().addAll(activityTitle, recentTransactionsTable);

        row.getChildren().addAll(chartCard, activityCard);
        contentBox.getChildren().add(row);
    }

    private void buildGoalsAndAdvisorSection() {
        HBox row = new HBox(20);

        // Financial Goals Card
        VBox goalsCard = new VBox(12);
        goalsCard.getStyleClass().add("content-card");
        goalsCard.setPadding(new Insets(18));
        HBox.setHgrow(goalsCard, Priority.ALWAYS);

        HBox goalsHeader = new HBox(12);
        Label goalsTitle = new Label("Active Financial Goals");
        goalsTitle.getStyleClass().add("section-title");
        HBox.setHgrow(goalsTitle, Priority.ALWAYS);

        Button manageGoalsBtn = new Button("View All Goals →");
        manageGoalsBtn.getStyleClass().addAll("btn-link");
        manageGoalsBtn.setOnAction(e -> context.navigateTo("GOALS"));
        goalsHeader.getChildren().addAll(goalsTitle, manageGoalsBtn);

        goalsContainer = new VBox(10);
        goalsCard.getChildren().addAll(goalsHeader, goalsContainer);

        // Advisor Recommendations Teaser Card
        VBox advisorCard = new VBox(12);
        advisorCard.getStyleClass().add("content-card");
        advisorCard.setPadding(new Insets(18));
        HBox.setHgrow(advisorCard, Priority.ALWAYS);

        HBox advisorHeader = new HBox(12);
        Label advisorTitle = new Label("Educational Advisor Insights");
        advisorTitle.getStyleClass().add("section-title");
        HBox.setHgrow(advisorTitle, Priority.ALWAYS);

        Button advisorBtn = new Button("Open Advisor →");
        advisorBtn.getStyleClass().addAll("btn-link");
        advisorBtn.setOnAction(e -> context.navigateTo("ADVISOR"));
        advisorHeader.getChildren().addAll(advisorTitle, advisorBtn);

        advisorTeaserContainer = new VBox(10);
        advisorCard.getChildren().addAll(advisorHeader, advisorTeaserContainer);

        row.getChildren().addAll(goalsCard, advisorCard);
        contentBox.getChildren().add(row);
    }

    public void refreshData() {
        Trader trader = context.getTrader();
        PortfolioSummary summary = context.getPortfolioService().getPortfolioSummary(trader);

        cashLabel.setText(UIContext.formatMoney(trader.getCashBalance()));
        holdingsLabel.setText(UIContext.formatMoney(summary.holdingsValue()));
        accountValueLabel.setText(UIContext.formatMoney(trader.getTotalAccountValue()));

        BigDecimal unPl = summary.unrealizedProfitLoss();
        unrealizedPlLabel.setText(UIContext.formatMoney(unPl));
        unrealizedPlLabel.getStyleClass().removeAll("text-positive", "text-negative");
        unrealizedPlLabel.getStyleClass().add(unPl.signum() >= 0 ? "text-positive" : "text-negative");

        BigDecimal rePl = summary.realizedProfitLoss();
        realizedPlLabel.setText(UIContext.formatMoney(rePl));
        realizedPlLabel.getStyleClass().removeAll("text-positive", "text-negative");
        realizedPlLabel.getStyleClass().add(rePl.signum() >= 0 ? "text-positive" : "text-negative");

        positionsCountLabel.setText(String.valueOf(summary.openPositions()));

        // Update PieChart
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
        Map<AssetCategory, BigDecimal> alloc = summary.allocationByCategory();
        for (Map.Entry<AssetCategory, BigDecimal> entry : alloc.entrySet()) {
            if (entry.getValue().signum() > 0) {
                chartData.add(new PieChart.Data(entry.getKey().name() + " (" + entry.getValue() + "%)",
                        entry.getValue().doubleValue()));
            }
        }
        if (chartData.isEmpty()) {
            chartData.add(new PieChart.Data("100% Cash Buffer", 100));
        }
        allocationChart.setData(chartData);

        // Update Recent Transactions
        List<Transaction> txs = context.getTradingService().getTransactions();
        int limit = Math.min(6, txs.size());
        recentTransactionsTable.setItems(FXCollections.observableArrayList(txs.subList(0, limit)));

        // Update Goals preview
        goalsContainer.getChildren().clear();
        List<FinancialGoal> goals = context.getGoalService().getGoals(trader.getUserId());
        if (goals.isEmpty()) {
            Label emptyGoals = new Label("No active goals yet. Click 'View All Goals' to set up a target.");
            emptyGoals.getStyleClass().add("text-muted");
            goalsContainer.getChildren().add(emptyGoals);
        } else {
            for (FinancialGoal g : goals.subList(0, Math.min(3, goals.size()))) {
                VBox goalRow = new VBox(4);
                HBox header = new HBox();
                Label gName = new Label(g.getName());
                gName.getStyleClass().add("font-semibold");
                Label gPerc = new Label(g.getProgressPercentage() + "% ("
                        + UIContext.formatMoney(g.getCurrentAmount()) + " / " + UIContext.formatMoney(g.getTargetAmount()) + ")");
                gPerc.getStyleClass().add("text-muted");
                HBox.setHgrow(gName, Priority.ALWAYS);
                header.getChildren().addAll(gName, gPerc);

                ProgressBar pb = new ProgressBar(Math.min(1.0, g.getProgressPercentage().doubleValue() / 100.0));
                pb.setMaxWidth(Double.MAX_VALUE);
                goalRow.getChildren().addAll(header, pb);
                goalsContainer.getChildren().add(goalRow);
            }
        }

        // Update Advisor Teaser
        advisorTeaserContainer.getChildren().clear();
        List<AdvisorRecommendation> recs = context.getAdvisorService().getRecommendations(trader.getUserId());
        if (recs.isEmpty()) {
            Label emptyRecs = new Label("Run the AI Financial Advisor to inspect your portfolio diversification and health.");
            emptyRecs.getStyleClass().add("text-muted");
            advisorTeaserContainer.getChildren().add(emptyRecs);
        } else {
            for (AdvisorRecommendation r : recs.subList(0, Math.min(3, recs.size()))) {
                VBox recRow = new VBox(2);
                recRow.getStyleClass().add("recommendation-mini-card");
                recRow.setPadding(new Insets(8, 12, 8, 12));

                Label rTitle = new Label("[" + r.getCategory().getDisplayName() + "] " + r.getTitle());
                rTitle.getStyleClass().add("font-semibold");

                Label rSummary = new Label(r.getSummary());
                rSummary.getStyleClass().add("text-muted");
                rSummary.setWrapText(true);

                recRow.getChildren().addAll(rTitle, rSummary);
                advisorTeaserContainer.getChildren().add(recRow);
            }
        }
    }
}
