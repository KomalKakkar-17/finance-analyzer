package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.exception.AuthenticationException;
import com.financeanalyzer.model.User;
import com.financeanalyzer.service.AuthService;
import com.financeanalyzer.ui.AlertHelper;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.Validators;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller and view builder for Login and User Registration.
 * Demonstrates FR-01 and Unit 5 form validation and event-driven programming.
 */
public class LoginController {

    private static final Logger LOGGER = AppLogger.getLogger(LoginController.class);

    private final AuthService authService;
    private final Consumer<User> onLoginSuccess;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button actionButton;
    private Hyperlink toggleModeLink;
    private boolean isRegisterMode = false;

    public LoginController(AuthService authService, Consumer<User> onLoginSuccess) {
        this.authService = Objects.requireNonNull(authService, "AuthService must not be null.");
        this.onLoginSuccess = Objects.requireNonNull(onLoginSuccess, "onLoginSuccess callback must not be null.");
    }

    /**
     * Builds and returns the Login/Register scene graph root.
     * Uses the premium gradient background and glassmorphism card from the design system.
     */
    public Parent getView() {
        // Full-window gradient background via .login-root
        StackPane root = new StackPane();
        root.getStyleClass().add("login-root");

        // Glassmorphism login card — centred
        VBox card = new VBox(18);
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("login-card");

        // App brand header
        Label icon = new Label("\uD83D\uDCB0"); // 💰 emoji icon
        icon.setStyle("-fx-font-size: 38px;");

        Label brand = new Label("Personal Finance Analyzer");
        brand.getStyleClass().add("login-title");
        brand.setWrapText(true);

        Label subtitle = new Label("Enhanced Prototype  \u2022  3-Tier Architecture  \u2022  Java 21");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);

        Separator sep = new Separator();
        sep.setPadding(new Insets(2, 0, 6, 0));

        // Mode Title
        Label modeTitle = new Label("Sign In to Your Account");
        modeTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -color-text-main;");

        // Form Fields
        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: -color-text-muted;");
        usernameField = new TextField();
        usernameField.setPromptText("Enter username (min. 6 chars)");
        usernameField.getStyleClass().add("text-field");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: -color-text-muted;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password (min. 6 chars)");
        passwordField.getStyleClass().add("password-field");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        // Enter key handling
        passwordField.setOnAction(e -> handleAction());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Primary action button
        actionButton = new Button("Sign In");
        actionButton.getStyleClass().addAll("button", "primary-button");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setOnAction(e -> handleAction());

        // Toggle link
        toggleModeLink = new Hyperlink("Need an account? Register here");
        toggleModeLink.setStyle("-fx-font-size: 12px;");
        toggleModeLink.setOnAction(e -> toggleMode(modeTitle));

        // Demo Quick-Access box
        VBox demoBox = new VBox(8);
        demoBox.setStyle(
            "-fx-background-color: rgba(79,70,229,0.07); -fx-padding: 12px 14px;" +
            "-fx-background-radius: 10px; -fx-border-color: rgba(79,70,229,0.20);" +
            "-fx-border-radius: 10px; -fx-border-width: 1px;");

        Label demoTitle = new Label("\u26A1  Pre-seeded Demo Account");
        demoTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 11px; -fx-text-fill: -color-primary;");
        Label demoCreds = new Label("Username: demo   \u2022   Password: demo123");
        demoCreds.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        Button quickDemoBtn = new Button("Quick Fill & Sign In as 'demo'");
        quickDemoBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5px 12px; -fx-cursor: hand;");
        quickDemoBtn.getStyleClass().addAll("button", "primary-button");
        quickDemoBtn.setMaxWidth(Double.MAX_VALUE);
        quickDemoBtn.setOnAction(e -> {
            usernameField.setText("demo");
            passwordField.setText("demo123");
            isRegisterMode = false;
            handleAction();
        });
        demoBox.getChildren().addAll(demoTitle, demoCreds, quickDemoBtn);

        card.getChildren().addAll(
                icon, brand, subtitle, sep,
                modeTitle,
                userLabel, usernameField,
                passLabel, passwordField,
                errorLabel,
                actionButton,
                toggleModeLink,
                demoBox
        );

        root.getChildren().add(card);
        return root;
    }

    private void toggleMode(Label modeTitle) {
        isRegisterMode = !isRegisterMode;
        clearErrors();
        if (isRegisterMode) {
            modeTitle.setText("Create New Account");
            actionButton.setText("Register & Sign In");
            toggleModeLink.setText("Already registered? Sign In");
        } else {
            modeTitle.setText("Sign In to Your Account");
            actionButton.setText("Sign In");
            toggleModeLink.setText("Need an account? Register here");
        }
    }

    private void handleAction() {
        clearErrors();
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText() : "";

        // Client-side validations: registration enforces min-length constraints; login only requires non-empty inputs
        if (isRegisterMode) {
            if (!Validators.isValidUsername(username)) {
                showError("Username must be at least 6 alphanumeric characters.", usernameField);
                return;
            }
            if (!Validators.isValidPassword(password)) {
                showError("Password must be at least 6 characters.", passwordField);
                return;
            }
        } else {
            if (username.isEmpty()) {
                showError("Please enter your username.", usernameField);
                return;
            }
            if (password.isEmpty()) {
                showError("Please enter your password.", passwordField);
                return;
            }
        }

        try {
            User user;
            if (isRegisterMode) {
                user = authService.register(username, password);
            } else {
                user = authService.login(username, password);
            }
            onLoginSuccess.accept(user);
        } catch (AuthenticationException ex) {
            LOGGER.log(Level.WARNING, "Authentication failed: {0}", ex.getMessage());
            showError(ex.getMessage(), null);
        }
    }

    private void showError(String msg, Control errorField) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        if (errorField != null) {
            errorField.getStyleClass().add("field-error");
            errorField.requestFocus();
        }
    }

    private void clearErrors() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        usernameField.getStyleClass().remove("field-error");
        passwordField.getStyleClass().remove("field-error");
    }
}
