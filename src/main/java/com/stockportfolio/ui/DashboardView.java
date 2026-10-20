package com.stockportfolio.ui;

import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.OrderStatus;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unified Fintech Dashboard: provides complete bird's-eye portfolio metrics,
 * full Investment Holdings breakdown, real-time Order Book, asset allocation pie chart,
 * and recent transaction activity without any 'virtual' terminology.
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

    // Core Tables inside Dashboard (as requested in user specs)
    private TableView<Position> holdingsTable = new TableView<>();
    private TableView<Order> orderBookTable = new TableView<>();
    private PieChart allocationChart;
    private TableView<Transaction> recentTransactionsTable;

    public DashboardView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        contentBox.setPadding(new Insets(28));
        contentBox.getStyleClass().add("view-container");

        buildHeader();
        buildMetricCards();
        buildHoldingsSection();
        buildOrderBookSection();
        buildChartsAndActivitySection();

        setContent(contentBox);
        refreshData();

        // Subscribe to real-time background market ticker and state mutations
        context.addDataChangeListener(this::refreshData);
    }

    private void buildHeader() {
        Trader trader = context.getTrader();
        VBox titleBox = new VBox(4);
        Label title = new Label("Investment & Trading Dashboard");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Welcome back, " + trader.getName() + " • Account: " + trader.getUserId()
                + " • Risk Preference: " + trader.getRiskPreference());
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox topBar = new HBox(titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button depositBtn = new Button("💳 Deposit Funds");
        depositBtn.getStyleClass().addAll("btn", "btn-secondary");
        depositBtn.setOnAction(e -> context.navigateTo("PROFILE"));

        Button exploreBtn = new Button("🔍 Explore Markets");
        exploreBtn.getStyleClass().addAll("btn", "btn-primary");
        exploreBtn.setOnAction(e -> context.navigateTo("EXPLORE"));

        HBox actionBox = new HBox(12, depositBtn, exploreBtn);
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
        VBox cashCard = createCard("Available Cash Balance", cashLabel, "Liquidity ready for investments", "card-accent-blue");

        holdingsLabel = new Label("₹0.00");
        VBox holdingsCard = createCard("Invested Portfolio Value", holdingsLabel, "Current market value of assets", "card-accent-purple");

        accountValueLabel = new Label("₹0.00");
        VBox accountCard = createCard("Total Account Value", accountValueLabel, "Cash balance + portfolio value", "card-accent-teal");

        unrealizedPlLabel = new Label("₹0.00");
        VBox unrealizedCard = createCard("Unrealized Profit / Loss", unrealizedPlLabel, "Net gain/loss on holdings", "card-accent-gold");

        realizedPlLabel = new Label("₹0.00");
        VBox realizedCard = createCard("Realized Net P/L", realizedPlLabel, "Profit/loss from closed positions", "card-accent-green");

        positionsCountLabel = new Label("0");
        VBox positionsCard = createCard("Active Holdings", positionsCountLabel, "Distinct assets held", "card-accent-cyan");

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

    /**
     * Holdings Section: shows exact investments across each stock, ETF, and bond.
     */
    private void buildHoldingsSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Investment Holdings");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Individual asset allocation, acquisition cost basis, current market value, and unrealized returns.");
        sub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button tradeMoreBtn = new Button("⚡ Buy More Shares");
        tradeMoreBtn.getStyleClass().addAll("btn-sm", "btn-primary");
        tradeMoreBtn.setOnAction(e -> context.navigateTo("EXPLORE"));

        head.getChildren().addAll(titleBox, tradeMoreBtn);

        holdingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        holdingsTable.setPrefHeight(230);

        TableColumn<Position, String> symbolCol = new TableColumn<>("Asset / Symbol");
        symbolCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAsset().getSymbol() + " (" + data.getValue().getAsset().getName() + ")"));
        symbolCol.setPrefWidth(200);

        TableColumn<Position, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAsset().getCategory().name()));
        catCol.setPrefWidth(90);

        TableColumn<Position, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        qtyCol.setPrefWidth(80);

        TableColumn<Position, String> avgPriceCol = new TableColumn<>("Avg Buy Price");
        avgPriceCol.setCellValueFactory(data -> new SimpleStringProperty(UIContext.formatMoney(data.getValue().getAverageCost())));
        avgPriceCol.setPrefWidth(110);

        TableColumn<Position, String> curPriceCol = new TableColumn<>("Current Price");
        curPriceCol.setCellValueFactory(data -> new SimpleStringProperty(UIContext.formatMoney(data.getValue().getAsset().getCurrentPrice())));
        curPriceCol.setPrefWidth(110);

        TableColumn<Position, String> totalCostCol = new TableColumn<>("Invested Amount");
        totalCostCol.setCellValueFactory(data -> new SimpleStringProperty(UIContext.formatMoney(data.getValue().getTotalCostBasis())));
        totalCostCol.setPrefWidth(120);

        TableColumn<Position, String> marketValCol = new TableColumn<>("Current Value");
        marketValCol.setCellValueFactory(data -> new SimpleStringProperty(UIContext.formatMoney(data.getValue().getMarketValue())));
        marketValCol.setPrefWidth(120);

        TableColumn<Position, String> plCol = new TableColumn<>("Unrealized P/L");
        plCol.setCellValueFactory(data -> {
            BigDecimal pl = data.getValue().getUnrealizedProfitLoss();
            BigDecimal pct = data.getValue().getReturnPercent();
            String prefix = pl.signum() >= 0 ? "+" : "";
            return new SimpleStringProperty(prefix + UIContext.formatMoney(pl) + " (" + prefix + pct + "%)");
        });
        plCol.setPrefWidth(160);
        plCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("+")) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    } else if (item.contains("-")) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #94a3b8;");
                    }
                }
            }
        });

        TableColumn<Position, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(100);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button tradeBtn = new Button("Trade");
            {
                tradeBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
                tradeBtn.setOnAction(e -> {
                    Position p = getTableView().getItems().get(getIndex());
                    context.navigateTo("EXPLORE", p.getAsset());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : tradeBtn);
            }
        });

        holdingsTable.getColumns().addAll(symbolCol, catCol, qtyCol, avgPriceCol, curPriceCol, totalCostCol, marketValCol, plCol, actionCol);
        card.getChildren().addAll(head, holdingsTable);
        contentBox.getChildren().add(card);
    }

    /**
     * Order Book Section: tracks pending limit orders and executed market trades directly in the dashboard.
     */
    private void buildOrderBookSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Order Book & Execution Log");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Audit active and historical market and limit orders with atomic execution controls.");
        sub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button recheckBtn = new Button("🔄 Re-check Pending Orders");
        recheckBtn.getStyleClass().addAll("btn-sm", "btn-outline");
        recheckBtn.setOnAction(e -> {
            List<Order> exec = context.getTradingService().processPendingOrders();
            context.notifyDataChanged();
            if (!exec.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, exec.size() + " limit order(s) satisfied price triggers and executed.");
                a.showAndWait();
            }
        });

        head.getChildren().addAll(titleBox, recheckBtn);

        orderBookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderBookTable.setPrefHeight(210);

        TableColumn<Order, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderId()));
        idCol.setPrefWidth(100);

        TableColumn<Order, String> symCol = new TableColumn<>("Asset");
        symCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAsset().getSymbol()));
        symCol.setPrefWidth(90);

        TableColumn<Order, String> sideCol = new TableColumn<>("Side");
        sideCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSide().name()));
        sideCol.setPrefWidth(70);

        TableColumn<Order, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderType().name()));
        typeCol.setPrefWidth(80);

        TableColumn<Order, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        qtyCol.setPrefWidth(60);

        TableColumn<Order, String> priceCol = new TableColumn<>("Trigger Price");
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getLimitPrice() != null ? UIContext.formatMoney(data.getValue().getLimitPrice()) : "Market Price"));
        priceCol.setPrefWidth(110);

        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        statusCol.setPrefWidth(90);

        TableColumn<Order, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(UIContext.formatDateTime(data.getValue().getSubmittedAt())));
        timeCol.setPrefWidth(140);

        TableColumn<Order, Void> cancelCol = new TableColumn<>("Cancel");
        cancelCol.setPrefWidth(80);
        cancelCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("✕");
            {
                cancelBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                cancelBtn.setOnAction(e -> {
                    Order o = getTableView().getItems().get(getIndex());
                    if (o.getStatus() == OrderStatus.PENDING) {
                        try {
                            context.getTradingService().cancelOrder(o.getOrderId());
                            context.notifyDataChanged();
                        } catch (TradingException ex) {
                            Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                            a.showAndWait();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Order o = getTableView().getItems().get(getIndex());
                    if (o.getStatus() == OrderStatus.PENDING) {
                        setGraphic(cancelBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        orderBookTable.getColumns().addAll(idCol, symCol, sideCol, typeCol, qtyCol, priceCol, statusCol, timeCol, cancelCol);
        card.getChildren().addAll(head, orderBookTable);
        contentBox.getChildren().add(card);
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
        allocationChart.setPrefHeight(240);
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
        recentTransactionsTable.setPrefHeight(240);
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

        // Populate Holdings Table
        List<Position> posList = trader.getPortfolio().getPositions();
        holdingsTable.setItems(FXCollections.observableArrayList(posList));

        // Populate Order Book Table
        List<Order> orders = context.getTradingService().getOrders();
        orderBookTable.setItems(FXCollections.observableArrayList(orders));

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
            chartData.add(new PieChart.Data("100% Cash Reserve", 100));
        }
        allocationChart.setData(chartData);

        // Update Recent Transactions
        List<Transaction> txs = context.getTradingService().getTransactions();
        int limit = Math.min(6, txs.size());
        recentTransactionsTable.setItems(FXCollections.observableArrayList(txs.subList(0, limit)));
    }
}
