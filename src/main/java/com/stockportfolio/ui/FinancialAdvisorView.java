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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Institutional Financial Advisor View:
 * 1. Personal Salary & Expense Allocation Planner ("Aplya X salary madhe kiti kuthe kharcha aani kiti kuthe investment keli pahije")
 * 2. Interactive Advisor Q&A Chat Terminal (Answers any investment, budgeting, risk or loan query)
 * 3. Strategic Asset Allocation & 8 Rule-Based Actionable Recommendations
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

    // Salary Planner UI
    private TextField salaryInputField;
    private ComboBox<String> frameworkCombo;
    private VBox salaryPlanResultBox;

    // Interactive Advisor Chat UI
    private TextField questionInput;
    private TextArea chatConversationArea;

    // Allocation comparison
    private Label allocationComparisonText;

    // Recommendations Table
    private TableView<AdvisorRecommendation> recTable = new TableView<>();

    public FinancialAdvisorView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(26));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildFinancialHealthCards();
        buildSalaryAllocationPlannerSection();
        buildInteractiveAdvisorChatSection();
        buildAllocationBenchmarkingSection();
        buildRecommendationsSection();

        setContent(container);
        refreshAdvisor();
        context.addDataChangeListener(this::refreshAdvisor);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Personal Financial Advisor & Wealth Strategist");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Personalized financial guidance: itemized salary budgeting, investment allocation breakdown, interactive Q&A assistant, and portfolio health audit.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button editProfileBtn = new Button("⚙ Edit Financial Profile");
        editProfileBtn.getStyleClass().addAll("btn", "btn-secondary");
        editProfileBtn.setOnAction(e -> showEditProfileDialog());

        Button rerunBtn = new Button("✨ Run Portfolio Audit");
        rerunBtn.getStyleClass().addAll("btn", "btn-primary");
        rerunBtn.setOnAction(e -> handleRunAdvisor());

        HBox actionBox = new HBox(12, editProfileBtn, rerunBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(titleBox, actionBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(header);
    }

    private void buildFinancialHealthCards() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Financial Health & Cashflow Diagnostics");
        title.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        surplusLabel = new Label("₹0.00");
        VBox c1 = createMetricBox("Monthly Surplus", surplusLabel, "Income − Expenses");

        savingsRateLabel = new Label("0.00%");
        VBox c2 = createMetricBox("Savings Rate", savingsRateLabel, "Surplus / Monthly Income");

        emergencyFundLabel = new Label("0.0 Months");
        VBox c3 = createMetricBox("Emergency Cushion", emergencyFundLabel, "Liquid cash / monthly expenses");

        dtiLabel = new Label("0.00%");
        VBox c4 = createMetricBox("Debt-to-Income (DTI)", dtiLabel, "Debt / Annual Income");

        riskPrefLabel = new Label("BALANCED");
        VBox c5 = createMetricBox("Risk Profile", riskPrefLabel, "Current investment tolerance");

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

    /**
     * Module 1: Salary Budgeting & Investment Allocator
     * Answers: "In X amount salary, how much to spend, where to spend, and where/how much to invest"
     */
    private void buildSalaryAllocationPlannerSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Personal Salary Budgeting & Investment Allocator");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Calculate exactly how much of your monthly income to spend on essentials, lifestyle desires, and exactly which assets to invest in.");
        sub.getStyleClass().add("card-subtitle");

        HBox inputBar = new HBox(14);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        salaryInputField = new TextField("60000");
        salaryInputField.setPromptText("Monthly Net Salary (₹)");
        salaryInputField.setPrefWidth(180);

        frameworkCombo = new ComboBox<>();
        frameworkCombo.getItems().addAll("The 50/30/20 Budgeting Rule", "The 70/15/15 Financial Framework");
        frameworkCombo.setValue("The 50/30/20 Budgeting Rule");
        frameworkCombo.setPrefWidth(260);

        Button calculateBtn = new Button("📊 Generate Itemized Plan");
        calculateBtn.getStyleClass().addAll("btn", "btn-primary");
        calculateBtn.setOnAction(e -> generateSalaryPlan());

        inputBar.getChildren().addAll(new Label("Enter Salary:"), salaryInputField, new Label("Framework:"), frameworkCombo, calculateBtn);

        salaryPlanResultBox = new VBox(12);
        salaryPlanResultBox.setStyle("-fx-background-color: #0b1120; -fx-padding: 16px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

        card.getChildren().addAll(title, sub, inputBar, salaryPlanResultBox);
        container.getChildren().add(card);

        // Run default generation
        generateSalaryPlan();
    }

    private void generateSalaryPlan() {
        try {
            BigDecimal salary = new BigDecimal(salaryInputField.getText().trim());
            if (salary.signum() <= 0) throw new NumberFormatException();

            salaryPlanResultBox.getChildren().clear();
            boolean is503020 = frameworkCombo.getValue().contains("50/30/20");

            if (is503020) {
                BigDecimal needs = salary.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal wants = salary.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal savings = salary.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);

                // Investment breakdown of the 20%
                BigDecimal etfInv = savings.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal stockInv = savings.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal bondInv = savings.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);

                Label planTitle = new Label("📋 50/30/20 Allocation Plan for Monthly Salary of " + UIContext.formatMoney(salary) + ":");
                planTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

                Label n1 = new Label("• 50% Essential Needs (" + UIContext.formatMoney(needs) + "/month):");
                n1.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
                Label n2 = new Label("    └ Rent & Housing: " + UIContext.formatMoney(needs.multiply(new BigDecimal("0.50")))
                        + " | Groceries & Food: " + UIContext.formatMoney(needs.multiply(new BigDecimal("0.25")))
                        + " | Utilities & Health: " + UIContext.formatMoney(needs.multiply(new BigDecimal("0.25"))));
                n2.setStyle("-fx-text-fill: #94a3b8;");

                Label w1 = new Label("• 30% Lifestyle & Personal Wants (" + UIContext.formatMoney(wants) + "/month):");
                w1.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
                Label w2 = new Label("    └ Dining out, weekend entertainment, gadgets, leisure travel, and OTT subscriptions.");
                w2.setStyle("-fx-text-fill: #94a3b8;");

                Label s1 = new Label("• 20% Mandatory Savings & Investments (" + UIContext.formatMoney(savings) + "/month):");
                s1.setStyle("-fx-font-weight: bold; -fx-text-fill: #34d399;");
                Label s2 = new Label("    Where to invest your " + UIContext.formatMoney(savings) + " monthly contribution:");
                s2.setStyle("-fx-text-fill: #cbd5e1;");
                Label s3 = new Label("    1. 60% in Broad Market Index ETF (NIFTYBEES): " + UIContext.formatMoney(etfInv) + " (Safe long-term compounding)");
                s3.setStyle("-fx-text-fill: #38bdf8;");
                Label s4 = new Label("    2. 25% in Bluechip Equities (TCS, Reliance): " + UIContext.formatMoney(stockInv) + " (High-growth capital expansion)");
                s4.setStyle("-fx-text-fill: #a78bfa;");
                Label s5 = new Label("    3. 15% in Sovereign Gold Bonds / Debt: " + UIContext.formatMoney(bondInv) + " (Capital preservation cushion)");
                s5.setStyle("-fx-text-fill: #fbbf24;");

                salaryPlanResultBox.getChildren().addAll(planTitle, n1, n2, w1, w2, s1, s2, s3, s4, s5);
            } else {
                BigDecimal living = salary.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal invest = salary.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal debt = salary.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal accelerated = salary.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);

                Label planTitle = new Label("📋 70/15/15 Financial Framework Plan for Monthly Salary of " + UIContext.formatMoney(salary) + ":");
                planTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #34d399;");

                Label l1 = new Label("• 70% Living Expenses (" + UIContext.formatMoney(living) + "/month):");
                l1.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
                Label l2 = new Label("    └ Covers all everyday essentials, rent, food, bills, and lifestyle desires combined.");
                l2.setStyle("-fx-text-fill: #94a3b8;");

                Label i1 = new Label("• 15% Core Wealth Investments (" + UIContext.formatMoney(invest) + "/month):");
                i1.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");
                Label i2 = new Label("    └ Invested every month into Index ETFs (Nifty 50) and bluechip stocks for compounding.");
                l2.setStyle("-fx-text-fill: #94a3b8;");

                Label d1 = new Label("• 15% Debt Clearance & Emergency Fund (" + UIContext.formatMoney(debt) + "/month):");
                d1.setStyle("-fx-font-weight: bold; -fx-text-fill: #fbbf24;");
                Label d2 = new Label("    └ Aggressively attacks outstanding credit card or personal debt until zero balance.");
                d2.setStyle("-fx-text-fill: #94a3b8;");

                Label acc = new Label("⚡ POST-DEBT ACCELERATION: Once your loans are paid off, the 15% debt budget joins investing,\n"
                        + "   accelerating your monthly investment rate to a massive 30% (" + UIContext.formatMoney(accelerated) + "/month)!");
                acc.setStyle("-fx-background-color: #131c2e; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-text-fill: #34d399; -fx-font-weight: bold;");

                salaryPlanResultBox.getChildren().addAll(planTitle, l1, l2, i1, i2, d1, d2, acc);
            }
        } catch (Exception ex) {
            salaryPlanResultBox.getChildren().clear();
            salaryPlanResultBox.getChildren().add(new Label("Please enter a valid numeric salary."));
        }
    }

    /**
     * Module 2: Interactive Advisor Q&A Terminal
     * Answers ANY financial question typed by the user or chosen from quick chips.
     */
    private void buildInteractiveAdvisorChatSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Interactive Financial Advisor Assistant (Q&A)");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Ask any question regarding investments, budgeting rules, debt payoff, or loans. The advisor evaluates your real balance sheet to give customized advice.");
        sub.getStyleClass().add("card-subtitle");

        // Quick prompt chips
        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        String[] sampleQuestions = {
                "How to invest ₹50k salary?",
                "50/30/20 vs 70/15/15?",
                "How to clear debt faster?",
                "What is SIP compounding?",
                "Can I take a loan on my stocks?",
                "How much emergency fund?"
        };

        for (String q : sampleQuestions) {
            Button chip = new Button(q);
            chip.getStyleClass().addAll("btn-sm", "btn-outline");
            chip.setOnAction(e -> {
                questionInput.setText(q);
                handleAskQuestion();
            });
            chips.getChildren().add(chip);
        }

        // Conversation history
        chatConversationArea = new TextArea();
        chatConversationArea.setEditable(false);
        chatConversationArea.setWrapText(true);
        chatConversationArea.setPrefHeight(180);
        chatConversationArea.setText("Advisor: Hello! I am your personal financial planning advisor. Ask me anything about budgeting your salary, choosing between stocks and ETFs, calculating loan EMIs, or managing portfolio risk!");
        chatConversationArea.setStyle("-fx-control-inner-background: #0b1120; -fx-text-fill: #e2e8f0; -fx-font-family: monospace; -fx-font-size: 13px;");

        // Input row
        HBox inputRow = new HBox(12);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        questionInput = new TextField();
        questionInput.setPromptText("Type your financial or investment question here...");
        HBox.setHgrow(questionInput, Priority.ALWAYS);
        questionInput.setOnAction(e -> handleAskQuestion());

        Button askBtn = new Button("💬 Ask Advisor");
        askBtn.getStyleClass().addAll("btn", "btn-primary");
        askBtn.setOnAction(e -> handleAskQuestion());

        inputRow.getChildren().addAll(questionInput, askBtn);

        card.getChildren().addAll(title, sub, chips, chatConversationArea, inputRow);
        container.getChildren().add(card);
    }

    private void handleAskQuestion() {
        String q = questionInput.getText().trim();
        if (q.isEmpty()) return;

        Trader trader = context.getTrader();
        FinancialProfile profile = context.getAdvisorService().getProfile(trader.getUserId());
        String answer = generateSmartAnswer(q, trader, profile);

        chatConversationArea.appendText("\n\nYou: " + q);
        chatConversationArea.appendText("\nAdvisor: " + answer);
        chatConversationArea.appendText("\n" + "─".repeat(70));
        questionInput.clear();
    }

    private String generateSmartAnswer(String query, Trader trader, FinancialProfile profile) {
        String q = query.toLowerCase();

        if (q.contains("salary") || q.contains("invest") && (q.contains("50") || q.contains("60") || q.contains("how to invest"))) {
            return "Based on your current salary profile of " + UIContext.formatMoney(profile.getMonthlyIncome()) + "/month:\n"
                    + "1. Ensure 50% covers essential needs (rent, groceries, electricity).\n"
                    + "2. Cap lifestyle wants to 30%.\n"
                    + "3. Invest the remaining 20% systematically:\n"
                    + "   • Deploy 60% into Nifty 50 Index ETFs (NIFTYBEES) for market return.\n"
                    + "   • Deploy 25% into large-cap bluechips (TCS, Reliance) for capital appreciation.\n"
                    + "   • Deploy 15% into Sovereign Gold Bonds for risk mitigation.\n"
                    + "You currently have " + UIContext.formatMoney(trader.getCashBalance()) + " in liquid cash ready to invest!";
        }

        if (q.contains("50/30/20") || q.contains("70/15/15") || q.contains("rule") || q.contains("which")) {
            return "Both rules are excellent but serve different stages:\n"
                    + "• Choose 50/30/20 if you have ZERO or low debt and want a clear balance between daily fun (30% wants) and future investing (20%).\n"
                    + "• Choose 70/15/15 if you have existing liabilities (e.g. your active debt of ₹" + profile.getExistingDebt().toPlainString() + "). "
                    + "It allocates 15% strictly for debt clearance and once loans are paid off, automatically expands to 30% investing!";
        }

        if (q.contains("debt") || q.contains("loan") || q.contains("clear")) {
            if (profile.getExistingDebt().signum() > 0) {
                return "You currently have ₹" + profile.getExistingDebt().toPlainString() + " in outstanding debt.\n"
                        + "Strategy: Use the 'Debt Avalanche' or 70/15/15 rule. Pay " + UIContext.formatMoney(profile.getRecommendedDebtOrEmergencyFifteen())
                        + " every month towards your loan. Clearing debt guarantees a risk-free return equivalent to your interest rate!";
            } else {
                return "You are completely debt-free! That gives you maximum financial leverage to invest up to 30% of your income into compounding equity assets.";
            }
        }

        if (q.contains("sip") || q.contains("compound")) {
            return "A Systematic Investment Plan (SIP) uses rupee-cost averaging: you buy more shares when prices fall, and fewer when prices rise.\n"
                    + "Compounding Formula: FV = P × [((1 + r)^n - 1) / r] × (1 + r).\n"
                    + "Investing ₹5,000/month at 12% CAGR for 15 years results in ₹25.2 Lakhs on an investment of just ₹9 Lakhs!";
        }

        if (q.contains("emergency") || q.contains("buffer")) {
            BigDecimal months = profile.getEmergencyFundMonthsCovered();
            return "An emergency buffer must cover 3 to 6 months of mandatory living expenses (" + UIContext.formatMoney(profile.getMonthlyExpenses()) + "/month).\n"
                    + "Your target buffer is " + UIContext.formatMoney(profile.getMonthlyExpenses().multiply(new BigDecimal("6"))) + ".\n"
                    + "You currently have " + months + " months of living expenses covered in liquid cash.";
        }

        if (q.contains("loan on stock") || q.contains("las") || q.contains("pledge")) {
            return "Yes! You can take a Loan Against Securities (LAS) by pledging your stocks up to 50% Loan-to-Value (LTV) at approximately 9.5% interest p.a.\n"
                    + "This provides instant liquidity without triggering capital gains taxes or losing dividends. Check our dedicated 'Loans' tab!";
        }

        if (q.contains("pe ratio") || q.contains("market cap") || q.contains("fundamentals")) {
            return "Key financial fundamentals:\n"
                    + "• Market Cap = Share Price × Total Outstanding Shares (indicates company size).\n"
                    + "• P/E Ratio = Price per share / Earnings per share (indicates how much you pay for ₹1 of profit).\n"
                    + "• Book Value = Net asset value of the company per share.\n"
                    + "You can inspect all these live under the 'Explore' tab for any selected stock!";
        }

        // Generic financial wisdom response
        return "Financial discipline begins with paying yourself first! Keep at least 20% of your salary invested every month across diversified assets, sustain a 6-month emergency buffer, and avoid taking high-interest personal loans. Let me know if you need specific calculations!";
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

        Label title = new Label("Actionable Advisory Recommendations & Audit Findings");
        title.getStyleClass().add("section-title");

        recTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recTable.setPrefHeight(280);

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

        TableColumn<AdvisorRecommendation, Void> actionCol = new TableColumn<>("Manage Status");
        actionCol.setPrefWidth(180);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn = new Button("Accept");
            private final Button completeBtn = new Button("Done");
            private final Button dismissBtn = new Button("✕");
            private final HBox btnBox = new HBox(6, acceptBtn, completeBtn, dismissBtn);

            {
                acceptBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
                completeBtn.getStyleClass().addAll("btn-sm", "btn-primary");
                dismissBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                btnBox.setAlignment(Pos.CENTER);

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
        alert.setTitle("Portfolio Audit Complete");
        alert.setHeaderText("Financial Advisory Rules Executed");
        alert.setContentText("Evaluated portfolio diversification, concentration, cash drag, and budgeting frameworks.\nTotal active recommendations: " + recs.size());
        alert.showAndWait();
    }

    private void showEditProfileDialog() {
        Trader trader = context.getTrader();
        FinancialProfile profile = context.getAdvisorService().getProfile(trader.getUserId());

        Dialog<FinancialProfile> dialog = new Dialog<>();
        dialog.setTitle("Edit Financial Profile");
        dialog.setHeaderText("Update monthly income, expenses, liabilities, and risk tolerance.");

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
        gp.add(new Label("Monthly Net Salary (INR):"), 0, 1);
        gp.add(incomeField, 1, 1);
        gp.add(new Label("Monthly Living Expenses (INR):"), 0, 2);
        gp.add(expensesField, 1, 2);
        gp.add(new Label("Liquid Cash Reserves (INR):"), 0, 3);
        gp.add(savingsField, 1, 3);
        gp.add(new Label("Total Outstanding Debt (INR):"), 0, 4);
        gp.add(debtField, 1, 4);

        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    profile.setRiskPreference(riskCombo.getValue());
                    profile.setMonthlyIncome(new BigDecimal(incomeField.getText().trim()));
                    profile.setMonthlyExpenses(new BigDecimal(expensesField.getText().trim()));
                    profile.setAvailableSavings(new BigDecimal(savingsField.getText().trim()));
                    profile.setExistingDebt(new BigDecimal(debtField.getText().trim()));
                    context.getAdvisorService().saveProfile(profile);
                    salaryInputField.setText(profile.getMonthlyIncome().toPlainString());
                    generateSalaryPlan();
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
