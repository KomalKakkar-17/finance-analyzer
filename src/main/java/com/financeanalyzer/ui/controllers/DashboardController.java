package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.dao.BudgetDao;
import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.dao.FixedCategoryDao;
import com.financeanalyzer.dao.SavingsGoalDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.model.User;
import com.financeanalyzer.rules.AnalysisContext;
import com.financeanalyzer.service.HeuristicEngine;
import com.financeanalyzer.service.TransactionService;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Controller for the Dashboard screen.
 * Implements FR-15.
 * Features KPI summary cards, PieChart of category expenses, BarChart of monthly trends,
 * Recent activity queue ListView (demonstrating LinkedList), and savings goal progress.
 */
public class DashboardController {

    private static final Logger LOGGER = AppLogger.getLogger(DashboardController.class);

    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final SavingsGoalDao savingsGoalDao;
    private final TransactionService transactionService;
    private final HeuristicEngine heuristicEngine;
    private final FixedCategoryDao fixedCategoryDao;
    private final User currentUser;

    public DashboardController(TransactionDao transactionDao,
                               BudgetDao budgetDao,
                               SavingsGoalDao savingsGoalDao,
                               TransactionService transactionService,
                               HeuristicEngine heuristicEngine,
                               FixedCategoryDao fixedCategoryDao,
                               User currentUser) {
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.budgetDao = Objects.requireNonNull(budgetDao, "BudgetDao must not be null.");
        this.savingsGoalDao = Objects.requireNonNull(savingsGoalDao, "SavingsGoalDao must not be null.");
        this.transactionService = Objects.requireNonNull(transactionService, "TransactionService must not be null.");
        this.heuristicEngine = Objects.requireNonNull(heuristicEngine, "HeuristicEngine must not be null.");
        this.fixedCategoryDao = Objects.requireNonNull(fixedCategoryDao, "FixedCategoryDao must not be null.");
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser must not be null.");
    }

    public Parent getView() {
        YearMonth currentMonth = YearMonth.now();
        Long userId = currentUser.getId();

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("content-area");

        // 1. Dashboard Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Financial Dashboard");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800;");

        Label monthBadge = new Label(DateUtil.formatDisplayMonth(currentMonth));
        monthBadge.getStyleClass().addAll("badge", "badge-active");
        header.getChildren().addAll(title, monthBadge);

        // 2. Recommendations Banner (if triggered and enabled)
        VBox recsBanner = createRecommendationsBanner(currentMonth);

        // 3. Four KPI Stat Cards
        HBox kpiRow = createKpiRow(currentMonth);

        // 4. Charts Row (PieChart & BarChart)
        HBox chartsRow = createChartsRow(currentMonth);

        // 5. Goals & Recent Activity Row
        HBox bottomRow = createBottomRow(currentMonth);

        content.getChildren().addAll(header);
        if (recsBanner != null) {
            content.getChildren().add(recsBanner);
        }
        content.getChildren().addAll(kpiRow, chartsRow, bottomRow);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private VBox createRecommendationsBanner(YearMonth currentMonth) {
        if (!currentUser.isRecommendationsEnabled()) return null;

        Long userId = currentUser.getId();
        BigDecimal income = transactionDao.getTotalIncomeInMonth(userId, currentMonth);
        BigDecimal expense = transactionDao.getTotalExpenseInMonth(userId, currentMonth);
        Map<String, BigDecimal> currentCats = transactionDao.getCategoryExpenseBreakdown(userId, currentMonth);
        Map<String, BigDecimal> lastCats = transactionDao.getCategoryExpenseBreakdown(userId, currentMonth.minusMonths(1));
        // Fetch user-defined fixed category names so rules can skip them
        java.util.Set<String> fixedNames = fixedCategoryDao.getFixedCategoryNames(userId);

        AnalysisContext ctx = new AnalysisContext(
                userId, currentMonth, income, expense, currentCats, lastCats,
                transactionDao.findRecent(userId, 30), fixedNames
        );

        List<String> recs = heuristicEngine.generateRecommendations(currentUser, ctx);
        if (recs.isEmpty()) return null;

        VBox banner = new VBox(8);
        banner.getStyleClass().add("card");
        banner.setStyle("-fx-background-color: rgba(245, 158, 11, 0.08); -fx-border-color: #F59E0B; -fx-border-radius: 10px; -fx-padding: 14px;");

        Label bannerTitle = new Label("\u26A0 Smart Heuristic Insights (" + recs.size() + " triggers)");
        bannerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #B45309;");
        banner.getChildren().add(bannerTitle);

        for (String r : recs) {
            Label item = new Label("\u2022 " + r);
            item.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");
            item.setWrapText(true);
            banner.getChildren().add(item);
        }

        return banner;
    }

    private HBox createKpiRow(YearMonth currentMonth) {
        Long userId = currentUser.getId();
        BigDecimal income = transactionDao.getTotalIncomeInMonth(userId, currentMonth);
        BigDecimal expense = transactionDao.getTotalExpenseInMonth(userId, currentMonth);
        BigDecimal net = income.subtract(expense);

        double savingsRate = 0.0;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = net.divide(income, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        HBox row = new HBox(16);
        row.getChildren().addAll(
                buildStatCard("Total Month Income", MoneyUtil.formatCurrency(income), "#10B981"),
                buildStatCard("Total Month Expense", MoneyUtil.formatCurrency(expense), "#EF4444"),
                buildStatCard("Net Cashflow", MoneyUtil.formatCurrency(net), net.compareTo(BigDecimal.ZERO) >= 0 ? "#10B981" : "#EF4444"),
                buildStatCard("Savings Rate", String.format("%.1f%%", savingsRate), savingsRate >= 20.0 ? "#10B981" : (savingsRate >= 10.0 ? "#F59E0B" : "#EF4444"))
        );
        for (javafx.scene.Node n : row.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
        return row;
    }

    private VBox buildStatCard(String label, String value, String colorHex) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");

        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        if (colorHex != null) {
            v.setStyle("-fx-text-fill: " + colorHex + ";");
        }
        card.getChildren().addAll(l, v);
        return card;
    }

    private HBox createChartsRow(YearMonth currentMonth) {
        HBox row = new HBox(16);

        // 1. PieChart: Category Spend Breakdown
        VBox pieBox = new VBox(10);
        pieBox.getStyleClass().add("card");
        pieBox.setPadding(new Insets(16));
        HBox.setHgrow(pieBox, Priority.ALWAYS);

        Label pieTitle = new Label("Category Expense Breakdown");
        pieTitle.getStyleClass().add("card-title");

        Map<String, BigDecimal> breakdown = transactionDao.getCategoryExpenseBreakdown(currentUser.getId(), currentMonth);
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, BigDecimal> e : breakdown.entrySet()) {
            pieData.add(new PieChart.Data(e.getKey(), e.getValue().doubleValue()));
        }

        PieChart pieChart = new PieChart(pieData);
        pieChart.setLabelsVisible(true);
        pieChart.setPrefHeight(260);
        pieBox.getChildren().addAll(pieTitle, pieChart);

        // 2. BarChart: Monthly Trend (Last 3 Months)
        VBox barBox = new VBox(10);
        barBox.getStyleClass().add("card");
        barBox.setPadding(new Insets(16));
        HBox.setHgrow(barBox, Priority.ALWAYS);

        Label barTitle = new Label("Monthly Income vs Expense Trend");
        barTitle.getStyleClass().add("card-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (\u20B9)");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setPrefHeight(260);

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expense");

        for (int i = 2; i >= 0; i--) {
            YearMonth m = currentMonth.minusMonths(i);
            String label = m.getMonth().name().substring(0, 3) + " " + m.getYear();
            BigDecimal inc = transactionDao.getTotalIncomeInMonth(currentUser.getId(), m);
            BigDecimal exp = transactionDao.getTotalExpenseInMonth(currentUser.getId(), m);

            incomeSeries.getData().add(new XYChart.Data<>(label, inc.doubleValue()));
            expenseSeries.getData().add(new XYChart.Data<>(label, exp.doubleValue()));
        }

        barChart.getData().addAll(incomeSeries, expenseSeries);
        barBox.getChildren().addAll(barTitle, barChart);

        row.getChildren().addAll(pieBox, barBox);
        return row;
    }

    private HBox createBottomRow(YearMonth currentMonth) {
        HBox row = new HBox(16);

        // 1. Savings Goals Snapshot
        VBox goalsBox = new VBox(12);
        goalsBox.getStyleClass().add("card");
        goalsBox.setPadding(new Insets(16));
        HBox.setHgrow(goalsBox, Priority.ALWAYS);

        Label goalsTitle = new Label("Active Savings Goals Progress");
        goalsTitle.getStyleClass().add("card-title");
        goalsBox.getChildren().add(goalsTitle);

        List<SavingsGoal> activeGoals = savingsGoalDao.findActiveGoals(currentUser.getId());
        if (activeGoals.isEmpty()) {
            goalsBox.getChildren().add(new Label("No active savings goals found."));
        } else {
            for (SavingsGoal g : activeGoals) {
                VBox item = new VBox(4);
                HBox top = new HBox();
                Label gTitle = new Label(g.getTitle());
                gTitle.setStyle("-fx-font-weight: bold;");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label gAmt = new Label(MoneyUtil.formatCurrency(g.getSavedAmount()) + " / " + MoneyUtil.formatCurrency(g.getTargetAmount()));
                gAmt.setStyle("-fx-font-size: 11px;");
                top.getChildren().addAll(gTitle, sp, gAmt);

                ProgressBar bar = new ProgressBar(g.getProgressPercentage() / 100.0);
                bar.setMaxWidth(Double.MAX_VALUE);

                item.getChildren().addAll(top, bar);
                goalsBox.getChildren().add(item);
            }
        }

        // 2. Recent Activity Queue (Unit 2 LinkedList Demonstration)
        VBox activityBox = new VBox(10);
        activityBox.getStyleClass().add("card");
        activityBox.setPadding(new Insets(16));
        HBox.setHgrow(activityBox, Priority.ALWAYS);

        Label activityTitle = new Label("Recent Activity (LinkedList Queue)");
        activityTitle.getStyleClass().add("card-title");

        ListView<String> activityList = new ListView<>();
        activityList.setPrefHeight(180);
        List<String> activities = transactionService.getRecentActivityList();
        if (activities.isEmpty()) {
            activityList.getItems().add("No recent actions recorded in this session.");
        } else {
            activityList.getItems().addAll(activities);
        }

        activityBox.getChildren().addAll(activityTitle, activityList);

        row.getChildren().addAll(goalsBox, activityBox);
        return row;
    }
}
