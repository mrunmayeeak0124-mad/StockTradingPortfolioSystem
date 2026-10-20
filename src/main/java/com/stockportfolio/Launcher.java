package com.stockportfolio;

/**
 * Entry point launcher for the Stock Trading and Portfolio Management System.
 * 
 * In JavaFX 11+, running a class that directly extends javafx.application.Application
 * without module-path flags can trigger:
 * "Error: JavaFX runtime components are missing, and are required to run this application".
 * 
 * This Launcher class does NOT extend Application, allowing the JVM to start cleanly
 * and initialize the JavaFX runtime from standard Maven dependencies in IntelliJ IDEA.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}