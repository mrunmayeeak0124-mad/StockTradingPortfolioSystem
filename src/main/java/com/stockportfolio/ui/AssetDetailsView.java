package com.stockportfolio.ui;

import com.stockportfolio.model.Asset;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.service.MarketAnalyticsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed analysis view for an individual asset featuring historical price trends,
 * technical indicators (Moving Averages), price bounds, and direct order execution shortcut.
 */
public class AssetDetailsView extends ScrollPane {

    private final UIContext context;
    private Asset currentAsset;
    private final VBox container = new VBox(20);

    private Label symbolLabel;
    private Label nameLabel;
    private Label categoryLabel;
    private Label priceLabel;
    private LineChart<String, Number> lineChart;

    private Label highLabel;
    private Label lowLabel;
    private Label sma7Label;
    private Label sma14Label;
    private Label changeLabel;

    public AssetDetailsView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildChart();
        buildStatsGrid();
        buildDisclaimer();

        setContent(container);
    }

    public void setAsset(Asset asset) {
        this.currentAsset = asset;
        refreshView();
    }

    private void buildHeader() {
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Market");
        backBtn.getStyleClass().addAll("btn", "btn-outline");
        backBtn.setOnAction(e -> context.navigateTo("MARKET"));

        VBox titleBox = new VBox(2);
        symbolLabel = new Label("---");
        symbolLabel.getStyleClass().add("view-title");

        nameLabel = new Label("Select an asset to view details");
        nameLabel.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(symbolLabel, nameLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        categoryLabel = new Label("CATEGORY");
        categoryLabel.getStyleClass().addAll("badge", "badge-primary");

        priceLabel = new Label("₹0.00");
        priceLabel.getStyleClass().addAll("view-title", "text-accent");

        Button tradeBtn = new Button("⚡ Place Order for this Asset");
        tradeBtn.getStyleClass().addAll("btn", "btn-primary");
        tradeBtn.setOnAction(e -> {
            if (currentAsset != null) {
                context.navigateTo("TRADING", currentAsset);
            }
        });

        topBox.getChildren().addAll(backBtn, titleBox, categoryLabel, priceLabel, tradeBtn);
        container.getChildren().add(topBox);
    }

    private void buildChart() {
        VBox chartBox = new VBox(10);
        chartBox.getStyleClass().add("content-card");
        chartBox.setPadding(new Insets(18));

        Label chartTitle = new Label("30-Day Simulated Price History");
        chartTitle.getStyleClass().add("section-title");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Observation Date");
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price (INR)");
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setPrefHeight(340);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true);

        chartBox.getChildren().addAll(chartTitle, lineChart);
        container.getChildren().add(chartBox);
    }

    private void buildStatsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        highLabel = new Label("---");
        VBox c1 = createStatCard("30-Day High", highLabel, "Highest simulated close");

        lowLabel = new Label("---");
        VBox c2 = createStatCard("30-Day Low", lowLabel, "Lowest simulated close");

        sma7Label = new Label("---");
        VBox c3 = createStatCard("7-Day SMA", sma7Label, "Short-term trend average");

        sma14Label = new Label("---");
        VBox c4 = createStatCard("14-Day SMA", sma14Label, "Medium-term trend average");

        changeLabel = new Label("---");
        VBox c5 = createStatCard("30-Day Return", changeLabel, "Net change over period");

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

    private VBox createStatCard(String title, Label valLabel, String sub) {
        VBox box = new VBox(4);
        box.getStyleClass().add("metric-card");
        box.setPadding(new Insets(14));

        Label t = new Label(title);
        t.getStyleClass().add("card-title");

        valLabel.getStyleClass().add("font-semibold");

        Label s = new Label(sub);
        s.getStyleClass().add("text-muted");
        s.setStyle("-fx-font-size: 11px;");

        box.getChildren().addAll(t, valLabel, s);
        return box;
    }

    private void buildDisclaimer() {
        HBox noteBox = new HBox(12);
        noteBox.getStyleClass().add("info-banner");
        noteBox.setPadding(new Insets(12, 16, 12, 16));

        Label note = new Label("💡 Educational Note: Moving averages (SMA) and historical price curves illustrate past observations. In real financial markets, past performance is never a reliable predictor of future returns.");
        note.setWrapText(true);
        note.getStyleClass().add("text-muted");
        noteBox.getChildren().add(note);
        container.getChildren().add(noteBox);
    }

    private void refreshView() {
        if (currentAsset == null) return;

        symbolLabel.setText(currentAsset.getSymbol());
        nameLabel.setText(currentAsset.getName() + " • " + currentAsset.getDetails());
        categoryLabel.setText(currentAsset.getCategory().name());
        priceLabel.setText(UIContext.formatMoney(currentAsset.getCurrentPrice()));

        // Populate Chart
        lineChart.getData().clear();
        List<PricePoint> history = context.getMarketService().getPriceHistory(currentAsset);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(currentAsset.getSymbol() + " Price");

        BigDecimal high = BigDecimal.ZERO;
        BigDecimal low = new BigDecimal("999999999.00");

        for (PricePoint pt : history) {
            series.getData().add(new XYChart.Data<>(pt.date().toString(), pt.price()));
            if (pt.price().compareTo(high) > 0) high = pt.price();
            if (pt.price().compareTo(low) < 0) low = pt.price();
        }
        lineChart.getData().add(series);

        // Stats
        highLabel.setText(UIContext.formatMoney(high));
        lowLabel.setText(UIContext.formatMoney(low));

        MarketAnalyticsService analytics = context.getAnalyticsService();
        analytics.calculateSMA(history, 7).ifPresentOrElse(
                sma -> sma7Label.setText(UIContext.formatMoney(sma)),
                () -> sma7Label.setText("N/A")
        );
        analytics.calculateSMA(history, 14).ifPresentOrElse(
                sma -> sma14Label.setText(UIContext.formatMoney(sma)),
                () -> sma14Label.setText("N/A")
        );

        if (!history.isEmpty()) {
            BigDecimal firstPrice = history.get(0).price();
            BigDecimal lastPrice = history.get(history.size() - 1).price();
            BigDecimal change = lastPrice.subtract(firstPrice);
            BigDecimal changePct = firstPrice.signum() > 0 ?
                    change.multiply(new BigDecimal("100")).divide(firstPrice, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
            changeLabel.setText(UIContext.formatPercent(changePct) + " (" + UIContext.formatMoney(change) + ")");
            changeLabel.getStyleClass().removeAll("text-positive", "text-negative");
            changeLabel.getStyleClass().add(change.signum() >= 0 ? "text-positive" : "text-negative");
        }
    }
}
