package com.stockportfolio.ui;

import com.stockportfolio.model.FinancialGoal;
import com.stockportfolio.model.GoalCategory;
import com.stockportfolio.model.GoalStatus;
import com.stockportfolio.service.GoalPlanningService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Financial Goal Planning & Progress Monitoring view.
 * Allows setting, funding, and projecting savings goals with hypothetical compounding returns.
 */
public class GoalsView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);
    private final VBox goalsCardList = new VBox(16);

    // Goal calculator elements
    private ComboBox<FinancialGoal> goalCalcCombo;
    private Slider returnSlider;
    private Label sliderValueLabel;
    private Label calcRequiredSavingsLabel;
    private Label calcLinearSavingsLabel;
    private Label compoundingSavingsSavingsLabel;

    public GoalsView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildGoalsSection();
        buildCalculatorSection();

        setContent(container);
        refreshGoals();
        context.addDataChangeListener(this::refreshGoals);
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Financial Goals & Milestone Tracking");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Define long-term wealth milestones, monitor savings velocity, and project required contributions.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button addGoalBtn = new Button("➕ Create New Goal");
        addGoalBtn.getStyleClass().addAll("btn", "btn-primary");
        addGoalBtn.setOnAction(e -> showAddGoalDialog());

        HBox header = new HBox(titleBox, addGoalBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(header);
    }

    private void buildGoalsSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Active Financial Milestones");
        title.getStyleClass().add("section-title");

        card.getChildren().addAll(title, goalsCardList);
        container.getChildren().add(card);
    }

    private void buildCalculatorSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Goal Savings Requirement & Compounding Simulator");
        title.getStyleClass().add("section-title");

        Label desc = new Label("Select a goal and adjust hypothetical annual return assumptions to see how compounding decreases monthly cash burden:");
        desc.getStyleClass().add("text-muted");

        GridPane gp = new GridPane();
        gp.setHgap(16);
        gp.setVgap(14);

        goalCalcCombo = new ComboBox<>();
        goalCalcCombo.getStyleClass().add("combo-box");
        goalCalcCombo.setPrefWidth(280);
        goalCalcCombo.setOnAction(e -> recalculateMonthlySavings());

        returnSlider = new Slider(0, 15, 8);
        returnSlider.setShowTickLabels(true);
        returnSlider.setShowTickMarks(true);
        returnSlider.setMajorTickUnit(3);
        returnSlider.setMinorTickCount(2);
        returnSlider.setPrefWidth(280);

        sliderValueLabel = new Label("8.0% p.a.");
        sliderValueLabel.getStyleClass().add("font-semibold");

        returnSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            sliderValueLabel.setText(String.format("%.1f%% p.a.", newVal.doubleValue()));
            recalculateMonthlySavings();
        });

        gp.add(new Label("Select Milestone:"), 0, 0);
        gp.add(goalCalcCombo, 1, 0);
        gp.add(new Label("Hypothetical Annual Return:"), 0, 1);
        HBox sliderBox = new HBox(10, returnSlider, sliderValueLabel);
        sliderBox.setAlignment(Pos.CENTER_LEFT);
        gp.add(sliderBox, 1, 1);

        // Results box
        HBox resBox = new HBox(20);
        resBox.setPadding(new Insets(14));
        resBox.getStyleClass().addAll("metric-card", "card-accent-teal");

        VBox r1 = new VBox(2);
        Label l1 = new Label("Linear Cash Contribution (0% Return):");
        l1.getStyleClass().add("card-subtitle");
        calcLinearSavingsLabel = new Label("₹0.00 / month");
        calcLinearSavingsLabel.getStyleClass().add("font-semibold");
        r1.getChildren().addAll(l1, calcLinearSavingsLabel);

        VBox r2 = new VBox(2);
        Label l2 = new Label("With Compounding Return:");
        l2.getStyleClass().add("card-subtitle");
        calcRequiredSavingsLabel = new Label("₹0.00 / month");
        calcRequiredSavingsLabel.getStyleClass().add("card-value");
        r2.getChildren().addAll(l2, calcRequiredSavingsLabel);

        VBox r3 = new VBox(2);
        Label l3 = new Label("Monthly Burden Reduced by:");
        l3.getStyleClass().add("card-subtitle");
        compoundingSavingsSavingsLabel = new Label("₹0.00 / month");
        compoundingSavingsSavingsLabel.getStyleClass().addAll("font-semibold", "text-positive");
        r3.getChildren().addAll(l3, compoundingSavingsSavingsLabel);

        resBox.getChildren().addAll(r1, r2, r3);

        card.getChildren().addAll(title, desc, gp, resBox);
        container.getChildren().add(card);
    }

    private void refreshGoals() {
        goalsCardList.getChildren().clear();
        List<FinancialGoal> goals = context.getGoalService().getGoals(context.getTrader().getUserId());

        if (goals.isEmpty()) {
            Label empty = new Label("No financial goals configured yet. Click '+ Create New Goal' above to begin planning.");
            empty.getStyleClass().add("text-muted");
            goalsCardList.getChildren().add(empty);
            return;
        }

        for (FinancialGoal g : goals) {
            VBox goalBox = new VBox(10);
            goalBox.getStyleClass().add("metric-card");
            goalBox.setPadding(new Insets(16));

            HBox h1 = new HBox(12);
            h1.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(g.getName());
            name.getStyleClass().add("view-title");
            HBox.setHgrow(name, Priority.ALWAYS);

            Label catBadge = new Label(g.getCategory().getDisplayName());
            catBadge.getStyleClass().addAll("badge", "badge-primary");

            Label statusBadge = new Label(g.getStatus().name());
            statusBadge.getStyleClass().addAll("badge", g.isAchieved() ? "badge-success" : "badge-secondary");

            Button fundBtn = new Button("💰 Add Savings");
            fundBtn.getStyleClass().addAll("btn-sm", "btn-primary");
            fundBtn.setOnAction(e -> showFundGoalDialog(g));

            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().addAll("btn-sm", "btn-danger");
            deleteBtn.setOnAction(e -> {
                context.getGoalService().deleteGoal(g.getGoalId());
                refreshGoals();
            });

            h1.getChildren().addAll(name, catBadge, statusBadge, fundBtn, deleteBtn);

            // Progress bar and amounts
            HBox h2 = new HBox(16);
            Label progressLbl = new Label("Saved: " + UIContext.formatMoney(g.getCurrentAmount())
                    + " of " + UIContext.formatMoney(g.getTargetAmount()) + " (" + g.getProgressPercentage() + "%)");
            progressLbl.getStyleClass().add("font-semibold");
            HBox.setHgrow(progressLbl, Priority.ALWAYS);

            long months = Math.max(0, g.getMonthsRemaining());
            Label timeLbl = new Label(months + " month(s) remaining (Target: " + g.getTargetDate() + ")");
            timeLbl.getStyleClass().add("text-muted");
            h2.getChildren().addAll(progressLbl, timeLbl);

            ProgressBar pb = new ProgressBar(Math.min(1.0, g.getProgressPercentage().doubleValue() / 100.0));
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.setPrefHeight(12);

            // Monthly required
            BigDecimal monthly = context.getGoalService().calculateRequiredMonthlySavings(g, BigDecimal.ZERO);
            Label mReq = new Label("💡 Required Monthly Savings: " + UIContext.formatMoney(monthly) + "/month to hit target on schedule.");
            mReq.getStyleClass().add("text-muted");

            goalBox.getChildren().addAll(h1, h2, pb, mReq);
            goalsCardList.getChildren().add(goalBox);
        }

        goalCalcCombo.getItems().clear();
        goalCalcCombo.getItems().addAll(goals);
        if (!goals.isEmpty()) {
            goalCalcCombo.setValue(goals.get(0));
            recalculateMonthlySavings();
        }
    }

    private void recalculateMonthlySavings() {
        FinancialGoal g = goalCalcCombo.getValue();
        if (g == null) return;

        BigDecimal linear = context.getGoalService().calculateRequiredMonthlySavings(g, BigDecimal.ZERO);
        calcLinearSavingsLabel.setText(UIContext.formatMoney(linear) + " / mo");

        BigDecimal rate = BigDecimal.valueOf(returnSlider.getValue());
        BigDecimal compounding = context.getGoalService().calculateRequiredMonthlySavings(g, rate);
        calcRequiredSavingsLabel.setText(UIContext.formatMoney(compounding) + " / mo");

        BigDecimal diff = linear.subtract(compounding).max(BigDecimal.ZERO);
        compoundingSavingsSavingsLabel.setText("-" + UIContext.formatMoney(diff) + " / mo");
    }

    private void showAddGoalDialog() {
        Dialog<FinancialGoal> dialog = new Dialog<>();
        dialog.setTitle("Create New Financial Goal");
        dialog.setHeaderText("Set a target amount and date for a new financial milestone.");

        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(12);
        gp.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Higher Education Fund");

        ComboBox<GoalCategory> catCombo = new ComboBox<>();
        catCombo.getItems().addAll(GoalCategory.values());
        catCombo.setValue(GoalCategory.WEALTH_BUILDING);

        TextField targetField = new TextField("500000.00");
        TextField currentField = new TextField("0.00");

        DatePicker datePicker = new DatePicker(LocalDate.now().plusYears(3));

        gp.add(new Label("Goal Name:"), 0, 0);
        gp.add(nameField, 1, 0);
        gp.add(new Label("Category:"), 0, 1);
        gp.add(catCombo, 1, 1);
        gp.add(new Label("Target Amount (INR):"), 0, 2);
        gp.add(targetField, 1, 2);
        gp.add(new Label("Initial Current Savings:"), 0, 3);
        gp.add(currentField, 1, 3);
        gp.add(new Label("Target Completion Date:"), 0, 4);
        gp.add(datePicker, 1, 4);

        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    String name = nameField.getText().trim();
                    if (name.isEmpty()) throw new IllegalArgumentException("Goal name cannot be empty.");
                    BigDecimal target = new BigDecimal(targetField.getText().trim());
                    BigDecimal current = new BigDecimal(currentField.getText().trim());
                    LocalDate date = datePicker.getValue();

                    FinancialGoal newGoal = new FinancialGoal(
                            context.getTrader().getUserId(), name, catCombo.getValue(), target, current, date);
                    context.getGoalService().addGoal(newGoal);
                    refreshGoals();
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Invalid input: " + ex.getMessage());
                    a.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showFundGoalDialog(FinancialGoal goal) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Allocate Savings to " + goal.getName());
        dialog.setHeaderText("Add funds towards this goal (Current: " + UIContext.formatMoney(goal.getCurrentAmount()) + ")");

        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(12);
        gp.setPadding(new Insets(20));

        TextField amountField = new TextField("10000.00");
        gp.add(new Label("Contribution Amount (INR):"), 0, 0);
        gp.add(amountField, 1, 0);

        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    BigDecimal amt = new BigDecimal(amountField.getText().trim());
                    context.getGoalService().contributeToGoal(goal.getGoalId(), context.getTrader().getUserId(), amt);
                    refreshGoals();
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Invalid amount: " + ex.getMessage());
                    a.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }
}
