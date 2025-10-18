package com.musclemmap.controllers;

import com.musclemmap.utils.DatabaseHelper;
import com.musclemmap.utils.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;
    @FXML private Label errorLabel;
    @FXML private Label passwordStrengthLabel;

    private DatabaseHelper dbHelper;

    @FXML
    public void initialize() {
        this.dbHelper = DatabaseHelper.getInstance();  // ✅ CORRECT - assigns to class field

        errorLabel.setVisible(false);
        passwordStrengthLabel.setVisible(false);

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });
    }


    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("⚠️ Please fill in all fields");
            return;
        }

        if (username.length() < 3) {
            showError("⚠️ Username must be at least 3 characters");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("⚠️ Please enter a valid email address");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("⚠️ Passwords do not match");
            return;
        }

        if (!PasswordUtil.isPasswordStrong(password)) {
            showError("⚠️ " + PasswordUtil.getPasswordStrengthMessage(password));
            return;
        }

        boolean success = dbHelper.registerUser(username, email, password);

        if (success) {
            showSuccess("✅ Registration successful! Please login.");

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::handleBackToLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            showError("❌ Registration failed. Username or email may already exist.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            Stage stage = (Stage) backToLoginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("💪 MuscleMap - Login");

        } catch (Exception e) {
            showError("❌ Error loading login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthLabel.setVisible(false);
            return;
        }

        String message = PasswordUtil.getPasswordStrengthMessage(password);
        passwordStrengthLabel.setText(message);
        passwordStrengthLabel.setVisible(true);

        if (message.contains("✓")) {
            passwordStrengthLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
        } else {
            passwordStrengthLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11px;");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-font-weight: bold;");
    }
}
