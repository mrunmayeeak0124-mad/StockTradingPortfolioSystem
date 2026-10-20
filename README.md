# InvestIQ: Stock Trading & Portfolio Management System
> **A Virtual Investment Simulator with an Educational Financial Advisor**  
> *Second-Year B.Tech Information Technology — Object-Oriented Programming (OOP) Mini-Project*

---

## 📸 Desktop Application UI Showcase

Here is a live look at the modern fintech interface of **InvestIQ**:

![InvestIQ Desktop UI Preview](docs/screenshots/app_preview.png)

---

## 📌 1. Project Overview & Objective

The **Stock Trading & Portfolio Management System (InvestIQ)** is a complete, professional desktop application developed in **Java 21** using **JavaFX**, **SQLite (JDBC)**, and **Maven**. It provides an authentic financial simulation environment where users can learn how financial markets operate, practice active stock trading and portfolio management using **virtual currency**, understand risk diversification, and receive rule-based educational advisory insights.

### Core Objectives
1. **Zero Financial Risk:** Operates strictly with virtual currency (starts with ₹1,00,000.00 mock balance). No real bank credentials, brokerage accounts, or real money are ever used.
2. **Pedagogical Integrity:** Designed from the ground up to demonstrate every foundational Object-Oriented Programming (OOP) concept with clean architectural separation.
3. **Institutional Precision:** All monetary amounts, transaction fees, and calculations use `java.math.BigDecimal` with `HALF_UP` rounding to prevent binary floating-point inaccuracies.
4. **Holistic Wealth Planning:** Goes beyond simple stock trading by incorporating ETF/Bond asset classes, goal tracking (retirement, education), EMI home loan planning, and automated portfolio rebalancing checks.
5. **Real-Time Multithreading:** Features a background market ticker thread updating simulated quotes at fixed intervals and asynchronous worker threads for non-blocking file handling.

---

## 🛠️ 2. Technology Stack & Prerequisites

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Language** | Java 21 (LTS) | Modern Java records, pattern matching, switch expressions, streams |
| **GUI Framework** | JavaFX 21 (OpenJFX) | JavaFX Controls, Charts (Line, Pie), CSS-based modern dark theme |
| **Build Tool** | Apache Maven 3.9+ | Automated dependency resolution and lifecycle management |
| **Database** | SQLite 3 via JDBC | Local transactional persistence with ACID compliance (`stocktrading.db`) |
| **Testing** | JUnit 5 (Jupiter) | 44 unit tests validating core math, trading rules, and persistence |
| **IDE** | IntelliJ IDEA | Fully configured and ready to open as an existing Maven project |

---

## 🏗️ 3. Software Architecture & Design Patterns

The codebase is structured under `com.stockportfolio` into distinct decoupled packages:

```
com.stockportfolio
├── database/        # Database initialization, SQLite connection management, and data seeder
├── exception/       # Custom business exception hierarchy (Checked & Unchecked)
├── model/           # Core domain entities, enums, records, and polymorphic hierarchies
├── provider/        # Market data provider abstraction and simulated exchange rates
├── repository/      # Data Access Objects (DAO) implementing JDBC CRUD queries
├── service/         # Business logic layer (Trading, Portfolio, Advisor, Loans, Goals, Multithreading)
├── ui/              # JavaFX views, event dispatcher (UIContext), and components
└── util/            # High-precision financial math (MoneyUtil, FeeCalculator, HomeLoanCalculator)
```

### Applied Design Patterns
- **Repository Pattern:** Separates database access code (`AssetRepository`, `TransactionRepository`, `TraderRepository`) from business logic.
- **Strategy / Provider Pattern:** `MarketDataProvider` interface allows seamless swapping between `MockMarketDataProvider` and real live REST API market providers.
- **Observer Pattern:** `UIContext` maintains dynamic data listeners (`notifyDataChanged()`) so that any trade, price refresh, or deposit automatically updates dashboard counters and charts across all tabs.
- **Model-View-Controller (MVC) Separation:** JavaFX views handle presentation and user input, delegating all validation and calculations to the service layer.

---

## 🎓 4. OOP Concepts Mapping (Viva Reference)

| OOP Principle | Implementation in Code | Method / Class Reference |
| :--- | :--- | :--- |
| **Encapsulation** | Private fields, validation invariants, immutable collections, `BigDecimal` scale control. | [`Position`](src/main/java/com/stockportfolio/model/Position.java), [`Trader`](src/main/java/com/stockportfolio/model/Trader.java), [`FinancialProfile`](src/main/java/com/stockportfolio/model/FinancialProfile.java) |
| **Inheritance** | Base asset hierarchy (`Stock`, `ETF`, `Bond` extending `Asset`) and order hierarchy (`BuyOrder`, `SellOrder` extending `Order`). | [`Asset.java`](src/main/java/com/stockportfolio/model/Asset.java), [`Order.java`](src/main/java/com/stockportfolio/model/Order.java) |
| **Abstraction** | Interfaces decoupling data sources and contracts from concrete implementations. | [`MarketDataProvider`](src/main/java/com/stockportfolio/provider/MarketDataProvider.java), [`ExchangeRateProvider`](src/main/java/com/stockportfolio/provider/ExchangeRateProvider.java) |
| **Polymorphism** | Dynamic dispatch of abstract methods (`getDetails()`, `validateBeforeExecution()`). | [`Stock.getDetails()`](src/main/java/com/stockportfolio/model/Stock.java), [`Bond.getDetails()`](src/main/java/com/stockportfolio/model/Bond.java) |
| **Multithreading** | Background daemon thread running scheduled price simulation (`ScheduledExecutorService`) and JavaFX `Platform.runLater` UI synchronization; asynchronous worker threads for non-blocking file exports. | [`MarketSimulationScheduler.java`](src/main/java/com/stockportfolio/service/MarketSimulationScheduler.java), [`DataExportView.java`](src/main/java/com/stockportfolio/ui/DataExportView.java) |
| **File Handling** | Generating structured CSV files using Java NIO (`Files`, `Path`, `BufferedWriter`) for transactions, holdings, and portfolio summaries. | [`CsvExportService.java`](src/main/java/com/stockportfolio/service/CsvExportService.java) |
| **Collections** | Strategic usage of `Map<String, Position>` for O(1) lookups, `Set<String>` for unique watchlists, and `List<Transaction>` for audit logs. | [`Portfolio.java`](src/main/java/com/stockportfolio/model/Portfolio.java), [`Watchlist.java`](src/main/java/com/stockportfolio/model/Watchlist.java) |
| **Exception Handling** | Custom checked and unchecked exception hierarchy with clean propagation. | [`TradingException`](src/main/java/com/stockportfolio/exception/TradingException.java), [`InsufficientFundsException`](src/main/java/com/stockportfolio/exception/InsufficientFundsException.java) |

---

## 📱 5. Application Modules (14 Comprehensive Views)

1. **Dashboard:** Real-time net worth cards (Cash, Holdings, Total Account Value), portfolio asset allocation pie chart, recent trade activity table, and quick navigation.
2. **Market Explorer:** Search and filter 10+ simulated assets (Stocks, ETFs, Sovereign Bonds), real-time simulated quote prices, and 1-click watchlist toggling.
3. **Asset Details:** Interactive historical price chart (`LineChart`), high/low metrics, and 7-day / 14-day Simple Moving Average (SMA) technical indicators.
4. **Trading Terminal:** Order placement screen supporting **Market** and **Limit** orders for both **Buy** and **Sell** sides, real-time fee calculation (0.10%), and atomic execution checks.
5. **Order Book:** Full audit table of all placed orders with real-time status (`PENDING`, `EXECUTED`, `CANCELLED`, `REJECTED`) and cancel order capability.
6. **Portfolio Holdings:** Real-time monitoring of all open positions: average cost basis, current market price, unrealized P/L, percentage return, and diversification score.
7. **Virtual Wallet:** Safe virtual deposit terminal (1-click preset buttons: ₹10,000, ₹50,000, ₹1,00,000) with complete deposit audit trail.
8. **Transaction History:** Immutable historical ledger of every BUY, SELL, and DEPOSIT event with date filtering and full transaction details.
9. **Market Insights & FX:** Comparative multi-asset price performance chart and simulated currency converter supporting INR, USD, EUR, and GBP.
10. **Educational Financial Advisor:** Rule-based diagnostic engine checking emergency fund health (< 3 months buffer), debt-to-income (DTI > 35%), single-stock concentration (> 25%), and target asset allocation drift, coupled with interactive **50/30/20 Budgeting** (Needs vs. Wants vs. Savings) and **70/15/15 Financial Framework** (Living vs. Investing vs. Debt Buffer with 30% post-debt acceleration).
11. **Financial Goal Planner:** Goal creation and tracking with monthly required savings calculation, compounding future value forecasting, and progress bars.
12. **Home Purchase & Loan Planner:** Comprehensive mortgage EMI calculator, amortization summary, upfront cash requirement check (down payment + stamp duty + legal), and LTV limits.
13. **Education Center:** Searchable financial literacy dictionary covering 16 key investment terms (P/E Ratio, Compounding, 50/30/20 Rule, 70/15/15 Framework, Diversification, LTV, EMI, etc.).
14. **Data Export & Reporting:** Instant CSV report generator exporting `transactions.csv`, `portfolio_holdings.csv`, and `performance_summary.csv` to the local `exports/` folder asynchronously.

---

## ⭐ 6. Top 5+ Unique Features of InvestIQ

Unlike basic trading calculators or static portfolio loggers, **InvestIQ** integrates 6 sophisticated features that distinguish it as an institutional-grade educational simulator:

1. **Multithreaded Real-Time Market Ticker & Autonomous Limit Order Triggering:**  
   A background daemon thread (`MarketSimulationScheduler`) dynamically updates asset quotes with realistic Brownian drift every 5 seconds. When quotes fluctuate, pending limit orders are evaluated and executed atomically without freezing the JavaFX user interface.
2. **Dual Personal Finance Budgeting Engine (50/30/20 & 70/15/15 Rules):**  
   Provides real-time mathematical breakdown comparing the trader's actual cash surplus against the standard **50% Needs / 30% Wants / 20% Savings** model and the **70% Living / 15% Investing / 15% Debt-Emergency** framework (which unlocks an accelerated 30% investing rate upon clearing debt).
3. **Comprehensive Home Purchase & Mortgage Feasibility Planner:**  
   Simulates real-world home buying by calculating monthly EMIs across tenures, breaking down principal vs. interest amortization, and enforcing upfront liquidity requirements (20% Down Payment + 6% Stamp Duty + Legal Fees) so users understand total acquisition costs.
4. **Compounding Wealth Goal Simulator:**  
   Allows users to define custom financial milestones (Retirement, House, Education) and calculates required monthly SIP contributions under both zero-growth linear models and compound interest models, displaying dynamic progress rings.
5. **Multi-Currency Real-Time FX Conversion:**  
   Facilitates cross-currency asset valuation across INR, USD, EUR, and GBP with instant exchange rate calculation, demonstrating foreign exchange risk in international portfolios.
6. **Non-Blocking Asynchronous CSV Data Export Engine:**  
   Offloads disk I/O file generation to independent worker threads (`new Thread(...).start()`), exporting audit-ready CSV reports for holdings and transactions with safe `Platform.runLater()` desktop notifications.

---

## 🚀 7. Step-by-Step Execution Guide (IntelliJ IDEA)

### Method 1: Running in IntelliJ IDEA (Recommended)
1. **Open Project:** Launch IntelliJ IDEA, select **Open**, and navigate to the project directory: `StockTradingPortfolioSystem`.
2. **Verify Project SDK:**
   - Go to **File ➔ Project Structure ➔ Project**.
   - Ensure the SDK is set to **Java 21**.
   - Ensure Language Level is set to **21 - Records, pattern matching...**.
3. **Launch Application:**
   - In the project tree, navigate to: `src/main/java/com/stockportfolio/Launcher.java`.
   - Right-click `Launcher.java` and click **Run 'Launcher.main()'**.
   - The JavaFX desktop interface will initialize maximized with demo trader Alex Morgan and initial holdings.

### Method 2: Running from Command Line / Terminal
```powershell
mvn javafx:run
```

---

## 🧪 8. Running Unit Tests

The test suite includes **46 automated unit tests** verifying all business calculations and edge cases:
- `TradingServiceTest` (15 tests: Buy/Sell math, fee deduction, limit order triggering, insufficient funds).
- `PortfolioServiceTest` (8 tests: Cost basis averaging, realized/unrealized P/L, portfolio summary).
- `MarketServiceTest` (6 tests: Mock price movement bounds, watchlist persistence, quote generation).
- `MarketAnalyticsServiceTest` (5 tests: SMA windows, percentage changes, volatility calculation).
- `FinancialAdvisorServiceTest` (7 tests: Allocation drift, concentration warnings, 50/30/20 & 70/15/15 rules).
- `HomeLoanCalculatorTest` (5 tests: Standard EMI math, zero-interest edge cases, upfront costs).

To run all unit tests:
```powershell
mvn test
```
**Expected Result:**
```
[INFO] Results:
[INFO] Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🔄 8. Database Reset & Schema Details

All persistent data is stored in a local SQLite database file at `database/stocktrading.db`.

If you ever wish to reset the application to its original clean demonstration state:
1. Close the running application.
2. Delete the file `database/stocktrading.db`.
3. Re-run `Launcher.java`. The `DatabaseManager` and `DataSeeder` will automatically recreate all tables and re-populate the demo assets, initial trader, and default financial profile.

---

## 📝 9. Viva Voce Questions & Answers (OOP Focus)

### Q1: Why did you use `BigDecimal` instead of `double` for monetary calculations?
> **Answer:** `double` and `float` use IEEE 754 binary floating-point representation, which cannot accurately represent fractional decimal values like `0.10` or `0.01` (causing rounding errors like `0.30000000000000004`). In financial systems, every paisa counts. `BigDecimal` provides arbitrary-precision signed decimal numbers with explicit control over rounding modes (`RoundingMode.HALF_UP`).

### Q2: How did you implement multithreading in this application?
> **Answer:** We implemented multithreading in two distinct areas:
> 1. In `MarketSimulationScheduler`, a `ScheduledExecutorService` runs a background daemon thread (`Market-Simulation-Worker`) that simulates live price ticks and evaluates pending limit orders every 5 seconds. To prevent UI thread freezing and race conditions, state updates are dispatched safely to the JavaFX Application Thread via `Platform.runLater()`.
> 2. In `DataExportView`, generating CSV reports runs asynchronously in dedicated worker threads, ensuring the UI remains 100% responsive during disk file I/O operations.

### Q3: How did you implement polymorphism in the asset and order structures?
> **Answer:** We created abstract base classes `Asset` and `Order`. Subclasses such as `Stock`, `ETF`, and `Bond` override the abstract method `getDetails()` to return asset-specific descriptions. Similarly, `BuyOrder` and `SellOrder` implement their own order execution logic polymorphically.

### Q4: What collections did you use and why?
> **Answer:**
> - `Map<String, Position>` in `Portfolio` provides $O(1)$ constant time lookups when querying whether a stock is already owned.
> - `Set<String>` in `Watchlist` guarantees uniqueness so that an asset cannot be favorited multiple times.
> - `List<Transaction>` preserves the chronological insertion order of financial audit events.

### Q5: How is database consistency maintained during a trade?
> **Answer:** Transactions are managed using SQLite connection transactions (`conn.setAutoCommit(false)`). When a trade executes, four atomic actions take place: updating trader cash, updating asset position, inserting the transaction log, and updating the order status. If any operation fails, `conn.rollback()` is executed, preventing partial updates.
