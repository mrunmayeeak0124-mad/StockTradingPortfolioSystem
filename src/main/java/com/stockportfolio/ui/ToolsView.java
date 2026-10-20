package com.stockportfolio.ui;

import com.stockportfolio.model.ConversionResult;
import com.stockportfolio.model.ExchangeRate;
import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.GoalCategory;
import com.stockportfolio.model.GoalStatus;
import com.stockportfolio.model.Trader;
import com.stockportfolio.service.CsvExportService;
import com.stockportfolio.service.CurrencyService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Tools Section: provides financial calculation and modeling tools including
 * SIP Calculator, Lumpsum Calculator, Foreign Currency Exchange Converter,
 * Data Export Engine, and Financial Milestone Tracker.
 */
public class ToolsView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    // SIP Calculator widgets
    private TextField sipAmountField;
    private TextField sipRateField;
    private TextField sipYearsField;
    private Label sipInvestedLabel;
    private Label sipReturnsLabel;
    private Label sipTotalLabel;

    // Lumpsum Calculator widgets
    private TextField lumpAmountField;
    private TextField lumpRateField;
    private TextField lumpYearsField;
    private Label lumpInvestedLabel;
    private Label lumpReturnsLabel;
    private Label lumpTotalLabel;

    // Currency Converter widgets
    private TextField convAmountField;
    private ComboBox<String> fromCurrencyCombo;
    private ComboBox<String> toCurrencyCombo;
    private Label convResultLabel;
    private Label convRateInfoLabel;

    // Goals preview
    private VBox goalsListBox = new VBox(10);

    public ToolsView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(26));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildSipCalculatorSection();
        buildLumpsumCalculatorSection();
        buildCurrencyExchangeSection();
        buildGoalsSection();
        buildDataExportSection();

        setContent(container);
        calculateSip();
        calculateLumpsum();
        refreshGoals();
        context.addDataChangeListener(this::refreshGoals);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Financial Engineering & Calculators (Tools)");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Institutional wealth-building utilities: Systematic Investment Plan (SIP) forecasting, compounding calculators, real-time FX currency converter, and data export.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    /**
     * SIP Calculator: P * ((1 + i)^n - 1) / i * (1 + i)
     */
    private void buildSipCalculatorSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("📈 Systematic Investment Plan (SIP) Calculator");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Simulate monthly recurring investments and visualize the exponential power of compounding interest.");
        sub.getStyleClass().add("card-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        sipAmountField = new TextField("5000");
        sipAmountField.textProperty().addListener((o, ov, nv) -> calculateSip());

        sipRateField = new TextField("12.0");
        sipRateField.textProperty().addListener((o, ov, nv) -> calculateSip());

        sipYearsField = new TextField("10");
        sipYearsField.textProperty().addListener((o, ov, nv) -> calculateSip());

        grid.add(new Label("Monthly Investment (₹):"), 0, 0);
        grid.add(sipAmountField, 1, 0);

        grid.add(new Label("Expected Annual Return (%):"), 0, 1);
        grid.add(sipRateField, 1, 1);

        grid.add(new Label("Investment Tenure (Years):"), 0, 2);
        grid.add(sipYearsField, 1, 2);

        // Result Cards
        GridPane resGrid = new GridPane();
        resGrid.setHgap(14);
        resGrid.setVgap(12);

        sipInvestedLabel = new Label("₹0.00");
        VBox r1 = createToolMetricBox("Total Invested Amount", sipInvestedLabel, "Actual principal contributed");

        sipReturnsLabel = new Label("₹0.00");
        VBox r2 = createToolMetricBox("Estimated Returns / Wealth Gain", sipReturnsLabel, "Compounded earnings");

        sipTotalLabel = new Label("₹0.00");
        VBox r3 = createToolMetricBox("Total Maturity Value", sipTotalLabel, "Future wealth created");

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

    private void calculateSip() {
        try {
            double p = Double.parseDouble(sipAmountField.getText().trim());
            double annualRate = Double.parseDouble(sipRateField.getText().trim());
            double years = Double.parseDouble(sipYearsField.getText().trim());

            if (p <= 0 || annualRate <= 0 || years <= 0) return;

            double i = (annualRate / 100.0) / 12.0;
            double n = years * 12.0;

            double fv = p * ((Math.pow(1.0 + i, n) - 1.0) / i) * (1.0 + i);
            double invested = p * n;
            double returns = fv - invested;

            sipInvestedLabel.setText(UIContext.formatMoney(BigDecimal.valueOf(invested).setScale(2, RoundingMode.HALF_UP)));
            sipReturnsLabel.setText(UIContext.formatMoney(BigDecimal.valueOf(returns).setScale(2, RoundingMode.HALF_UP)));
            sipTotalLabel.setText(UIContext.formatMoney(BigDecimal.valueOf(fv).setScale(2, RoundingMode.HALF_UP)));
        } catch (Exception ex) {
            // Keep default
        }
    }

    /**
     * Lumpsum / Compounding Investment Calculator: FV = P * (1 + r)^n
     */
    private void buildLumpsumCalculatorSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("💰 Lumpsum / Compounding Investment Calculator");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Calculate the growth of a single one-time deposit compounded annually over long-term market cycles.");
        sub.getStyleClass().add("card-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        lumpAmountField = new TextField("100000");
        lumpAmountField.textProperty().addListener((o, ov, nv) -> calculateLumpsum());

        lumpRateField = new TextField("14.0");
        lumpRateField.textProperty().addListener((o, ov, nv) -> calculateLumpsum());

        lumpYearsField = new TextField("15");
        lumpYearsField.textProperty().addListener((o, ov, nv) -> calculateLumpsum());

        grid.add(new Label("One-Time Investment (₹):"), 0, 0);
        grid.add(lumpAmountField, 1, 0);

        grid.add(new Label("Expected CAGR (%):"), 0, 1);
        grid.add(lumpRateField, 1, 1);

        grid.add(new Label("Horizon (Years):"), 0, 2);
        grid.add(lumpYearsField, 1, 2);

        // Result Cards
        GridPane resGrid = new GridPane();
        resGrid.setHgap(14);
        resGrid.setVgap(12);

        lumpInvestedLabel = new Label("₹0.00");
        VBox r1 = createToolMetricBox("Principal Invested", lumpInvestedLabel, "Initial capital committed");

        lumpReturnsLabel = new Label("₹0.00");
        VBox r2 = createToolMetricBox("Wealth Gain", lumpReturnsLabel, "Total profit generated");

        lumpTotalLabel = new Label("₹0.00");
        VBox r3 = createToolMetricBox("Estimated Maturity Value", lumpTotalLabel, "Total portfolio worth");

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

    private void calculateLumpsum() {
        try {
            double p = Double.parseDouble(lumpAmountField.getText().trim());
            double r = Double.parseDouble(lumpRateField.getText().trim()) / 100.0;
            double n = Double.parseDouble(lumpYearsField.getText().trim());

            if (p <= 0 || r <= 0 || n <= 0) return;

            double fv = p * Math.pow(1.0 + r, n);
            double gain = fv - p;

            lumpInvestedLabel.setText(UIContext.formatMoney(BigDecimal.valueOf(p).setScale(2, RoundingMode.HALF_UP)));
            lumpReturnsLabel.setText(UIContext.formatMoney(BigDecimal.valueOf(gain).setScale(2, RoundingMode.HALF_UP)));
            lumpTotalLabel.setText(UIContext.formatMoney(BigDecimal.valueOf(fv).setScale(2, RoundingMode.HALF_UP)));
        } catch (Exception ex) {
            // Keep default
        }
    }

    /**
     * Currency Exchange & FX Converter (Point 10)
     */
    private void buildCurrencyExchangeSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("💱 Foreign Exchange (FX) & Currency Converter");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Instantly convert monetary amounts between global currencies at live institutional reference rates.");
        sub.getStyleClass().add("card-subtitle");

        HBox convRow = new HBox(16);
        convRow.setAlignment(Pos.CENTER_LEFT);

        convAmountField = new TextField("1000");
        convAmountField.setPrefWidth(140);
        convAmountField.textProperty().addListener((o, ov, nv) -> handleCurrencyConversion());

        fromCurrencyCombo = new ComboBox<>();
        fromCurrencyCombo.getItems().addAll("USD", "INR", "EUR", "GBP", "JPY");
        fromCurrencyCombo.setValue("USD");
        fromCurrencyCombo.setOnAction(e -> handleCurrencyConversion());

        toCurrencyCombo = new ComboBox<>();
        toCurrencyCombo.getItems().addAll("INR", "USD", "EUR", "GBP", "JPY");
        toCurrencyCombo.setValue("INR");
        toCurrencyCombo.setOnAction(e -> handleCurrencyConversion());

        Button swapBtn = new Button("⇄");
        swapBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
        swapBtn.setOnAction(e -> {
            String from = fromCurrencyCombo.getValue();
            String to = toCurrencyCombo.getValue();
            fromCurrencyCombo.setValue(to);
            toCurrencyCombo.setValue(from);
            handleCurrencyConversion();
        });

        convRow.getChildren().addAll(new Label("Amount:"), convAmountField, fromCurrencyCombo, swapBtn, toCurrencyCombo);

        VBox resBox = new VBox(6);
        resBox.setStyle("-fx-background-color: #0b1120; -fx-padding: 14px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

        convResultLabel = new Label("₹83,250.00 INR");
        convResultLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #34d399;");

        convRateInfoLabel = new Label("Exchange Rate: 1 USD = 83.2500 INR");
        convRateInfoLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        resBox.getChildren().addAll(convResultLabel, convRateInfoLabel);

        card.getChildren().addAll(title, sub, convRow, resBox);
        container.getChildren().add(card);

        handleCurrencyConversion();
    }

    private void handleCurrencyConversion() {
        try {
            BigDecimal amt = new BigDecimal(convAmountField.getText().trim());
            String from = fromCurrencyCombo.getValue();
            String to = toCurrencyCombo.getValue();

            if (from.equalsIgnoreCase(to)) {
                convResultLabel.setText(amt.toPlainString() + " " + to);
                convRateInfoLabel.setText("Exchange Rate: 1.0000");
                return;
            }

            ConversionResult res = context.getCurrencyService().convert(amt, from, to);
            convResultLabel.setText(res.convertedAmount().toPlainString() + " " + to);
            convRateInfoLabel.setText("Exchange Rate: 1 " + from + " = " + res.rate().rate().toPlainString() + " " + to);
        } catch (Exception ex) {
            convResultLabel.setText("---");
            convRateInfoLabel.setText("Please enter a valid amount.");
        }
    }

    /**
     * Financial Goals Tracker Section
     */
    private void buildGoalsSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("🎯 Financial Milestones & Goal Tracker");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Track capital targets for home down payments, retirement, and emergency funds.");
        sub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button addGoalBtn = new Button("+ Add New Goal");
        addGoalBtn.getStyleClass().addAll("btn-sm", "btn-primary");
        addGoalBtn.setOnAction(e -> showAddGoalDialog());

        head.getChildren().addAll(titleBox, addGoalBtn);

        card.getChildren().addAll(head, goalsListBox);
        container.getChildren().add(card);
    }

    private void refreshGoals() {
        goalsListBox.getChildren().clear();
        Trader trader = context.getTrader();
        List<FinancialGoal> goals = context.getGoalService().getGoals(trader.getUserId());

        if (goals.isEmpty()) {
            goalsListBox.getChildren().add(new Label("No financial goals created yet."));
            return;
        }

        for (FinancialGoal g : goals) {
            VBox gRow = new VBox(6);
            gRow.setStyle("-fx-background-color: #0b1120; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

            HBox h = new HBox(12);
            h.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(g.getName() + " (" + g.getCategory().name() + ")");
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
            HBox.setHgrow(name, Priority.ALWAYS);

            Label progress = new Label(UIContext.formatMoney(g.getCurrentAmount()) + " / " + UIContext.formatMoney(g.getTargetAmount())
                    + " (" + g.getProgressPercentage() + "%)");
            progress.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

            h.getChildren().addAll(name, progress);

            ProgressBar pb = new ProgressBar(Math.min(1.0, g.getProgressPercentage().doubleValue() / 100.0));
            pb.setMaxWidth(Double.MAX_VALUE);

            HBox bRow = new HBox(12);
            BigDecimal monthlyReq = context.getGoalService().calculateRequiredMonthlySavings(g, BigDecimal.ZERO);
            Label rec = new Label("Required Monthly Contribution: " + UIContext.formatMoney(monthlyReq) + " | Target Date: " + g.getTargetDate());
            rec.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            HBox.setHgrow(rec, Priority.ALWAYS);

            Button contributeBtn = new Button("+ Contribute ₹10,000");
            contributeBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
            contributeBtn.setOnAction(e -> {
                context.getGoalService().contributeToGoal(g.getGoalId(), context.getTrader().getUserId(), new BigDecimal("10000.00"));
                context.notifyDataChanged();
            });

            bRow.getChildren().addAll(rec, contributeBtn);
            gRow.getChildren().addAll(h, pb, bRow);
            goalsListBox.getChildren().add(gRow);
        }
    }

    private void showAddGoalDialog() {
        Dialog<FinancialGoal> dialog = new Dialog<>();
        dialog.setTitle("Create Financial Milestone");
        dialog.setHeaderText("Set a target amount, deadline, and category.");

        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(12);
        gp.setPadding(new Insets(20));

        TextField nameField = new TextField();
        ComboBox<GoalCategory> catCombo = new ComboBox<>();
        catCombo.getItems().addAll(GoalCategory.values());
        catCombo.setValue(GoalCategory.WEALTH_BUILDING);

        TextField targetField = new TextField("500000");
        TextField currentField = new TextField("0");
        DatePicker datePicker = new DatePicker(LocalDate.now().plusYears(3));

        gp.add(new Label("Goal Name:"), 0, 0);
        gp.add(nameField, 1, 0);
        gp.add(new Label("Category:"), 0, 1);
        gp.add(catCombo, 1, 1);
        gp.add(new Label("Target Amount (₹):"), 0, 2);
        gp.add(targetField, 1, 2);
        gp.add(new Label("Current Savings (₹):"), 0, 3);
        gp.add(currentField, 1, 3);
        gp.add(new Label("Target Date:"), 0, 4);
        gp.add(datePicker, 1, 4);

        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    String gId = "GOL-" + System.currentTimeMillis();
                    FinancialGoal g = new FinancialGoal(
                            gId, context.getTrader().getUserId(), nameField.getText().trim(),
                            catCombo.getValue(), new BigDecimal(targetField.getText().trim()),
                            new BigDecimal(currentField.getText().trim()), datePicker.getValue()
                    );
                    context.getGoalService().addGoal(g);
                    context.notifyDataChanged();
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Invalid goal input: " + ex.getMessage());
                    a.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Non-blocking Asynchronous CSV Data Export Engine
     */
    private void buildDataExportSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("📥 Data Export & Report Engine");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Export institutional CSV reports of your portfolio holdings and complete transaction audit ledger.");
        sub.getStyleClass().add("card-subtitle");

        HBox btnBox = new HBox(16);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        Button expHoldingsBtn = new Button("📥 Export Holdings CSV");
        expHoldingsBtn.getStyleClass().addAll("btn", "btn-secondary");
        expHoldingsBtn.setOnAction(e -> exportDataAsynchronously("HOLDINGS"));

        Button expTransBtn = new Button("📥 Export Audit Transactions CSV");
        expTransBtn.getStyleClass().addAll("btn", "btn-secondary");
        expTransBtn.setOnAction(e -> exportDataAsynchronously("TRANSACTIONS"));

        btnBox.getChildren().addAll(expHoldingsBtn, expTransBtn);

        card.getChildren().addAll(title, sub, btnBox);
        container.getChildren().add(card);
    }

    private void exportDataAsynchronously(String type) {
        new Thread(() -> {
            try {
                CsvExportService exporter = new CsvExportService();
                Trader trader = context.getTrader();
                Path path;
                if ("HOLDINGS".equals(type)) {
                    path = exporter.exportHoldings(trader.getPortfolio().getPositions());
                } else {
                    path = exporter.exportTransactions(context.getTradingService().getTransactions());
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("CSV Report Export Succeeded");
                    alert.setHeaderText("Asynchronous Export Complete");
                    alert.setContentText("Report generated successfully at:\n" + path.toAbsolutePath());
                    alert.showAndWait();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private VBox createToolMetricBox(String title, Label valLabel, String sub) {
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
