package com.stockportfolio.service;

import com.stockportfolio.model.Order;
import com.stockportfolio.ui.UIContext;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Multithreaded Market Simulation Engine.
 * 
 * Demonstrates Java Multithreading & Concurrency:
 * 1. ScheduledExecutorService runs a background daemon thread that updates prices at fixed intervals.
 * 2. Background thread executes computational simulation and pending limit-order triggers.
 * 3. Platform.runLater() safely synchronizes UI state changes with the JavaFX Application Thread,
 *    preventing GUI freezing and race conditions.
 */
public class MarketSimulationScheduler {

    private final UIContext context;
    private ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MarketSimulationScheduler(UIContext context) {
        this.context = context;
    }

    public synchronized void start(int intervalSeconds) {
        if (running.get()) return;

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Market-Simulation-Worker");
            thread.setDaemon(true); // Daemon thread terminates automatically when app closes
            return thread;
        });

        running.set(true);
        executor.scheduleAtFixedRate(this::tick, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (!running.get()) return;
        running.set(false);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void toggle(int intervalSeconds) {
        if (running.get()) {
            stop();
        } else {
            start(intervalSeconds);
        }
    }

    private void tick() {
        try {
            // Background Thread: fetch simulated prices and re-evaluate pending orders
            List<Order> changedOrders = context.getMarketService().refreshPrices();
            context.getTradingService().processPendingOrders();

            // JavaFX Application Thread: dispatch reactive UI updates
            Platform.runLater(context::notifyDataChanged);
        } catch (Exception e) {
            // Logging or graceful handling without crashing worker thread
        }
    }
}
