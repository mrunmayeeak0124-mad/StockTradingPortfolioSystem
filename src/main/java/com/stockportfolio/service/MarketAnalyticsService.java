package com.stockportfolio.service;

import com.stockportfolio.model.PricePoint;
import com.stockportfolio.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    /**
     * Record holding historical price series and percentage metrics for different timeframes.
     */
    public record TimeframeSeries(
            String timeframe,
            List<PricePoint> points,
            BigDecimal startPrice,
            BigDecimal currentPrice,
            BigDecimal absoluteChange,
            BigDecimal percentChange,
            boolean isPositive
    ) {}

    /**
     * Generates realistic multi-timeframe price series with organic fluctuations,
     * intraday volatility for 1D, and massive exponential compounding curves for 1Y/5Y/ALL
     * matching institutional charting terminals.
     */
    public TimeframeSeries generateTimeframeSeries(com.stockportfolio.model.Asset asset, String timeframe) {
        String tf = (timeframe == null || timeframe.isBlank()) ? "1D" : timeframe.toUpperCase().trim();
        BigDecimal endPrice = asset != null ? asset.getCurrentPrice() : new BigDecimal("2460.90");
        double endVal = endPrice.doubleValue();

        List<PricePoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now();

        double startVal;
        int numPoints;

        switch (tf) {
            case "1D" -> {
                // Intraday jagged curve matching Image 1: morning dip/spike, sharp pullback (-0.17% to -0.80%)
                numPoints = 38;
                double changePct = -0.0017; // -0.17%
                startVal = endVal / (1.0 + changePct);
                double current = startVal;
                java.time.LocalTime t = java.time.LocalTime.of(9, 15);

                for (int i = 0; i < numPoints; i++) {
                    if (i == numPoints - 1) {
                        current = endVal;
                    } else {
                        double progress = (double) i / (double) numPoints;
                        // Intraday swing wave: rapid dip, spike, descending channel
                        double swing = Math.sin(progress * 12.0) * 0.0035 + (Math.cos(progress * 24.0) * 0.002);
                        if (progress > 0.15 && progress < 0.35) swing += 0.008; // early high peak
                        if (progress > 0.55 && progress < 0.75) swing -= 0.009; // midday slump
                        current = startVal * (1.0 + swing - (progress * 0.004));
                    }
                    LocalDate pDate = today;
                    String label = t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    points.add(new PricePoint(pDate, BigDecimal.valueOf(current).setScale(2, RoundingMode.HALF_UP)));
                    t = t.plusMinutes(10);
                }
            }
            case "1W" -> {
                numPoints = 7;
                startVal = endVal * 0.985;
                for (int i = 0; i < numPoints; i++) {
                    double progress = (double) i / (numPoints - 1);
                    double val = (i == numPoints - 1) ? endVal : startVal + (endVal - startVal) * progress + (Math.sin(i * 1.5) * endVal * 0.012);
                    points.add(new PricePoint(today.minusDays(numPoints - 1 - i), BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP)));
                }
            }
            case "1M" -> {
                numPoints = 30;
                startVal = endVal * 0.96;
                for (int i = 0; i < numPoints; i++) {
                    double progress = (double) i / (numPoints - 1);
                    double val = (i == numPoints - 1) ? endVal : startVal + (endVal - startVal) * progress + (Math.sin(i * 0.7) * endVal * 0.02);
                    points.add(new PricePoint(today.minusDays(numPoints - 1 - i), BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP)));
                }
            }
            case "3M" -> {
                numPoints = 45;
                startVal = endVal * 0.91;
                for (int i = 0; i < numPoints; i++) {
                    double progress = (double) i / (numPoints - 1);
                    double val = (i == numPoints - 1) ? endVal : startVal + (endVal - startVal) * progress + (Math.sin(i * 0.5) * endVal * 0.035);
                    points.add(new PricePoint(today.minusDays((numPoints - 1 - i) * 2L), BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP)));
                }
            }
            case "6M" -> {
                numPoints = 60;
                startVal = endVal * 0.84;
                for (int i = 0; i < numPoints; i++) {
                    double progress = (double) i / (numPoints - 1);
                    double val = (i == numPoints - 1) ? endVal : startVal + (endVal - startVal) * progress + (Math.sin(i * 0.4) * endVal * 0.045);
                    points.add(new PricePoint(today.minusDays((numPoints - 1 - i) * 3L), BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP)));
                }
            }
            case "1Y" -> {
                numPoints = 52;
                startVal = endVal * 0.78; // +28% gain over 1 year
                for (int i = 0; i < numPoints; i++) {
                    double progress = (double) i / (numPoints - 1);
                    double wave = Math.sin(progress * 8.0) * 0.06 - (progress > 0.4 && progress < 0.6 ? 0.08 : 0.0);
                    double val = (i == numPoints - 1) ? endVal : startVal * (1.0 + (progress * 0.28) + wave);
                    points.add(new PricePoint(today.minusWeeks(numPoints - 1 - i), BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP)));
                }
            }
            case "5Y" -> {
                numPoints = 60;
                startVal = endVal * 0.35; // ~+185% gain over 5 years
                for (int i = 0; i < numPoints; i++) {
                    double progress = (double) i / (numPoints - 1);
                    double val = (i == numPoints - 1) ? endVal : startVal * Math.pow(endVal / startVal, progress) * (1.0 + Math.sin(progress * 10.0) * 0.08);
                    points.add(new PricePoint(today.minusMonths(numPoints - 1 - i), BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP)));
                }
            }
            default -> { // "ALL"
                // Multi-year compounding curve matching Image 2 (+1,542.38% All)
                numPoints = 75;
                startVal = endVal / 16.42; // starting around ₹149.84 for TCS!
                for (int i = 0; i < numPoints; i++) {
                    double progress = (double) i / (numPoints - 1);
                    // Exponential growth with realistic market cycles (2008 crash dip, 2020 crash dip, bull runs)
                    double expBase = startVal * Math.pow(endVal / startVal, Math.pow(progress, 1.25));
                    double cycleDips = Math.sin(progress * 14.0) * (expBase * 0.12);
                    if (progress > 0.68 && progress < 0.78) {
                        cycleDips -= expBase * 0.22; // market correction dip
                    }
                    if (progress > 0.82 && progress < 0.92) {
                        cycleDips += expBase * 0.18; // bull market peak
                    }
                    double val = (i == numPoints - 1) ? endVal : Math.max(startVal * 0.8, expBase + cycleDips);
                    points.add(new PricePoint(today.minusMonths((numPoints - 1 - i) * 3L), BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP)));
                }
            }
        }

        BigDecimal startPrice = points.get(0).price();
        BigDecimal currentPrice = points.get(points.size() - 1).price();
        BigDecimal diff = currentPrice.subtract(startPrice);
        BigDecimal pct = startPrice.signum() > 0 ? diff.multiply(BigDecimal.valueOf(100)).divide(startPrice, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        boolean isPositive = diff.signum() >= 0;

        return new TimeframeSeries(tf, points, startPrice, currentPrice, diff, pct, isPositive);
    }
}