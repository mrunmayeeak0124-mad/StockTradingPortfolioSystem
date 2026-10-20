package com.stockportfolio.service;

import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service to export transaction history, portfolio holdings, and performance summaries
 * into clean, formatted CSV files.
 */
public class CsvExportService {

    public static final String DEFAULT_EXPORT_DIR = "exports";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Ensures the default exports directory exists and returns its path. */
    public Path getExportDirectory() throws IOException {
        Path dir = Paths.get(DEFAULT_EXPORT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    /**
     * Exports transaction history to a CSV file.
     * Columns: Transaction ID, Timestamp, Type, Asset Symbol, Asset Name, Quantity, Price, Fee, Gross Value, Net Amount, Status
     */
    public Path exportTransactions(List<Transaction> transactions, Path destination) throws IOException {
        if (destination == null) {
            destination = getExportDirectory().resolve("transactions_" + LocalDateTime.now().format(FILE_TS) + ".csv");
        }
        ensureParentDirectory(destination);

        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write("TransactionID,Timestamp,Type,Symbol,AssetName,Quantity,Price,Fee,GrossValue,NetAmount,Status\n");
            for (Transaction tx : transactions) {
                String symbol = tx.getAsset() != null ? tx.getAsset().getSymbol() : "N/A";
                String assetName = tx.getAsset() != null ? escapeCsv(tx.getAsset().getName()) : "N/A";
                writer.write(String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s\n",
                        tx.getTransactionId(),
                        tx.getTimestamp().toString(),
                        tx.getType().name(),
                        symbol,
                        assetName,
                        tx.getQuantity(),
                        tx.getExecutionPrice().toPlainString(),
                        tx.getFee().toPlainString(),
                        tx.getGrossValue().toPlainString(),
                        tx.getNetAmount().toPlainString(),
                        tx.getStatus()
                ));
            }
        }
        return destination;
    }

    /**
     * Exports current open portfolio holdings to a CSV file.
     * Columns: Asset ID, Symbol, Name, Category, Quantity, Avg Cost Basis, Current Price, Total Cost, Market Value, Unrealized P/L, Gain %
     */
    public Path exportHoldings(List<Position> positions, Path destination) throws IOException {
        if (destination == null) {
            destination = getExportDirectory().resolve("portfolio_holdings_" + LocalDateTime.now().format(FILE_TS) + ".csv");
        }
        ensureParentDirectory(destination);

        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write("AssetID,Symbol,Name,Category,Quantity,AvgCost,CurrentPrice,TotalCostBasis,MarketValue,UnrealizedPL,PercentageGain\n");
            for (Position p : positions) {
                writer.write(String.format("%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s\n",
                        p.getAsset().getAssetId(),
                        p.getAsset().getSymbol(),
                        escapeCsv(p.getAsset().getName()),
                        p.getAsset().getCategory().name(),
                        p.getQuantity(),
                        p.getAverageCost().toPlainString(),
                        p.getCurrentPrice().toPlainString(),
                        p.getTotalCostBasis().toPlainString(),
                        p.getMarketValue().toPlainString(),
                        p.getUnrealizedProfitLoss().toPlainString(),
                        p.getReturnPercent().toPlainString() + "%"
                ));
            }
        }
        return destination;
    }

    /**
     * Exports overall portfolio performance and account summary to CSV.
     */
    public Path exportPerformanceSummary(Trader trader, PortfolioSummary summary, Path destination) throws IOException {
        if (destination == null) {
            destination = getExportDirectory().resolve("performance_summary_" + LocalDateTime.now().format(FILE_TS) + ".csv");
        }
        ensureParentDirectory(destination);

        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write("Metric,Value\n");
            writer.write("Trader ID," + trader.getUserId() + "\n");
            writer.write("Trader Name," + escapeCsv(trader.getName()) + "\n");
            writer.write("Report Generated," + LocalDateTime.now().toString() + "\n");
            writer.write("Virtual Cash Balance," + trader.getCashBalance().toPlainString() + "\n");
            writer.write("Total Cost Basis of Open Holdings," + summary.totalCostBasis().toPlainString() + "\n");
            writer.write("Total Market Value of Open Holdings," + summary.holdingsValue().toPlainString() + "\n");
            writer.write("Total Account Value (Cash + Holdings)," + trader.getTotalAccountValue().toPlainString() + "\n");
            writer.write("Unrealized Profit/Loss," + summary.unrealizedProfitLoss().toPlainString() + "\n");
            writer.write("Realized Profit/Loss," + summary.realizedProfitLoss().toPlainString() + "\n");
            writer.write("Total Net Return %," + summary.returnPercent().toPlainString() + "%\n");
            writer.write("Open Positions Count," + summary.openPositions() + "\n");
        }
        return destination;
    }

    private void ensureParentDirectory(Path destination) throws IOException {
        Path parent = destination.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
