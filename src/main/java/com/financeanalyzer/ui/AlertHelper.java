package com.financeanalyzer.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

/**
 * Standardized alert, dialog, and toast helper for JavaFX UI feedback.
 * Demonstrates Unit 5 user feedback principles.
 */
public final class AlertHelper {

    private AlertHelper() {
        // Utility
    }

    public static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showWarning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Optional<String> showTextInput(String title, String header, String prompt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(prompt);
        return dialog.showAndWait();
    }

    /**
     * Non-blocking modern toast notification overlay.
     */
    public static void showToast(Stage stage, String message) {
        if (stage == null || !stage.isShowing()) return;

        Popup popup = new Popup();
        popup.setAutoFix(true);

        Label label = new Label(message);
        label.setStyle("""
            -fx-background-color: #263238;
            -fx-text-fill: #FFFFFF;
            -fx-padding: 10px 18px;
            -fx-background-radius: 20px;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0, 0, 4);
        """);

        StackPane pane = new StackPane(label);
        pane.setStyle("-fx-background-color: transparent;");
        popup.getContent().add(pane);

        popup.setOnShown(e -> {
            popup.setX(stage.getX() + stage.getWidth() / 2 - label.getWidth() / 2);
            popup.setY(stage.getY() + stage.getHeight() - 90);
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(2200));
        fadeOut.setOnFinished(e -> popup.hide());

        popup.show(stage);
        fadeIn.play();
        fadeOut.play();
    }
}
