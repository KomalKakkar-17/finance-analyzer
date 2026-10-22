package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.model.User;
import com.financeanalyzer.service.AuthService;
import com.financeanalyzer.util.AppLogger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Primary layout shell controller utilizing BorderPane.
 * Features top application bar, user session badge, dark/light theme switcher,
 * sidebar navigation drawer, and dynamic center view swapping.
 * Visibly demonstrates Unit 5 BorderPane, VBox, HBox, and StackPane composition.
 */
public class MainShellController {

    private static final Logger LOGGER = AppLogger.getLogger(MainShellController.class);

    private final AuthService authService;
    private final User currentUser;
    private final Stage primaryStage;
    private final Runnable onLogout;

    private BorderPane rootLayout;
    private StackPane centerContentArea;
    private boolean isDarkMode = false;
    private final List<Button> navButtons = new ArrayList<>();

    // Sub-controllers / View Suppliers
    private Node dashboardView;
    private Node transactionsView;
    private Node budgetsView;
    private Node goalsView;
    private Node recurringView;
    private Node reportsSettingsView;

    public MainShellController(AuthService authService, User currentUser, Stage primaryStage, Runnable onLogout) {
        this.authService = Objects.requireNonNull(authService, "AuthService must not be null.");
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser must not be null.");
        this.primaryStage = Objects.requireNonNull(primaryStage, "primaryStage must not be null.");
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout must not be null.");
    }

    public void setViews(Node dashboardView, Node transactionsView, Node budgetsView,
                         Node goalsView, Node recurringView, Node reportsSettingsView) {
        this.dashboardView = dashboardView;
        this.transactionsView = transactionsView;
        this.budgetsView = budgetsView;
        this.goalsView = goalsView;
        this.recurringView = recurringView;
        this.reportsSettingsView = reportsSettingsView;
    }

    public Parent getView() {
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root");

        // Top Bar
        rootLayout.setTop(createTopBar());

        // Left Sidebar
        rootLayout.setLeft(createSidebar());

        // Center Content Container
        centerContentArea = new StackPane();
        centerContentArea.getStyleClass().add("content-area");
        rootLayout.setCenter(centerContentArea);

        // Default to Transactions View (or Dashboard if available)
        if (dashboardView != null) {
            showView(dashboardView, navButtons.get(0));
        } else if (transactionsView != null) {
            showView(transactionsView, navButtons.size() > 1 ? navButtons.get(1) : navButtons.get(0));
        }

        return rootLayout;
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Personal Finance Analyzer");
        title.getStyleClass().add("app-title");

        Label badge = new Label("PROTOTYPE");
        badge.getStyleClass().addAll("badge", "badge-active");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Theme Toggle Button
        Button themeBtn = new Button("\u263D Theme");
        themeBtn.getStyleClass().add("button");
        themeBtn.setOnAction(e -> toggleTheme(themeBtn));

        // User Profile Badge
        Label userLabel = new Label("Logged in as: " + currentUser.getUsername());
        userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-main;");

        // Logout Button
        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().addAll("button", "danger-button");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            onLogout.run();
        });

        topBar.getChildren().addAll(title, badge, spacer, themeBtn, userLabel, logoutBtn);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        navButtons.clear();

        Button dashBtn = createNavButton("\u25A4  Dashboard", () -> dashboardView);
        Button txnBtn = createNavButton("\u21C4  Transactions", () -> transactionsView);
        Button budgetBtn = createNavButton("\u25C8  Budgets", () -> budgetsView);
        Button goalsBtn = createNavButton("\u25C9  Savings Goals", () -> goalsView);
        Button recBtn = createNavButton("\u21BB  Recurring Rules", () -> recurringView);
        Button repBtn = createNavButton("\u2699  Reports & Settings", () -> reportsSettingsView);

        navButtons.add(dashBtn);
        navButtons.add(txnBtn);
        navButtons.add(budgetBtn);
        navButtons.add(goalsBtn);
        navButtons.add(recBtn);
        navButtons.add(repBtn);

        sidebar.getChildren().addAll(dashBtn, txnBtn, budgetBtn, goalsBtn, recBtn, new Separator(), repBtn);
        return sidebar;
    }

    private Button createNavButton(String text, java.util.function.Supplier<Node> viewSupplier) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            Node target = viewSupplier.get();
            if (target != null) {
                showView(target, btn);
            }
        });
        return btn;
    }

    public void showView(Node view, Button activeButton) {
        if (view == null) return;
        centerContentArea.getChildren().setAll(view);
        for (Button b : navButtons) {
            b.getStyleClass().remove("active");
        }
        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void toggleTheme(Button themeBtn) {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            rootLayout.getStyleClass().add("theme-dark");
            themeBtn.setText("\u2600 Light");
        } else {
            rootLayout.getStyleClass().remove("theme-dark");
            themeBtn.setText("\u263D Dark");
        }
    }
}
