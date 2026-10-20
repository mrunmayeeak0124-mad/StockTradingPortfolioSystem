package com.stockportfolio.ui;

import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.OrderType;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.model.Stock;
import com.stockportfolio.model.TransactionType;
import com.stockportfolio.service.MarketAnalyticsService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified Explore Section: Market Explorer, Institutional Stock Fundamentals,
 * Multi-Timeframe Historical Technical Charts (1D to ALL matching actual trading terminals),
 * and integrated Buy/Sell Trading Terminal.
 */
public class ExploreView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(22);

    // Selected Asset State
    private Asset selectedAsset;
    private String currentTimeframe = "1D";

    // Market Table Components
    private final TableView<Asset> marketTable = new TableView<>();
    private final TextField searchField = new TextField();
    private String selectedCategoryFilter = "ALL";
    private boolean watchlistOnlyFilter = false;

    // Technical Chart Components
    private Label chartAssetSymbolLabel;
    private Label chartAssetNameLabel;
    private Label chartPriceLabel;
    private Label chartChangeLabel;
    private LineChart<String, Number> technicalChart;
    private HBox timeframeBtnBox;

    // Fundamentals Labels
    private Label mcapLabel;
    private Label roiLabel;
    private Label bookValueLabel;
    private Label volumeLabel;
    private Label peRatioLabel;
    private Label range52Label;

    // Trading Desk Inputs
    private final ToggleGroup sideGroup = new ToggleGroup();
    private final RadioButton buyRadio = new RadioButton("BUY");
    private final RadioButton sellRadio = new RadioButton("SELL");

    private final ToggleGroup typeGroup = new ToggleGroup();
    private final RadioButton marketRadio = new RadioButton("MARKET");
    private final RadioButton limitRadio = new RadioButton("LIMIT");

    private final TextField quantityField = new TextField("10");
    private final TextField limitPriceField = new TextField();
    private final Label orderGrossLabel = new Label("₹0.00");
    private final Label orderFeeLabel = new Label("₹0.00");
    private final Label orderTotalLabel = new Label("₹0.00");

    public ExploreView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(26));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildMarketExplorerSection();
        buildChartAndFundamentalsSection();
        buildTradingDeskSection();

        setContent(container);

        // Select first asset by default
        List<Asset> assets = new ArrayList<>(context.getTradingService().getAssets().values());
        if (!assets.isEmpty()) {
            selectAsset(assets.get(0));
        }

        refreshMarketTable();
        context.addDataChangeListener(this::refreshMarketTable);
    }

    public void selectAsset(Asset asset) {
        if (asset == null) return;
        this.selectedAsset = asset;
        updateChartAndFundamentals();
        updateTradingCalculation();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Market Explore & Trading Terminal");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Discover public stocks, ETFs, and sovereign bonds. Inspect real-time fundamentals, multi-year charts, and execute market/limit trades.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    private void buildMarketExplorerSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(18));

        // Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("🔍 Search by stock name, ticker or sector...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, o, n) -> refreshMarketTable());

        Button allBtn = createCategoryBtn("ALL", "All Assets");
        Button stockBtn = createCategoryBtn("STOCK", "Stocks");
        Button etfBtn = createCategoryBtn("ETF", "ETFs");
        Button bondBtn = createCategoryBtn("BOND", "Bonds");

        Button starFilterBtn = new Button("⭐ Watchlist Only");
        starFilterBtn.getStyleClass().addAll("btn-sm", "btn-outline");
        starFilterBtn.setOnAction(e -> {
            watchlistOnlyFilter = !watchlistOnlyFilter;
            starFilterBtn.setStyle(watchlistOnlyFilter ? "-fx-background-color: #f59e0b; -fx-text-fill: #000;" : "");
            refreshMarketTable();
        });

        filterBar.getChildren().addAll(searchField, allBtn, stockBtn, etfBtn, bondBtn, starFilterBtn);

        // Assets Table
        marketTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        marketTable.setPrefHeight(230);

        TableColumn<Asset, String> symCol = new TableColumn<>("Symbol");
        symCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSymbol()));
        symCol.setPrefWidth(90);

        TableColumn<Asset, String> nameCol = new TableColumn<>("Company / Asset Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(220);

        TableColumn<Asset, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory().name()));
        catCol.setPrefWidth(85);

        TableColumn<Asset, String> priceCol = new TableColumn<>("Market Price");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getCurrentPrice())));
        priceCol.setPrefWidth(110);

        TableColumn<Asset, String> mcapCol = new TableColumn<>("Market Cap");
        mcapCol.setCellValueFactory(d -> {
            if (d.getValue() instanceof Stock s) {
                return new SimpleStringProperty(s.getMarketCap());
            }
            return new SimpleStringProperty("Benchmark");
        });
        mcapCol.setPrefWidth(130);

        TableColumn<Asset, Void> watchCol = new TableColumn<>("Watch");
        watchCol.setPrefWidth(70);
        watchCol.setCellFactory(col -> new TableCell<>() {
            private final Button star = new Button("☆");
            {
                star.getStyleClass().addAll("btn-sm", "btn-link");
                star.setOnAction(e -> {
                    Asset a = getTableView().getItems().get(getIndex());
                    context.getMarketService().toggleWatchlist(context.getTrader().getUserId(), a.getAssetId());
                    updateStar(a);
                });
            }
            private void updateStar(Asset a) {
                boolean inWatch = context.getMarketService().isInWatchlist(context.getTrader().getUserId(), a.getAssetId());
                star.setText(inWatch ? "★" : "☆");
                star.setStyle(inWatch ? "-fx-text-fill: #f59e0b; -fx-font-size: 14px;" : "-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    updateStar(getTableView().getItems().get(getIndex()));
                    setGraphic(star);
                }
            }
        });

        TableColumn<Asset, Void> actionCol = new TableColumn<>("Analyze & Trade");
        actionCol.setPrefWidth(130);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button selBtn = new Button("View Chart ➔");
            {
                selBtn.getStyleClass().addAll("btn-sm", "btn-primary");
                selBtn.setOnAction(e -> {
                    Asset a = getTableView().getItems().get(getIndex());
                    selectAsset(a);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : selBtn);
            }
        });

        marketTable.getColumns().addAll(symCol, nameCol, catCol, priceCol, mcapCol, watchCol, actionCol);
        card.getChildren().addAll(filterBar, marketTable);
        container.getChildren().add(card);
    }

    private Button createCategoryBtn(String catKey, String label) {
        Button btn = new Button(label);
        btn.getStyleClass().addAll("btn-sm", "btn-secondary");
        btn.setOnAction(e -> {
            selectedCategoryFilter = catKey;
            refreshMarketTable();
        });
        return btn;
    }

    private void refreshMarketTable() {
        String search = searchField.getText().trim().toLowerCase();
        List<Asset> all = new ArrayList<>(context.getTradingService().getAssets().values());

        List<Asset> filtered = all.stream().filter(a -> {
            if (!selectedCategoryFilter.equals("ALL") && !a.getCategory().name().equalsIgnoreCase(selectedCategoryFilter)) {
                return false;
            }
            if (watchlistOnlyFilter && !context.getMarketService().isInWatchlist(context.getTrader().getUserId(), a.getAssetId())) {
                return false;
            }
            if (!search.isEmpty()) {
                return a.getName().toLowerCase().contains(search) || a.getSymbol().toLowerCase().contains(search);
            }
            return true;
        }).collect(Collectors.toList());

        marketTable.setItems(FXCollections.observableArrayList(filtered));
    }

    /**
     * Technical Chart & Financial Fundamentals Section matching Image 1 & Image 2.
     */
    private void buildChartAndFundamentalsSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        // Header info matching TCS Image
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        chartAssetSymbolLabel = new Label("TCS • NSE");
        chartAssetSymbolLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        chartAssetNameLabel = new Label("Tata Consultancy Services");
        chartAssetNameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");
        titleBox.getChildren().addAll(chartAssetSymbolLabel, chartAssetNameLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        chartPriceLabel = new Label("₹2,460.90");
        chartPriceLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #f8fafc;");

        chartChangeLabel = new Label("-4.10 (0.17%) 1D");
        chartChangeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        priceBox.getChildren().addAll(chartPriceLabel, chartChangeLabel);

        header.getChildren().addAll(titleBox, priceBox);

        // Timeframe selector bar: [ 1D | 1W | 1M | 3M | 6M | 1Y | 5Y | All ]
        timeframeBtnBox = new HBox(8);
        timeframeBtnBox.setAlignment(Pos.CENTER_LEFT);
        String[] tfs = {"1D", "1W", "1M", "3M", "6M", "1Y", "5Y", "ALL"};
        for (String tf : tfs) {
            Button tb = new Button(tf);
            tb.getStyleClass().addAll("btn-sm", tf.equals("1D") ? "btn-primary" : "btn-outline");
            tb.setOnAction(e -> {
                currentTimeframe = tf;
                for (var node : timeframeBtnBox.getChildren()) {
                    if (node instanceof Button b) {
                        b.getStyleClass().removeAll("btn-primary", "btn-outline");
                        b.getStyleClass().add(b.getText().equals(tf) ? "btn-primary" : "btn-outline");
                    }
                }
                updateChartAndFundamentals();
            });
            timeframeBtnBox.getChildren().add(tb);
        }

        javafx.scene.layout.Region tfSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(tfSpacer, Priority.ALWAYS);
        Label liveBadge = new Label("● Yahoo Finance Live Stream");
        liveBadge.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-padding: 4 12 4 12; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        timeframeBtnBox.getChildren().addAll(tfSpacer, liveBadge);

        // LineChart for price series
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAnimated(false);
        xAxis.setTickLabelsVisible(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        technicalChart = new LineChart<>(xAxis, yAxis);
        technicalChart.setPrefHeight(300);
        technicalChart.setAnimated(false);
        technicalChart.setLegendVisible(false);
        technicalChart.setCreateSymbols(false);

        // Fundamentals Grid (Point 7)
        GridPane fundGrid = new GridPane();
        fundGrid.setHgap(14);
        fundGrid.setVgap(12);

        mcapLabel = new Label("₹14.15 Lakh Cr");
        VBox f1 = createFundamentalCard("Market Cap", mcapLabel, "Total market valuation");

        roiLabel = new Label("+18.4% (1Y)");
        VBox f2 = createFundamentalCard("Return on Investment (ROI)", roiLabel, "1-Year annualized return");

        bookValueLabel = new Label("₹482.50");
        VBox f3 = createFundamentalCard("Book Value", bookValueLabel, "Net asset value per unit");

        volumeLabel = new Label("2,845,000");
        VBox f4 = createFundamentalCard("24h Total Volume", volumeLabel, "Units traded today");

        peRatioLabel = new Label("28.6x");
        VBox f5 = createFundamentalCard("P/E Ratio", peRatioLabel, "Price to earnings valuation");

        range52Label = new Label("₹1,980.00 – ₹2,680.00");
        VBox f6 = createFundamentalCard("52-Week Range", range52Label, "Yearly lowest & highest");

        fundGrid.add(f1, 0, 0);
        fundGrid.add(f2, 1, 0);
        fundGrid.add(f3, 2, 0);
        fundGrid.add(f4, 0, 1);
        fundGrid.add(f5, 1, 1);
        fundGrid.add(f6, 2, 1);

        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            fundGrid.getColumnConstraints().add(cc);
        }

        card.getChildren().addAll(header, timeframeBtnBox, technicalChart, fundGrid);
        container.getChildren().add(card);
    }

    private VBox createFundamentalCard(String title, Label valLabel, String sub) {
        VBox b = new VBox(4);
        b.setStyle("-fx-background-color: #0b1120; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");

        valLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label s = new Label(sub);
        s.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");

        b.getChildren().addAll(t, valLabel, s);
        return b;
    }

    private void updateChartAndFundamentals() {
        if (selectedAsset == null) return;

        chartAssetSymbolLabel.setText(selectedAsset.getSymbol() + " • " + (selectedAsset instanceof Stock s ? s.getExchange() : "NSE"));
        chartAssetNameLabel.setText(selectedAsset.getName());
        chartPriceLabel.setText(UIContext.formatMoney(selectedAsset.getCurrentPrice()));

        // Populate fundamentals
        if (selectedAsset instanceof Stock st) {
            mcapLabel.setText(st.getMarketCap());
            roiLabel.setText("+" + st.getOneYearRoi() + "% (1Y)");
            bookValueLabel.setText(UIContext.formatMoney(st.getBookValue()));
            volumeLabel.setText(String.format("%,d shares", st.getTotalVolume()));
            peRatioLabel.setText(st.getPeRatio() + "x");
            range52Label.setText(UIContext.formatMoney(st.getFiftyTwoWeekLow()) + " – " + UIContext.formatMoney(st.getFiftyTwoWeekHigh()));
        } else {
            mcapLabel.setText("Index Benchmark");
            roiLabel.setText("+12.8% (1Y)");
            bookValueLabel.setText("NAV: " + UIContext.formatMoney(selectedAsset.getCurrentPrice()));
            volumeLabel.setText("1,450,000 units");
            peRatioLabel.setText("22.1x");
            range52Label.setText(UIContext.formatMoney(selectedAsset.getCurrentPrice().multiply(BigDecimal.valueOf(0.85)))
                    + " – " + UIContext.formatMoney(selectedAsset.getCurrentPrice().multiply(BigDecimal.valueOf(1.15))));
        }

        // Generate Timeframe series matching Image 1 & 2
        MarketAnalyticsService.TimeframeSeries series = context.getAnalyticsService().generateTimeframeSeries(selectedAsset, currentTimeframe);
        chartPriceLabel.setText(UIContext.formatMoney(series.currentPrice()));

        technicalChart.getData().clear();
        XYChart.Series<String, Number> chartSeries = new XYChart.Series<>();
        chartSeries.setName(selectedAsset.getSymbol());

        for (int i = 0; i < series.points().size(); i++) {
            PricePoint pt = series.points().get(i);
            chartSeries.getData().add(new XYChart.Data<>(String.valueOf(i), pt.price()));
        }
        technicalChart.getData().add(chartSeries);

        // Update change label with color
        String sign = series.isPositive() ? "+" : "";
        chartChangeLabel.setText(sign + UIContext.formatMoney(series.absoluteChange()) + " (" + sign + series.percentChange() + "%) " + currentTimeframe);
        if (series.isPositive()) {
            chartChangeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            chartSeries.getNode().setStyle("-fx-stroke: #10b981; -fx-stroke-width: 2.2px;");
        } else {
            chartChangeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
            chartSeries.getNode().setStyle("-fx-stroke: #ef4444; -fx-stroke-width: 2.2px;");
        }
    }

    /**
     * Trading Desk Section: directly execute buy and sell market/limit orders.
     */
    private void buildTradingDeskSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Trading Desk — Execute Order");
        title.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        buyRadio.setToggleGroup(sideGroup);
        buyRadio.setSelected(true);
        sellRadio.setToggleGroup(sideGroup);
        HBox sideBox = new HBox(16, buyRadio, sellRadio);

        marketRadio.setToggleGroup(typeGroup);
        marketRadio.setSelected(true);
        limitRadio.setToggleGroup(typeGroup);
        HBox typeBox = new HBox(16, marketRadio, limitRadio);

        sideGroup.selectedToggleProperty().addListener((o, ov, nv) -> updateTradingCalculation());
        typeGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            limitPriceField.setDisable(!limitRadio.isSelected());
            updateTradingCalculation();
        });

        quantityField.setPrefWidth(120);
        quantityField.textProperty().addListener((o, ov, nv) -> updateTradingCalculation());

        limitPriceField.setPromptText("Enter Limit Price");
        limitPriceField.setDisable(true);
        limitPriceField.textProperty().addListener((o, ov, nv) -> updateTradingCalculation());

        grid.add(new Label("Order Side:"), 0, 0);
        grid.add(sideBox, 1, 0);

        grid.add(new Label("Order Type:"), 0, 1);
        grid.add(typeBox, 1, 1);

        grid.add(new Label("Order Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);

        grid.add(new Label("Target Limit Price:"), 0, 3);
        grid.add(limitPriceField, 1, 3);

        // Summary box
        VBox costBox = new VBox(8);
        costBox.setStyle("-fx-background-color: #0b1120; -fx-padding: 16px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

        HBox r1 = new HBox(new Label("Gross Amount:"), orderGrossLabel);
        HBox.setHgrow(r1.getChildren().get(0), Priority.ALWAYS);
        HBox r2 = new HBox(new Label("Brokerage & Exchange Fee (0.10%):"), orderFeeLabel);
        HBox.setHgrow(r2.getChildren().get(0), Priority.ALWAYS);
        HBox r3 = new HBox(new Label("Net Payable / Estimated Proceeds:"), orderTotalLabel);
        HBox.setHgrow(r3.getChildren().get(0), Priority.ALWAYS);
        orderTotalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 15px;");

        costBox.getChildren().addAll(r1, r2, r3);

        Button executeBtn = new Button("⚡ Confirm & Execute Order");
        executeBtn.getStyleClass().addAll("btn", "btn-primary");
        executeBtn.setMaxWidth(Double.MAX_VALUE);
        executeBtn.setOnAction(e -> handleOrderExecution());

        card.getChildren().addAll(title, grid, costBox, executeBtn);
        container.getChildren().add(card);
    }

    private void updateTradingCalculation() {
        if (selectedAsset == null) return;
        try {
            int qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();

            BigDecimal price = selectedAsset.getCurrentPrice();
            if (limitRadio.isSelected() && !limitPriceField.getText().trim().isEmpty()) {
                price = new BigDecimal(limitPriceField.getText().trim());
            }

            BigDecimal gross = price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal fee = gross.multiply(BigDecimal.valueOf(0.0010)).setScale(2, RoundingMode.HALF_UP);
            boolean isBuy = buyRadio.isSelected();
            BigDecimal net = isBuy ? gross.add(fee) : gross.subtract(fee);

            orderGrossLabel.setText(UIContext.formatMoney(gross));
            orderFeeLabel.setText(UIContext.formatMoney(fee));
            orderTotalLabel.setText(UIContext.formatMoney(net));
        } catch (Exception ex) {
            orderGrossLabel.setText("₹0.00");
            orderFeeLabel.setText("₹0.00");
            orderTotalLabel.setText("₹0.00");
        }
    }

    private void handleOrderExecution() {
        if (selectedAsset == null) return;
        try {
            int qty = Integer.parseInt(quantityField.getText().trim());
            TransactionType side = buyRadio.isSelected() ? TransactionType.BUY : TransactionType.SELL;
            OrderType type = marketRadio.isSelected() ? OrderType.MARKET : OrderType.LIMIT;
            BigDecimal limitPrice = null;

            if (type == OrderType.LIMIT) {
                if (limitPriceField.getText().trim().isEmpty()) {
                    throw new TradingException("Please enter a valid target limit price.");
                }
                limitPrice = new BigDecimal(limitPriceField.getText().trim());
            }

            context.getTradingService().placeOrder(selectedAsset, side, type, qty, limitPrice);
            context.notifyDataChanged();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Placed Successfully");
            alert.setHeaderText(side + " order for " + qty + " units of " + selectedAsset.getSymbol() + " submitted.");
            alert.setContentText("Status: " + (type == OrderType.MARKET ? "EXECUTED" : "PENDING LIMIT TRIGGER")
                    + "\nAvailable cash balance and holdings have been updated.");
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Order Execution Error");
            alert.setHeaderText("Failed to place order");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }
}
