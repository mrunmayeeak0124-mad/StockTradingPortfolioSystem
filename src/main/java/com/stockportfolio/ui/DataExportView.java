package com.stockportfolio.ui;

import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Trader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Data Export & Report Generation View.
 * Exports audit ledgers, current portfolio positions, and financial summaries to standard CSV formats.
 */
public class DataExportView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);
    private Label statusLabel;

    public DataExportView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildExportCards();
        buildStatusCard();

        setContent(container);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Data Export & Audit Report Generator");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Export your transactions, holdings, and account performance summaries to CSV spreadsheets.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    private void buildExportCards() {
        VBox card = new VBox(18);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(22));

        Label title = new Label("Available Export Formats");
        title.getStyleClass().add("section-title");

        VBox list = new VBox(14);
        list.getChildren().addAll(
                createExportItem("Transaction Audit Ledger (CSV)",
                        "Complete log of all deposits, market executions, limit fills, fees paid, and realized P/L.",
                        "Export Transactions", this::exportTransactions),
                createExportItem("Current Portfolio Open Holdings (CSV)",
                        "Snapshot of all currently open positions with weighted cost basis, current prices, and unrealized gains.",
                        "Export Holdings", this::exportHoldings),
                createExportItem("Comprehensive Performance Valuation Report (CSV)",
                        "Executive summary of total account value, cash balance, realized/unrealized return percentages, and metrics.",
                        "Export Summary", this::exportSummary)
        );

        card.getChildren().addAll(title, list);
        container.getChildren().add(card);
    }

    private HBox createExportItem(String title, String desc, String btnText, Runnable action) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("metric-card");
        row.setPadding(new Insets(16));

        VBox text = new VBox(4);
        Label t = new Label(title);
        t.getStyleClass().add("font-semibold");
        t.setStyle("-fx-font-size: 15px;");

        Label d = new Label(desc);
        d.setWrapText(true);
        d.getStyleClass().add("text-muted");
        text.getChildren().addAll(t, d);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button btn = new Button("📥 " + btnText);
        btn.getStyleClass().addAll("btn", "btn-primary");
        btn.setOnAction(e -> action.run());

        row.getChildren().addAll(text, btn);
        return row;
    }

    private void buildStatusCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Export Storage Location");
        title.getStyleClass().add("section-title");

        try {
            Path dir = context.getExportService().getExportDirectory().toAbsolutePath();
            Label pathLabel = new Label("Export Directory: " + dir);
            pathLabel.getStyleClass().add("font-semibold");

            statusLabel = new Label("Ready to generate reports.");
            statusLabel.getStyleClass().add("text-muted");

            Button openFolderBtn = new Button("📂 Open Exports Folder in File Explorer");
            openFolderBtn.getStyleClass().addAll("btn", "btn-secondary");
            openFolderBtn.setOnAction(e -> {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(dir.toFile());
                    }
                } catch (Exception ex) {
                    showNotice("Notice", "Folder located at: " + dir);
                }
            });

            card.getChildren().addAll(title, pathLabel, statusLabel, openFolderBtn);
        } catch (IOException e) {
            card.getChildren().add(new Label("Failed to resolve exports directory."));
        }

        container.getChildren().add(card);
    }

    private void exportTransactions() {
        statusLabel.setText("⏳ Generating transactions CSV report on background thread...");
        new Thread(() -> {
            try {
                Path path = context.getExportService().exportTransactions(
                        context.getTradingService().getTransactions(), null);
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Last Generated: " + path.getFileName() + " at " + path.toAbsolutePath());
                    showNotice("Export Succeeded (Multithreaded)", "Transactions saved to:\n" + path.toAbsolutePath());
                });
            } catch (IOException e) {
                Platform.runLater(() -> showNotice("Export Error", "Failed to write CSV: " + e.getMessage()));
            }
        }, "Transactions-Export-Worker").start();
    }

    private void exportHoldings() {
        statusLabel.setText("⏳ Generating portfolio holdings CSV report on background thread...");
        new Thread(() -> {
            try {
                Path path = context.getExportService().exportHoldings(
                        context.getTrader().getPortfolio().getPositions(), null);
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Last Generated: " + path.getFileName() + " at " + path.toAbsolutePath());
                    showNotice("Export Succeeded (Multithreaded)", "Portfolio holdings saved to:\n" + path.toAbsolutePath());
                });
            } catch (IOException e) {
                Platform.runLater(() -> showNotice("Export Error", "Failed to write CSV: " + e.getMessage()));
            }
        }, "Holdings-Export-Worker").start();
    }

    private void exportSummary() {
        statusLabel.setText("⏳ Generating performance summary CSV report on background thread...");
        new Thread(() -> {
            try {
                Trader trader = context.getTrader();
                PortfolioSummary summary = context.getPortfolioService().getPortfolioSummary(trader);
                Path path = context.getExportService().exportPerformanceSummary(trader, summary, null);
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Last Generated: " + path.getFileName() + " at " + path.toAbsolutePath());
                    showNotice("Export Succeeded (Multithreaded)", "Performance report saved to:\n" + path.toAbsolutePath());
                });
            } catch (IOException e) {
                Platform.runLater(() -> showNotice("Export Error", "Failed to write CSV: " + e.getMessage()));
            }
        }, "Summary-Export-Worker").start();
    }

    private void showNotice(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
