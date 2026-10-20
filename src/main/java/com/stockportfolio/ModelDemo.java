package com.stockportfolio;

import com.stockportfolio.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary console demo to prove the model classes work.
 * All values are simulated demonstration data.
 */
public class ModelDemo {

    public static void main(String[] args) {
        List<Asset> assets = new ArrayList<>();
        assets.add(new Stock("A001", "Reliance Industries", "RELIANCE", "NSE",
                "INR", "Energy", new BigDecimal("2450.50")));
        assets.add(new ETF("A002", "Nifty 50 Index Fund", "NIFTYBEES", "INR",
                "Large Cap", "Nifty 50", new BigDecimal("250.75")));
        assets.add(new Bond("A003", "Govt Bond 2034", "Government of India", "INR",
                new BigDecimal("1000"), new BigDecimal("7.10"),
                LocalDate.of(2034, 6, 15), new BigDecimal("985.00")));

        System.out.println("--- Polymorphism: same call, different output ---");
        for (Asset a : assets) {
            System.out.println(a.getSymbol() + " -> " + a.getDetails());
        }

        System.out.println("\n--- Trader ---");
        Trader trader = new Trader("T001", "Mrunmayee", "", new BigDecimal("100000"));
        System.out.println(trader);
        trader.deposit(new BigDecimal("5000"));
        System.out.println("Cash after deposit: " + trader.getCashBalance());

        System.out.println("\n--- Validation test ---");
        try {
            trader.deposit(new BigDecimal("-10"));
        } catch (Exception e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}