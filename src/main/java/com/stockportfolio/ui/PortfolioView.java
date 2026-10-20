package com.stockportfolio.ui;

import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.Trader;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Portfolio Management & Performance Analysis View.
 * Tracks all open positions, calculates weighted average cost bases, realized/unrealized P/L,
 * and visualizes asset allocation with concentration diagnostics.
 */
public class PortfolioView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(22);

    private Label costBasisLabel;
    private Label marketValueLabel;
    private Label unrealizedPlLabel;
    private Label realizedPlLabel;
    private Label totalReturnLabel;

    private TableView<Position> holdingsTable = new TableView<>();
    private PieChart allocationPieChart;
    private VBox concentrationAlertBox;

    public PortfolioView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildSummaryCards();
        buildHoldingsTable();
        buildChartsAndDiversification();

        setContent(container);
        refreshPortfolio();
        context.addDataChangeListener(this::refreshPortfolio);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Investment Portfolio & Holdings");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Real-time valuation of open positions, weighted cost basis, and realized profit/loss auditing.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button exportBtn = new Button("📥 Export Holdings CSV");
        exportBtn.getStyleClass().addAll("btn", "btn-secondary");
        exportBtn.setOnAction(e -> handleExportHoldings());

        HBox header = new HBox(titleBox, exportBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(header);
    }

    private void buildSummaryCards() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        costBasisLabel = new Label("₹0.00");
        VBox c1 = createStatCard("Total Cost Basis", costBasisLabel, "Cumulative acquisition expenditure", "card-accent-blue");

        marketValueLabel = new Label("₹0.00");
        VBox c2 = createStatCard("Current Market Value", marketValueLabel, "Sum of all open positions", "card-accent-purple");

        unrealizedPlLabel = new Label("₹0.00");
        VBox c3 = createStatCard("Unrealized Profit / Loss", unrealizedPlLabel, "Market Value − Cost Basis", "card-accent-gold");

        realizedPlLabel = new Label("₹0.00");
        VBox c4 = createStatCard("Realized Profit / Loss", realizedPlLabel, "Net gain from completed sales", "card-accent-green");

        totalReturnLabel = new Label("0.00%");
        VBox c5 = createStatCard("Portfolio Return %", totalReturnLabel, "Unrealized P/L / Cost Basis", "card-accent-teal");

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

        container.getChildren().add(grid);
    }

    private VBox createStatCard(String title, Label valLabel, String sub, String accent) {
        VBox box = new VBox(4);
        box.getStyleClass().addAll("metric-card", accent);
        box.setPadding(new Insets(14, 18, 14, 18));

        Label t = new Label(title);
        t.getStyleClass().add("card-title");

        valLabel.getStyleClass().add("card-value");

        Label s = new Label(sub);
        s.getStyleClass().add("card-subtitle");

        box.getChildren().addAll(t, valLabel, s);
        return box;
    }

    private void buildHoldingsTable() {
        VBox card = new VBox(12);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(18));

        Label t = new Label("Open Investment Holdings");
        t.getStyleClass().add("section-title");

        holdingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        holdingsTable.setPrefHeight(280);

        TableColumn<Position, String> symbolCol = new TableColumn<>("Symbol");
        symbolCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAsset().getSymbol()));
        symbolCol.setPrefWidth(90);

        TableColumn<Position, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAsset().getName()));
        nameCol.setPrefWidth(160);

        TableColumn<Position, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAsset().getCategory().name()));
        catCol.setPrefWidth(80);

        TableColumn<Position, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantity())));
        qtyCol.setPrefWidth(70);

        TableColumn<Position, String> avgCostCol = new TableColumn<>("Avg Cost Basis");
        avgCostCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getAverageCost())));
        avgCostCol.setPrefWidth(110);

        TableColumn<Position, String> curPriceCol = new TableColumn<>("Current Price");
        curPriceCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getCurrentPrice())));
        curPriceCol.setPrefWidth(110);

        TableColumn<Position, String> totalCostCol = new TableColumn<>("Total Cost Basis");
        totalCostCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getTotalCostBasis())));
        totalCostCol.setPrefWidth(120);

        TableColumn<Position, String> mktValCol = new TableColumn<>("Market Value");
        mktValCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getMarketValue())));
        mktValCol.setPrefWidth(120);

        TableColumn<Position, String> unPlCol = new TableColumn<>("Unrealized P/L");
        unPlCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getUnrealizedProfitLoss())));
        unPlCol.setPrefWidth(120);

        TableColumn<Position, String> returnCol = new TableColumn<>("Gain / Loss %");
        returnCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatPercent(d.getValue().getReturnPercent())));
        returnCol.setPrefWidth(100);

        // Quick Sell Action
        TableColumn<Position, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(90);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button sellBtn = new Button("⚡ Sell");

            {
                sellBtn.getStyleClass().addAll("btn-sm", "btn-sell");
                sellBtn.setOnAction(e -> {
                    Position p = getTableView().getItems().get(getIndex());
                    context.navigateTo("TRADING", p.getAsset());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(sellBtn);
                }
            }
        });

        holdingsTable.getColumns().addAll(symbolCol, nameCol, catCol, qtyCol, avgCostCol,
                curPriceCol, totalCostCol, mktValCol, unPlCol, returnCol, actionCol);

        card.getChildren().addAll(t, holdingsTable);
        container.getChildren().add(card);
    }

    private void buildChartsAndDiversification() {
        HBox row = new HBox(20);

        // Pie Chart
        VBox chartBox = new VBox(12);
        chartBox.getStyleClass().add("content-card");
        chartBox.setPadding(new Insets(18));
        HBox.setHgrow(chartBox, Priority.ALWAYS);

        Label cTitle = new Label("Current Asset Allocation");
        cTitle.getStyleClass().add("section-title");

        allocationPieChart = new PieChart();
        allocationPieChart.setPrefHeight(250);
        allocationPieChart.setLabelsVisible(true);
        chartBox.getChildren().addAll(cTitle, allocationPieChart);

        // Concentration Diagnostics
        concentrationAlertBox = new VBox(12);
        concentrationAlertBox.getStyleClass().add("content-card");
        concentrationAlertBox.setPadding(new Insets(18));
        HBox.setHgrow(concentrationAlertBox, Priority.ALWAYS);

        Label dTitle = new Label("Portfolio Diversification & Risk Analysis");
        dTitle.getStyleClass().add("section-title");
        concentrationAlertBox.getChildren().add(dTitle);

        row.getChildren().addAll(chartBox, concentrationAlertBox);
        container.getChildren().add(row);
    }

    private void refreshPortfolio() {
        Trader trader = context.getTrader();
        PortfolioSummary summary = context.getPortfolioService().getPortfolioSummary(trader);

        costBasisLabel.setText(UIContext.formatMoney(summary.totalCostBasis()));
        marketValueLabel.setText(UIContext.formatMoney(summary.holdingsValue()));

        BigDecimal unPl = summary.unrealizedProfitLoss();
        unrealizedPlLabel.setText(UIContext.formatMoney(unPl));
        unrealizedPlLabel.getStyleClass().removeAll("text-positive", "text-negative");
        unrealizedPlLabel.getStyleClass().add(unPl.signum() >= 0 ? "text-positive" : "text-negative");

        BigDecimal rePl = summary.realizedProfitLoss();
        realizedPlLabel.setText(UIContext.formatMoney(rePl));
        realizedPlLabel.getStyleClass().removeAll("text-positive", "text-negative");
        realizedPlLabel.getStyleClass().add(rePl.signum() >= 0 ? "text-positive" : "text-negative");

        BigDecimal ret = summary.returnPercent();
        totalReturnLabel.setText(UIContext.formatPercent(ret));
        totalReturnLabel.getStyleClass().removeAll("text-positive", "text-negative");
        totalReturnLabel.getStyleClass().add(ret.signum() >= 0 ? "text-positive" : "text-negative");

        List<Position> positions = trader.getPortfolio().getPositions();
        holdingsTable.setItems(FXCollections.observableArrayList(positions));

        // Update PieChart
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
        for (Map.Entry<AssetCategory, BigDecimal> entry : summary.allocationByCategory().entrySet()) {
            if (entry.getValue().signum() > 0) {
                chartData.add(new PieChart.Data(entry.getKey().name() + " (" + entry.getValue() + "%)",
                        entry.getValue().doubleValue()));
            }
        }
        if (chartData.isEmpty()) {
            chartData.add(new PieChart.Data("100% Cash Buffer", 100));
        }
        allocationPieChart.setData(chartData);

        // Check concentration risks
        concentrationAlertBox.getChildren().clear();
        Label dTitle = new Label("Portfolio Diversification & Risk Analysis");
        dTitle.getStyleClass().add("section-title");
        concentrationAlertBox.getChildren().add(dTitle);

        boolean hasConcentration = false;
        BigDecimal totalVal = summary.holdingsValue();
        if (totalVal.signum() > 0) {
            for (Position p : positions) {
                BigDecimal weight = p.getMarketValue().multiply(new BigDecimal("100"))
                        .divide(totalVal, 2, java.math.RoundingMode.HALF_UP);
                if (weight.compareTo(new BigDecimal("25.00")) > 0) {
                    hasConcentration = true;
                    HBox alert = new HBox(8);
                    alert.getStyleClass().addAll("info-banner", "banner-warning");
                    alert.setPadding(new Insets(10));
                    Label l = new Label("⚠️ High Concentration in " + p.getAsset().getSymbol() + " (" + weight + "% of portfolio): "
                            + "Individual stock risk is elevated. Consider spreading capital across ETFs or Bonds.");
                    l.setWrapText(true);
                    alert.getChildren().add(l);
                    concentrationAlertBox.getChildren().add(alert);
                }
            }
        }

        if (!hasConcentration) {
            HBox safe = new HBox(8);
            safe.getStyleClass().add("info-banner");
            safe.setPadding(new Insets(10));
            Label l = new Label("✅ Healthy Distribution: No single holding exceeds 25% of total portfolio value. Unsystematic single-stock risk is well contained.");
            l.setWrapText(true);
            safe.getChildren().add(l);
            concentrationAlertBox.getChildren().add(safe);
        }
    }

    private void handleExportHoldings() {
        try {
            Path path = context.getExportService().exportHoldings(
                    context.getTrader().getPortfolio().getPositions(), null);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("CSV Export Successful");
            alert.setHeaderText("Portfolio Holdings Exported");
            alert.setContentText("Report generated successfully at:\n" + path.toAbsolutePath());
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setContentText("Could not write CSV file: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
