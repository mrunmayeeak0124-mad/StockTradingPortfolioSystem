package com.stockportfolio;

import com.stockportfolio.database.DatabaseManager;
import com.stockportfolio.exception.InsufficientFundsException;
import com.stockportfolio.exception.InsufficientHoldingsException;
import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.OrderQuote;
import com.stockportfolio.model.OrderStatus;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.repository.OrderRepository;
import com.stockportfolio.repository.TraderRepository;
import com.stockportfolio.service.AccountService;
import com.stockportfolio.service.TradingService;
import com.stockportfolio.util.FeeCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the trading rules. Every test uses its own temporary database.
 * Setup: cash 10000.00, asset A001 priced at 100.00, simulated fee 0.10%.
 *   Buy 10 @ 100.00  -> value 1000.00, fee 1.00, total cost 1001.00
 */
class TradingServiceTest {

    private Map<String, Asset> assets;
    private TradingService service;
    private Asset stock;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        DatabaseManager.setDatabasePath(tempDir.resolve("test.db").toString());
        DatabaseManager.initialize();
        FeeCalculator.setFeeRatePercent(new BigDecimal("0.10"));

        assets = new AssetRepository().findAllAsMap();
        Trader trader = new AccountService().createTrader("Test Trader", "", new BigDecimal("10000.00"));
        service = new TradingService(trader, assets);
        stock = assets.get("A001");
        service.updateAssetPrice(stock, new BigDecimal("100.00"));
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.setDatabasePath("database/stocktrading.db");
        FeeCalculator.setFeeRatePercent(new BigDecimal("0.10"));
    }

    /** Compares money by value, so 8999.0 and 8999.00 count as equal. */
    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }

    private int held() {
        return service.getTrader().getPortfolio().getQuantityHeld("A001");
    }

    /** Buy 10 @ 100 (cost 1001.00), price rises to 110, sell 4. */
    private void buyTenAt100SellFourAt110() throws TradingException {
        service.placeMarketOrder(TransactionType.BUY, stock, 10);
        service.updateAssetPrice(stock, new BigDecimal("110.00"));
        service.placeMarketOrder(TransactionType.SELL, stock, 4);
    }

    // 1
    @Test
    void validBuyDeductsCashAndCreatesHolding() throws TradingException {
        Transaction t = service.placeMarketOrder(TransactionType.BUY, stock, 10);

        assertMoney("1.00", t.getFee());
        assertMoney("-1001.00", t.getNetAmount());
        assertMoney("8999.00", service.getTrader().getCashBalance());
        Position p = service.getTrader().getPortfolio().getPosition("A001");
        assertNotNull(p);
        assertEquals(10, p.getQuantity());
        assertMoney("1001.00", p.getTotalCostBasis());
    }

    // 2
    @Test
    void buyWithInsufficientCashFailsSafely() {
        assertThrows(InsufficientFundsException.class,
                () -> service.placeMarketOrder(TransactionType.BUY, stock, 1000));

        assertMoney("10000.00", service.getTrader().getCashBalance());
        assertTrue(service.getTrader().getPortfolio().isEmpty());
        assertEquals(0, service.getOrders().size());
        assertEquals(1, service.getTransactions().size());      // only the starting deposit
    }

    // 3
    @Test
    void validSaleUpdatesCashAndHoldings() throws TradingException {
        service.placeMarketOrder(TransactionType.BUY, stock, 10);
        service.updateAssetPrice(stock, new BigDecimal("110.00"));
        Transaction sale = service.placeMarketOrder(TransactionType.SELL, stock, 4);

        assertMoney("0.44", sale.getFee());                     // 440.00 x 0.10%
        assertMoney("439.56", sale.getNetAmount());             // 440.00 - 0.44
        assertMoney("9438.56", service.getTrader().getCashBalance());   // 8999.00 + 439.56
        assertEquals(6, held());
    }

    // 4
    @Test
    void sellMoreThanHeldIsRejected() throws TradingException {
        service.placeMarketOrder(TransactionType.BUY, stock, 10);

        assertThrows(InsufficientHoldingsException.class,
                () -> service.placeMarketOrder(TransactionType.SELL, stock, 11));
        assertEquals(10, held());
        assertMoney("8999.00", service.getTrader().getCashBalance());

        Asset notOwned = assets.get("A002");
        assertThrows(InsufficientHoldingsException.class,
                () -> service.placeMarketOrder(TransactionType.SELL, notOwned, 1));
    }

    // 5
    @Test
    void multiplePurchasesUpdateAverageCost() throws TradingException {
        service.placeMarketOrder(TransactionType.BUY, stock, 10);       // cost 1001.00
        service.updateAssetPrice(stock, new BigDecimal("120.00"));
        service.placeMarketOrder(TransactionType.BUY, stock, 10);       // 1200.00 + 1.20 = 1201.20

        Position p = service.getTrader().getPortfolio().getPosition("A001");
        assertEquals(20, p.getQuantity());
        assertMoney("2202.20", p.getTotalCostBasis());                  // 1001.00 + 1201.20
        assertMoney("110.11", p.getAverageCost());                      // 2202.20 / 20
        assertMoney("7797.80", service.getTrader().getCashBalance());   // 10000 - 2202.20
    }

    // 6
    @Test
    void partialSalePreservesRemainingCostBasis() throws TradingException {
        buyTenAt100SellFourAt110();

        Position p = service.getTrader().getPortfolio().getPosition("A001");
        assertEquals(6, p.getQuantity());
        assertMoney("600.60", p.getTotalCostBasis());                   // 1001.00 - (1001.00 x 4/10 = 400.40)
        assertMoney("100.10", p.getAverageCost());                      // 600.60 / 6
    }

    // 7
    @Test
    void realizedAndUnrealizedProfitLossIncludeFees() throws TradingException {
        buyTenAt100SellFourAt110();
        Trader t = service.getTrader();

        assertMoney("39.16", t.getPortfolio().getRealizedProfitLoss());     // 439.56 - 400.40
        assertMoney("660.00", t.getPortfolio().getTotalMarketValue());      // 6 x 110.00
        assertMoney("59.40", t.getPortfolio().getUnrealizedProfitLoss());   // 660.00 - 600.60
        assertMoney("10098.56", t.getTotalAccountValue());                  // 9438.56 + 660.00

        // account value - starting money = realized + unrealized (nothing double counted)
        assertMoney("98.56", t.getTotalAccountValue().subtract(new BigDecimal("10000.00")));
        assertMoney("98.56", t.getPortfolio().getRealizedProfitLoss()
                .add(t.getPortfolio().getUnrealizedProfitLoss()));
    }

    // 8
    @Test
    void pendingLimitBuyExecutesOnlyWhenPriceConditionIsMet() throws TradingException {
        Order order = service.placeLimitOrder(TransactionType.BUY, stock, 5, new BigDecimal("95.00"));
        assertEquals(OrderStatus.PENDING, order.getStatus());

        List<Order> changed = service.updateAssetPrice(stock, new BigDecimal("97.00"));   // above the limit
        assertTrue(changed.isEmpty());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0, held());

        changed = service.updateAssetPrice(stock, new BigDecimal("95.00"));               // equal to the limit
        assertEquals(1, changed.size());
        assertEquals(OrderStatus.EXECUTED, order.getStatus());
        assertEquals(5, held());
        assertMoney("9524.52", service.getTrader().getCashBalance());   // 10000 - (475.00 + 0.48 fee)
    }

    // 9
    @Test
    void pendingLimitSellExecutesOnlyWhenPriceConditionIsMet() throws TradingException {
        service.placeMarketOrder(TransactionType.BUY, stock, 10);
        Order order = service.placeLimitOrder(TransactionType.SELL, stock, 5, new BigDecimal("105.00"));

        assertTrue(service.updateAssetPrice(stock, new BigDecimal("104.00")).isEmpty());
        assertEquals(OrderStatus.PENDING, order.getStatus());

        assertEquals(1, service.updateAssetPrice(stock, new BigDecimal("106.00")).size());
        assertEquals(OrderStatus.EXECUTED, order.getStatus());
        assertEquals(5, held());
        assertMoney("9528.47", service.getTrader().getCashBalance());   // 8999.00 + (530.00 - 0.53)
    }

    // 10
    @Test
    void cancelledOrderIsNeverExecuted() throws TradingException {
        Order order = service.placeLimitOrder(TransactionType.BUY, stock, 5, new BigDecimal("95.00"));
        service.cancelOrder(order.getOrderId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        List<Order> changed = service.updateAssetPrice(stock, new BigDecimal("90.00"));   // below the limit
        assertTrue(changed.isEmpty());
        assertEquals(0, held());
        assertMoney("10000.00", service.getTrader().getCashBalance());

        assertThrows(TradingException.class, () -> service.cancelOrder(order.getOrderId()));

        // the CANCELLED status was saved in the database
        List<Order> saved = new OrderRepository().findByTrader(
                service.getTrader().getUserId(), new AssetRepository().findAllAsMap());
        assertEquals(1, saved.size());
        assertEquals(OrderStatus.CANCELLED, saved.get(0).getStatus());
    }

    // 11
    @Test
    void pendingOrderIsRejectedWhenCashIsNoLongerEnough() throws TradingException {
        Order limit = service.placeLimitOrder(TransactionType.BUY, stock, 50, new BigDecimal("95.00"));
        service.placeMarketOrder(TransactionType.BUY, stock, 90);       // 9000.00 + 9.00 -> cash 991.00

        List<Order> changed = service.updateAssetPrice(stock, new BigDecimal("95.00"));
        assertEquals(1, changed.size());
        assertEquals(OrderStatus.REJECTED, limit.getStatus());
        assertEquals(90, held());
        assertMoney("991.00", service.getTrader().getCashBalance());
    }

    // 12
    @Test
    void portfolioIsCorrectAfterRestart() throws TradingException {
        buyTenAt100SellFourAt110();
        Trader live = service.getTrader();

        // "Restart": rebuild everything using ONLY the database
        Map<String, Asset> freshAssets = new AssetRepository().findAllAsMap();
        Trader reloaded = new TraderRepository().findFirst(freshAssets).orElseThrow();

        assertEquals(0, live.getCashBalance().compareTo(reloaded.getCashBalance()));
        assertEquals(6, reloaded.getPortfolio().getQuantityHeld("A001"));
        assertMoney("600.60", reloaded.getPortfolio().getPosition("A001").getTotalCostBasis());
        assertMoney("39.16", reloaded.getPortfolio().getRealizedProfitLoss());
        assertMoney("10098.56", reloaded.getTotalAccountValue());
    }

    // 13
    @Test
    void invalidInputsAreRejected() {
        assertThrows(InvalidInputException.class,
                () -> service.placeMarketOrder(TransactionType.BUY, stock, 0));
        assertThrows(InvalidInputException.class,
                () -> service.placeMarketOrder(TransactionType.BUY, stock, -5));
        assertThrows(InvalidInputException.class,
                () -> service.placeMarketOrder(TransactionType.BUY, null, 1));
        assertThrows(InvalidInputException.class,
                () -> service.placeLimitOrder(TransactionType.BUY, stock, 1, null));
        assertThrows(InvalidInputException.class, () -> service.deposit(new BigDecimal("-10")));
        assertThrows(InvalidInputException.class, () -> service.deposit(BigDecimal.ZERO));
        assertThrows(InvalidInputException.class, () -> service.deposit(new BigDecimal("0.004")));
        assertThrows(InvalidInputException.class, () -> service.deposit(null));

        assertMoney("10000.00", service.getTrader().getCashBalance());
    }

    // 14
    @Test
    void depositIsRecordedAsTransaction() {
        service.deposit(new BigDecimal("500"));

        assertMoney("10500.00", service.getTrader().getCashBalance());
        List<Transaction> transactions = service.getTransactions();
        assertEquals(2, transactions.size());                  // starting deposit + this one
        assertEquals(TransactionType.DEPOSIT, transactions.get(0).getType());
        assertMoney("500.00", transactions.get(0).getNetAmount());
    }

    // 15
    @Test
    void quoteShowsValueFeeAndTotal() throws TradingException {
        OrderQuote buy = service.getQuote(TransactionType.BUY, stock, 10);
        assertMoney("1000.00", buy.grossValue());
        assertMoney("1.00", buy.fee());
        assertMoney("1001.00", buy.netAmount());               // total payable

        OrderQuote sell = service.getQuote(TransactionType.SELL, stock, 10);
        assertMoney("999.00", sell.netAmount());               // expected proceeds

        assertMoney("10000.00", service.getTrader().getCashBalance());   // a quote changes nothing
    }
}