package com.stockportfolio.ui;

import com.stockportfolio.model.Asset;
import com.stockportfolio.model.ConversionResult;
import com.stockportfolio.model.ExchangeRate;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.service.CurrencyService;
import com.stockportfolio.service.MarketAnalyticsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Market Insights View: technical market trends, multi-asset comparative price movements,
 * and built-in offline currency conversion (USD <-> INR).
 */
public class MarketInsightsView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    private LineChart<String, Number> comparativeChart;
    private ComboBox<Asset> assetSelect1;
    private ComboBox<Asset> assetSelect2;

    // Currency converter widgets
    private TextField convAmountField;
    private ComboBox<String> fromCurrencyCombo;
    private ComboBox<String> toCurrencyCombo;
    private Label convResultLabel;
    private Label convRateInfoLabel;
    private Label convSourceLabel;

    public MarketInsightsView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildComparativeChartSection();
        buildCurrencyConverterSection();
        buildEducationalBanner();

        setContent(container);
        refreshChart();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Market Insights & Foreign Exchange");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Analyze asset trends, moving averages, relative price movements, and multi-currency exchange rates.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    private void buildComparativeChartSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        HBox chartHeader = new HBox(16);
        chartHeader.setAlignment(Pos.CENTER_LEFT);

        Label cTitle = new Label("Comparative Price Trends (Last 30 Days)");
        cTitle.getStyleClass().add("section-title");
        HBox.setHgrow(cTitle, Priority.ALWAYS);

        List<Asset> assets = new ArrayList<>(context.getTradingService().getAssets().values());
        assetSelect1 = new ComboBox<>();
        assetSelect1.getStyleClass().add("combo-box");
        assetSelect1.getItems().addAll(assets);
        if (!assets.isEmpty()) assetSelect1.setValue(assets.get(0));

        assetSelect2 = new ComboBox<>();
        assetSelect2.getStyleClass().add("combo-box");
        assetSelect2.getItems().addAll(assets);
        if (assets.size() > 1) assetSelect2.setValue(assets.get(1));

        assetSelect1.setOnAction(e -> refreshChart());
        assetSelect2.setOnAction(e -> refreshChart());

        chartHeader.getChildren().addAll(cTitle, new Label("Asset A:"), assetSelect1, new Label("Asset B:"), assetSelect2);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Observation Date");
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Simulated Price (INR)");
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        comparativeChart = new LineChart<>(xAxis, yAxis);
        comparativeChart.setPrefHeight(320);
        comparativeChart.setAnimated(false);
        comparativeChart.setCreateSymbols(false);

        card.getChildren().addAll(chartHeader, comparativeChart);
        container.getChildren().add(card);
    }

    private void refreshChart() {
        comparativeChart.getData().clear();
        Asset a1 = assetSelect1.getValue();
        Asset a2 = assetSelect2.getValue();

        if (a1 != null) {
            XYChart.Series<String, Number> s1 = new XYChart.Series<>();
            s1.setName(a1.getSymbol() + " (" + a1.getName() + ")");
            List<PricePoint> h1 = context.getMarketService().getPriceHistory(a1);
            for (PricePoint pt : h1) {
                s1.getData().add(new XYChart.Data<>(pt.date().toString(), pt.price()));
            }
            comparativeChart.getData().add(s1);
        }

        if (a2 != null && !a2.getAssetId().equals(a1 != null ? a1.getAssetId() : "")) {
            XYChart.Series<String, Number> s2 = new XYChart.Series<>();
            s2.setName(a2.getSymbol() + " (" + a2.getName() + ")");
            List<PricePoint> h2 = context.getMarketService().getPriceHistory(a2);
            for (PricePoint pt : h2) {
                s2.getData().add(new XYChart.Data<>(pt.date().toString(), pt.price()));
            }
            comparativeChart.getData().add(s2);
        }
    }

    private void buildCurrencyConverterSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Currency Exchange & Conversion Calculator");
        title.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        convAmountField = new TextField("1000.00");
        convAmountField.getStyleClass().add("text-input");
        convAmountField.setPrefWidth(180);

        fromCurrencyCombo = new ComboBox<>();
        fromCurrencyCombo.getItems().addAll("USD", "INR");
        fromCurrencyCombo.setValue("USD");
        fromCurrencyCombo.getStyleClass().add("combo-box");

        toCurrencyCombo = new ComboBox<>();
        toCurrencyCombo.getItems().addAll("INR", "USD");
        toCurrencyCombo.setValue("INR");
        toCurrencyCombo.getStyleClass().add("combo-box");

        Button convertBtn = new Button("🔄 Convert Currency");
        convertBtn.getStyleClass().addAll("btn", "btn-primary");
        convertBtn.setOnAction(e -> handleConvert());

        grid.add(new Label("Amount:"), 0, 0);
        grid.add(convAmountField, 1, 0);
        grid.add(new Label("From:"), 2, 0);
        grid.add(fromCurrencyCombo, 3, 0);
        grid.add(new Label("To:"), 4, 0);
        grid.add(toCurrencyCombo, 5, 0);
        grid.add(convertBtn, 6, 0);

        // Result presentation
        HBox resultBox = new HBox(20);
        resultBox.setAlignment(Pos.CENTER_LEFT);
        resultBox.setPadding(new Insets(12, 16, 12, 16));
        resultBox.getStyleClass().addAll("metric-card", "card-accent-teal");

        VBox r1 = new VBox(2);
        Label resTitle = new Label("Converted Amount:");
        resTitle.getStyleClass().add("card-subtitle");
        convResultLabel = new Label("₹83,500.00");
        convResultLabel.getStyleClass().add("card-value");
        r1.getChildren().addAll(resTitle, convResultLabel);

        VBox r2 = new VBox(2);
        Label rateTitle = new Label("Exchange Rate Applied:");
        rateTitle.getStyleClass().add("card-subtitle");
        convRateInfoLabel = new Label("1 USD = 83.50 INR");
        convRateInfoLabel.getStyleClass().add("font-semibold");
        r2.getChildren().addAll(rateTitle, convRateInfoLabel);

        VBox r3 = new VBox(2);
        Label srcTitle = new Label("Data Source:");
        srcTitle.getStyleClass().add("card-subtitle");
        convSourceLabel = new Label("MockExchangeRateProvider (Simulated Benchmark)");
        convSourceLabel.getStyleClass().add("text-muted");
        r3.getChildren().addAll(srcTitle, convSourceLabel);

        resultBox.getChildren().addAll(r1, r2, r3);

        card.getChildren().addAll(title, grid, resultBox);
        container.getChildren().add(card);

        handleConvert();
    }

    private void handleConvert() {
        String amtText = convAmountField.getText().trim();
        BigDecimal amt;
        try {
            amt = new BigDecimal(amtText);
            if (amt.signum() <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            convResultLabel.setText("Invalid Amount");
            return;
        }

        String from = fromCurrencyCombo.getValue();
        String to = toCurrencyCombo.getValue();

        try {
            CurrencyService cs = context.getCurrencyService();
            ConversionResult res = cs.convert(amt, from, to);

            convResultLabel.setText((to.equals("INR") ? "₹" : "$") + String.format("%,.2f", res.convertedAmount()));
            convRateInfoLabel.setText("1 " + from + " = " + res.rate().rate().toPlainString() + " " + to);
            convSourceLabel.setText(res.rate().sourceLabel() + " • " + UIContext.formatDateTime(res.rate().lastUpdated()));
        } catch (Exception e) {
            convResultLabel.setText("Rate Unavailable");
            convRateInfoLabel.setText("Could not fetch rate");
            convSourceLabel.setText("Offline");
        }
    }

    private void buildEducationalBanner() {
        HBox banner = new HBox(12);
        banner.getStyleClass().add("info-banner");
        banner.setPadding(new Insets(12, 16, 12, 16));
        Label l = new Label("💡 Educational Perspective: Currency exchange rates introduce currency risk for international investments. A strengthening domestic currency reduces overseas returns, while a depreciating currency augments foreign gains.");
        l.setWrapText(true);
        l.getStyleClass().add("text-muted");
        banner.getChildren().add(l);
        container.getChildren().add(banner);
    }
}
