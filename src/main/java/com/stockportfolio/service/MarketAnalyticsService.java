package com.stockportfolio.service;

import com.stockportfolio.model.PricePoint;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Simple statistics over a price history (oldest first).
 * When there is not enough data, methods return Optional.empty() or an empty list
 * instead of failing. These numbers DESCRIBE THE PAST. They do not predict the future.
 */
public class MarketAnalyticsService {

    public static final String HISTORY_DISCLAIMER =
            "Past price movements describe history only. They do not reliably predict future returns.";

    /** Latest price minus the previous price. Needs at least 2 observations. */
    public Optional<BigDecimal> priceChange(List<PricePoint> history) {
        if (history == null || history.size() < 2) {
            return Optional.empty();
        }
        BigDecimal latest = history.get(history.size() - 1).price();
        BigDecimal previous = history.get(history.size() - 2).price();
        return Optional.of(latest.subtract(previous));
    }

    /**
     * Percentage change between the latest price and the price 'observationsBack'
     * observations earlier. Empty if the history is too short.
     */
    public Optional<BigDecimal> percentChange(List<PricePoint> history, int observationsBack) {
        if (history == null || observationsBack <= 0 || history.size() <= observationsBack) {
            return Optional.empty();
        }
        BigDecimal latest = history.get(history.size() - 1).price();
        BigDecimal earlier = history.get(history.size() - 1 - observationsBack).price();
        return Optional.of(MoneyUtil.percent(latest.subtract(earlier), earlier));
    }

    /**
     * Simple moving average: each point is the average of the last 'window' prices.
     * Empty list if there are fewer observations than the window.
     */
    public List<PricePoint> simpleMovingAverage(List<PricePoint> history, int window) {
        List<PricePoint> result = new ArrayList<>();
        if (history == null || window <= 0 || history.size() < window) {
            return result;
        }
        for (int end = window - 1; end < history.size(); end++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = end - window + 1; i <= end; i++) {
                sum = sum.add(history.get(i).price());
            }
            result.add(new PricePoint(history.get(end).date(),
                    sum.divide(BigDecimal.valueOf(window), 2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    /** Returns the most recent simple moving average, or empty if history is insufficient. */
    public Optional<BigDecimal> calculateSMA(List<PricePoint> history, int window) {
        List<PricePoint> smaList = simpleMovingAverage(history, window);
        if (smaList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(smaList.get(smaList.size() - 1).price());
    }

    /**
     * Volatility = standard deviation of the daily percentage changes, in percent.
     * Needs at least 3 observations. (double is fine here: this is a statistic, not money.)
     */
    public Optional<BigDecimal> volatilityPercent(List<PricePoint> history) {
        if (history == null || history.size() < 3) {
            return Optional.empty();
        }
        int count = history.size() - 1;
        double[] returns = new double[count];
        for (int i = 1; i < history.size(); i++) {
            double previous = history.get(i - 1).price().doubleValue();
            double current = history.get(i).price().doubleValue();
            returns[i - 1] = (current - previous) / previous;
        }
        double mean = 0;
        for (double r : returns) {
            mean += r;
        }
        mean /= count;
        double sumOfSquares = 0;
        for (double r : returns) {
            sumOfSquares += (r - mean) * (r - mean);
        }
        double standardDeviation = Math.sqrt(sumOfSquares / (count - 1));
        return Optional.of(BigDecimal.valueOf(standardDeviation * 100).setScale(2, RoundingMode.HALF_UP));
    }
}