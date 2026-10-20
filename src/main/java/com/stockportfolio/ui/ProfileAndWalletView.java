package com.stockportfolio.ui;

import com.stockportfolio.model.FinancialProfile;
import com.stockportfolio.model.RiskPreference;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

/**
 * Profile & Wallet Section: Combines user profile management, personal cashflow configuration,
 * wallet balance monitoring, fast preset money deposits, and deposit audit history.
 */
public class ProfileAndWalletView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    // Profile details
    private Label traderNameLabel;
    private Label traderIdLabel;
    private Label traderEmailLabel;
    private Label traderRiskLabel;
    private Label salaryLabel;
    private Label expensesLabel;
    private Label debtLabel;

    // Wallet metrics
    private Label cashBalanceLabel;
    private Label holdingsValueLabel;
    private Label totalAccountValueLabel;
    private TextField customDepositField;

    // Deposit History
    private TableView<Transaction> depositHistoryTable = new TableView<>();

    public ProfileAndWalletView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(26));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildProfileAndFinancialHealthSection();
        buildWalletAndDepositSection();
        buildDepositHistorySection();

        setContent(container);
        refreshProfileAndWallet();
        context.addDataChangeListener(this::refreshProfileAndWallet);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("User Profile & Wallet Management");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Manage personal investor credentials, cashflow configurations, wallet liquidity balances, and capital deposits.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button editProfileBtn = new Button("⚙ Edit Financial Parameters");
        editProfileBtn.getStyleClass().addAll("btn", "btn-secondary");
        editProfileBtn.setOnAction(e -> showEditProfileDialog());

        HBox header = new HBox(titleBox, editProfileBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(header);
    }

    private void buildProfileAndFinancialHealthSection() {
        HBox row = new HBox(20);

        // Card 1: User Profile Details
        VBox profileCard = new VBox(12);
        profileCard.getStyleClass().add("content-card");
        profileCard.setPadding(new Insets(20));
        HBox.setHgrow(profileCard, Priority.ALWAYS);

        Label pTitle = new Label("Trader Profile Information");
        pTitle.getStyleClass().add("section-title");

        traderNameLabel = new Label("Alex Morgan");
        traderNameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");

        traderIdLabel = new Label("Account ID: TRD-001");
        traderIdLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: 600; -fx-font-size: 13px;");

        traderEmailLabel = new Label("Email: alex.morgan@college.edu");
        traderEmailLabel.setStyle("-fx-text-fill: #94a3b8;");

        traderRiskLabel = new Label("Risk Profile: BALANCED");
        traderRiskLabel.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");

        profileCard.getChildren().addAll(pTitle, traderNameLabel, traderIdLabel, traderEmailLabel, traderRiskLabel);

        // Card 2: Financial Health Metrics
        VBox finCard = new VBox(12);
        finCard.getStyleClass().add("content-card");
        finCard.setPadding(new Insets(20));
        HBox.setHgrow(finCard, Priority.ALWAYS);

        Label fTitle = new Label("Personal Financial Metrics");
        fTitle.getStyleClass().add("section-title");

        salaryLabel = new Label("Monthly Net Salary: ₹0.00");
        salaryLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px;");

        expensesLabel = new Label("Monthly Living Expenses: ₹0.00");
        expensesLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px;");

        debtLabel = new Label("Total Outstanding Debt: ₹0.00");
        debtLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px;");

        finCard.getChildren().addAll(fTitle, salaryLabel, expensesLabel, debtLabel);

        row.getChildren().addAll(profileCard, finCard);
        container.getChildren().add(row);
    }

    private void buildWalletAndDepositSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("💳 Wallet & Capital Deposit Terminal");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Monitor available cash reserves and add funds directly to your wallet for deploying into market assets.");
        sub.getStyleClass().add("card-subtitle");

        // Wallet Balance Cards
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        cashBalanceLabel = new Label("₹0.00");
        VBox c1 = createWalletStatCard("Available Cash in Wallet", cashBalanceLabel, "Ready to buy stocks or ETFs");

        holdingsValueLabel = new Label("₹0.00");
        VBox c2 = createWalletStatCard("Invested Portfolio Value", holdingsValueLabel, "Total market value of holdings");

        totalAccountValueLabel = new Label("₹0.00");
        VBox c3 = createWalletStatCard("Total Net Worth", totalAccountValueLabel, "Cash balance + portfolio value");

        grid.add(c1, 0, 0);
        grid.add(c2, 1, 0);
        grid.add(c3, 2, 0);

        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            grid.getColumnConstraints().add(cc);
        }

        // Quick Preset Deposit Buttons
        Label presetTitle = new Label("Quick Add Funds (1-Click Presets):");
        presetTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        HBox presetBox = new HBox(12);
        presetBox.setAlignment(Pos.CENTER_LEFT);

        Button p10k = new Button("+ ₹10,000");
        p10k.getStyleClass().addAll("btn-sm", "btn-secondary");
        p10k.setOnAction(e -> handleDeposit(new BigDecimal("10000.00")));

        Button p25k = new Button("+ ₹25,000");
        p25k.getStyleClass().addAll("btn-sm", "btn-secondary");
        p25k.setOnAction(e -> handleDeposit(new BigDecimal("25000.00")));

        Button p50k = new Button("+ ₹50,000");
        p50k.getStyleClass().addAll("btn-sm", "btn-secondary");
        p50k.setOnAction(e -> handleDeposit(new BigDecimal("50000.00")));

        Button p100k = new Button("+ ₹1,00,000");
        p100k.getStyleClass().addAll("btn-sm", "btn-primary");
        p100k.setOnAction(e -> handleDeposit(new BigDecimal("100000.00")));

        presetBox.getChildren().addAll(p10k, p25k, p50k, p100k);

        // Custom Deposit Form
        HBox customBox = new HBox(14);
        customBox.setAlignment(Pos.CENTER_LEFT);

        customDepositField = new TextField();
        customDepositField.setPromptText("Enter custom deposit amount (₹)");
        customDepositField.setPrefWidth(220);

        Button depositBtn = new Button("💳 Deposit to Wallet");
        depositBtn.getStyleClass().addAll("btn", "btn-primary");
        depositBtn.setOnAction(e -> {
            try {
                BigDecimal amt = new BigDecimal(customDepositField.getText().trim());
                handleDeposit(amt);
                customDepositField.clear();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Please enter a valid numeric deposit amount.");
                a.showAndWait();
            }
        });

        customBox.getChildren().addAll(new Label("Custom Amount:"), customDepositField, depositBtn);

        card.getChildren().addAll(title, sub, grid, presetTitle, presetBox, customBox);
        container.getChildren().add(card);
    }

    private void handleDeposit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Deposit amount must be positive.");
            a.showAndWait();
            return;
        }

        context.getTradingService().depositFunds(amount);
        context.notifyDataChanged();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Deposit Successful");
        alert.setHeaderText("Funds Deposited to Wallet");
        alert.setContentText(UIContext.formatMoney(amount) + " successfully credited to your wallet balance.");
        alert.showAndWait();
    }

    private void buildDepositHistorySection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("📜 Deposit Audit History");
        title.getStyleClass().add("section-title");

        depositHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        depositHistoryTable.setPrefHeight(200);

        TableColumn<Transaction, String> idCol = new TableColumn<>("Transaction ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionId()));
        idCol.setPrefWidth(140);

        TableColumn<Transaction, String> amtCol = new TableColumn<>("Deposited Amount");
        amtCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getNetAmount())));
        amtCol.setPrefWidth(160);

        TableColumn<Transaction, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatDateTime(d.getValue().getTimestamp())));
        timeCol.setPrefWidth(180);

        TableColumn<Transaction, String> notesCol = new TableColumn<>("Description / Notes");
        notesCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType() + " - " + d.getValue().getStatus()));
        notesCol.setPrefWidth(220);

        depositHistoryTable.getColumns().addAll(idCol, amtCol, timeCol, notesCol);
        card.getChildren().addAll(title, depositHistoryTable);
        container.getChildren().add(card);
    }

    private void refreshProfileAndWallet() {
        Trader trader = context.getTrader();
        FinancialProfile profile = context.getAdvisorService().getProfile(trader.getUserId());

        traderNameLabel.setText(trader.getName());
        traderIdLabel.setText("Account ID: " + trader.getUserId());
        traderEmailLabel.setText("Email: " + trader.getEmail());
        traderRiskLabel.setText("Risk Profile: " + trader.getRiskPreference().name());

        salaryLabel.setText("Monthly Net Salary: " + UIContext.formatMoney(profile.getMonthlyIncome()));
        expensesLabel.setText("Monthly Living Expenses: " + UIContext.formatMoney(profile.getMonthlyExpenses()));
        debtLabel.setText("Total Outstanding Debt: " + UIContext.formatMoney(profile.getExistingDebt()));

        cashBalanceLabel.setText(UIContext.formatMoney(trader.getCashBalance()));
        holdingsValueLabel.setText(UIContext.formatMoney(context.getPortfolioService().getPortfolioSummary(trader).holdingsValue()));
        totalAccountValueLabel.setText(UIContext.formatMoney(trader.getTotalAccountValue()));

        // Filter transactions for deposits only
        List<Transaction> deposits = context.getTradingService().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .toList();
        depositHistoryTable.setItems(FXCollections.observableArrayList(deposits));
    }

    private void showEditProfileDialog() {
        Trader trader = context.getTrader();
        FinancialProfile profile = context.getAdvisorService().getProfile(trader.getUserId());

        Dialog<FinancialProfile> dialog = new Dialog<>();
        dialog.setTitle("Edit Financial Profile Parameters");
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
                    refreshProfileAndWallet();
                    context.notifyDataChanged();
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Invalid numbers entered: " + ex.getMessage());
                    a.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private VBox createWalletStatCard(String title, Label valLabel, String sub) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #0b1120; -fx-padding: 14px 18px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");

        valLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label s = new Label(sub);
        s.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        box.getChildren().addAll(t, valLabel, s);
        return box;
    }
}
