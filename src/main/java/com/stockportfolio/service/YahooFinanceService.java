package com.stockportfolio.service;

import com.stockportfolio.model.PricePoint;
import com.stockportfolio.service.MarketAnalyticsService.TimeframeSeries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service integrating Yahoo Finance API for real-time market data, quotes,
 * and authentic historical price action (e.g. AAPL, TCS, RELIANCE).
 * If offline or unreachable, seamlessly provides realistic fractal simulation curves.
 */
public class YahooFinanceService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // Cache responses for 60 seconds to prevent unnecessary network overhead
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private record CacheEntry(TimeframeSeries series, long cachedAt) {
        boolean isExpired() {
            return (System.currentTimeMillis() - cachedAt) > 60_000L;
        }
    }

    /**
     * Maps an internal symbol to its Yahoo Finance compatible symbol.
     */
    public static String mapSymbolToYahoo(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "AAPL";
        }
        String s = symbol.trim().toUpperCase();
        return switch (s) {
            case "AAPL" -> "AAPL";
            case "TCS" -> "TCS.NS";
            case "RELIANCE" -> "RELIANCE.NS";
            case "INFY" -> "INFY.NS";
            case "HDFCBANK" -> "HDFCBANK.NS";
            case "ITC" -> "ITC.NS";
            case "TATAMOTORS" -> "TATAMOTORS.NS";
            case "NIFTYBEES" -> "NIFTYBEES.NS";
            case "GOLDBEES" -> "GOLDBEES.NS";
            case "BANKBEES" -> "BANKBEES.NS";
            default -> s.contains(".") ? s : s;
        };
    }

    /**
     * Fetches real-time / historical timeframe series directly from Yahoo Finance.
     * Returns null if the network is unavailable or parsing fails.
     */
    public TimeframeSeries fetchYahooSeries(String symbol, String timeframe) {
        String yfSymbol = mapSymbolToYahoo(symbol);
        String tf = (timeframe == null || timeframe.isBlank()) ? "1D" : timeframe.toUpperCase().trim();
        String cacheKey = yfSymbol + ":" + tf;

        CacheEntry cached = CACHE.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.series();
        }

        String queryParams = mapTimeframeToQuery(tf);
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yfSymbol + "?range=" + queryParams;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                TimeframeSeries parsed = parseYahooChartResponse(response.body(), tf);
                if (parsed != null && parsed.points().size() >= 3) {
                    CACHE.put(cacheKey, new CacheEntry(parsed, System.currentTimeMillis()));
                    return parsed;
                }
            }
        } catch (Exception ignored) {
            // Graceful fallback to local realistic generator
        }
        return null;
    }

    private static String mapTimeframeToQuery(String tf) {
        return switch (tf) {
            case "1D" -> "1d&interval=5m";
            case "1W" -> "5d&interval=15m";
            case "1M" -> "1mo&interval=1d";
            case "3M" -> "3mo&interval=1d";
            case "6M" -> "6mo&interval=1d";
            case "1Y" -> "1y&interval=1wk";
            case "5Y" -> "5y&interval=1mo";
            default -> "max&interval=1mo"; // "ALL"
        };
    }

    private static TimeframeSeries parseYahooChartResponse(String json, String tf) {
        Pattern tsPattern = Pattern.compile("\"timestamp\":\\[([^\\]]+)\\]");
        Matcher tsMatcher = tsPattern.matcher(json);
        List<Long> timestamps = new ArrayList<>();
        if (tsMatcher.find()) {
            for (String s : tsMatcher.group(1).split(",")) {
                try {
                    timestamps.add(Long.parseLong(s.trim()));
                } catch (Exception ignored) {}
            }
        }

        Pattern closePattern = Pattern.compile("\"close\":\\[([^\\]]+)\\]");
        Matcher closeMatcher = closePattern.matcher(json);
        List<BigDecimal> closes = new ArrayList<>();
        if (closeMatcher.find()) {
            for (String s : closeMatcher.group(1).split(",")) {
                String str = s.trim();
                if (!str.isEmpty() && !str.equalsIgnoreCase("null")) {
                    try {
                        closes.add(new BigDecimal(str).setScale(2, RoundingMode.HALF_UP));
                    } catch (Exception ignored) {}
                }
            }
        }

        if (closes.size() < 3) {
            return null;
        }

        List<PricePoint> points = new ArrayList<>();
        ZoneId zone = ZoneId.systemDefault();
        int count = Math.min(timestamps.size(), closes.size());

        for (int i = 0; i < count; i++) {
            long epochSec = timestamps.get(i);
            LocalDate d = Instant.ofEpochSecond(epochSec).atZone(zone).toLocalDate();
            points.add(new PricePoint(d, closes.get(i)));
        }

        // If timestamps and closes differed in length, populate with current closes
        if (points.isEmpty()) {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < closes.size(); i++) {
                points.add(new PricePoint(today.minusDays(closes.size() - 1 - i), closes.get(i)));
            }
        }

        BigDecimal startPrice = points.get(0).price();
        BigDecimal currentPrice = points.get(points.size() - 1).price();
        BigDecimal diff = currentPrice.subtract(startPrice);
        BigDecimal pct = startPrice.signum() > 0
                ? diff.multiply(BigDecimal.valueOf(100)).divide(startPrice, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        boolean isPositive = diff.signum() >= 0;

        return new TimeframeSeries(tf, points, startPrice, currentPrice, diff, pct, isPositive);
    }
}
