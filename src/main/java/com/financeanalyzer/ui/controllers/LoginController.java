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
     */
    public Parent getView() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: -color-bg;");
        root.setPadding(new Insets(30));

        // Card Container
        VBox card = new VBox(16);
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(36, 32, 36, 32));

        // App Header & Branding
        Label brand = new Label("Enhanced Personal Finance Analyzer");
        brand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brand.getStyleClass().add("app-title");

        Label subtitle = new Label("University Course Prototype \u2022 3-Tier Architecture");
        subtitle.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");

        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 8, 0));

        // Mode Title
        Label modeTitle = new Label("Sign In to Your Account");
        modeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        modeTitle.getStyleClass().add("card-title");

        // Form Fields
        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-main;");
        usernameField = new TextField();
        usernameField.setPromptText("Enter username (min. 6 chars)");
        usernameField.getStyleClass().add("text-field");

        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-main;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password (min. 6 chars)");
        passwordField.getStyleClass().add("password-field");

        // Real-time listener for Enter key to submit
        passwordField.setOnAction(e -> handleAction());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Error message label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Action Button
        actionButton = new Button("Sign In");
        actionButton.getStyleClass().addAll("button", "primary-button");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setOnAction(e -> handleAction());

        // Mode Toggle Link
        toggleModeLink = new Hyperlink("Need an account? Register here");
        toggleModeLink.setStyle("-fx-font-size: 12px;");
        toggleModeLink.setOnAction(e -> toggleMode(modeTitle));

        // Demo Seed Hint Box
        VBox demoHintBox = new VBox(6);
        demoHintBox.setStyle("-fx-background-color: rgba(59, 130, 246, 0.08); -fx-padding: 12px; -fx-background-radius: 8px; -fx-border-color: rgba(59, 130, 246, 0.2); -fx-border-radius: 8px;");
        Label demoTitle = new Label("Pre-seeded Demo Account Available:");
        demoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-primary;");
        Label demoCreds = new Label("Username: demo   \u2022   Password: demo123");
        demoCreds.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main;");

        Button quickDemoBtn = new Button("Quick Fill & Sign In as 'demo'");
        quickDemoBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px; -fx-cursor: hand;");
        quickDemoBtn.setOnAction(e -> {
            usernameField.setText("demo");
            passwordField.setText("demo123");
            isRegisterMode = false;
            handleAction();
        });
        demoHintBox.getChildren().addAll(demoTitle, demoCreds, quickDemoBtn);

        card.getChildren().addAll(
                brand, subtitle, sep,
                modeTitle,
                userLabel, usernameField,
                passLabel, passwordField,
                errorLabel,
                actionButton,
                toggleModeLink,
                demoHintBox
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
