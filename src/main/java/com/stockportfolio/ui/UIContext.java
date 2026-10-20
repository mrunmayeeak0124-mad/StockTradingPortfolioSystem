package com.stockportfolio.ui;

import com.stockportfolio.model.Asset;
import com.stockportfolio.model.Trader;
import com.stockportfolio.service.AdvisorService;
import com.stockportfolio.service.CsvExportService;
import com.stockportfolio.service.CurrencyService;
import com.stockportfolio.service.FinancialAdvisorService;
import com.stockportfolio.service.GoalPlanningService;
import com.stockportfolio.service.MarketAnalyticsService;
import com.stockportfolio.service.MarketService;
import com.stockportfolio.service.PortfolioService;
import com.stockportfolio.service.TradingService;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import javafx.scene.control.Alert;

/**
 * Shared application context and event bus for all JavaFX view controllers.
 */
public class UIContext {

    private Trader trader;
    private final TradingService tradingService;
    private final MarketService marketService;
    private final MarketAnalyticsService analyticsService;
    private final PortfolioService portfolioService;
    private final AdvisorService advisorService;
    private final GoalPlanningService goalService;
    private final CsvExportService exportService;
    private final CurrencyService currencyService;

    private BiConsumer<String, Object> navigationHandler;
    private final List<Runnable> dataChangeListeners = new ArrayList<>();

    private static final NumberFormat INR_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public UIContext(Trader trader, TradingService tradingService, MarketService marketService,
                     MarketAnalyticsService analyticsService, PortfolioService portfolioService,
                     CurrencyService currencyService) {
        this.trader = trader;
        this.tradingService = tradingService;
        this.marketService = marketService;
        this.analyticsService = analyticsService;
        this.portfolioService = portfolioService;
        this.currencyService = currencyService;
        this.advisorService = new FinancialAdvisorService();
        this.goalService = new GoalPlanningService();
        this.exportService = new CsvExportService();
    }

    public Trader getTrader() { return trader; }
    public void setTrader(Trader trader) {
        if (trader == null) return;
        this.trader = trader;
        this.tradingService.setTrader(trader);
        notifyDataChanged();
    }

    public TradingService getTradingService() { return tradingService; }
    public MarketService getMarketService() { return marketService; }
    public MarketAnalyticsService getAnalyticsService() { return analyticsService; }
    public PortfolioService getPortfolioService() { return portfolioService; }
    public AdvisorService getAdvisorService() { return advisorService; }
    public GoalPlanningService getGoalService() { return goalService; }
    public CsvExportService getExportService() { return exportService; }
    public CurrencyService getCurrencyService() { return currencyService; }

    public void setNavigationHandler(BiConsumer<String, Object> handler) {
        this.navigationHandler = handler;
    }

    public void navigateTo(String viewName) {
        navigateTo(viewName, null);
    }

    public void navigateTo(String viewName, Object param) {
        if (navigationHandler != null) {
            navigationHandler.accept(viewName, param);
        }
    }

    public void addDataChangeListener(Runnable listener) {
        dataChangeListeners.add(listener);
    }

    /** Broadcasts that data has changed (trade executed, deposit made, price updated). */
    public void notifyDataChanged() {
        this.trader = tradingService.getTrader();
        for (Runnable listener : dataChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                // Keep notifying remaining listeners
            }
        }
    }

    // ---------------- UI Display Formatters ----------------

    public static String formatMoney(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        BigDecimal rounded = MoneyUtil.round(amount);
        return "₹" + String.format(Locale.US, "%,.2f", rounded);
    }

    public static String formatPercent(BigDecimal percent) {
        if (percent == null) return "0.00%";
        String prefix = percent.signum() > 0 ? "+" : "";
        return prefix + String.format(Locale.US, "%.2f", percent) + "%";
    }

    public static String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) return "N/A";
        return ldt.format(DATE_TIME_FORMAT);
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
