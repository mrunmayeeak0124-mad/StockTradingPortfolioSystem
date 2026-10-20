package com.stockportfolio.ui;

import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Transaction History View: complete immutable audit trail of deposits, purchases, and sales.
 * Supports filtering and export to CSV.
 */
public class TransactionsView extends VBox {

    private final UIContext context;
    private final TableView<Transaction> txTable = new TableView<>();
    private final ComboBox<String> typeFilterCombo = new ComboBox<>();
    private final TextField searchField = new TextField();

    public TransactionsView(UIContext context) {
        this.context = context;
        setSpacing(20);
        setPadding(new Insets(28));
        getStyleClass().add("view-container");

        buildHeader();
        buildFilterBar();
        buildTable();

        refreshTransactions();
        context.addDataChangeListener(this::refreshTransactions);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Transaction Audit History");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Complete immutable ledger of all deposits, executions, fees, and realized returns.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button exportBtn = new Button("📥 Export Transactions CSV");
        exportBtn.getStyleClass().addAll("btn", "btn-secondary");
        exportBtn.setOnAction(e -> handleExport());

        HBox header = new HBox(titleBox, exportBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private void buildFilterBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("filter-bar");
        bar.setPadding(new Insets(10, 16, 10, 16));

        Label filterLbl = new Label("Filter by Type:");
        filterLbl.getStyleClass().add("form-label");

        typeFilterCombo.getItems().addAll("All Transactions", "Deposits Only", "Buys Only", "Sells Only");
        typeFilterCombo.setValue("All Transactions");
        typeFilterCombo.getStyleClass().add("combo-box");
        typeFilterCombo.setOnAction(e -> refreshTransactions());

        searchField.setPromptText("Filter by asset symbol or ID...");
        searchField.setPrefWidth(260);
        searchField.getStyleClass().add("text-input");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshTransactions());

        bar.getChildren().addAll(filterLbl, typeFilterCombo, searchField);
        getChildren().add(bar);
    }

    private void buildTable() {
        txTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(txTable, Priority.ALWAYS);

        TableColumn<Transaction, String> idCol = new TableColumn<>("Tx ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionId()));
        idCol.setPrefWidth(110);

        TableColumn<Transaction, String> timeCol = new TableColumn<>("Date & Time");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatDateTime(d.getValue().getTimestamp())));
        timeCol.setPrefWidth(140);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));
        typeCol.setPrefWidth(80);

        TableColumn<Transaction, String> assetCol = new TableColumn<>("Asset");
        assetCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAsset() != null ? d.getValue().getAsset().getSymbol() : "Wallet Deposit"));
        assetCol.setPrefWidth(120);

        TableColumn<Transaction, String> qtyCol = new TableColumn<>("Units");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getQuantity() > 0 ? String.valueOf(d.getValue().getQuantity()) : "-"));
        qtyCol.setPrefWidth(60);

        TableColumn<Transaction, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getExecutionPrice().signum() > 0 ? UIContext.formatMoney(d.getValue().getExecutionPrice()) : "-"));
        priceCol.setPrefWidth(100);

        TableColumn<Transaction, String> feeCol = new TableColumn<>("Fee");
        feeCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getFee())));
        feeCol.setPrefWidth(80);

        TableColumn<Transaction, String> grossCol = new TableColumn<>("Gross");
        grossCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getGrossValue())));
        grossCol.setPrefWidth(100);

        TableColumn<Transaction, String> netCol = new TableColumn<>("Net Amount");
        netCol.setCellValueFactory(d -> new SimpleStringProperty(UIContext.formatMoney(d.getValue().getNetAmount())));
        netCol.setPrefWidth(110);

        TableColumn<Transaction, String> plCol = new TableColumn<>("Realized P/L");
        plCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getType() == TransactionType.SELL ? UIContext.formatMoney(d.getValue().getRealizedProfitLoss()) : "-"));
        plCol.setPrefWidth(100);

        TableColumn<Transaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setPrefWidth(90);

        txTable.getColumns().addAll(idCol, timeCol, typeCol, assetCol, qtyCol, priceCol, feeCol, grossCol, netCol, plCol, statusCol);
        getChildren().add(txTable);
    }

    private void refreshTransactions() {
        List<Transaction> list = context.getTradingService().getTransactions();
        String typeSel = typeFilterCombo.getValue();
        if ("Deposits Only".equals(typeSel)) {
            list = list.stream().filter(t -> t.getType() == TransactionType.DEPOSIT).toList();
        } else if ("Buys Only".equals(typeSel)) {
            list = list.stream().filter(t -> t.getType() == TransactionType.BUY).toList();
        } else if ("Sells Only".equals(typeSel)) {
            list = list.stream().filter(t -> t.getType() == TransactionType.SELL).toList();
        }

        String search = searchField.getText().trim().toLowerCase();
        if (!search.isEmpty()) {
            list = list.stream().filter(t -> {
                boolean idMatch = t.getTransactionId().toLowerCase().contains(search);
                boolean assetMatch = t.getAsset() != null &&
                        (t.getAsset().getSymbol().toLowerCase().contains(search) || t.getAsset().getName().toLowerCase().contains(search));
                return idMatch || assetMatch;
            }).toList();
        }

        txTable.setItems(FXCollections.observableArrayList(list));
    }

    private void handleExport() {
        try {
            Path path = context.getExportService().exportTransactions(
                    context.getTradingService().getTransactions(), null);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("CSV Export Successful");
            alert.setHeaderText("Transaction Audit Ledger Exported");
            alert.setContentText("File written to:\n" + path.toAbsolutePath());
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setContentText("Could not write CSV file: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
