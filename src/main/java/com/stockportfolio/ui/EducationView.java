package com.stockportfolio.ui;

import com.stockportfolio.model.EducationTopic;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Financial Education Center: searchable investor glossary and a comprehensive
 * pre-investment diligence checklist.
 */
public class EducationView extends ScrollPane {

    private final UIContext context;
    private final VBox container = new VBox(24);
    private final VBox topicsContainer = new VBox(14);
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final List<EducationTopic> allTopics = EducationTopic.getDefaultTopics();

    public EducationView(UIContext context) {
        this.context = context;
        setFitToWidth(true);
        getStyleClass().add("view-scroll-pane");

        container.setPadding(new Insets(28));
        container.getStyleClass().add("view-container");

        buildHeader();
        buildChecklistSection();
        buildGlossarySection();

        setContent(container);
        refreshTopics();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Financial Education & Investor Glossary");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Master foundational financial literacy, market mechanisms, and prudent investment principles.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        container.getChildren().add(titleBox);
    }

    private void buildChecklistSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("✅ 7-Step 'Before You Invest' Pre-Flight Checklist");
        title.getStyleClass().add("section-title");

        Label desc = new Label("Complete this checklist before deploying hard-earned capital into any financial instrument:");
        desc.getStyleClass().add("text-muted");

        VBox list = new VBox(10);
        list.getChildren().addAll(
                createCheckItem("1. Clarify Investment Objective", "Are you aiming for capital appreciation, passive dividend income, or capital preservation?"),
                createCheckItem("2. Define Time Horizon", "Is your holding period short (<1 yr), medium (2-5 yrs), or long (>5 yrs)? Avoid volatile equities for short horizons."),
                createCheckItem("3. Ensure 3-6 Month Emergency Cash", "Never invest funds without first securing liquid emergency savings for medical and job contingencies."),
                createCheckItem("4. Confirm Risk & Volatility Tolerance", "Could you sleep soundly if this position experienced a temporary -20% market correction?"),
                createCheckItem("5. Verify Transaction Fees & Costs", "Ensure brokerage, exchange levies, and expense ratios will not disproportionately erode net returns."),
                createCheckItem("6. Check Portfolio Diversification", "Will this purchase cause a single asset to exceed 20-25% of your total holdings?"),
                createCheckItem("7. Understand the Underlying Business", "Do you understand how the company makes revenue and cash profit, or is this purely speculation?")
        );

        card.getChildren().addAll(title, desc, list);
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

    private void buildGlossarySection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Searchable Financial Glossary & Concepts");
        title.getStyleClass().add("section-title");

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 14, 10, 14));
        filterBar.getStyleClass().add("filter-bar");

        searchField.setPromptText("Search terms e.g. compounding, bond, liquidity, EMI...");
        searchField.setPrefWidth(320);
        searchField.getStyleClass().add("text-input");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshTopics());

        categoryCombo.getItems().addAll("All Categories", "Investments", "Core Principles", "Market Mechanics", "Lending & Home Loans", "Personal Finance");
        categoryCombo.setValue("All Categories");
        categoryCombo.getStyleClass().add("combo-box");
        categoryCombo.setOnAction(e -> refreshTopics());

        filterBar.getChildren().addAll(new Label("Filter Terms:"), categoryCombo, searchField);

        card.getChildren().addAll(title, filterBar, topicsContainer);
        container.getChildren().add(card);
    }

    private void refreshTopics() {
        topicsContainer.getChildren().clear();
        String query = searchField.getText().trim().toLowerCase();
        String catSel = categoryCombo.getValue();

        List<EducationTopic> filtered = allTopics.stream().filter(t -> {
            boolean matchesCat = "All Categories".equals(catSel) || t.getCategory().equalsIgnoreCase(catSel);
            boolean matchesQuery = query.isEmpty()
                    || t.getTerm().toLowerCase().contains(query)
                    || t.getDefinition().toLowerCase().contains(query)
                    || t.getDetailedExplanation().toLowerCase().contains(query);
            return matchesCat && matchesQuery;
        }).toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("No matching financial terms found. Try adjusting your search query.");
            empty.getStyleClass().add("text-muted");
            topicsContainer.getChildren().add(empty);
            return;
        }

        for (EducationTopic topic : filtered) {
            VBox topicCard = new VBox(8);
            topicCard.getStyleClass().add("metric-card");
            topicCard.setPadding(new Insets(16));

            HBox h = new HBox(12);
            h.setAlignment(Pos.CENTER_LEFT);

            Label termLabel = new Label(topic.getTerm());
            termLabel.getStyleClass().add("view-title");
            termLabel.setStyle("-fx-font-size: 17px;");
            HBox.setHgrow(termLabel, Priority.ALWAYS);

            Label badge = new Label(topic.getCategory());
            badge.getStyleClass().addAll("badge", "badge-primary");
            h.getChildren().addAll(termLabel, badge);

            Label defLabel = new Label("Definition: " + topic.getDefinition());
            defLabel.getStyleClass().add("font-semibold");
            defLabel.setWrapText(true);

            Label expLabel = new Label(topic.getDetailedExplanation());
            expLabel.getStyleClass().add("text-muted");
            expLabel.setWrapText(true);

            Label exLabel = new Label("📝 Practical Example: " + topic.getExample());
            exLabel.setStyle("-fx-text-fill: #38bdf8;");
            exLabel.setWrapText(true);

            Label takeLabel = new Label("💡 Key Takeaway: " + topic.getKeyTakeaway());
            takeLabel.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
            takeLabel.setWrapText(true);

            topicCard.getChildren().addAll(h, defLabel, expLabel, exLabel, takeLabel);
            topicsContainer.getChildren().add(topicCard);
        }
    }
}
