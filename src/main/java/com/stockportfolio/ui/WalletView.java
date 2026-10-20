package com.stockportfolio.ui;

import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
 * Virtual Wallet & Trading Account Management view.
 * Handles virtual cash deposits, tracks liquidity reserves, and audits deposit records.
 */
public class WalletView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    private Label cashBalanceLabel;
    private Label totalAccountValueLabel;
    private Label traderNameLabel;
    private Label traderIdLabel;
    private TextField depositAmountField;

    private TableView<Transaction> depositTable = new TableView<>();

    public WalletView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildAccountCards();
        buildDepositForm();
        buildDepositHistory();

        setContent(container);
        refreshWallet();
        context.addDataChangeListener(this::refreshWallet);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Virtual Wallet & Account Funds");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Manage simulated liquidity reserves. Safe simulation only — no real financial accounts or cards.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    private void buildAccountCards() {
        HBox row = new HBox(20);

        // Trader Profile Card
        VBox profileCard = new VBox(10);
        profileCard.getStyleClass().add("content-card");
        profileCard.setPadding(new Insets(20));
        HBox.setHgrow(profileCard, Priority.ALWAYS);

        Label pTitle = new Label("Trader Profile Information");
        pTitle.getStyleClass().add("section-title");

        traderNameLabel = new Label("Alex Morgan");
        traderNameLabel.getStyleClass().add("view-title");

        traderIdLabel = new Label("ID: TRD-001");
        traderIdLabel.getStyleClass().add("text-muted");

        Label emailLabel = new Label("Email: " + context.getTrader().getEmail());
        emailLabel.getStyleClass().add("text-muted");

        profileCard.getChildren().addAll(pTitle, traderNameLabel, traderIdLabel, emailLabel);

        // Cash Balance Card
        VBox cashCard = new VBox(10);
        cashCard.getStyleClass().addAll("metric-card", "card-accent-blue");
        cashCard.setPadding(new Insets(20));
        HBox.setHgrow(cashCard, Priority.ALWAYS);

        Label cTitle = new Label("Available Virtual Cash");
        cTitle.getStyleClass().add("card-title");

        cashBalanceLabel = new Label("₹0.00");
        cashBalanceLabel.getStyleClass().add("card-value");

        Label cSub = new Label("Unallocated virtual funds available to execute buy orders.");
        cSub.getStyleClass().add("card-subtitle");

        cashCard.getChildren().addAll(cTitle, cashBalanceLabel, cSub);

        // Total Account Card
        VBox totalCard = new VBox(10);
        totalCard.getStyleClass().addAll("metric-card", "card-accent-teal");
        totalCard.setPadding(new Insets(20));
        HBox.setHgrow(totalCard, Priority.ALWAYS);

        Label totTitle = new Label("Total Account Valuation");
        totTitle.getStyleClass().add("card-title");

        totalAccountValueLabel = new Label("₹0.00");
        totalAccountValueLabel.getStyleClass().add("card-value");

        Label totSub = new Label("Combined value of virtual cash plus open market holdings.");
        totSub.getStyleClass().add("card-subtitle");

        totalCard.getChildren().addAll(totTitle, totalAccountValueLabel, totSub);

        row.getChildren().addAll(profileCard, cashCard, totalCard);
        container.getChildren().add(row);
    }

    private void buildDepositForm() {
        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("content-card");
        formCard.setPadding(new Insets(22));

        Label fTitle = new Label("Add Virtual Simulation Funds");
        fTitle.getStyleClass().add("section-title");

        Label fDesc = new Label("Specify the simulated amount you want to deposit into your virtual trading wallet:");
        fDesc.getStyleClass().add("text-muted");

        HBox inputRow = new HBox(12);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        depositAmountField = new TextField("50000.00");
        depositAmountField.setPromptText("Enter deposit amount...");
        depositAmountField.getStyleClass().add("text-input");
        depositAmountField.setPrefWidth(260);

        Button depositBtn = new Button("➕ Deposit Virtual Money");
        depositBtn.getStyleClass().addAll("btn", "btn-primary");
        depositBtn.setOnAction(e -> handleDeposit());

        inputRow.getChildren().addAll(depositAmountField, depositBtn);

        // Preset quick deposit buttons
        HBox presetBox = new HBox(10);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        Label quickLbl = new Label("Quick Presets:");
        quickLbl.getStyleClass().add("form-label");

        Button p1 = new Button("+ ₹10,000");
        Button p2 = new Button("+ ₹50,000");
        Button p3 = new Button("+ ₹1,00,000");
        Button p4 = new Button("+ ₹5,00,000");

        for (Button b : List.of(p1, p2, p3, p4)) {
            b.getStyleClass().addAll("btn-sm", "btn-outline");
        }

        p1.setOnAction(e -> depositAmountField.setText("10000.00"));
        p2.setOnAction(e -> depositAmountField.setText("50000.00"));
        p3.setOnAction(e -> depositAmountField.setText("100000.00"));
        p4.setOnAction(e -> depositAmountField.setText("500000.00"));

        presetBox.getChildren().addAll(quickLbl, p1, p2, p3, p4);

        HBox warningBox = new HBox(10);
        warningBox.getStyleClass().add("info-banner");
        warningBox.setPadding(new Insets(10));
        Label warnLabel = new Label("🔒 Security Safeguard: This application operates entirely on virtual test currency. No bank logins, UPI, credit/debit card numbers, or live broker credentials are ever solicited or accepted.");
        warnLabel.setWrapText(true);
        warnLabel.getStyleClass().add("text-muted");
        warningBox.getChildren().add(warnLabel);

        formCard.getChildren().addAll(fTitle, fDesc, inputRow, presetBox, warningBox);
        container.getChildren().add(formCard);
    }

    private void buildDepositHistory() {
        VBox card = new VBox(12);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(18));

        Label title = new Label("Deposit Audit History");
        title.getStyleClass().add("section-title");

        depositTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        depositTable.setPrefHeight(220);

        TableColumn<Transaction, String> idCol = new TableColumn<>("Transaction ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionId()));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Timestamp");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatDateTime(d.getValue().getTimestamp())));

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Deposit Amount");
        amountCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getNetAmount())));

        TableColumn<Transaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        depositTable.getColumns().addAll(idCol, dateCol, typeCol, amountCol, statusCol);
        card.getChildren().addAll(title, depositTable);
        container.getChildren().add(card);
    }

    private void handleDeposit() {
        String text = depositAmountField.getText().trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
            if (amount.signum() <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Please enter a valid positive deposit amount.");
            return;
        }

        try {
            Transaction tx = context.getTradingService().deposit(amount);
            context.notifyDataChanged();
            showAlert(Alert.AlertType.INFORMATION, "Virtual Deposit Confirmed",
                    "Successfully credited " + UIContext.formatMoney(tx.getNetAmount()) + " to virtual wallet.\n"
                            + "Audit Transaction ID: " + tx.getTransactionId() + "\n"
                            + "Updated Virtual Cash: " + UIContext.formatMoney(context.getTrader().getCashBalance()));
            depositAmountField.setText("50000.00");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Deposit Error", e.getMessage());
        }
    }

    private void refreshWallet() {
        Trader trader = context.getTrader();
        traderNameLabel.setText(trader.getName());
        traderIdLabel.setText("Account ID: " + trader.getUserId() + " • Role: " + trader.getRole());
        cashBalanceLabel.setText(UIContext.formatMoney(trader.getCashBalance()));
        totalAccountValueLabel.setText(UIContext.formatMoney(trader.getTotalAccountValue()));

        List<Transaction> deposits = context.getTradingService().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .toList();
        depositTable.setItems(FXCollections.observableArrayList(deposits));
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
