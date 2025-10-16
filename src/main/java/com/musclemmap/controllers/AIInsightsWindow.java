package com.musclemmap.controllers;

import com.musclemmap.models.Workout;
import com.musclemmap.services.ExerciseRecommendationEngine;
import com.musclemmap.services.IntelligentDietGenerator;
import com.musclemmap.services.IntelligentWorkoutGenerator;
import com.musclemmap.services.WeaknessDetectionService;
import com.musclemmap.utils.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AIInsightsWindow {
    private final int userId;
    private final DatabaseHelper db;
    private IntelligentWorkoutGenerator.WorkoutPlan lastGeneratedPlan = null;

    public AIInsightsWindow(int userId, DatabaseHelper db) {
        this.userId = userId;
        this.db = db;
    }

    public void show(String initialTab) {
        Stage stage = new Stage();
        stage.setTitle("AI Insights");
        stage.initModality(Modality.NONE);
        BorderPane root = createContent(initialTab);
        Scene scene = new Scene(root, 900, 680);
        stage.setScene(scene);
        stage.show();
    }

    public BorderPane createContent(String initialTab) {
        TabPane tabs = new TabPane();
        Tab planTab = new Tab("Plan");
        Tab recsTab = new Tab("Recommendations");
        Tab weakTab = new Tab("Weaknesses");
        Tab dietTab = new Tab("Diet");
        planTab.setClosable(false);
        recsTab.setClosable(false);
        weakTab.setClosable(false);
        dietTab.setClosable(false);

        String tabStyle = "-fx-background-color: #2b2b2b; -fx-text-fill: white;";
        planTab.setStyle(tabStyle);
        recsTab.setStyle(tabStyle);
        weakTab.setStyle(tabStyle);
        dietTab.setStyle(tabStyle);

        planTab.setContent(buildPlanContent());
        recsTab.setContent(buildRecommendationsContent());
        weakTab.setContent(buildWeaknessesContent());
        dietTab.setContent(buildDietContent());

        tabs.getTabs().addAll(planTab, recsTab, weakTab, dietTab);
        if (initialTab != null) {
            switch (initialTab) {
                case "Plan" -> tabs.getSelectionModel().select(planTab);
                case "Recommendations" -> tabs.getSelectionModel().select(recsTab);
                case "Weaknesses" -> tabs.getSelectionModel().select(weakTab);
                case "Diet" -> tabs.getSelectionModel().select(dietTab);
                default -> {}
            }
        }

        tabs.setStyle("-fx-background-color: #1a1a1a;");

        BorderPane root = new BorderPane(tabs);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #111111; -fx-border-color: #2b2b2b;");
        return root;
    }

    private VBox buildPlanContent() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("📋 Workout Plan Generator");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        box.getChildren().add(title);

        ListView<String> list = new ListView<>();
        list.setPrefHeight(420);
        list.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
        
        Button refreshBtn = createStyledButton("Generate Plan", "#00d4ff");
        Button saveFirst = createStyledButton("Save First Session", "#4CAF50");
        saveFirst.setDisable(true);

        refreshBtn.setOnAction(evt -> {
            refreshBtn.setDisable(true);
            list.getItems().clear();
            javafx.concurrent.Task<IntelligentWorkoutGenerator.WorkoutPlan> task = new javafx.concurrent.Task<>() {
                @Override
                protected IntelligentWorkoutGenerator.WorkoutPlan call() {
                    IntelligentWorkoutGenerator iwg = new IntelligentWorkoutGenerator();
                    return iwg.generatePlanForUser(userId, 3, 60);
                }
            };

            task.setOnSucceeded(t -> {
                var plan = task.getValue();
                lastGeneratedPlan = plan;
                ObservableList<String> items = FXCollections.observableArrayList();
                for (IntelligentWorkoutGenerator.SessionPlan s : plan.sessions) {
                    items.add((s.date != null ? s.date.toString() : "") + " - " + s.name);
                    for (IntelligentWorkoutGenerator.PlannedExercise pe : s.exercises) {
                        String weightTxt = pe.suggestedWeight > 0 ? String.format("%.1f", pe.suggestedWeight) : "auto";
                        items.add("  • " + pe.exercise.getName() + "  " + pe.sets + "x" + pe.targetReps + " @ " + weightTxt);
                    }
                    items.add("");
                }
                list.setItems(items);
                saveFirst.setDisable(plan.sessions.isEmpty());
                refreshBtn.setDisable(false);
            });

            task.setOnFailed(t -> {
                list.getItems().add("Failed to generate plan: " + task.getException().getMessage());
                refreshBtn.setDisable(false);
            });

            new Thread(task).start();
        });

        saveFirst.setOnAction(e -> {
            try {
                IntelligentWorkoutGenerator.WorkoutPlan planToSave = lastGeneratedPlan;
                if (planToSave == null || planToSave.sessions.isEmpty()) {
                    IntelligentWorkoutGenerator iwg = new IntelligentWorkoutGenerator();
                    planToSave = iwg.generatePlanForUser(userId, 3, 60);
                }

                if (planToSave != null && !planToSave.sessions.isEmpty()) {
                    IntelligentWorkoutGenerator iwg2 = new IntelligentWorkoutGenerator();
                    Workout w = iwg2.materializeWorkout(planToSave.sessions.get(0));
                    w.setEndTime(LocalDateTime.now());
                    db.saveWorkout(w, userId);
                    showToast("Workout saved.");
                } else {
                    showToast("No plan available to save.");
                }
            } catch (Exception ex) {
                showToast("Failed to save workout: " + ex.getMessage());
            }
        });

        HBox controls = new HBox(8, refreshBtn, saveFirst);
        box.getChildren().addAll(controls, list);
        return box;
    }

    private VBox buildRecommendationsContent() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("✨ Exercise Recommendations");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        box.getChildren().add(title);

        ExerciseRecommendationEngine engine = new ExerciseRecommendationEngine();
        ExerciseRecommendationEngine.RecommendationRequest req = new ExerciseRecommendationEngine.RecommendationRequest();
        req.userId = userId;
        req.goal = "hypertrophy";
        req.availableEquipment.add("Dumbbell");
        req.availableEquipment.add("Barbell");
        req.availableEquipment.add("Cable");
        req.limit = 10;

        TableView<ExerciseRecommendationEngine.Recommendation> table = new TableView<>();
        TableColumn<ExerciseRecommendationEngine.Recommendation, String> nameCol = new TableColumn<>("Exercise");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().exercise.getName()));
        nameCol.setPrefWidth(180);

        TableColumn<ExerciseRecommendationEngine.Recommendation, String> muscleCol = new TableColumn<>("Muscle");
        muscleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().exercise.getMuscleGroup()));
        muscleCol.setPrefWidth(120);

        TableColumn<ExerciseRecommendationEngine.Recommendation, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f", c.getValue().score)));
        scoreCol.setPrefWidth(80);

        TableColumn<ExerciseRecommendationEngine.Recommendation, String> whyCol = new TableColumn<>("Rationale");
        whyCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().rationale));
        whyCol.setPrefWidth(380);

        table.getColumns().add(nameCol);
        table.getColumns().add(muscleCol);
        table.getColumns().add(scoreCol);
        table.getColumns().add(whyCol);

        Button refresh = createStyledButton("Refresh Recommendations", "#00d4ff");
        refresh.setOnAction(evt -> {
            refresh.setDisable(true);
            table.getItems().clear();
            javafx.concurrent.Task<List<ExerciseRecommendationEngine.Recommendation>> task = new javafx.concurrent.Task<>() {
                @Override
                protected List<ExerciseRecommendationEngine.Recommendation> call() {
                    return engine.recommend(req);
                }
            };
            task.setOnSucceeded(t -> {
                table.setItems(FXCollections.observableArrayList(task.getValue()));
                refresh.setDisable(false);
            });
            task.setOnFailed(t -> {
                table.setPlaceholder(new Label("Failed to load recommendations"));
                refresh.setDisable(false);
            });
            new Thread(task).start();
        });

        refresh.fire();

        box.getChildren().addAll(refresh, table);
        return box;
    }

    private VBox buildWeaknessesContent() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("⚠️ Weakness Analysis");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        box.getChildren().add(title);

        WeaknessDetectionService wds = new WeaknessDetectionService();
        ListView<String> list = new ListView<>();
        list.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
        
        Button analyze = createStyledButton("Analyze Weaknesses", "#00d4ff");
        analyze.setOnAction(evt -> {
            analyze.setDisable(true);
            list.getItems().clear();
            javafx.concurrent.Task<List<WeaknessDetectionService.WeakArea>> task = new javafx.concurrent.Task<>() {
                @Override
                protected List<WeaknessDetectionService.WeakArea> call() {
                    return wds.analyzeUser(userId);
                }
            };
            task.setOnSucceeded(t -> {
                var weak = task.getValue();
                ObservableList<String> items = FXCollections.observableArrayList();
                for (WeaknessDetectionService.WeakArea w : weak) {
                    items.add(w.muscleGroup + ": " + String.format("%.2f", w.score) + " — " + w.note);
                }
                if (items.isEmpty()) items.add("No major imbalances detected.");
                list.setItems(items);
                analyze.setDisable(false);
            });
            task.setOnFailed(t -> {
                list.setPlaceholder(new Label("Analysis failed"));
                analyze.setDisable(false);
            });
            new Thread(task).start();
        });

        box.getChildren().addAll(analyze, list);
        return box;
    }

    private VBox buildDietContent() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("🥗 Diet Planner");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        box.getChildren().add(title);

        IntelligentDietGenerator idg = new IntelligentDietGenerator();
        IntelligentDietGenerator.DietRequest dr = new IntelligentDietGenerator.DietRequest();
        dr.gender = "male";
        dr.age = 25;
        dr.heightCm = 175;
        dr.weightKg = 75;
        dr.activityLevel = "moderate";
        dr.goal = "muscle_gain";
        dr.mealsPerDay = 4;
        dr.workoutMinutesPerDay = 60;

        TextField ageField = new TextField(String.valueOf(dr.age));
        TextField weightField = new TextField(String.valueOf(dr.weightKg));
        TextField heightField = new TextField(String.valueOf(dr.heightCm));
        ComboBox<String> goalBox = new ComboBox<>(FXCollections.observableArrayList("muscle_gain", "fat_loss", "maintenance"));
        goalBox.getSelectionModel().select(dr.goal);
        Button gen = createStyledButton("Generate Meal Plan", "#00d4ff");

        VBox resultBox = new VBox(8);
        resultBox.setPrefHeight(420);

        gen.setOnAction(evt -> {
            try {
                dr.age = Integer.parseInt(ageField.getText());
                dr.weightKg = Double.parseDouble(weightField.getText());
                dr.heightCm = Double.parseDouble(heightField.getText());
                dr.goal = goalBox.getSelectionModel().getSelectedItem();
            } catch (Exception ex) {
                showToast("Invalid input");
                return;
            }

            gen.setDisable(true);
            resultBox.getChildren().clear();

            javafx.concurrent.Task<IntelligentDietGenerator.MealPlan> task = new javafx.concurrent.Task<>() {
                @Override
                protected IntelligentDietGenerator.MealPlan call() {
                    return idg.generateMealPlan(dr);
                }
            };

            task.setOnSucceeded(t -> {
                IntelligentDietGenerator.MealPlan mp = task.getValue();
                Label macros = new Label(String.format("Calories: %.0f  Protein: %.0fg  Carbs: %.0fg  Fats: %.0fg",
                        mp.targets.calories, mp.targets.proteinG, mp.targets.carbsG, mp.targets.fatsG));
                macros.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
                
                VBox mealsBox = new VBox(8);
                int i = 1;
                for (IntelligentDietGenerator.Meal m : mp.meals) {
                    Label mealTitle = new Label("Meal " + i++ + " — " + m.name);
                    mealTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

                    VBox card = new VBox(4);
                    card.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 8px; -fx-padding: 8px;");
                    
                    List<String> lines = new ArrayList<>();
                    for (IntelligentDietGenerator.MealItem it : m.items) {
                        lines.add("• " + it.name + " — " + Math.round(it.grams) + " g");
                    }
                    Label items = new Label(String.join("\n", lines));
                    items.setStyle("-fx-text-fill: #b0b0b0;");
                    
                    card.getChildren().addAll(mealTitle, items);
                    mealsBox.getChildren().add(card);
                }

                Label shop = new Label("Shopping List:\n" + String.join("\n", mp.shoppingList));
                shop.setStyle("-fx-text-fill: white;");
                
                resultBox.getChildren().addAll(macros, mealsBox, shop);
                gen.setDisable(false);
            });

            task.setOnFailed(t -> {
                showToast("Failed to generate meal plan");
                gen.setDisable(false);
            });

            new Thread(task).start();
        });

        ageField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
        weightField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
        heightField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
        goalBox.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");

        HBox form = new HBox(8,
                new VBox(4, new Label("Age"), ageField),
                new VBox(4, new Label("Weight (kg)"), weightField),
                new VBox(4, new Label("Height (cm)"), heightField),
                new VBox(4, new Label("Goal"), goalBox),
                gen
        );
        form.setAlignment(Pos.CENTER_LEFT);

        form.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                vbox.getChildren().stream()
                    .filter(child -> child instanceof Label)
                    .forEach(label -> ((Label) label).setStyle("-fx-text-fill: white;"));
            }
        });

        ScrollPane scroll = new ScrollPane(resultBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        box.getChildren().addAll(form, scroll);
        return box;
    }

    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        String baseStyle = String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 8px;
            -fx-padding: 10 20;
            """, color);
        
        String hoverStyle = String.format("""
            -fx-background-color: derive(%s, -20%%);
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 8px;
            -fx-padding: 10 20;
            -fx-scale-x: 1.05;
            -fx-scale-y: 1.05;
            """, color);

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    private void showToast(String message) {
        Stage toast = new Stage();
        toast.initModality(Modality.NONE);
        toast.setAlwaysOnTop(true);
        Label label = new Label(message);
        label.setStyle("-fx-background-color: #323232; -fx-text-fill: white; -fx-padding: 10 16; -fx-background-radius: 8px;");
        BorderPane root = new BorderPane(label);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root);
        scene.setFill(null);
        toast.setScene(scene);
        toast.initStyle(StageStyle.TRANSPARENT);
        toast.setWidth(220);
        toast.setHeight(80);
        toast.show();
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(toast::close);
        }).start();
    }
}


