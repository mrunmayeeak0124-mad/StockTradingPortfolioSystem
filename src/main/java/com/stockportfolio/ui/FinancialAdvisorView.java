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
