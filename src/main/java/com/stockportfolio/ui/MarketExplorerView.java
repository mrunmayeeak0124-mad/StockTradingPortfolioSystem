package com.stockportfolio.ui;

import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.Watchlist;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Market Explorer view enabling searching, filtering across Stocks, ETFs, and Bonds,
 * toggling watchlist favorites, and jumping straight to asset analytics or placing orders.
 */
public class MarketExplorerView extends VBox {

    private final UIContext context;
    private final TableView<Asset> assetTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final Button onlyWatchlistBtn = new Button("⭐ Watchlist Only: OFF");
    private boolean watchlistFilterActive = false;

    public MarketExplorerView(UIContext context) {
        this.context = context;
        setSpacing(20);
        setPadding(new Insets(28));
        getStyleClass().add("view-container");

        buildHeader();
        buildFilterBar();
        buildTable();

        refreshAssets();
        context.addDataChangeListener(this::refreshAssets);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Market Explorer");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Browse simulated investment assets across Indian Equities, Index Funds & Sovereign Bonds.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button refreshQuotesBtn = new Button("🔄 Refresh Market Quotes");
        refreshQuotesBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshQuotesBtn.setOnAction(e -> {
            try {
                context.getMarketService().refreshPrices();
                context.notifyDataChanged();
            } catch (Exception ex) {
                UIContext.showError("Market Refresh Error", ex.getMessage());
            }
        });

        HBox header = new HBox(titleBox, refreshQuotesBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private void buildFilterBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("filter-bar");
        bar.setPadding(new Insets(12, 16, 12, 16));

        searchField.setPromptText("Search by company name, ticker symbol, or sector...");
        searchField.setPrefWidth(320);
        searchField.getStyleClass().add("text-input");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshAssets());

        categoryCombo.getItems().addAll("All Categories", "Stocks Only", "ETFs Only", "Bonds Only");
        categoryCombo.setValue("All Categories");
        categoryCombo.getStyleClass().add("combo-box");
        categoryCombo.setOnAction(e -> refreshAssets());

        onlyWatchlistBtn.getStyleClass().addAll("btn", "btn-outline");
        onlyWatchlistBtn.setOnAction(e -> {
            watchlistFilterActive = !watchlistFilterActive;
            onlyWatchlistBtn.setText(watchlistFilterActive ? "⭐ Watchlist Only: ON" : "⭐ Watchlist Only: OFF");
            if (watchlistFilterActive) {
                onlyWatchlistBtn.getStyleClass().add("btn-warning-active");
            } else {
                onlyWatchlistBtn.getStyleClass().remove("btn-warning-active");
            }
            refreshAssets();
        });

        bar.getChildren().addAll(new Label("Filter:"), categoryCombo, searchField, onlyWatchlistBtn);
        getChildren().add(bar);
    }

    private void buildTable() {
        assetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(assetTable, Priority.ALWAYS);

        TableColumn<Asset, String> symbolCol = new TableColumn<>("Symbol");
        symbolCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSymbol()));
        symbolCol.setPrefWidth(120);

        TableColumn<Asset, String> nameCol = new TableColumn<>("Asset Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(220);

        TableColumn<Asset, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().name()));
        catCol.setPrefWidth(100);

        TableColumn<Asset, String> detailCol = new TableColumn<>("Details");
        detailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDetails()));
        detailCol.setPrefWidth(240);

        TableColumn<Asset, String> priceCol = new TableColumn<>("Simulated Price");
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(
                UIContext.formatMoney(data.getValue().getCurrentPrice())));
        priceCol.setPrefWidth(120);

        // Actions column
        TableColumn<Asset, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button chartBtn = new Button("📊 Chart");
            private final Button tradeBtn = new Button("⚡ Trade");
            private final Button starBtn = new Button("⭐");
            private final HBox btnBox = new HBox(6, starBtn, chartBtn, tradeBtn);

            {
                chartBtn.getStyleClass().addAll("btn-sm", "btn-outline");
                tradeBtn.getStyleClass().addAll("btn-sm", "btn-primary");
                starBtn.getStyleClass().addAll("btn-sm", "btn-star");
                btnBox.setAlignment(Pos.CENTER);

                chartBtn.setOnAction(e -> {
                    Asset a = getTableView().getItems().get(getIndex());
                    context.navigateTo("ASSET_DETAILS", a);
                });

                tradeBtn.setOnAction(e -> {
                    Asset a = getTableView().getItems().get(getIndex());
                    context.navigateTo("TRADING", a);
                });

                starBtn.setOnAction(e -> {
                    Asset a = getTableView().getItems().get(getIndex());
                    context.getMarketService().toggleWatchlist(context.getTrader().getUserId(), a.getAssetId());
                    refreshAssets();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Asset a = getTableView().getItems().get(getIndex());
                    boolean inWatchlist = context.getMarketService().isInWatchlist(
                            context.getTrader().getUserId(), a.getAssetId());
                    starBtn.setText(inWatchlist ? "★" : "☆");
                    setGraphic(btnBox);
                }
            }
        });

        assetTable.getColumns().addAll(symbolCol, nameCol, catCol, detailCol, priceCol, actionCol);
        getChildren().add(assetTable);
    }

    private void refreshAssets() {
        String catSelection = categoryCombo.getValue();
        AssetCategory cat = null;
        if ("Stocks Only".equals(catSelection)) cat = AssetCategory.STOCK;
        else if ("ETFs Only".equals(catSelection)) cat = AssetCategory.ETF;
        else if ("Bonds Only".equals(catSelection)) cat = AssetCategory.BOND;

        List<Asset> list = context.getMarketService().searchAssets(searchField.getText(), cat);

        if (watchlistFilterActive) {
            Watchlist wl = context.getMarketService().getWatchlist(context.getTrader().getUserId());
            list = list.stream().filter(wl::contains).toList();
        }

        assetTable.setItems(FXCollections.observableArrayList(list));
    }
}
