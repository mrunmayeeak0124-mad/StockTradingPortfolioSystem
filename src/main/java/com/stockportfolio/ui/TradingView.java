package com.stockportfolio.ui;

import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.OrderQuote;
import com.stockportfolio.model.OrderStatus;
import com.stockportfolio.model.OrderType;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
import com.stockportfolio.service.TradingService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Buy & Sell Trading Desk supporting instant simulated Market Orders and conditional Limit Orders
 * with real-time quote breakdowns, fee transparency, and validation feedback.
 */
public class TradingView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    private ComboBox<Asset> assetCombo;
    private final ToggleGroup sideGroup = new ToggleGroup();
    private RadioButton buyRadio;
    private RadioButton sellRadio;

    private final ToggleGroup typeGroup = new ToggleGroup();
    private RadioButton marketRadio;
    private RadioButton limitRadio;

    private TextField limitPriceField;
    private VBox limitPriceBox;
    private TextField quantityField;

    // Quote preview card elements
    private Label previewPriceLabel;
    private Label previewGrossLabel;
    private Label previewFeeLabel;
    private Label previewNetLabel;
    private Label previewBalanceLabel;
    private Label validationMessageLabel;

    private Button submitOrderBtn;

    public TradingView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildTradingForm();

        setContent(container);
        context.addDataChangeListener(this::updateQuotePreview);
    }

    public void selectAsset(Asset asset) {
        if (asset != null && assetCombo != null) {
            assetCombo.setValue(asset);
        }
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Trading Execution Desk");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Place virtual Buy and Sell orders. Execute immediately at Market price or place conditional Limit orders.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button myOrdersBtn = new Button("📋 View Order Book");
        myOrdersBtn.getStyleClass().addAll("btn", "btn-outline");
        myOrdersBtn.setOnAction(e -> context.navigateTo("ORDERS"));

        HBox header = new HBox(titleBox, myOrdersBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(header);
    }

    private void buildTradingForm() {
        HBox splitPane = new HBox(28);

        // Left Form
        VBox formCard = new VBox(18);
        formCard.getStyleClass().add("content-card");
        formCard.setPadding(new Insets(24));
        HBox.setHgrow(formCard, Priority.ALWAYS);

        Label formTitle = new Label("Order Parameters");
        formTitle.getStyleClass().add("section-title");

        // 1. Asset selection
        VBox assetBox = new VBox(6);
        Label assetLbl = new Label("Select Asset to Trade:");
        assetLbl.getStyleClass().add("form-label");
        assetCombo = new ComboBox<>();
        assetCombo.setMaxWidth(Double.MAX_VALUE);
        assetCombo.getStyleClass().add("combo-box");
        List<Asset> allAssets = new ArrayList<>(context.getTradingService().getAssets().values());
        assetCombo.getItems().addAll(allAssets);
        if (!allAssets.isEmpty()) assetCombo.setValue(allAssets.get(0));
        assetCombo.setOnAction(e -> updateQuotePreview());
        assetBox.getChildren().addAll(assetLbl, assetCombo);

        // 2. Side selection (BUY / SELL)
        VBox sideBox = new VBox(6);
        Label sideLbl = new Label("Order Side:");
        sideLbl.getStyleClass().add("form-label");
        buyRadio = new RadioButton("BUY (Go Long)");
        buyRadio.setToggleGroup(sideGroup);
        buyRadio.setSelected(true);
        buyRadio.getStyleClass().addAll("radio-button", "radio-buy");

        sellRadio = new RadioButton("SELL (Exit Holding)");
        sellRadio.setToggleGroup(sideGroup);
        sellRadio.getStyleClass().addAll("radio-button", "radio-sell");

        sideGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateQuotePreview());
        HBox sideRadios = new HBox(20, buyRadio, sellRadio);
        sideBox.getChildren().addAll(sideLbl, sideRadios);

        // 3. Order Type (MARKET / LIMIT)
        VBox typeBox = new VBox(6);
        Label typeLbl = new Label("Execution Type:");
        typeLbl.getStyleClass().add("form-label");
        marketRadio = new RadioButton("MARKET Order (Instant Execution)");
        marketRadio.setToggleGroup(typeGroup);
        marketRadio.setSelected(true);

        limitRadio = new RadioButton("LIMIT Order (Target Price)");
        limitRadio.setToggleGroup(typeGroup);

        typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isLimit = limitRadio.isSelected();
            limitPriceBox.setVisible(isLimit);
            limitPriceBox.setManaged(isLimit);
            updateQuotePreview();
        });
        HBox typeRadios = new HBox(20, marketRadio, limitRadio);
        typeBox.getChildren().addAll(typeLbl, typeRadios);

        // 4. Limit Price input (conditional)
        limitPriceBox = new VBox(6);
        Label limitPriceLbl = new Label("Limit Trigger Price (INR):");
        limitPriceLbl.getStyleClass().add("form-label");
        limitPriceField = new TextField();
        limitPriceField.setPromptText("e.g. 2400.00");
        limitPriceField.getStyleClass().add("text-input");
        limitPriceField.textProperty().addListener((obs, oldVal, newVal) -> updateQuotePreview());
        limitPriceBox.getChildren().addAll(limitPriceLbl, limitPriceField);
        limitPriceBox.setVisible(false);
        limitPriceBox.setManaged(false);

        // 5. Quantity input
        VBox qtyBox = new VBox(6);
        Label qtyLbl = new Label("Number of Units (Quantity):");
        qtyLbl.getStyleClass().add("form-label");
        quantityField = new TextField("1");
        quantityField.getStyleClass().add("text-input");
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> updateQuotePreview());
        qtyBox.getChildren().addAll(qtyLbl, quantityField);

        submitOrderBtn = new Button("Confirm & Execute Trade");
        submitOrderBtn.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        submitOrderBtn.setMaxWidth(Double.MAX_VALUE);
        submitOrderBtn.setOnAction(e -> handleOrderSubmission());

        formCard.getChildren().addAll(formTitle, assetBox, sideBox, typeBox, limitPriceBox, qtyBox, submitOrderBtn);

        // Right Order Review Breakdown Card
        VBox reviewCard = new VBox(18);
        reviewCard.getStyleClass().add("content-card");
        reviewCard.setPadding(new Insets(24));
        reviewCard.setPrefWidth(380);

        Label reviewTitle = new Label("Order Estimation & Fees");
        reviewTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        previewPriceLabel = new Label("₹0.00");
        previewGrossLabel = new Label("₹0.00");
        previewFeeLabel = new Label("₹0.00");
        previewNetLabel = new Label("₹0.00");
        previewNetLabel.getStyleClass().add("font-bold");
        previewBalanceLabel = new Label("₹0.00");

        addRow(grid, 0, "Quoted Price:", previewPriceLabel);
        addRow(grid, 1, "Gross Order Value:", previewGrossLabel);
        addRow(grid, 2, "Simulated Fee (0.10%):", previewFeeLabel);
        addRow(grid, 3, "Total Net Amount:", previewNetLabel);
        addRow(grid, 4, "Your Available Cash / Units:", previewBalanceLabel);

        validationMessageLabel = new Label("");
        validationMessageLabel.getStyleClass().add("text-negative");
        validationMessageLabel.setWrapText(true);

        VBox noticeBox = new VBox(4);
        noticeBox.getStyleClass().add("info-banner");
        noticeBox.setPadding(new Insets(10));
        Label nTitle = new Label("Virtual Brokerage Rule:");
        nTitle.getStyleClass().add("font-semibold");
        Label nDesc = new Label("Buys require Total Cost = Gross + Fee. Sells deliver Proceeds = Gross - Fee. Atomic persistence ensures balance integrity.");
        nDesc.setWrapText(true);
        nDesc.getStyleClass().add("text-muted");
        noticeBox.getChildren().addAll(nTitle, nDesc);

        reviewCard.getChildren().addAll(reviewTitle, grid, validationMessageLabel, noticeBox);

        splitPane.getChildren().addAll(formCard, reviewCard);
        container.getChildren().add(splitPane);

        updateQuotePreview();
    }

    private void addRow(GridPane grid, int row, String labelText, Label valLabel) {
        Label l = new Label(labelText);
        l.getStyleClass().add("text-muted");
        grid.add(l, 0, row);
        grid.add(valLabel, 1, row);
    }

    private void updateQuotePreview() {
        Asset asset = assetCombo.getValue();
        if (asset == null) return;

        Trader trader = context.getTrader();
        TransactionType side = buyRadio.isSelected() ? TransactionType.BUY : TransactionType.SELL;
        boolean isLimit = limitRadio.isSelected();

        int qty = 1;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
        } catch (Exception ignored) {
            qty = 0;
        }

        BigDecimal price = asset.getCurrentPrice();
        previewPriceLabel.setText(UIContext.formatMoney(price));

        if (side == TransactionType.BUY) {
            previewBalanceLabel.setText(UIContext.formatMoney(trader.getCashBalance()) + " (Cash)");
        } else {
            int held = trader.getPortfolio().getQuantityHeld(asset.getAssetId());
            previewBalanceLabel.setText(held + " unit(s) held");
        }

        if (qty <= 0) {
            previewGrossLabel.setText("₹0.00");
            previewFeeLabel.setText("₹0.00");
            previewNetLabel.setText("₹0.00");
            validationMessageLabel.setText("⚠️ Quantity must be a positive integer greater than zero.");
            submitOrderBtn.setDisable(true);
            return;
        }

        try {
            OrderQuote quote = context.getTradingService().getQuote(side, asset, qty);
            previewGrossLabel.setText(UIContext.formatMoney(quote.grossValue()));
            previewFeeLabel.setText(UIContext.formatMoney(quote.fee()));
            previewNetLabel.setText(UIContext.formatMoney(quote.netAmount()));

            if (side == TransactionType.BUY) {
                if (trader.getCashBalance().compareTo(quote.netAmount()) < 0) {
                    validationMessageLabel.setText("⚠️ Insufficient virtual cash. You need "
                            + UIContext.formatMoney(quote.netAmount()) + " but have "
                            + UIContext.formatMoney(trader.getCashBalance()));
                    submitOrderBtn.setDisable(true);
                    return;
                }
            } else {
                int held = trader.getPortfolio().getQuantityHeld(asset.getAssetId());
                if (held < qty) {
                    validationMessageLabel.setText("⚠️ Insufficient units. You hold " + held
                            + " unit(s) of " + asset.getSymbol() + " but tried to sell " + qty);
                    submitOrderBtn.setDisable(true);
                    return;
                }
            }

            validationMessageLabel.setText("");
            submitOrderBtn.setDisable(false);

        } catch (TradingException e) {
            validationMessageLabel.setText("⚠️ " + e.getMessage());
            submitOrderBtn.setDisable(true);
        }
    }

    private void handleOrderSubmission() {
        Asset asset = assetCombo.getValue();
        if (asset == null) return;

        TransactionType side = buyRadio.isSelected() ? TransactionType.BUY : TransactionType.SELL;
        boolean isLimit = limitRadio.isSelected();
        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Quantity", "Please enter a valid positive whole number.");
            return;
        }

        TradingService service = context.getTradingService();

        try {
            if (!isLimit) {
                // Market Order
                Transaction tx = service.placeMarketOrder(side, asset, qty);
                context.notifyDataChanged();
                showAlert(Alert.AlertType.INFORMATION, "Trade Executed Successfully",
                        "Market order executed!\n"
                                + "Side: " + tx.getType() + "\n"
                                + "Asset: " + asset.getSymbol() + " (" + asset.getName() + ")\n"
                                + "Units: " + qty + "\n"
                                + "Execution Price: " + UIContext.formatMoney(tx.getExecutionPrice()) + "\n"
                                + "Fee: " + UIContext.formatMoney(tx.getFee()) + "\n"
                                + "Net Amount: " + UIContext.formatMoney(tx.getNetAmount()));
            } else {
                // Limit Order
                BigDecimal limitPrice;
                try {
                    limitPrice = new BigDecimal(limitPriceField.getText().trim());
                    if (limitPrice.signum() <= 0) throw new IllegalArgumentException();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Limit Price", "Please enter a valid limit price greater than zero.");
                    return;
                }

                Order order = service.placeLimitOrder(side, asset, qty, limitPrice);
                context.notifyDataChanged();

                if (order.getStatus() == OrderStatus.EXECUTED) {
                    showAlert(Alert.AlertType.INFORMATION, "Limit Condition Met & Executed",
                            "The limit condition was immediately met at current market price! Order executed successfully.");
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Limit Order Placed (Pending)",
                            "Limit order ORD ID: " + order.getOrderId() + " submitted as PENDING.\n"
                                    + "It will execute automatically when market price touches or crosses "
                                    + UIContext.formatMoney(limitPrice));
                }
            }

            updateQuotePreview();

        } catch (TradingException e) {
            showAlert(Alert.AlertType.ERROR, "Order Rejection", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
