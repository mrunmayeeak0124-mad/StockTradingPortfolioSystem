package com.stockportfolio.ui;

import com.stockportfolio.model.HomeLoanPlan;
import com.stockportfolio.util.HomeLoanCalculator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

/**
 * Home Purchase & Mortgage Loan Planning View.
 * Provides interactive EMI calculation, loan amortization summaries, and a comprehensive
 * legal/financial property purchase checklist.
 */
public class HomePlannerView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);

    // Inputs
    private TextField housePriceField;
    private TextField downPaymentField;
    private TextField rateField;
    private Slider tenureSlider;
    private Label tenureLabel;
    private TextField additionalCostsField;

    // Outputs
    private Label emiValueLabel;
    private Label principalValueLabel;
    private Label totalRepaymentLabel;
    private Label totalInterestLabel;
    private Label upfrontCashLabel;
    private Label ltvRatioLabel;

    public HomePlannerView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildDisclaimer();
        buildCalculatorSection();
        buildEducationalChecklist();

        setContent(container);
        recalculate();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Home Purchase & Mortgage Loan Planner");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Estimate monthly EMI, total interest burden, upfront cash obligations, and legal verification essentials.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    private void buildDisclaimer() {
        HBox banner = new HBox(12);
        banner.getStyleClass().add("info-banner");
        banner.setPadding(new Insets(12, 16, 12, 16));

        Label text = new Label("💡 Educational Planning Tool: This mortgage simulator helps you evaluate affordability before speaking to a lender. It does not guarantee bank loan sanction, property title validity, or specific builder credentials.");
        text.setWrapText(true);
        text.getStyleClass().add("text-muted");
        banner.getChildren().add(text);
        container.getChildren().add(banner);
    }

    private void buildCalculatorSection() {
        HBox split = new HBox(24);

        // Left: Inputs Card
        VBox inputCard = new VBox(16);
        inputCard.getStyleClass().add("content-card");
        inputCard.setPadding(new Insets(20));
        HBox.setHgrow(inputCard, Priority.ALWAYS);

        Label inTitle = new Label("Property & Mortgage Parameters");
        inTitle.getStyleClass().add("section-title");

        GridPane gp = new GridPane();
        gp.setHgap(14);
        gp.setVgap(14);

        housePriceField = new TextField("6000000.00");
        housePriceField.getStyleClass().add("text-input");
        housePriceField.textProperty().addListener((obs, oldVal, newVal) -> recalculate());

        downPaymentField = new TextField("1200000.00");
        downPaymentField.getStyleClass().add("text-input");
        downPaymentField.textProperty().addListener((obs, oldVal, newVal) -> recalculate());

        rateField = new TextField("8.50");
        rateField.getStyleClass().add("text-input");
        rateField.textProperty().addListener((obs, oldVal, newVal) -> recalculate());

        tenureSlider = new Slider(5, 30, 20);
        tenureSlider.setShowTickLabels(true);
        tenureSlider.setShowTickMarks(true);
        tenureSlider.setMajorTickUnit(5);
        tenureSlider.setMinorTickCount(4);
        tenureSlider.setSnapToTicks(true);
        tenureSlider.setPrefWidth(220);

        tenureLabel = new Label("20 Years");
        tenureLabel.getStyleClass().add("font-semibold");
        tenureSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            tenureLabel.setText(String.format("%d Years", newVal.intValue()));
            recalculate();
        });

        additionalCostsField = new TextField("420000.00"); // 7% stamp duty/reg
        additionalCostsField.getStyleClass().add("text-input");
        additionalCostsField.textProperty().addListener((obs, oldVal, newVal) -> recalculate());

        gp.add(new Label("Property Price (INR):"), 0, 0);
        gp.add(housePriceField, 1, 0);

        gp.add(new Label("Available Down Payment:"), 0, 1);
        gp.add(downPaymentField, 1, 1);

        gp.add(new Label("Annual Interest Rate (%):"), 0, 2);
        gp.add(rateField, 1, 2);

        gp.add(new Label("Loan Tenure:"), 0, 3);
        HBox sliderRow = new HBox(10, tenureSlider, tenureLabel);
        sliderRow.setAlignment(Pos.CENTER_LEFT);
        gp.add(sliderRow, 1, 3);

        gp.add(new Label("Stamp Duty & Legal Charges:"), 0, 4);
        gp.add(additionalCostsField, 1, 4);

        Button resetBtn = new Button("🔄 Recalculate");
        resetBtn.getStyleClass().addAll("btn", "btn-secondary");
        resetBtn.setOnAction(e -> recalculate());
        gp.add(resetBtn, 1, 5);

        inputCard.getChildren().addAll(inTitle, gp);

        // Right: Outputs Card
        VBox outputCard = new VBox(16);
        outputCard.getStyleClass().add("content-card");
        outputCard.setPadding(new Insets(20));
        outputCard.setPrefWidth(400);

        Label outTitle = new Label("Repayment Breakdown & Cash Requirement");
        outTitle.getStyleClass().add("section-title");

        VBox emiCard = new VBox(4);
        emiCard.getStyleClass().addAll("metric-card", "card-accent-teal");
        emiCard.setPadding(new Insets(14));
        Label emiSub = new Label("Estimated Monthly Installment (EMI):");
        emiSub.getStyleClass().add("card-subtitle");
        emiValueLabel = new Label("₹0.00");
        emiValueLabel.getStyleClass().add("card-value");
        emiCard.getChildren().addAll(emiSub, emiValueLabel);

        GridPane resGrid = new GridPane();
        resGrid.setHgap(12);
        resGrid.setVgap(12);

        principalValueLabel = new Label("₹0.00");
        totalInterestLabel = new Label("₹0.00");
        totalRepaymentLabel = new Label("₹0.00");
        upfrontCashLabel = new Label("₹0.00");
        ltvRatioLabel = new Label("80.00%");

        addResultRow(resGrid, 0, "Loan Principal Amount:", principalValueLabel);
        addResultRow(resGrid, 1, "Total Interest Over Tenure:", totalInterestLabel);
        addResultRow(resGrid, 2, "Total Repayment (P + I):", totalRepaymentLabel);
        addResultRow(resGrid, 3, "Upfront Cash Required:", upfrontCashLabel);
        addResultRow(resGrid, 4, "Loan-to-Value (LTV):", ltvRatioLabel);

        outputCard.getChildren().addAll(outTitle, emiCard, resGrid);

        split.getChildren().addAll(inputCard, outputCard);
        container.getChildren().add(split);
    }

    private void addResultRow(GridPane grid, int row, String label, Label val) {
        Label l = new Label(label);
        l.getStyleClass().add("text-muted");
        grid.add(l, 0, row);
        grid.add(val, 1, row);
    }

    private void recalculate() {
        try {
            BigDecimal price = new BigDecimal(housePriceField.getText().trim());
            BigDecimal down = new BigDecimal(downPaymentField.getText().trim());
            BigDecimal rate = new BigDecimal(rateField.getText().trim());
            int tenure = (int) tenureSlider.getValue();
            BigDecimal costs = new BigDecimal(additionalCostsField.getText().trim());

            HomeLoanPlan plan = HomeLoanCalculator.calculatePlan(price, down, rate, tenure, costs);

            emiValueLabel.setText(UIContext.formatMoney(plan.getMonthlyEmi()));
            principalValueLabel.setText(UIContext.formatMoney(plan.getLoanAmount()));
            totalInterestLabel.setText(UIContext.formatMoney(plan.getTotalInterest()));
            totalRepaymentLabel.setText(UIContext.formatMoney(plan.getTotalRepayment()));
            upfrontCashLabel.setText(UIContext.formatMoney(plan.getTotalUpfrontCashRequired())
                    + " (Down + Taxes)");
            ltvRatioLabel.setText(plan.getLoanToValueRatio().toPlainString() + "% LTV");

        } catch (Exception e) {
            emiValueLabel.setText("Check Inputs");
        }
    }

    private void buildEducationalChecklist() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Essential First-Time Homebuyer's Checklist");
        title.getStyleClass().add("section-title");

        VBox list = new VBox(10);
        list.getChildren().addAll(
                createCheckItem("Down Payment Readiness", "Ensure you have at least 20% down payment saved in cash without depleting emergency funds."),
                createCheckItem("Stamp Duty & Statutory Taxes", "Budget 6-8% in out-of-pocket cash for stamp duty, registration fees, and municipal property cess."),
                createCheckItem("Debt-to-Income (DTI) Sanity Check", "Ensure your total monthly EMI does not exceed 35-40% of net post-tax take-home pay."),
                createCheckItem("Title Deeds & Clear Ownership", "Hire an independent property advocate to verify 30-year chain deeds and non-encumbrance certificate."),
                createCheckItem("Sanctioned Plans & RERA Verification", "Verify layout approval from municipal corporation and registered project status on state RERA portal."),
                createCheckItem("Maintenance & Society Dues", "Inquire about recurring society maintenance, parking charges, property taxes, and sinking fund contributions."),
                createCheckItem("Loan Prepayment Flexibility", "Confirm floating-rate loan terms have ZERO prepayment penalty under regulatory rules."),
                createCheckItem("Liquid Emergency Buffer Retention", "Never spend every rupee on house possession. Maintain at least 6 months of living expenses in reserve.")
        );

        card.getChildren().addAll(title, list);
        container.getChildren().add(card);
    }

    private HBox createCheckItem(String title, String desc) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        CheckBox cb = new CheckBox();
        cb.getStyleClass().add("check-box");

        VBox text = new VBox(2);
        Label t = new Label(title);
        t.getStyleClass().add("font-semibold");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.getStyleClass().add("text-muted");
        text.getChildren().addAll(t, d);

        row.getChildren().addAll(cb, text);
        return row;
    }
}
