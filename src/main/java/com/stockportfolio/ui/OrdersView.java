package com.stockportfolio.ui;

import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.OrderStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Order Management View: displays order history, tracks pending limit orders,
 * allows order cancellation, and triggers pending order execution checks.
 */
public class OrdersView extends VBox {

    private final UIContext context;
    private final TableView<Order> orderTable = new TableView<>();
    private final ComboBox<String> statusFilterCombo = new ComboBox<>();

    public OrdersView(UIContext context) {
        this.context = context;
        setSpacing(20);
        setPadding(new Insets(28));
        getStyleClass().add("view-container");

        buildHeader();
        buildFilterBar();
        buildTable();
        buildInfoBanner();

        refreshOrders();
        context.addDataChangeListener(this::refreshOrders);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Order Management Book");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Audit all submitted market and limit orders. Cancel pending limit orders anytime.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button processBtn = new Button("🔄 Re-evaluate Pending Orders");
        processBtn.getStyleClass().addAll("btn", "btn-secondary");
        processBtn.setOnAction(e -> {
            List<Order> executed = context.getTradingService().processPendingOrders();
            context.notifyDataChanged();
            if (!executed.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Pending Orders Processed",
                        executed.size() + " pending order(s) satisfied market conditions and were executed/updated.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Evaluation Complete",
                        "All pending orders evaluated. Market conditions did not trigger any executions.");
            }
        });

        HBox header = new HBox(titleBox, processBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private void buildFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("filter-bar");
        bar.setPadding(new Insets(10, 16, 10, 16));

        Label filterLbl = new Label("Filter by Status:");
        filterLbl.getStyleClass().add("form-label");

        statusFilterCombo.getItems().addAll("All Orders", "Pending Only", "Executed Only", "Cancelled Only", "Rejected Only");
        statusFilterCombo.setValue("All Orders");
        statusFilterCombo.getStyleClass().add("combo-box");
        statusFilterCombo.setOnAction(e -> refreshOrders());

        bar.getChildren().addAll(filterLbl, statusFilterCombo);
        getChildren().add(bar);
    }

    private void buildTable() {
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        TableColumn<Order, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderId()));
        idCol.setPrefWidth(120);

        TableColumn<Order, String> assetCol = new TableColumn<>("Asset");
        assetCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAsset().getSymbol() + " (" + data.getValue().getAsset().getName() + ")"));
        assetCol.setPrefWidth(200);

        TableColumn<Order, String> sideCol = new TableColumn<>("Side");
        sideCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSide().name()));
        sideCol.setPrefWidth(80);

        TableColumn<Order, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        qtyCol.setPrefWidth(80);

        TableColumn<Order, String> typeCol = new TableColumn<>("Order Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderType().name()));
        typeCol.setPrefWidth(100);

        TableColumn<Order, String> limitCol = new TableColumn<>("Limit Price");
        limitCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getLimitPrice() != null ? UIContext.formatMoney(data.getValue().getLimitPrice()) : "N/A"));
        limitCol.setPrefWidth(110);

        TableColumn<Order, String> timeCol = new TableColumn<>("Submitted At");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                UIContext.formatDateTime(data.getValue().getSubmittedAt())));
        timeCol.setPrefWidth(160);

        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        statusCol.setPrefWidth(110);

        // Cancel Action Column
        TableColumn<Order, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("✕ Cancel");

            {
                cancelBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                cancelBtn.setOnAction(e -> {
                    Order ord = getTableView().getItems().get(getIndex());
                    try {
                        context.getTradingService().cancelOrder(ord.getOrderId());
                        context.notifyDataChanged();
                        showAlert(Alert.AlertType.INFORMATION, "Order Cancelled",
                                "Order " + ord.getOrderId() + " has been cancelled. Cancelled orders will never execute.");
                    } catch (TradingException ex) {
                        showAlert(Alert.AlertType.ERROR, "Cancellation Failed", ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Order ord = getTableView().getItems().get(getIndex());
                    if (ord.getStatus() == OrderStatus.PENDING) {
                        setGraphic(cancelBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        orderTable.getColumns().addAll(idCol, assetCol, sideCol, qtyCol, typeCol, limitCol, timeCol, statusCol, actionCol);
        getChildren().add(orderTable);
    }

    private void buildInfoBanner() {
        HBox banner = new HBox(12);
        banner.getStyleClass().add("info-banner");
        banner.setPadding(new Insets(10, 14, 10, 14));
        Label l = new Label("💡 Limit Order Rules: A Limit BUY executes when market price ≤ limit price. A Limit SELL executes when market price ≥ limit price. Once cancelled, an order is permanently locked and cannot execute.");
        l.setWrapText(true);
        l.getStyleClass().add("text-muted");
        banner.getChildren().add(l);
        getChildren().add(banner);
    }

    private void refreshOrders() {
        List<Order> orders = context.getTradingService().getOrders();
        String sel = statusFilterCombo.getValue();
        if ("Pending Only".equals(sel)) {
            orders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).toList();
        } else if ("Executed Only".equals(sel)) {
            orders = orders.stream().filter(o -> o.getStatus() == OrderStatus.EXECUTED).toList();
        } else if ("Cancelled Only".equals(sel)) {
            orders = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).toList();
        } else if ("Rejected Only".equals(sel)) {
            orders = orders.stream().filter(o -> o.getStatus() == OrderStatus.REJECTED).toList();
        }
        orderTable.setItems(FXCollections.observableArrayList(orders));
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
