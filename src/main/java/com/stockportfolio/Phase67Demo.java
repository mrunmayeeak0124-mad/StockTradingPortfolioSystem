package com.stockportfolio;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.model.*;
import com.stockportfolio.provider.MockExchangeRateProvider;
import com.stockportfolio.provider.MockMarketDataProvider;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.service.AccountService;
import com.stockportfolio.service.CurrencyService;
import com.stockportfolio.service.MarketAnalyticsService;
import com.stockportfolio.service.MarketService;
import com.stockportfolio.service.PortfolioService;
import com.stockportfolio.service.TradingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Temporary demo of Phase 6 and Phase 7. Run it ONCE on a FRESH database
 * (delete database/stocktrading.db first). All market data is simulated.
 */
public class Phase67Demo {

    public static void main(String[] args) throws Exception {
        DatabaseManager.initialize();
        Map<String, Asset> assets = new AssetRepository().findAllAsMap();

        AccountService accountService = new AccountService();
        Trader trader = accountService.loadTrader(assets)
                .orElseGet(() -> accountService.createTrader("Mrunmayee", "", new BigDecimal("100000")));
        TradingService trading = new TradingService(trader, assets);
        PortfolioService portfolioService = new PortfolioService();
        MarketService market = new MarketService(new MockMarketDataProvider(), trading);
        MarketAnalyticsService analytics = new MarketAnalyticsService();
        CurrencyService currency = new CurrencyService(new MockExchangeRateProvider());

        Asset reliance = assets.get("A001");
        trading.placeMarketOrder(TransactionType.BUY, reliance, 10);
        trading.placeMarketOrder(TransactionType.BUY, assets.get("A007"), 20);   // NIFTYBEES (ETF)
        trading.placeMarketOrder(TransactionType.BUY, assets.get("A010"), 5);    // Govt Bond 2034

        printPortfolio(trading, portfolioService);
        printMarket(trading, market, analytics, reliance);
        printCurrency(currency);
        printRefresh(trading, market, reliance);
    }

    private static void printPortfolio(TradingService trading, PortfolioService service) {
        Trader trader = trading.getTrader();
        System.out.println("=== PORTFOLIO (Phase 6) ===");
        for (HoldingSummary h : service.getHoldings(trader)) {
            System.out.println(String.format(
                    "%-26s %-5s qty %3d | avg %9s | price %9s | cost %10s | value %10s | P/L %8s (%s%%) | share %s%%",
                    h.name(), h.category(), h.quantity(), h.averageCost(), h.currentPrice(),
                    h.totalCost(), h.marketValue(), h.unrealizedProfitLoss(), h.returnPercent(),
                    h.portfolioSharePercent()));
        }

        PortfolioSummary s = service.getSummary(trader);
        System.out.println("\nCash " + s.cash() + " (" + s.cashPercent() + "%) | Invested "
                + s.holdingsValue() + " (" + s.investedPercent() + "%) | Account value "
                + s.totalAccountValue());
        System.out.println("Cost basis " + s.totalCostBasis() + " | Unrealized P/L " + s.unrealizedProfitLoss()
                + " | Realized P/L " + s.realizedProfitLoss() + " | Return " + s.returnPercent()
                + "% | Open positions " + s.openPositions());
        System.out.println("Allocation: " + s.allocationByCategory());

        DiversificationInfo d = service.getDiversification(trader);
        System.out.println("\nDiversification: " + d.level() + " | positions " + d.positionCount()
                + " | categories " + d.categoryCount() + " | largest " + d.largestHoldingSymbol()
                + " " + d.largestHoldingPercent() + "%");
        System.out.println(d.message());

        List<Transaction> transactions = trading.getTransactions();
        PerformanceSummary p = service.getPerformance(trader, transactions);
        System.out.println("\nPerformance: deposited " + p.totalDeposited() + " | account value "
                + p.accountValue() + " | net P/L " + p.netProfitLoss() + " | return on deposits "
                + p.returnOnDepositsPercent() + "% | fees paid " + p.totalFeesPaid()
                + " | trades " + p.tradeCount());

        System.out.println("\nAccount value history:");
        for (PricePoint point : service.getAccountValueHistory(transactions)) {
            System.out.println("  " + point.date() + " = " + point.price());
        }
    }

    private static void printMarket(TradingService trading, MarketService market,
                                    MarketAnalyticsService analytics, Asset reliance) throws Exception {
        String traderId = trading.getTrader().getUserId();
        System.out.println("\n=== MARKET INSIGHTS (Phase 7) ===");
        System.out.println("Source: " + market.getSourceLabel() + " | simulated: " + market.isSimulated()
                + " | last updated: " + market.getLastUpdated());

        MarketQuote quote = market.getQuote(reliance);
        System.out.println("RELIANCE quote: " + quote.price() + " | previous close " + quote.previousClose()
                + " | change " + quote.change() + " (" + quote.changePercent() + "%)");

        List<PricePoint> history = market.getPriceHistory(reliance);
        System.out.println("History points: " + history.size());
        System.out.println("Latest change: " + show(analytics.priceChange(history)));
        System.out.println("Change over 5 observations: " + show(analytics.percentChange(history, 5)) + "%");
        List<PricePoint> sma = analytics.simpleMovingAverage(history, 5);
        System.out.println("5-point moving average: " + sma.size() + " points, last = "
                + (sma.isEmpty() ? "n/a" : sma.get(sma.size() - 1).price().toString()));
        System.out.println("10,000-point moving average: " + analytics.simpleMovingAverage(history, 10000).size()
                + " points (not enough data)");
        System.out.println("Volatility (std dev of daily changes): " + show(analytics.volatilityPercent(history)) + "%");
        System.out.println(MarketAnalyticsService.HISTORY_DISCLAIMER);

        market.addToWatchlist(traderId, reliance);
        market.addToWatchlist(traderId, trading.getAssets().get("A002"));
        System.out.print("\nWatchlist:");
        for (Asset asset : market.getWatchlistAssets(traderId)) {
            System.out.print(" " + asset.getSymbol());
        }
        System.out.println();

        System.out.print("Search \"bank\":");
        for (Asset asset : market.searchAssets("bank", null)) {
            System.out.print(" [" + asset.getName() + "]");
        }
        System.out.println();
        System.out.print("Filter ETFs:");
        for (Asset asset : market.searchAssets("", AssetCategory.ETF)) {
            System.out.print(" " + asset.getSymbol());
        }
        System.out.println();
    }

    private static void printCurrency(CurrencyService currency) throws Exception {
        System.out.println("\n=== CURRENCY CONVERTER ===");
        ConversionResult a = currency.convert(new BigDecimal("10000"), "INR", "USD");
        System.out.println("10000 INR = " + a.convertedAmount() + " USD (rate " + a.rate().rate()
                + ", " + a.rate().sourceLabel() + ", updated " + a.rate().lastUpdated() + ")");
        ConversionResult b = currency.convert(new BigDecimal("100"), "USD", "INR");
        System.out.println("100 USD = " + b.convertedAmount() + " INR (rate " + b.rate().rate() + ")");
    }

    private static void printRefresh(TradingService trading, MarketService market, Asset reliance)
            throws Exception {
        System.out.println("\n=== PRICE REFRESH (random simulated moves) ===");
        System.out.println("RELIANCE before: " + reliance.getCurrentPrice()
                + " | account value " + trading.getTrader().getTotalAccountValue());
        List<Order> changed = market.refreshPrices();
        System.out.println("RELIANCE after:  " + reliance.getCurrentPrice()
                + " | account value " + trading.getTrader().getTotalAccountValue());
        System.out.println("Pending orders changed by the refresh: " + changed.size());
    }

    private static String show(Optional<BigDecimal> value) {
        return value.map(BigDecimal::toPlainString).orElse("n/a");
    }
}