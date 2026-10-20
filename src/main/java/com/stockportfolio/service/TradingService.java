package com.stockportfolio.service;

import com.stockportfolio.exception.DatabaseException;
import com.stockportfolio.exception.InsufficientFundsException;
import com.stockportfolio.exception.InsufficientHoldingsException;
import com.stockportfolio.exception.InvalidInputException;
import com.stockportfolio.exception.TradingException;
import com.stockportfolio.model.Asset;
import com.stockportfolio.model.BuyOrder;
import com.stockportfolio.model.Order;
import com.stockportfolio.model.OrderQuote;
import com.stockportfolio.model.OrderStatus;
import com.stockportfolio.model.OrderType;
import com.stockportfolio.model.SellOrder;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
import com.stockportfolio.repository.AssetRepository;
import com.stockportfolio.repository.OrderRepository;
import com.stockportfolio.repository.TraderRepository;
import com.stockportfolio.repository.TransactionRepository;
import com.stockportfolio.util.FeeCalculator;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The one place where deposits, quotes, orders and cancellations happen.
 *
 * Errors the screens must handle:
 *   TradingException (checked)      - not enough cash or holdings, order cannot be cancelled...
 *   InvalidInputException           - bad quantity, bad amount, missing asset...
 *   DatabaseException               - saving failed (memory is reloaded from the database first)
 *
 * After a DatabaseException, call getTrader() again: it may be a freshly loaded object.
 */
public class TradingService {

    private Trader trader;
    private List<Order> orders;
    private final Map<String, Asset> assets;

    private final PersistenceService persistence = new PersistenceService();
    private final TraderRepository traderRepository = new TraderRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final AssetRepository assetRepository = new AssetRepository();

    public TradingService(Trader trader, Map<String, Asset> assets) {
        if (trader == null || assets == null) {
            throw new InvalidInputException("A trader and the asset list are required.");
        }
        this.trader = trader;
        this.assets = assets;
        this.orders = new ArrayList<>(orderRepository.findByTrader(trader.getUserId(), assets));
    }

    // ------------------------------------------------------------------ reading

    public Trader getTrader() {
        return trader;
    }

    public void setTrader(Trader newTrader) {
        if (newTrader == null) {
            throw new InvalidInputException("Trader cannot be null.");
        }
        this.trader = newTrader;
        this.orders = new ArrayList<>(orderRepository.findByTrader(newTrader.getUserId(), assets));
    }

    public Map<String, Asset> getAssets() {
        return assets;
    }

    /** Copy of all orders, oldest first. */
    public List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    public List<Order> getPendingOrders() {
        List<Order> pending = new ArrayList<>();
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.PENDING) {
                pending.add(order);
            }
        }
        return pending;
    }

    /** All transactions of the trader, newest first, read from the database. */
    public List<Transaction> getTransactions() {
        return transactionRepository.findByTrader(trader.getUserId(), assets);
    }

    // ------------------------------------------------------------------ wallet

    /** Adds VIRTUAL money and records it as a DEPOSIT transaction. */
    public Transaction deposit(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidInputException("Please enter a deposit amount.");
        }
        BigDecimal rounded = MoneyUtil.round(amount);
        if (rounded.signum() <= 0) {
            throw new InvalidInputException("Deposit must be at least 0.01.");
        }
        if (rounded.compareTo(AccountService.MAX_VIRTUAL_AMOUNT) > 0) {
            throw new InvalidInputException("A single deposit cannot be more than "
                    + AccountService.MAX_VIRTUAL_AMOUNT + ".");
        }
        trader.deposit(rounded);
        Transaction deposit = Transaction.forDeposit(trader.getUserId(), rounded);
        try {
            persistence.saveDeposit(trader, deposit);
        } catch (DatabaseException e) {
            reloadFromDatabase();
            throw e;
        }
        return deposit;
    }

    // ------------------------------------------------------------------ quotes

    /** Price, order value, fee and total/proceeds for the review screen. Changes nothing. */
    public OrderQuote getQuote(TransactionType side, Asset asset, int quantity)
            throws TradingException {
        requirePriceAvailable(asset);
        if (side != TransactionType.BUY && side != TransactionType.SELL) {
            throw new InvalidInputException("An order must be a BUY or a SELL.");
        }
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero.");
        }
        BigDecimal price = asset.getCurrentPrice();
        BigDecimal gross = grossValue(price, quantity);
        BigDecimal fee = FeeCalculator.calculateFee(gross);
        BigDecimal net = side == TransactionType.BUY ? gross.add(fee) : gross.subtract(fee);
        return new OrderQuote(side, asset, quantity, price, gross, fee, net);
    }

    // ------------------------------------------------------------------ placing orders

    /** Executes immediately at the current price. Nothing changes if it fails. */
    public Transaction placeMarketOrder(TransactionType side, Asset asset, int quantity)
            throws TradingException {
        requirePriceAvailable(asset);
        Order order = createOrder(side, asset, quantity, OrderType.MARKET, null);
        Transaction transaction = executeAndSave(order, asset.getCurrentPrice());
        orders.add(order);
        return transaction;
    }

    /**
     * Saves a PENDING limit order. If the market price already satisfies the limit,
     * it executes straight away. Returns the order so the screen can show its status.
     */
    public Order placeLimitOrder(TransactionType side, Asset asset, int quantity,
                                 BigDecimal limitPrice) throws TradingException {
        requirePriceAvailable(asset);
        Order order = createOrder(side, asset, quantity, OrderType.LIMIT, limitPrice);
        validateFundsOrHoldings(order);

        persistence.saveOrder(trader, order);     // if this fails, nothing in memory has changed
        orders.add(order);

        processOrder(order);                      // executes now if the condition is already met
        return order;
    }

    public Order placeOrder(Asset asset, TransactionType side, OrderType type, int quantity,
                            BigDecimal limitPrice) throws TradingException {
        if (type == OrderType.MARKET) {
            placeMarketOrder(side, asset, quantity);
            return orders.get(orders.size() - 1);
        } else {
            return placeLimitOrder(side, asset, quantity, limitPrice);
        }
    }

    public Transaction depositFunds(BigDecimal amount) {
        return deposit(amount);
    }

    /** Cancels a PENDING order. Cancelled orders are never executed. */
    public Order cancelOrder(String orderId) throws TradingException {
        Order order = findOrder(orderId);
        order.cancel();                           // throws if the order is not PENDING
        try {
            persistence.saveOrder(trader, order);
        } catch (DatabaseException e) {
            reloadFromDatabase();
            throw e;
        }
        return order;
    }

    // ------------------------------------------------------------------ price changes

    /**
     * Sets a new price for an asset, saves it, then re-checks every pending order.
     * (In Phase 7 the market-data provider will call this.)
     * Returns the orders whose status changed (EXECUTED or REJECTED).
     */
    public List<Order> updateAssetPrice(Asset asset, BigDecimal newPrice) {
        BigDecimal oldPrice = asset.getCurrentPrice();
        asset.setCurrentPrice(newPrice);          // throws if the price is not above zero
        try {
            assetRepository.updatePrice(asset.getAssetId(), asset.getCurrentPrice());
        } catch (DatabaseException e) {
            asset.setCurrentPrice(oldPrice);      // put the old price back
            throw e;
        }
        return processPendingOrders();
    }

    /** Executes pending orders whose condition is now met; rejects those no longer affordable. */
    public List<Order> processPendingOrders() {
        List<Order> changed = new ArrayList<>();
        for (Order order : new ArrayList<>(orders)) {      // copy: a reload may replace the list
            if (processOrder(order)) {
                changed.add(order);
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ private helpers

    /** Returns true if the order changed status. */
    private boolean processOrder(Order order) {
        BigDecimal price = order.getAsset().getCurrentPrice();
        if (order.getStatus() != OrderStatus.PENDING || !order.canExecuteAt(price)) {
            return false;
        }
        try {
            executeAndSave(order, price);
        } catch (TradingException e) {
            rejectAndSave(order);                 // cash or holdings are no longer enough
        }
        return true;
    }

    private Transaction executeAndSave(Order order, BigDecimal price) throws TradingException {
        BigDecimal gross = grossValue(price, order.getQuantity());
        BigDecimal fee = FeeCalculator.calculateFee(gross);

        Transaction transaction = order.execute(trader, price, fee);   // throws BEFORE changing anything
        try {
            persistence.saveTrade(trader, order, transaction);
        } catch (DatabaseException e) {
            reloadFromDatabase();                 // memory goes back to the last saved state
            throw e;
        }
        return transaction;
    }

    private void rejectAndSave(Order order) {
        try {
            order.reject();
        } catch (TradingException e) {
            return;                               // not pending any more: nothing to do
        }
        try {
            persistence.saveOrder(trader, order);
        } catch (DatabaseException e) {
            reloadFromDatabase();
            throw e;
        }
    }

    private void validateFundsOrHoldings(Order order) throws TradingException {
        if (order.getSide() == TransactionType.BUY) {
            BigDecimal gross = grossValue(order.getLimitPrice(), order.getQuantity());
            BigDecimal estimatedCost = gross.add(FeeCalculator.calculateFee(gross));
            if (trader.getCashBalance().compareTo(estimatedCost) < 0) {
                throw new InsufficientFundsException(estimatedCost, trader.getCashBalance());
            }
        } else {
            int held = trader.getPortfolio().getQuantityHeld(order.getAsset().getAssetId());
            if (held < order.getQuantity()) {
                throw new InsufficientHoldingsException("You hold only " + held + " unit(s) of "
                        + order.getAsset().getSymbol() + " but tried to sell "
                        + order.getQuantity() + ".");
            }
        }
    }

    private Order createOrder(TransactionType side, Asset asset, int quantity,
                              OrderType type, BigDecimal limitPrice) {
        if (side == null) {
            throw new InvalidInputException("Order side is required.");
        }
        return switch (side) {
            case BUY -> new BuyOrder(asset, quantity, type, limitPrice);
            case SELL -> new SellOrder(asset, quantity, type, limitPrice);
            default -> throw new InvalidInputException("An order must be a BUY or a SELL.");
        };
    }

    private void requirePriceAvailable(Asset asset) throws TradingException {
        if (asset == null) {
            throw new InvalidInputException("Please select an asset.");
        }
        BigDecimal price = asset.getCurrentPrice();
        if (price == null || price.signum() <= 0) {
            throw new TradingException("No valid price is available for " + asset.getSymbol() + ".");
        }
    }

    private BigDecimal grossValue(BigDecimal price, int quantity) {
        return MoneyUtil.round(price.multiply(BigDecimal.valueOf(quantity)));
    }

    private Order findOrder(String orderId) throws TradingException {
        for (Order order : orders) {
            if (order.getOrderId().equals(orderId)) {
                return order;
            }
        }
        throw new TradingException("Order not found: " + orderId);
    }

    /** Replaces the in-memory trader and orders with what is saved in the database. */
    private void reloadFromDatabase() {
        String traderId = trader.getUserId();
        this.trader = traderRepository.findById(traderId, assets)
                .orElseThrow(() -> new DatabaseException("The saved trader could not be reloaded."));
        this.orders = new ArrayList<>(orderRepository.findByTrader(traderId, assets));
    }
}