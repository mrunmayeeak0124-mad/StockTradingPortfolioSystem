package com.stockportfolio.service;

import com.stockportfolio.model.Asset;
import com.stockportfolio.model.AssetCategory;
import com.stockportfolio.model.DiversificationInfo;
import com.stockportfolio.model.HoldingSummary;
import com.stockportfolio.model.PerformanceSummary;
import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.PortfolioSummary;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.PricePoint;
import com.stockportfolio.model.Trader;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.TransactionType;
import com.stockportfolio.repository.PriceHistoryRepository;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Turns a Trader's data into figures for the portfolio screens.
 * It only READS data. It never changes cash or holdings.
 */
public class PortfolioService {

    /** Illustrative limits (percent of invested money), not financial advice. */
    public static final BigDecimal CONCENTRATED_PERCENT = new BigDecimal("60");
    public static final BigDecimal MODERATE_PERCENT = new BigDecimal("40");

    private static final String RULE_NOTE = " (The 60% / 40% limits are simple illustrative rules, not financial advice.)";

    private final PriceHistoryRepository historyRepository = new PriceHistoryRepository();

    /** One row per open position, largest market value first. Empty list for an empty portfolio. */
    public List<HoldingSummary> getHoldings(Trader trader) {
        Portfolio portfolio = trader.getPortfolio();
        BigDecimal totalValue = portfolio.getTotalMarketValue();
        List<HoldingSummary> rows = new ArrayList<>();
        for (Position p : portfolio.getPositions()) {
            Asset asset = p.getAsset();
            rows.add(new HoldingSummary(asset.getAssetId(), asset.getName(), asset.getSymbol(),
                    asset.getCategory(), p.getQuantity(), p.getAverageCost(), p.getCurrentPrice(),
                    p.getTotalCostBasis(), p.getMarketValue(), p.getUnrealizedProfitLoss(),
                    p.getReturnPercent(), MoneyUtil.percent(p.getMarketValue(), totalValue)));
        }
        rows.sort(Comparator.comparing(HoldingSummary::marketValue).reversed());
        return rows;
    }

    public PortfolioSummary getSummary(Trader trader) {
        Portfolio portfolio = trader.getPortfolio();
        BigDecimal cash = trader.getCashBalance();
        BigDecimal holdings = portfolio.getTotalMarketValue();
        BigDecimal total = trader.getTotalAccountValue();
        return new PortfolioSummary(cash, holdings, total,
                MoneyUtil.percent(cash, total), MoneyUtil.percent(holdings, total),
                portfolio.getTotalCostBasis(), portfolio.getUnrealizedProfitLoss(),
                portfolio.getRealizedProfitLoss(), portfolio.getTotalReturnPercent(),
                portfolio.getOpenPositionCount(), portfolio.getAllocationByCategory());
    }

    public PortfolioSummary getPortfolioSummary(Trader trader) {
        return getSummary(trader);
    }

    public DiversificationInfo getDiversification(Trader trader) {
        List<HoldingSummary> holdings = getHoldings(trader);
        if (holdings.isEmpty()) {
            return new DiversificationInfo(DiversificationInfo.Level.NO_HOLDINGS, 0, 0, "-",
                    BigDecimal.ZERO.setScale(2),
                    "You do not own any assets yet, so there is nothing to diversify. "
                            + "Diversification means spreading money across different assets and categories.");
        }

        HoldingSummary largest = holdings.get(0);            // the list is sorted, largest first
        Set<AssetCategory> categories = new HashSet<>();
        for (HoldingSummary holding : holdings) {
            categories.add(holding.category());
        }
        BigDecimal share = largest.portfolioSharePercent();

        DiversificationInfo.Level level;
        String message;
        if (holdings.size() == 1 || share.compareTo(CONCENTRATED_PERCENT) >= 0) {
            level = DiversificationInfo.Level.CONCENTRATED;
            message = String.format("%s%% of your invested money is in one asset (%s). If that single "
                    + "investment falls in value, your portfolio is affected strongly. Spreading money "
                    + "across several assets and categories can reduce this, but it cannot remove "
                    + "overall market risk.", share, largest.symbol()) + RULE_NOTE;
        } else if (share.compareTo(MODERATE_PERCENT) >= 0 || categories.size() < 2) {
            level = DiversificationInfo.Level.MODERATE;
            message = String.format("Your largest holding (%s) is %s%% of your invested money, and you "
                            + "hold %d categor%s. Adding different kinds of assets can spread risk further.",
                    largest.symbol(), share, categories.size(),
                    categories.size() == 1 ? "y" : "ies") + RULE_NOTE;
        } else {
            level = DiversificationInfo.Level.DIVERSIFIED;
            message = String.format("Your money is spread across %d assets in %d categories, and the "
                            + "largest holding (%s) is %s%%. Diversification reduces the effect of one "
                            + "investment doing badly, but it does not remove overall market risk.",
                    holdings.size(), categories.size(), largest.symbol(), share) + RULE_NOTE;
        }
        return new DiversificationInfo(level, holdings.size(), categories.size(),
                largest.symbol(), share, message);
    }

    /** Needs the trader's transactions (newest or oldest first, either works). */
    public PerformanceSummary getPerformance(Trader trader, List<Transaction> transactions) {
        BigDecimal deposited = BigDecimal.ZERO.setScale(2);
        BigDecimal fees = BigDecimal.ZERO.setScale(2);
        int trades = 0;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.DEPOSIT) {
                deposited = deposited.add(t.getNetAmount());
            } else {
                fees = fees.add(t.getFee());
                trades++;
            }
        }
        BigDecimal accountValue = trader.getTotalAccountValue();
        BigDecimal net = accountValue.subtract(deposited);
        return new PerformanceSummary(deposited, accountValue, net,
                MoneyUtil.percent(net, deposited),
                trader.getPortfolio().getRealizedProfitLoss(),
                trader.getPortfolio().getUnrealizedProfitLoss(), fees, trades);
    }

    /**
     * Account value at the end of each day, from the first transaction until today.
     * Uses saved (simulated) price history for past days and the CURRENT price for today.
     */
    public List<PricePoint> getAccountValueHistory(List<Transaction> transactions) {
        Map<String, NavigableMap<LocalDate, BigDecimal>> history = new HashMap<>();
        Map<String, BigDecimal> currentPrices = new HashMap<>();
        for (Transaction t : transactions) {
            Asset asset = t.getAsset();
            if (asset == null || history.containsKey(asset.getAssetId())) {
                continue;
            }
            NavigableMap<LocalDate, BigDecimal> prices = new TreeMap<>();
            for (PricePoint point : historyRepository.findByAsset(asset.getAssetId())) {
                prices.put(point.date(), point.price());
            }
            history.put(asset.getAssetId(), prices);
            currentPrices.put(asset.getAssetId(), asset.getCurrentPrice());
        }
        return calculateValueHistory(transactions, history, currentPrices, LocalDate.now());
    }

    /**
     * The calculation itself, with plain inputs so it can be tested without a database.
     * For each day: cash = sum of net amounts so far; holdings = units held x that day's price.
     */
    public static List<PricePoint> calculateValueHistory(
            List<Transaction> transactions,
            Map<String, NavigableMap<LocalDate, BigDecimal>> priceHistory,
            Map<String, BigDecimal> currentPrices,
            LocalDate today) {

        List<PricePoint> result = new ArrayList<>();
        if (transactions == null || transactions.isEmpty()) {
            return result;
        }
        LocalDate start = today;
        for (Transaction t : transactions) {
            LocalDate day = t.getTimestamp().toLocalDate();
            if (day.isBefore(start)) {
                start = day;
            }
        }

        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            BigDecimal cash = BigDecimal.ZERO.setScale(2);
            Map<String, Integer> units = new HashMap<>();
            for (Transaction t : transactions) {
                if (t.getTimestamp().toLocalDate().isAfter(day)) {
                    continue;
                }
                cash = cash.add(t.getNetAmount());
                if (t.getType() == TransactionType.BUY) {
                    units.merge(t.getAsset().getAssetId(), t.getQuantity(), Integer::sum);
                } else if (t.getType() == TransactionType.SELL) {
                    units.merge(t.getAsset().getAssetId(), -t.getQuantity(), Integer::sum);
                }
            }
            BigDecimal holdings = BigDecimal.ZERO.setScale(2);
            for (Map.Entry<String, Integer> entry : units.entrySet()) {
                if (entry.getValue() <= 0) {
                    continue;
                }
                BigDecimal price = priceOn(entry.getKey(), day, today, priceHistory, currentPrices);
                holdings = holdings.add(price.multiply(BigDecimal.valueOf(entry.getValue())));
            }
            result.add(new PricePoint(day, MoneyUtil.round(cash.add(holdings))));
        }
        return result;
    }

    private static BigDecimal priceOn(String assetId, LocalDate day, LocalDate today,
                                      Map<String, NavigableMap<LocalDate, BigDecimal>> priceHistory,
                                      Map<String, BigDecimal> currentPrices) {
        BigDecimal current = currentPrices.getOrDefault(assetId, BigDecimal.ZERO);
        if (day.equals(today)) {
            return current;
        }
        NavigableMap<LocalDate, BigDecimal> prices = priceHistory.get(assetId);
        if (prices != null) {
            Map.Entry<LocalDate, BigDecimal> entry = prices.floorEntry(day);   // last known price on or before that day
            if (entry != null) {
                return entry.getValue();
            }
        }
        return current;
    }
}