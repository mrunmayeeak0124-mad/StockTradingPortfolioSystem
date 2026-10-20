package com.stockportfolio.ui;

import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Trader;
import com.stockportfolio.util.HomeLoanCalculator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Loans Section: Comprehensive lending and financing advisory center.
 * Features:
 * 1. Loan Against Stocks & Securities (LAS) — 50% LTV pledge valuation
 * 2. Loan Against Mutual Funds — Overdraft credit lines
 * 3. Home Mortgage Loan Planner & Amortization
 * 4. Personal Loan & Salary Affordability Planner
 */
public class LoansView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    // LAS widgets
    private Label lasPortfolioValueLabel;
    private Label lasEligibleLimitLabel;
    private TextField lasBorrowAmountField;
    private Label lasMonthlyInterestLabel;

    // Mutual Fund Loan widgets
    private TextField mfHoldingsField;
    private Label mfEligibleLimitLabel;
    private Label mfMonthlyCostLabel;

    // Home Mortgage Loan widgets
    private TextField homePriceField;
    private TextField homeDownPayField;
    private TextField homeRateField;
    private TextField homeTenureField;
    private Label homeEmiLabel;
    private Label homeTotalInterestLabel;
    private Label homeUpfrontCashLabel;

    // Personal Loan widgets
    private TextField personalLoanAmountField;
    private TextField personalRateField;
    private TextField personalTenureField;
    private Label personalEmiLabel;
    private Label personalFoirLabel;

    public LoansView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(26));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildLoanAgainstStocksSection();
        buildLoanAgainstMutualFundsSection();
        buildHomeLoanMortgageSection();
        buildPersonalLoanSection();

        setContent(container);
        calculateLas();
        calculateMfLoan();
        calculateHomeLoan();
        calculatePersonalLoan();

        context.addDataChangeListener(() -> {
            calculateLas();
            calculatePersonalLoan();
        });
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Institutional Lending & Credit Facilities (Loans)");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Explore secured and unsecured credit facilities: Loan Against Securities (LAS), Mutual Fund Overdrafts, Home Purchase Mortgages, and Personal Loan EMI planners.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    /**
     * 1. Loan Against Stocks & Securities (LAS)
     */
    private void buildLoanAgainstStocksSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("📈 Loan Against Stocks & Securities (LAS)");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Pledge your listed equity shares to obtain instant liquidity without selling your holdings, avoiding capital gains tax while continuing to earn corporate dividends.");
        sub.getStyleClass().add("card-subtitle");

        GridPane statGrid = new GridPane();
        statGrid.setHgap(16);
        statGrid.setVgap(12);

        lasPortfolioValueLabel = new Label("₹0.00");
        VBox c1 = createLoanStatCard("Current Stock Holdings Value", lasPortfolioValueLabel, "Pledgeable equity collateral");

        lasEligibleLimitLabel = new Label("₹0.00");
        VBox c2 = createLoanStatCard("Maximum Loan Limit (50% LTV)", lasEligibleLimitLabel, "Regulatory LTV cap on equities");

        statGrid.add(c1, 0, 0);
        statGrid.add(c2, 1, 0);
        for (int i = 0; i < 2; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(50.0);
            statGrid.getColumnConstraints().add(cc);
        }

        // Borrow calculator
        HBox calcRow = new HBox(16);
        calcRow.setAlignment(Pos.CENTER_LEFT);

        lasBorrowAmountField = new TextField("20000");
        lasBorrowAmountField.setPrefWidth(160);
        lasBorrowAmountField.textProperty().addListener((o, ov, nv) -> calculateLas());

        lasMonthlyInterestLabel = new Label("₹0.00");
        lasMonthlyInterestLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        calcRow.getChildren().addAll(
                new Label("Desired Borrow Amount (₹):"), lasBorrowAmountField,
                new Label("Annual Interest (9.50%):"),
                new Label("Monthly Interest Cost:"), lasMonthlyInterestLabel
        );

        card.getChildren().addAll(title, sub, statGrid, calcRow);
        container.getChildren().add(card);
    }

    private void calculateLas() {
        Trader trader = context.getTrader();
        PortfolioSummary summary = context.getPortfolioService().getPortfolioSummary(trader);
        BigDecimal holdings = summary.holdingsValue();
        BigDecimal limit = holdings.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);

        lasPortfolioValueLabel.setText(UIContext.formatMoney(holdings));
        lasEligibleLimitLabel.setText(UIContext.formatMoney(limit));

        try {
            BigDecimal borrow = new BigDecimal(lasBorrowAmountField.getText().trim());
            // Monthly interest at 9.50% p.a. = borrow * 0.095 / 12
            BigDecimal monthlyInterest = borrow.multiply(new BigDecimal("0.095"))
                    .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            lasMonthlyInterestLabel.setText(UIContext.formatMoney(monthlyInterest) + " / month");
        } catch (Exception ex) {
            lasMonthlyInterestLabel.setText("₹0.00");
        }
    }

    /**
     * 2. Loan Against Mutual Funds
     */
    private void buildLoanAgainstMutualFundsSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("📊 Loan Against Mutual Funds (Overdraft Facility)");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Pledge equity or debt mutual fund units for a revolving credit line. Pay interest only on the utilized amount.");
        sub.getStyleClass().add("card-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        mfHoldingsField = new TextField("100000");
        mfHoldingsField.setPrefWidth(160);
        mfHoldingsField.textProperty().addListener((o, ov, nv) -> calculateMfLoan());

        mfEligibleLimitLabel = new Label("₹65,000.00");
        mfEligibleLimitLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34d399; -fx-font-size: 15px;");

        mfMonthlyCostLabel = new Label("₹514.58 / month");
        mfMonthlyCostLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 15px;");

        grid.add(new Label("Mutual Fund Portfolio Value (₹):"), 0, 0);
        grid.add(mfHoldingsField, 1, 0);

        grid.add(new Label("Eligible Overdraft Limit (65% LTV):"), 0, 1);
        grid.add(mfEligibleLimitLabel, 1, 1);

        grid.add(new Label("Monthly Interest Cost (at 9.5% on full limit):"), 0, 2);
        grid.add(mfMonthlyCostLabel, 1, 2);

        card.getChildren().addAll(title, sub, grid);
        container.getChildren().add(card);
    }

    private void calculateMfLoan() {
        try {
            BigDecimal mfVal = new BigDecimal(mfHoldingsField.getText().trim());
            BigDecimal limit = mfVal.multiply(new BigDecimal("0.65")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal monthlyInterest = limit.multiply(new BigDecimal("0.095"))
                    .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

            mfEligibleLimitLabel.setText(UIContext.formatMoney(limit));
            mfMonthlyCostLabel.setText(UIContext.formatMoney(monthlyInterest) + " / month");
        } catch (Exception ex) {
            // Keep defaults
        }
    }

    /**
     * 3. Home Purchase & Mortgage Loan Planner
     */
    private void buildHomeLoanMortgageSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("🏠 Home Purchase & Mortgage Loan Planner");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Calculate monthly mortgage EMIs, total interest outlay, and verify mandatory upfront liquidity (20% Down Payment + 6% Stamp Duty + Legal).");
        sub.getStyleClass().add("card-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        homePriceField = new TextField("5000000"); // 50 Lakhs
        homePriceField.textProperty().addListener((o, ov, nv) -> calculateHomeLoan());

        homeDownPayField = new TextField("20.0"); // 20%
        homeDownPayField.textProperty().addListener((o, ov, nv) -> calculateHomeLoan());

        homeRateField = new TextField("8.50"); // 8.5%
        homeRateField.textProperty().addListener((o, ov, nv) -> calculateHomeLoan());

        homeTenureField = new TextField("20"); // 20 years
        homeTenureField.textProperty().addListener((o, ov, nv) -> calculateHomeLoan());

        grid.add(new Label("Property Valuation (₹):"), 0, 0);
        grid.add(homePriceField, 1, 0);

        grid.add(new Label("Down Payment (%):"), 0, 1);
        grid.add(homeDownPayField, 1, 1);

        grid.add(new Label("Interest Rate (% p.a.):"), 0, 2);
        grid.add(homeRateField, 1, 2);

        grid.add(new Label("Loan Tenure (Years):"), 0, 3);
        grid.add(homeTenureField, 1, 3);

        // Results
        GridPane resGrid = new GridPane();
        resGrid.setHgap(14);
        resGrid.setVgap(12);

        homeEmiLabel = new Label("₹0.00");
        VBox r1 = createLoanStatCard("Monthly Mortgage EMI", homeEmiLabel, "Fixed monthly installment");

        homeTotalInterestLabel = new Label("₹0.00");
        VBox r2 = createLoanStatCard("Total Interest Outlay", homeTotalInterestLabel, "Bank borrowing cost");

        homeUpfrontCashLabel = new Label("₹0.00");
        VBox r3 = createLoanStatCard("Upfront Cash Required", homeUpfrontCashLabel, "Down payment + Stamp Duty (6%)");

        resGrid.add(r1, 0, 0);
        resGrid.add(r2, 1, 0);
        resGrid.add(r3, 2, 0);

        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            resGrid.getColumnConstraints().add(cc);
        }

        card.getChildren().addAll(title, sub, grid, resGrid);
        container.getChildren().add(card);
    }

    private void calculateHomeLoan() {
        try {
            BigDecimal propPrice = new BigDecimal(homePriceField.getText().trim());
            BigDecimal downPct = new BigDecimal(homeDownPayField.getText().trim());
            BigDecimal rate = new BigDecimal(homeRateField.getText().trim());
            int years = Integer.parseInt(homeTenureField.getText().trim());

            BigDecimal downAmount = propPrice.multiply(downPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal loanAmount = propPrice.subtract(downAmount);

            BigDecimal emi = HomeLoanCalculator.calculateEMI(loanAmount, rate, years);
            BigDecimal totalPayment = emi.multiply(new BigDecimal(years * 12));
            BigDecimal totalInterest = totalPayment.subtract(loanAmount);

            // Upfront cash = Down Payment + 6% Stamp Duty + 1% Registration
            BigDecimal stampDuty = propPrice.multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal upfront = downAmount.add(stampDuty);

            homeEmiLabel.setText(UIContext.formatMoney(emi));
            homeTotalInterestLabel.setText(UIContext.formatMoney(totalInterest));
            homeUpfrontCashLabel.setText(UIContext.formatMoney(upfront));
        } catch (Exception ex) {
            // Keep default
        }
    }

    /**
     * 4. Personal Loan & Affordability Planner
     */
    private void buildPersonalLoanSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("💳 Personal Loan & Debt Affordability Planner");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Simulate unsecured personal loan installments and evaluate debt burden against your net monthly salary.");
        sub.getStyleClass().add("card-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        personalLoanAmountField = new TextField("300000"); // 3 Lakhs
        personalLoanAmountField.textProperty().addListener((o, ov, nv) -> calculatePersonalLoan());

        personalRateField = new TextField("11.50"); // 11.5%
        personalRateField.textProperty().addListener((o, ov, nv) -> calculatePersonalLoan());

        personalTenureField = new TextField("3"); // 3 years
        personalTenureField.textProperty().addListener((o, ov, nv) -> calculatePersonalLoan());

        personalEmiLabel = new Label("₹0.00");
        personalEmiLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 15px;");

        personalFoirLabel = new Label("0.00% of Monthly Salary (Safe)");
        personalFoirLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34d399; -fx-font-size: 14px;");

        grid.add(new Label("Desired Loan Principal (₹):"), 0, 0);
        grid.add(personalLoanAmountField, 1, 0);

        grid.add(new Label("Annual Interest Rate (%):"), 0, 1);
        grid.add(personalRateField, 1, 1);

        grid.add(new Label("Tenure (Years):"), 0, 2);
        grid.add(personalTenureField, 1, 2);

        grid.add(new Label("Monthly EMI:"), 0, 3);
        grid.add(personalEmiLabel, 1, 3);

        grid.add(new Label("Salary Debt Burden (FOIR):"), 0, 4);
        grid.add(personalFoirLabel, 1, 4);

        card.getChildren().addAll(title, sub, grid);
        container.getChildren().add(card);
    }

    private void calculatePersonalLoan() {
        try {
            BigDecimal principal = new BigDecimal(personalLoanAmountField.getText().trim());
            BigDecimal rate = new BigDecimal(personalRateField.getText().trim());
            int years = Integer.parseInt(personalTenureField.getText().trim());

            BigDecimal emi = HomeLoanCalculator.calculateEMI(principal, rate, years);
            personalEmiLabel.setText(UIContext.formatMoney(emi) + " / month");

            // Evaluate FOIR against trader profile salary
            FinancialProfile profile = context.getAdvisorService().getProfile(context.getTrader().getUserId());
            BigDecimal income = profile.getMonthlyIncome();
            if (income.signum() > 0) {
                BigDecimal foir = emi.multiply(new BigDecimal("100")).divide(income, 2, RoundingMode.HALF_UP);
                boolean safe = foir.compareTo(new BigDecimal("40.00")) <= 0;
                personalFoirLabel.setText(foir + "% of Monthly Salary (" + (safe ? "Safe & Manageable" : "High Debt Risk!") + ")");
                personalFoirLabel.setStyle(safe ? "-fx-font-weight: bold; -fx-text-fill: #34d399; -fx-font-size: 14px;"
                        : "-fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-font-size: 14px;");
            }
        } catch (Exception ex) {
            // Keep default
        }
    }

    private VBox createLoanStatCard(String title, Label valLabel, String sub) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #0b1120; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");

        valLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label s = new Label(sub);
        s.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");

        box.getChildren().addAll(t, valLabel, s);
        return box;
    }
}
