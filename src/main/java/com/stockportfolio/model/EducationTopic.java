package com.stockportfolio.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Educational concepts and glossary definitions for beginner investors.
 */
public class EducationTopic {

    private final String id;
    private final String term;
    private final String category;
    private final String definition;
    private final String detailedExplanation;
    private final String example;
    private final String keyTakeaway;

    public EducationTopic(String id, String term, String category, String definition,
                          String detailedExplanation, String example, String keyTakeaway) {
        this.id = id;
        this.term = term;
        this.category = category;
        this.definition = definition;
        this.detailedExplanation = detailedExplanation;
        this.example = example;
        this.keyTakeaway = keyTakeaway;
    }

    public String getId() { return id; }
    public String getTerm() { return term; }
    public String getCategory() { return category; }
    public String getDefinition() { return definition; }
    public String getDetailedExplanation() { return detailedExplanation; }
    public String getExample() { return example; }
    public String getKeyTakeaway() { return keyTakeaway; }

    @Override
    public String toString() {
        return term + " (" + category + ")";
    }

    /** Pre-loaded glossary of essential financial concepts. */
    public static List<EducationTopic> getDefaultTopics() {
        List<EducationTopic> list = new ArrayList<>();

        list.add(new EducationTopic(
                "ED01", "Stocks & Shares", "Investments",
                "Units of partial equity ownership in a public corporation.",
                "When you buy a share, you own a tiny fractional slice of that company. If the company grows and earns profits, share prices typically rise, and some companies pay cash dividends. If the company underperforms, share value drops.",
                "Buying 10 shares of Reliance Industries at ₹2,450 makes you a shareholder entitled to corporate dividends.",
                "Equities offer high long-term growth potential but carry short-term price fluctuations."
        ));

        list.add(new EducationTopic(
                "ED02", "Exchange Traded Funds (ETFs)", "Investments",
                "Pooled investment funds traded on stock exchanges like individual shares.",
                "An ETF holds an underlying basket of securities (such as the top 50 companies in an index or physical gold). They provide instant broad diversification at very low management cost without having to buy dozens of individual stocks.",
                "Nifty BeES tracks all 50 companies of the Nifty 50 index in a single tradable unit.",
                "ETFs are an ideal building block for beginner and long-term passive investors."
        ));

        list.add(new EducationTopic(
                "ED03", "Bonds & Coupon Rates", "Investments",
                "Fixed-income debt instruments where an investor loans money to an entity (government or corporation).",
                "Bonds have a face value (principal), a coupon rate (annual interest paid to the lender), and a maturity date when the face value is refunded in full. They offer stability and predictable income compared to stocks.",
                "A ₹1,000 Government Bond with a 7.10% coupon pays ₹71.00 interest annually until 2034.",
                "Bonds help cushion equity downturns and provide steady, defensive portfolio income."
        ));

        list.add(new EducationTopic(
                "ED04", "Risk vs. Return", "Core Principles",
                "The fundamental financial relationship: potential returns increase in tandem with risk.",
                "Low-risk investments like bank fixed deposits and sovereign bonds guarantee capital return but offer modest yields. High-risk investments like equities or growth sectors offer higher wealth creation but feature higher chance of capital loss.",
                "Equities may deliver 12-15% over a decade but can fluctuate -20% in a bad year.",
                "Align your asset mix with your psychological tolerance and investment time horizon."
        ));

        list.add(new EducationTopic(
                "ED05", "Diversification", "Core Principles",
                "Spreading investments across different assets and sectors to reduce individual risk.",
                "Diversification ensures that a steep drop in one company or sector does not devastate your overall wealth. 'Do not put all your eggs in one basket.'",
                "Holding IT stocks, banking stocks, gold ETFs, and government bonds together cushions single-sector crashes.",
                "Diversification eliminates unsystematic (company-specific) risk, though market risk remains."
        ));

        list.add(new EducationTopic(
                "ED06", "Liquidity", "Core Principles",
                "How rapidly an asset can be converted into ready cash without losing market value.",
                "Cash and large-cap stocks are highly liquid. Real estate and fixed-term provident funds are illiquid because selling them takes months or imposes penalties.",
                "You can sell Nifty BeES shares in seconds, whereas selling a house takes months.",
                "Never invest money you need in the next 1-2 years in illiquid or highly volatile instruments."
        ));

        list.add(new EducationTopic(
                "ED07", "Volatility", "Market Mechanics",
                "The degree of variation and rapid swings of an asset's trading price over time.",
                "High volatility means prices swing dramatically up and down within short intervals. It indicates short-term uncertainty rather than permanent loss of business value.",
                "A tech stock swinging between ₹1,400 and ₹1,650 in a single week shows high volatility.",
                "Long-term investors should not panic during routine market volatility cycles."
        ));

        list.add(new EducationTopic(
                "ED08", "Market Order vs. Limit Order", "Market Mechanics",
                "Market orders execute immediately at the best available price; Limit orders execute only at or better than a target price.",
                "A Market Order prioritizes speed of execution over exact price. A Limit Order guarantees your execution price (or better) but may remain pending forever if the market doesn't reach your limit.",
                "A limit buy on TCS at ₹3,800 will trigger only when the market falls to ₹3,800 or below.",
                "Use limit orders to prevent paying excessive prices in rapidly moving markets."
        ));

        list.add(new EducationTopic(
                "ED09", "Brokerage & Transaction Fees", "Market Mechanics",
                "Fees, commissions, stamp duties, and regulatory levies charged on order execution.",
                "Trading costs eat into net returns. High-frequency turnover multiplies fee deductions, making disciplined, low-turnover investing more profitable for ordinary traders.",
                "Buying ₹10,000 worth of shares with a 0.10% fee costs ₹10.00, making the total payable ₹10,010.00.",
                "Always check transaction costs before executing frequent or small-sized trades."
        ));

        list.add(new EducationTopic(
                "ED10", "Inflation & Compounding", "Core Principles",
                "Inflation erodes purchasing power; Compounding generates earnings on reinvested earnings.",
                "If inflation is 6% per annum, money sitting in a 3% savings account actually loses purchasing power over time. Compounding is 'interest on interest' that turns small regular savings into exponential wealth over 15-20 years.",
                "₹10,000 invested at 12% annual return compounds to ~₹31,000 in 10 years and ~₹96,000 in 20 years.",
                "Start early to harness the exponential power of compounding against inflation."
        ));

        list.add(new EducationTopic(
                "ED11", "Equated Monthly Installment (EMI)", "Lending & Home Loans",
                "A fixed monthly payment made by a borrower to a lender to repay a loan.",
                "Every EMI consists of two parts: the interest accrued on the outstanding balance and a portion of the principal. In early years, interest dominates; in later years, principal dominates.",
                "A ₹40,00,000 loan at 8.5% for 20 years results in an EMI of approximately ₹34,713 per month.",
                "Ensure your total household EMIs stay below 40% of your net monthly income."
        ));

        list.add(new EducationTopic(
                "ED12", "Down Payment & Loan-to-Value (LTV)", "Lending & Home Loans",
                "The initial upfront cash paid by the buyer, and the ratio of loan amount to total property cost.",
                "Lenders typically finance only 75% to 85% of property value (LTV). The remaining 15% to 25% must come from the buyer's own personal savings as a down payment.",
                "For a ₹50 Lakh flat, an 80% LTV requires you to pay ₹10 Lakh in cash as a down payment.",
                "A larger down payment reduces loan principal, lowers monthly EMI, and saves massive interest."
        ));

        list.add(new EducationTopic(
                "ED13", "Stamp Duty & Registration Costs", "Lending & Home Loans",
                "Mandatory statutory taxes and government charges paid when purchasing real estate.",
                "Property buyers often forget that stamp duty (typically 5-7%) and registration fees (1%) are NOT included in the bank loan and must be paid in cash out of pocket at deed signing.",
                "On a ₹50 Lakh home, stamp duty and registration can add ₹3.5 Lakh to ₹4 Lakh in upfront cash.",
                "Always budget an extra 8-10% above the property price for legal and government levies."
        ));

        list.add(new EducationTopic(
                "ED14", "Emergency Savings Buffer", "Personal Finance",
                "Liquid cash reserves set aside to cover unexpected life emergencies.",
                "An emergency buffer covers job loss, medical emergencies, or home repairs without forcing you to sell your stock portfolio at a loss during a market crash.",
                "If monthly living expenses are ₹35,000, a 6-month emergency buffer is ₹2,10,000 kept in liquid deposits.",
                "Establish a 3 to 6 month emergency buffer BEFORE committing capital to risky equities."
        ));

        list.add(new EducationTopic(
                "ED15", "The 50/30/20 Budgeting Rule", "Personal Finance & Budgeting",
                "A balanced budgeting framework dividing monthly income into 50% Needs, 30% Wants, and 20% Savings & Investing.",
                "The 50/30/20 rule is an intuitive personal budgeting standard: (1) 50% Needs covers survival non-negotiables (rent, groceries, basic utilities, insurance); (2) 30% Wants covers discretionary lifestyle desires (dining out, entertainment, gadgets, leisure); (3) 20% Savings & Investing ensures future wealth creation and emergency reserves.",
                "On a monthly income of ₹50,000: ₹25,000 goes to rent and essentials, ₹15,000 to lifestyle and dining, and ₹10,000 directly into stocks, ETFs, and liquid savings.",
                "Ensures you enjoy today's lifestyle while systematically securing a 20% minimum savings habit for the future."
        ));

        list.add(new EducationTopic(
                "ED16", "The 70/15/15 Financial Framework", "Personal Finance & Budgeting",
                "A framework allocating 70% to Living Expenses, 15% to Investing, and 15% to Debt Repayment or Emergency Savings.",
                "This pragmatic rule unifies daily life: 70% covers all living expenses (needs and wants combined). 15% is immediately invested into long-term compounding assets. The remaining 15% aggressively attacks debt or builds your emergency fund. Crucially, once non-mortgage debts are cleared, that 15% shifts directly into investing, catapulting your investment rate to an accelerated 30%!",
                "On a monthly income of ₹60,000: ₹42,000 covers all monthly living expenses, ₹9,000 goes to equity investments, and ₹9,000 repays loans. Once debt-free, ₹18,000 (30%) is invested every month.",
                "Provides clear discipline for debt elimination with an automatic acceleration pathway to 30% long-term wealth compounding."
        ));

        return list;
    }
}
