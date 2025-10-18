package com.musclemmap.controllers;

import com.musclemmap.models.User;
import com.musclemmap.utils.DatabaseHelper;
import com.musclemmap.utils.SessionManager;
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

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;
    @FXML private CheckBox rememberMeCheckbox;

    private DatabaseHelper dbHelper;

    @FXML
    public void initialize() {
        // FIXED: Changed from new DatabaseHelper() to getInstance()
        dbHelper = DatabaseHelper.getInstance();
        errorLabel.setVisible(false);

        passwordField.setOnAction(e -> handleLogin());
    }


    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("⚠️ Please enter both username and password");
            return;
        }

        User user = dbHelper.authenticateUser(username, password);

        if (user != null) {
            SessionManager.getInstance().login(user);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Parent root = loader.load();

                MainController mainController = loader.getController();
                mainController.setCurrentUser(user);

                Scene scene = new Scene(root);
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("💪 MuscleMap - " + user.getUsername());
                stage.setMaximized(true);

                System.out.println("🚀 Login successful! Welcome " + user.getUsername());

            } catch (Exception e) {
                showError("❌ Error loading main application: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showError("❌ Invalid username or password");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("💪 MuscleMap - Register");

        } catch (Exception e) {
            showError("❌ Error loading registration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 12px; -fx-font-weight: bold;");
    }
}
