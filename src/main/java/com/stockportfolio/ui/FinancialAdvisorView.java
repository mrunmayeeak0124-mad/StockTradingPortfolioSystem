package com.stockportfolio.ui;

import com.stockportfolio.model.AdvisorRecommendation;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.RecommendationCategory;
import com.stockportfolio.model.RecommendationStatus;
import com.stockportfolio.model.RiskPreference;
import com.stockportfolio.model.Trader;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Educational Financial Advisor View: rule-based financial planning, asset allocation benchmarking,
 * health diagnosis, and actionable recommendation status lifecycle management.
 */
public class FinancialAdvisorView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    // Profile Health stats
    private Label surplusLabel;
    private Label savingsRateLabel;
    private Label emergencyFundLabel;
    private Label dtiLabel;
    private Label riskPrefLabel;

    // 50/30/20 Framework labels
    private Label needsFiftyLabel;
    private Label wantsThirtyLabel;
    private Label savingsTwentyLabel;
    private Label fiftySummaryLabel;

    // 70/15/15 Framework labels
    private Label livingSeventyLabel;
    private Label investingFifteenLabel;
    private Label debtFifteenLabel;
    private Label postDebtThirtyLabel;
    private Label seventySummaryLabel;

    // Allocation comparison
    private Label allocationComparisonText;

    // Recommendations Table
    private TableView<AdvisorRecommendation> recTable = new TableView<>();

    public FinancialAdvisorView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildDisclaimer();
        buildFinancialHealthCards();
        buildBudgetingFrameworksSection();
        buildAllocationBenchmarkingSection();
        buildRecommendationsSection();

        setContent(container);
        refreshAdvisor();
        context.addDataChangeListener(this::refreshAdvisor);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Rule-Based Educational Financial Advisor");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Personalized guidance evaluating risk tolerance, asset allocation alignment, and savings feasibility.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button editProfileBtn = new Button("⚙ Edit Financial Profile");
        editProfileBtn.getStyleClass().addAll("btn", "btn-secondary");
        editProfileBtn.setOnAction(e -> showEditProfileDialog());

        Button rerunBtn = new Button("✨ Run Advisor Engine");
        rerunBtn.getStyleClass().addAll("btn", "btn-primary");
        rerunBtn.setOnAction(e -> handleRunAdvisor());

        HBox actionBox = new HBox(12, editProfileBtn, rerunBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(titleBox, actionBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(header);
    }

    private void buildDisclaimer() {
        HBox banner = new HBox(12);
        banner.getStyleClass().addAll("info-banner", "banner-warning");
        banner.setPadding(new Insets(12, 16, 12, 16));

        Label text = new Label("⚠️ EDUCATIONAL DISCLAIMER: This advisor module provides rule-based simulations and pedagogical guidance for academic demonstration only. It does not provide personalized investment advice, guaranteed returns, or certified fiduciary services.");
        text.setWrapText(true);
        text.getStyleClass().add("font-semibold");
        banner.getChildren().add(text);
        container.getChildren().add(banner);
    }

    private void buildFinancialHealthCards() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Financial Health & Cashflow Overview");
        title.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        surplusLabel = new Label("₹0.00");
        VBox c1 = createMetricBox("Monthly Surplus", surplusLabel, "Income − Expenses");

        savingsRateLabel = new Label("0.00%");
        VBox c2 = createMetricBox("Savings Rate", savingsRateLabel, "Surplus / Monthly Income");

        emergencyFundLabel = new Label("0.0 Months");
        VBox c3 = createMetricBox("Emergency Buffer", emergencyFundLabel, "Liquid cash / expenses");

        dtiLabel = new Label("0.00%");
        VBox c4 = createMetricBox("Debt-to-Income (DTI)", dtiLabel, "Debt / Annual Income");

        riskPrefLabel = new Label("BALANCED");
        VBox c5 = createMetricBox("Risk Profile", riskPrefLabel, "Investment risk preference");

        grid.add(c1, 0, 0);
        grid.add(c2, 1, 0);
        grid.add(c3, 2, 0);
        grid.add(c4, 3, 0);
        grid.add(c5, 4, 0);

        for (int i = 0; i < 5; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(20.0);
            grid.getColumnConstraints().add(cc);
        }

        card.getChildren().addAll(title, grid);
        container.getChildren().add(card);
    }

    private VBox createMetricBox(String title, Label valLabel, String sub) {
        VBox box = new VBox(4);
        box.getStyleClass().add("metric-card");
        box.setPadding(new Insets(12, 16, 12, 16));

        Label t = new Label(title);
        t.getStyleClass().add("card-title");

        valLabel.getStyleClass().add("card-value");

        Label s = new Label(sub);
        s.getStyleClass().add("card-subtitle");

        box.getChildren().addAll(t, valLabel, s);
        return box;
    }

    private void buildBudgetingFrameworksSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Personal Finance Budgeting Frameworks (50/30/20 & 70/15/15)");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Rule-based income allocation models to benchmark essential needs, lifestyle desires, debt elimination, and investment momentum.");
        sub.getStyleClass().add("card-subtitle");

        // Section 1: 50/30/20 Rule
        VBox rule50Box = new VBox(10);
        rule50Box.setStyle("-fx-background-color: #0b1120; -fx-padding: 16px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");
        Label r50Title = new Label("📌 The 50/30/20 Budgeting Rule");
        r50Title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        Label r50Desc = new Label("Standard balanced budget: 50% for Needs, 30% for Wants, and 20% for Savings & Investing.");
        r50Desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        GridPane g50 = new GridPane();
        g50.setHgap(14);
        g50.setVgap(10);

        needsFiftyLabel = new Label("₹0.00");
        VBox nBox = createMetricBox("50% Needs (Max)", needsFiftyLabel, "Rent, groceries, utilities, basics");

        wantsThirtyLabel = new Label("₹0.00");
        VBox wBox = createMetricBox("30% Wants (Cap)", wantsThirtyLabel, "Dining, leisure, subscriptions");

        savingsTwentyLabel = new Label("₹0.00");
        VBox sBox = createMetricBox("20% Savings (Target)", savingsTwentyLabel, "Stocks, ETFs, emergency cash");

        g50.add(nBox, 0, 0);
        g50.add(wBox, 1, 0);
        g50.add(sBox, 2, 0);

        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            g50.getColumnConstraints().add(cc);
        }

        fiftySummaryLabel = new Label();
        fiftySummaryLabel.setWrapText(true);
        fiftySummaryLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        rule50Box.getChildren().addAll(r50Title, r50Desc, g50, fiftySummaryLabel);

        // Section 2: 70/15/15 Rule
        VBox rule70Box = new VBox(10);
        rule70Box.setStyle("-fx-background-color: #0b1120; -fx-padding: 16px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");
        Label r70Title = new Label("📌 The 70/15/15 Financial Framework");
        r70Title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #34d399;");
        Label r70Desc = new Label("Structured cashflow: 70% for Living Expenses, 15% for Investing, and 15% for Debt Repayment or Emergency Buffer (Expands to 30% investing after debt!).");
        r70Desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        GridPane g70 = new GridPane();
        g70.setHgap(14);
        g70.setVgap(10);

        livingSeventyLabel = new Label("₹0.00");
        VBox lBox = createMetricBox("70% Living Expenses", livingSeventyLabel, "Needs & lifestyle combined");

        investingFifteenLabel = new Label("₹0.00");
        VBox invBox = createMetricBox("15% Core Investing", investingFifteenLabel, "Long-term market compounding");

        debtFifteenLabel = new Label("₹0.00");
        VBox debtBox = createMetricBox("15% Debt / Buffer", debtFifteenLabel, "Pay loans / build rainy-day");

        postDebtThirtyLabel = new Label("₹0.00");
        VBox accBox = createMetricBox("30% Post-Debt Rate", postDebtThirtyLabel, "Invested once debt is zero!");

        g70.add(lBox, 0, 0);
        g70.add(invBox, 1, 0);
        g70.add(debtBox, 2, 0);
        g70.add(accBox, 3, 0);

        for (int i = 0; i < 4; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(25.0);
            g70.getColumnConstraints().add(cc);
        }

        seventySummaryLabel = new Label();
        seventySummaryLabel.setWrapText(true);
        seventySummaryLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        rule70Box.getChildren().addAll(r70Title, r70Desc, g70, seventySummaryLabel);

        card.getChildren().addAll(title, sub, rule50Box, rule70Box);
        container.getChildren().add(card);
    }

    private void buildAllocationBenchmarkingSection() {
        VBox card = new VBox(12);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Strategic Asset Allocation vs. Benchmark");
        title.getStyleClass().add("section-title");

        allocationComparisonText = new Label("Analyzing current allocation against benchmark model...");
        allocationComparisonText.setWrapText(true);
        allocationComparisonText.setStyle("-fx-font-size: 14px; -fx-line-spacing: 4px;");

        card.getChildren().addAll(title, allocationComparisonText);
        container.getChildren().add(card);
    }

    private void buildRecommendationsSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Actionable Advisory Recommendations & Audit Log");
        title.getStyleClass().add("section-title");

        recTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recTable.setPrefHeight(320);

        TableColumn<AdvisorRecommendation, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory().getDisplayName()));
        catCol.setPrefWidth(140);

        TableColumn<AdvisorRecommendation, String> titleCol = new TableColumn<>("Recommendation & Finding");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setPrefWidth(180);

        TableColumn<AdvisorRecommendation, String> summaryCol = new TableColumn<>("Summary & Rationale");
        summaryCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSummary() + " | Reason: " + d.getValue().getReasoning()));
        summaryCol.setPrefWidth(320);

        TableColumn<AdvisorRecommendation, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        statusCol.setPrefWidth(90);

        // Action Column for updating recommendation status
        TableColumn<AdvisorRecommendation, Void> actionCol = new TableColumn<>("Manage Status");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button reviewBtn = new Button("Review");
            private final Button acceptBtn = new Button("Accept");
            private final Button completeBtn = new Button("Done");
            private final Button dismissBtn = new Button("✕");
            private final HBox btnBox = new HBox(6, reviewBtn, acceptBtn, completeBtn, dismissBtn);

            {
                reviewBtn.getStyleClass().addAll("btn-sm", "btn-outline");
                acceptBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
                completeBtn.getStyleClass().addAll("btn-sm", "btn-primary");
                dismissBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                btnBox.setAlignment(Pos.CENTER);

                reviewBtn.setOnAction(e -> updateStatus(RecommendationStatus.REVIEWED));
                acceptBtn.setOnAction(e -> updateStatus(RecommendationStatus.ACCEPTED));
                completeBtn.setOnAction(e -> updateStatus(RecommendationStatus.COMPLETED));
                dismissBtn.setOnAction(e -> updateStatus(RecommendationStatus.DISMISSED));
            }

            private void updateStatus(RecommendationStatus st) {
                AdvisorRecommendation r = getTableView().getItems().get(getIndex());
                context.getAdvisorService().updateRecommendationStatus(r.getRecommendationId(), st);
                refreshAdvisor();
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btnBox);
                }
            }
        });

        recTable.getColumns().addAll(catCol, titleCol, summaryCol, statusCol, actionCol);
        card.getChildren().addAll(title, recTable);
        container.getChildren().add(card);
    }

    private void refreshAdvisor() {
        Trader trader = context.getTrader();
        FinancialProfile profile = context.getAdvisorService().getProfile(trader.getUserId());

        surplusLabel.setText(UIContext.formatMoney(profile.getMonthlySurplus()));
        savingsRateLabel.setText(UIContext.formatPercent(profile.getSavingsRate()));
        emergencyFundLabel.setText(profile.getEmergencyFundMonthsCovered().toPlainString() + " Months");
        dtiLabel.setText(UIContext.formatPercent(profile.getDebtToIncomeRatio()));
        riskPrefLabel.setText(profile.getRiskPreference().name());

        // 50/30/20 values
        needsFiftyLabel.setText(UIContext.formatMoney(profile.getRecommendedNeedsFifty()));
        wantsThirtyLabel.setText(UIContext.formatMoney(profile.getRecommendedWantsThirty()));
        savingsTwentyLabel.setText(UIContext.formatMoney(profile.getRecommendedSavingsTwenty()));

        BigDecimal surplus = profile.getMonthlySurplus();
        BigDecimal targetSavings20 = profile.getRecommendedSavingsTwenty();
        if (surplus.compareTo(targetSavings20) >= 0) {
            fiftySummaryLabel.setText("✅ Excellent! Your current monthly surplus of " + UIContext.formatMoney(surplus)
                    + " meets or exceeds the 20% savings target (" + UIContext.formatMoney(targetSavings20) + ").");
        } else {
            fiftySummaryLabel.setText("⚠️ Gap Detected: Your surplus is " + UIContext.formatMoney(surplus)
                    + ", which is below the recommended 20% savings target of " + UIContext.formatMoney(targetSavings20)
                    + ". Check if wants or non-essential expenses can be optimized.");
        }

        // 70/15/15 values
        livingSeventyLabel.setText(UIContext.formatMoney(profile.getRecommendedLivingExpensesSeventy()));
        investingFifteenLabel.setText(UIContext.formatMoney(profile.getRecommendedInvestingFifteen()));
        debtFifteenLabel.setText(UIContext.formatMoney(profile.getRecommendedDebtOrEmergencyFifteen()));
        postDebtThirtyLabel.setText(UIContext.formatMoney(profile.getAcceleratedPostDebtInvestingThirty()));

        if (profile.getExistingDebt().signum() > 0) {
            seventySummaryLabel.setText("⚡ Active Debt (₹" + profile.getExistingDebt().toPlainString() + "): Allocate "
                    + UIContext.formatMoney(profile.getRecommendedDebtOrEmergencyFifteen())
                    + " (15%) every month towards accelerating debt elimination while sustaining 15% in investments. Once cleared, shift full 30% ("
                    + UIContext.formatMoney(profile.getAcceleratedPostDebtInvestingThirty()) + ") into investments!");
        } else {
            seventySummaryLabel.setText("🎉 Zero Debt Status: You are debt-free! You can deploy the full accelerated 30% ("
                    + UIContext.formatMoney(profile.getAcceleratedPostDebtInvestingThirty()) + ") monthly into long-term investing!");
        }

        // Allocation comparison
        Map<AssetCategory, BigDecimal> actual = trader.getPortfolio().getAllocationByCategory();
        Map<AssetCategory, BigDecimal> target = context.getAdvisorService().getTargetAllocation(profile.getRiskPreference());

        StringBuilder sb = new StringBuilder();
        sb.append("Benchmark Targets for ").append(profile.getRiskPreference()).append(" Profile:\n");
        sb.append("• Stocks: ").append(target.get(AssetCategory.STOCK)).append("%  |  Current: ")
          .append(actual.getOrDefault(AssetCategory.STOCK, BigDecimal.ZERO)).append("%\n");
        sb.append("• ETFs: ").append(target.get(AssetCategory.ETF)).append("%  |  Current: ")
          .append(actual.getOrDefault(AssetCategory.ETF, BigDecimal.ZERO)).append("%\n");
        sb.append("• Bonds: ").append(target.get(AssetCategory.BOND)).append("%  |  Current: ")
          .append(actual.getOrDefault(AssetCategory.BOND, BigDecimal.ZERO)).append("%\n\n");
        sb.append("Diagnostic Commentary:\n");
        sb.append(context.getAdvisorService().explainAllocationDifference(actual, target));
        allocationComparisonText.setText(sb.toString());

        // Load recommendations
        List<AdvisorRecommendation> recs = context.getAdvisorService().getRecommendations(trader.getUserId());
        recTable.setItems(FXCollections.observableArrayList(recs));
    }

    private void handleRunAdvisor() {
        Trader trader = context.getTrader();
        FinancialProfile profile = context.getAdvisorService().getProfile(trader.getUserId());
        List<FinancialGoal> goals = context.getGoalService().getGoals(trader.getUserId());

        List<AdvisorRecommendation> recs = context.getAdvisorService().generateRecommendations(trader, profile, goals);
        refreshAdvisor();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Advisor Evaluation Finished");
        alert.setHeaderText("AI Financial Advisor Execution Complete");
        alert.setContentText("Analyzed portfolio and profile against financial safety rules.\n"
                + "Total active recommendations: " + recs.size());
        alert.showAndWait();
    }

    private void showEditProfileDialog() {
        Trader trader = context.getTrader();
        FinancialProfile profile = context.getAdvisorService().getProfile(trader.getUserId());

        Dialog<FinancialProfile> dialog = new Dialog<>();
        dialog.setTitle("Edit Personal Financial Profile");
        dialog.setHeaderText("Update your monthly cashflow, liabilities, and risk tolerance.");

        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(12);
        gp.setPadding(new Insets(20));

        ComboBox<RiskPreference> riskCombo = new ComboBox<>();
        riskCombo.getItems().addAll(RiskPreference.values());
        riskCombo.setValue(profile.getRiskPreference());

        TextField incomeField = new TextField(profile.getMonthlyIncome().toPlainString());
        TextField expensesField = new TextField(profile.getMonthlyExpenses().toPlainString());
        TextField savingsField = new TextField(profile.getAvailableSavings().toPlainString());
        TextField debtField = new TextField(profile.getExistingDebt().toPlainString());

        gp.add(new Label("Risk Preference:"), 0, 0);
        gp.add(riskCombo, 1, 0);
        gp.add(new Label("Monthly Gross Income (INR):"), 0, 1);
        gp.add(incomeField, 1, 1);
        gp.add(new Label("Monthly Living Expenses (INR):"), 0, 2);
        gp.add(expensesField, 1, 2);
        gp.add(new Label("Available Liquid Savings (INR):"), 0, 3);
        gp.add(savingsField, 1, 3);
        gp.add(new Label("Total Outstanding Debt (INR):"), 0, 4);
        gp.add(debtField, 1, 4);

        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    profile.setRiskPreference(riskCombo.getValue());
                    profile.setMonthlyIncome(new BigDecimal(incomeField.getText().trim()));
                    profile.setMonthlyExpenses(new BigDecimal(expensesField.getText().trim()));
                    profile.setAvailableSavings(new BigDecimal(savingsField.getText().trim()));
                    profile.setExistingDebt(new BigDecimal(debtField.getText().trim()));
                    context.getAdvisorService().saveProfile(profile);
                    refreshAdvisor();
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Invalid numbers entered: " + ex.getMessage());
                    a.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }
}
