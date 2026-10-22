package com.financeanalyzer.ui.components;

import com.financeanalyzer.model.AnalyticsDTO;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.AlertLevel;
import com.financeanalyzer.service.InModalAnalyticsService;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.MoneyUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom JavaFX component displaying real-time in-modal spend and goal impact preview.
 * Debounces inputs, offloads analytics calculation to a background Task, and updates via Platform.runLater.
 * Visibly demonstrates FR-12 and Unit 5 dynamic event-driven GUI principles.
 */
public class SpendPreviewCard {

    private static final Logger LOGGER = AppLogger.getLogger(SpendPreviewCard.class);

    private final InModalAnalyticsService analyticsService;
    private final VBox rootBox;

    private Label titleLabel;
    private Label spentValueLabel;
    private Label budgetValueLabel;
    private Label remainingValueLabel;
    private Label utilisationLabel;
    private ProgressBar progressBar;
    private Label alertBanner;
    private VBox goalImpactBox;

    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SpendPreviewCard-Debouncer");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> pendingTask;

    public SpendPreviewCard(InModalAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.rootBox = buildView();
    }

    public Parent getView() {
        return rootBox;
    }

    private VBox buildView() {
        VBox box = new VBox(10);
        box.getStyleClass().add("spend-preview-card");
        box.setPadding(new Insets(14));

        titleLabel = new Label("In-Modal Live Spend Analysis");
        titleLabel.getStyleClass().add("preview-title");

        spentValueLabel = new Label("Current Spent: \u20B90.00");
        budgetValueLabel = new Label("Monthly Budget: No budget set");
        remainingValueLabel = new Label("Remaining Budget: -");
        utilisationLabel = new Label("Projected Utilisation: 0%");
        utilisationLabel.setStyle("-fx-font-weight: bold;");

        progressBar = new ProgressBar(0.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        alertBanner = new Label();
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
        alertBanner.setWrapText(true);

        Label goalsHeader = new Label("Savings Goal Impact:");
        goalsHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        goalImpactBox = new VBox(6);

        box.getChildren().addAll(
                titleLabel,
                spentValueLabel,
                budgetValueLabel,
                remainingValueLabel,
                utilisationLabel,
                progressBar,
                alertBanner,
                goalsHeader,
                goalImpactBox
        );

        return box;
    }

    /**
     * Debounced preview calculation trigger.
     */
    public void updatePreview(Long userId, Category category, BigDecimal amount, boolean isExpense) {
        if (debounceExecutor.isShutdown()) return;

        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }

        // 200ms debounce
        pendingTask = debounceExecutor.schedule(() -> {
            if (analyticsService == null) return;

            Task<AnalyticsDTO> task = new Task<>() {
                @Override
                protected AnalyticsDTO call() {
                    return analyticsService.calculatePreview(userId, category, amount, isExpense);
                }
            };

            task.setOnSucceeded(e -> {
                AnalyticsDTO dto = task.getValue();
                Platform.runLater(() -> renderAnalytics(dto));
            });

            task.setOnFailed(e -> {
                LOGGER.log(Level.WARNING, "Failed to compute in-modal preview", task.getException());
            });

            Thread worker = new Thread(task, "Analytics-Worker");
            worker.setDaemon(true);
            worker.start();
        }, 200, TimeUnit.MILLISECONDS);
    }

    private void renderAnalytics(AnalyticsDTO dto) {
        if (dto == null) return;

        spentValueLabel.setText("Current Month Spent: " + MoneyUtil.formatCurrency(dto.getCategorySpent()));

        if (dto.isHasBudget()) {
            budgetValueLabel.setText("Monthly Budget: " + MoneyUtil.formatCurrency(dto.getBudgetLimit()));
            remainingValueLabel.setText("Remaining: " + MoneyUtil.formatCurrency(dto.getRemainingBudget()));

            double util = dto.getProjectedUtilisationPercent().doubleValue();
            utilisationLabel.setText(String.format("Projected Utilisation: %.1f%%", util));
            progressBar.setProgress(Math.min(1.0, util / 100.0));

            // Alert Banner
            alertBanner.setVisible(true);
            alertBanner.setManaged(true);
            alertBanner.getStyleClass().removeAll("banner-warning", "banner-critical");

            if (dto.getAlertLevel() == AlertLevel.EXCEEDED) {
                alertBanner.getStyleClass().add("banner-critical");
                alertBanner.setText(String.format("Critical Alert: Transaction exceeds monthly cap by %s!",
                        MoneyUtil.formatCurrency(dto.getProposedAmount().subtract(dto.getRemainingBudget()).max(BigDecimal.ZERO))));
            } else if (dto.getAlertLevel() == AlertLevel.WARNING) {
                alertBanner.getStyleClass().add("banner-warning");
                alertBanner.setText(String.format("Warning: Category spend will reach %.1f%% of monthly limit.", util));
            } else {
                alertBanner.setVisible(false);
                alertBanner.setManaged(false);
            }
        } else {
            budgetValueLabel.setText("Monthly Budget: No budget configured");
            remainingValueLabel.setText("Remaining: N/A");
            utilisationLabel.setText("Projected Utilisation: N/A");
            progressBar.setProgress(0.0);
            alertBanner.setVisible(false);
            alertBanner.setManaged(false);
        }

        // Render Goal Impacts
        goalImpactBox.getChildren().clear();
        if (dto.getGoalImpacts().isEmpty()) {
            Label noGoals = new Label("No active savings goals affected.");
            noGoals.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px;");
            goalImpactBox.getChildren().add(noGoals);
        } else {
            for (AnalyticsDTO.GoalImpact impact : dto.getGoalImpacts()) {
                Label impactLabel = new Label("\u2022 " + impact.getGoalTitle() + ": " + impact.getMessage());
                impactLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main;");
                impactLabel.setWrapText(true);
                goalImpactBox.getChildren().add(impactLabel);
            }
        }
    }

    public void shutdown() {
        debounceExecutor.shutdownNow();
    }
}
