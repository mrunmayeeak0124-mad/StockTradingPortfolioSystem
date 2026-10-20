package com.stockportfolio;

import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.Bond;
import com.stockportfolio.model.DiversificationInfo;
import com.stockportfolio.model.ETF;
import com.stockportfolio.model.HoldingSummary;
import com.stockportfolio.model.PerformanceSummary;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.model.Stock;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
import com.stockportfolio.service.PortfolioService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the portfolio figures. No database is needed: everything is built in memory.
 * Base example: 10 units bought for a total cost of 1001.00 (price 100 + 1.00 fee), now priced 110.00.
 */
class PortfolioServiceTest {

    private final PortfolioService service = new PortfolioService();

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }

    private static Stock stock(String id, String symbol, String price) {
        return new Stock(id, "Test " + symbol, symbol, "NSE", "INR", "Tech", new BigDecimal(price));
    }

    private static ETF etf(String id, String symbol, String price) {
        return new ETF(id, "Test " + symbol, symbol, "INR", "Large Cap", "Test Index", new BigDecimal(price));
    }

    private static Bond bond(String id, String price) {
        return new Bond(id, "Test Bond " + id, "Test Issuer", "INR", new BigDecimal("1000"),
                new BigDecimal("7.00"), LocalDate.of(2030, 1, 1), new BigDecimal(price));
    }

    private static Trader newTrader(String cash) {
        return new Trader("T1", "Test Trader", "", new BigDecimal(cash));
    }

    // 1
    @Test
    void holdingRowShowsCostValueAndProfitLoss() {
        Trader trader = newTrader("5000.00");
        trader.getPortfolio().recordBuy(stock("S1", "AAA", "110.00"), 10, new BigDecimal("1001.00"));

        List<HoldingSummary> rows = service.getHoldings(trader);

        assertEquals(1, rows.size());
        HoldingSummary row = rows.get(0);
        assertEquals("AAA", row.symbol());
        assertEquals(10, row.quantity());
        assertMoney("100.10", row.averageCost());          // 1001.00 / 10
        assertMoney("110.00", row.currentPrice());
        assertMoney("1001.00", row.totalCost());
        assertMoney("1100.00", row.marketValue());         // 10 x 110.00
        assertMoney("99.00", row.unrealizedProfitLoss());  // 1100.00 - 1001.00
        assertMoney("9.89", row.returnPercent());          // 99.00 / 1001.00 x 100
        assertMoney("100.00", row.portfolioSharePercent());
    }

    // 2
    @Test
    void summarySeparatesCashFromInvestments() {
        Trader trader = newTrader("5000.00");
        trader.getPortfolio().recordBuy(stock("S1", "AAA", "110.00"), 10, new BigDecimal("1001.00"));

        PortfolioSummary summary = service.getSummary(trader);

        assertMoney("5000.00", summary.cash());
        assertMoney("1100.00", summary.holdingsValue());
        assertMoney("6100.00", summary.totalAccountValue());     // cash + holdings
        assertMoney("81.97", summary.cashPercent());             // 5000 / 6100
        assertMoney("18.03", summary.investedPercent());         // 1100 / 6100
        assertMoney("1001.00", summary.totalCostBasis());
        assertMoney("99.00", summary.unrealizedProfitLoss());
        assertMoney("0.00", summary.realizedProfitLoss());
        assertEquals(1, summary.openPositions());
        assertMoney("100.00", summary.allocationByCategory().get(AssetCategory.STOCK));
    }

    // 3
    @Test
    void emptyPortfolioIsHandledSafely() {
        Trader trader = newTrader("5000.00");

        assertTrue(service.getHoldings(trader).isEmpty());

        PortfolioSummary summary = service.getSummary(trader);
        assertMoney("5000.00", summary.totalAccountValue());
        assertMoney("0.00", summary.holdingsValue());
        assertMoney("100.00", summary.cashPercent());
        assertMoney("0.00", summary.investedPercent());
        assertMoney("0.00", summary.returnPercent());            // no division by zero
        assertTrue(summary.allocationByCategory().isEmpty());

        assertEquals(DiversificationInfo.Level.NO_HOLDINGS, service.getDiversification(trader).level());

        PerformanceSummary performance = service.getPerformance(trader, List.of());
        assertMoney("0.00", performance.returnOnDepositsPercent());   // nothing deposited: no division by zero

        assertTrue(service.getAccountValueHistory(List.of()).isEmpty());
    }

    // 4
    @Test
    void allocationAndConcentratedDiversification() {
        Trader trader = newTrader("1000.00");
        trader.getPortfolio().recordBuy(stock("S1", "AAA", "110.00"), 10, new BigDecimal("1001.00"));  // value 1100.00
        trader.getPortfolio().recordBuy(etf("E1", "FUND", "50.00"), 10, new BigDecimal("500.00"));     // value 500.00

        PortfolioSummary summary = service.getSummary(trader);
        assertMoney("68.75", summary.allocationByCategory().get(AssetCategory.STOCK));   // 1100 / 1600
        assertMoney("31.25", summary.allocationByCategory().get(AssetCategory.ETF));     // 500 / 1600

        DiversificationInfo info = service.getDiversification(trader);
        assertEquals(DiversificationInfo.Level.CONCENTRATED, info.level());
        assertEquals("AAA", info.largestHoldingSymbol());
        assertMoney("68.75", info.largestHoldingPercent());
        assertEquals(2, info.categoryCount());
        assertTrue(info.message().contains("cannot remove overall market risk"));
    }

    // 5
    @Test
    void spreadAcrossCategoriesIsDiversified() {
        Trader trader = newTrader("1000.00");
        trader.getPortfolio().recordBuy(stock("S1", "AAA", "30.00"), 10, new BigDecimal("300.00"));    // 300
        trader.getPortfolio().recordBuy(stock("S2", "BBB", "30.00"), 10, new BigDecimal("300.00"));    // 300
        trader.getPortfolio().recordBuy(etf("E1", "FUND", "20.00"), 10, new BigDecimal("200.00"));     // 200
        trader.getPortfolio().recordBuy(bond("B1", "20.00"), 10, new BigDecimal("200.00"));            // 200

        DiversificationInfo info = service.getDiversification(trader);

        assertEquals(DiversificationInfo.Level.DIVERSIFIED, info.level());
        assertEquals(4, info.positionCount());
        assertEquals(3, info.categoryCount());
        assertMoney("30.00", info.largestHoldingPercent());       // 300 / 1000
    }

    // 6
    @Test
    void singleCategoryIsModerate() {
        Trader trader = newTrader("1000.00");
        trader.getPortfolio().recordBuy(stock("S1", "AAA", "30.00"), 10, new BigDecimal("300.00"));
        trader.getPortfolio().recordBuy(stock("S2", "BBB", "30.00"), 10, new BigDecimal("300.00"));

        DiversificationInfo info = service.getDiversification(trader);

        assertEquals(DiversificationInfo.Level.MODERATE, info.level());   // 50% each, but only one category
        assertEquals(1, info.categoryCount());
    }

    // 7
    @Test
    void performanceSummaryReconcilesWithProfitLoss() throws Exception {
        Trader trader = newTrader("10000.00");
        Stock aaa = stock("S1", "AAA", "110.00");
        trader.withdraw(new BigDecimal("1001.00"));                                       // cash 8999.00
        trader.getPortfolio().recordBuy(aaa, 10, new BigDecimal("1001.00"));

        Transaction deposit = Transaction.forDeposit("T1", new BigDecimal("10000.00"));
        Transaction buy = Transaction.restore("TXN-1", "T1", "ORD-1", aaa, TransactionType.BUY, 10,
                new BigDecimal("100.00"), new BigDecimal("1.00"), new BigDecimal("1000.00"),
                new BigDecimal("-1001.00"), new BigDecimal("0.00"), LocalDateTime.now(), "COMPLETED");

        PerformanceSummary p = service.getPerformance(trader, List.of(deposit, buy));

        assertMoney("10000.00", p.totalDeposited());
        assertMoney("10099.00", p.accountValue());                // 8999.00 cash + 1100.00 holdings
        assertMoney("99.00", p.netProfitLoss());                  // 10099.00 - 10000.00
        assertMoney("0.99", p.returnOnDepositsPercent());         // 99.00 / 10000.00 x 100
        assertMoney("0.00", p.realizedProfitLoss());
        assertMoney("99.00", p.unrealizedProfitLoss());
        assertMoney("1.00", p.totalFeesPaid());
        assertEquals(1, p.tradeCount());
        assertMoney("99.00", p.realizedProfitLoss().add(p.unrealizedProfitLoss()));   // = net P/L
    }

    // 8
    @Test
    void accountValueHistoryUsesPastPricesAndTodaysCurrentPrice() {
        LocalDate today = LocalDate.of(2026, 10, 20);
        Stock aaa = stock("S1", "AAA", "110.00");

        Transaction deposit = Transaction.restore("TXN-1", "T1", null, null, TransactionType.DEPOSIT, 0,
                new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("1000.00"),
                new BigDecimal("1000.00"), new BigDecimal("0.00"),
                today.minusDays(2).atTime(10, 0), "COMPLETED");
        Transaction buy = Transaction.restore("TXN-2", "T1", "ORD-1", aaa, TransactionType.BUY, 5,
                new BigDecimal("100.00"), new BigDecimal("0.00"), new BigDecimal("500.00"),
                new BigDecimal("-500.00"), new BigDecimal("0.00"),
                today.minusDays(1).atTime(10, 0), "COMPLETED");

        NavigableMap<LocalDate, BigDecimal> prices = new TreeMap<>();
        prices.put(today.minusDays(2), new BigDecimal("100.00"));
        prices.put(today.minusDays(1), new BigDecimal("100.00"));
        Map<String, NavigableMap<LocalDate, BigDecimal>> history = new HashMap<>();
        history.put("S1", prices);
        Map<String, BigDecimal> currentPrices = Map.of("S1", new BigDecimal("110.00"));

        List<PricePoint> result = PortfolioService.calculateValueHistory(
                List.of(deposit, buy), history, currentPrices, today);

        assertEquals(3, result.size());
        assertEquals(today.minusDays(2), result.get(0).date());
        assertMoney("1000.00", result.get(0).price());     // only the deposit so far
        assertMoney("1000.00", result.get(1).price());     // cash 500.00 + 5 x 100.00
        assertEquals(today, result.get(2).date());
        assertMoney("1050.00", result.get(2).price());     // cash 500.00 + 5 x 110.00 (today's current price)
    }
}