package org.example.musclemmap;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);

        // 🎯 CRITICAL: Load your CSS file here
        try {
            String cssFile = HelloApplication.class.getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssFile);
            System.out.println("✅ CSS loaded successfully from: " + cssFile);
        } catch (Exception e) {
            System.err.println("❌ CSS file not found! Make sure styles.css is in src/main/resources/css/");
            e.printStackTrace();
        }

        stage.setTitle("💪 MuscleMap - Your Fitness Companion");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        // Add these JVM arguments to disable modules
        System.setProperty("java.awt.headless", "false");
        launch();
    }
}
