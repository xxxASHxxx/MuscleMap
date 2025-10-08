package com.musclemmap.controllers;

import com.musclemmap.utils.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import com.musclemmap.models.*;
import com.musclemmap.utils.DatabaseHelper;
import com.musclemmap.utils.UIStyler;
import javafx.scene.control.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.util.stream.Collectors;

import java.util.*;


public class MainController implements Initializable {
    @FXML
    private void handleStartWorkout() {
        if (currentWorkout != null) {
            showAlert(Alert.AlertType.WARNING, "Active Workout", "Please finish your current workout first!");
            return;
        }

        // Create new workout
        currentWorkout = new Workout("Workout - " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        currentWorkout.setUserId(currentUser.getId());

        // Start timer
        workoutStartTime = System.currentTimeMillis();
        startWorkoutTimer();

        startWorkoutBtn.setDisable(true);
        endWorkoutBtn.setDisable(false);

        System.out.println("🏋️ Started new workout: " + currentWorkout.getName());
        showAlert(Alert.AlertType.INFORMATION, "Workout Started", "Good luck with your workout! 💪");
    }

    @FXML
    private void handleEndWorkout() {
        if (currentWorkout == null) {
            showAlert(Alert.AlertType.WARNING, "No Active Workout", "No workout in progress!");
            return;
        }

        // Stop timer
        if (workoutTimer != null) {
            workoutTimer.stop();
        }

        // Complete workout
        currentWorkout.completeWorkout();

        // Save to database
        int workoutId = dbHelper.saveWorkout(currentWorkout, currentUser.getId());

        if (workoutId > 0) {
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    String.format("Workout saved! 💪\n\nTotal Volume: %,.0f lbs\nTotal Sets: %d\nTotal Reps: %d\nDuration: %s",
                            currentWorkout.getTotalVolume(),
                            currentWorkout.getTotalSets(),
                            currentWorkout.getTotalReps(),
                            currentWorkout.getFormattedDuration()));

            // Refresh progress tab
            if (progressController != null) {
                progressController.setCurrentUser(currentUser);
            }

            // Refresh home tab stats
            loadUserWorkouts();
            updateUserStats();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save workout to database!");
        }

        // Reset
        currentWorkout = null;
        workoutTimerLabel.setText("⏱️ Timer: 00:00:00");
        startWorkoutBtn.setDisable(false);
        endWorkoutBtn.setDisable(true);

        System.out.println("✅ Workout completed and saved");
    }

    private void startWorkoutTimer() {
        workoutTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            long elapsedMillis = System.currentTimeMillis() - workoutStartTime;
            long seconds = (elapsedMillis / 1000) % 60;
            long minutes = (elapsedMillis / (1000 * 60)) % 60;
            long hours = (elapsedMillis / (1000 * 60 * 60));

            workoutTimerLabel.setText(String.format("⏱️ Timer: %02d:%02d:%02d", hours, minutes, seconds));
        }));
        workoutTimer.setCycleCount(Timeline.INDEFINITE);
        workoutTimer.play();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private SessionManager sessionManager;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab homeTab, workoutTab, muscleMapTab, progressTab;
    @FXML private ComboBox<Exercise> exerciseComboBox;
    @FXML private TextField weightField;
    @FXML private TextField repsField;
    @FXML private Label currentWorkoutStatsLabel;
    // Home Tab Components
    @FXML
    private Label welcomeLabel, streakLabel, lastWorkoutLabel;
    @FXML
    private VBox recentWorkoutsBox;
    @FXML
    private Button startQuickWorkoutBtn;

    // Muscle Map Tab Components
    @FXML
    private ScrollPane muscleMapScrollPane;
    @FXML
    private Pane muscleMapPane;
    // Progress Tab fields (from main.fxml)
    @FXML private ComboBox<String> exerciseFilterCombo;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private VBox progressChartsBox;
    @FXML
    private ListView<Exercise> exerciseListView;
    @FXML
    private Label selectedMuscleLabel;
    @FXML
    private TextArea exerciseDescriptionArea;

    // Workout Tab Components
    @FXML
    private ComboBox<String> routineSelector;
    @FXML
    private Button startWorkoutBtn, endWorkoutBtn;
    @FXML
    private Label workoutTimerLabel;
    @FXML
    private VBox exerciseLogBox;

    // Progress Tab Components

   @FXML
    // Add with other private fields
    private ProgressController progressController;  // NEW - ProgressController instance

    @FXML
    // Declare the progress controller instance
    private DatabaseHelper dbHelper;
    private User currentUser;
    private Workout activeWorkout;
    private ObservableList<Exercise> exercises;
    @FXML
    private Button logoutButton;
    private Workout currentWorkout = null;
    private Timeline workoutTimer;
    private long workoutStartTime = 0;
    @FXML
    private void handleLogSet() {
        if (currentWorkout == null) {
            showAlert(Alert.AlertType.WARNING, "No Active Workout", "Start a workout first!");
            return;
        }

        try {
            // Get selected exercise
            Exercise selectedExercise = exerciseComboBox.getValue();
            if (selectedExercise == null) {
                showAlert(Alert.AlertType.WARNING, "No Exercise Selected", "Please select an exercise!");
                return;
            }

            // Parse weight and reps
            double weight = Double.parseDouble(weightField.getText().trim());
            int reps = Integer.parseInt(repsField.getText().trim());

            if (weight <= 0 || reps <= 0) {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Weight and reps must be positive!");
                return;
            }

            // Create and add set
            WorkoutSet set = new WorkoutSet(selectedExercise, reps, weight);
            set.setCompleted(true);
            currentWorkout.addSet(set);
            addSetToExerciseLog(selectedExercise.getName(), reps, weight);
            // Update display
            updateCurrentWorkoutDisplay();

            // Clear fields
            weightField.clear();
            repsField.clear();

            System.out.println("✅ Logged set: " + set);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for weight and reps!");
        }
    }
    private void addSetToExerciseLog(String exerciseName, int reps, double weight) {
        HBox setEntry = new HBox(15);
        setEntry.setAlignment(Pos.CENTER_LEFT);
        setEntry.setStyle(
                "-fx-background-color: linear-gradient(to right, #2b2b2b, #1a1a1a);" +
                        "-fx-padding: 12px 20px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-color: #4CAF50;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.4), 8, 0, 0, 3);"
        );

        Label exerciseLabel = new Label("✅ " + exerciseName);
        exerciseLabel.setStyle(
                "-fx-text-fill: #4CAF50;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 200px;"
        );

        Label detailsLabel = new Label(reps + " reps @ " + String.format("%.0f", weight) + " lbs");
        detailsLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );

        Label volumeLabel = new Label("Volume: " + String.format("%.0f", reps * weight) + " lbs");
        volumeLabel.setStyle(
                "-fx-text-fill: #00d4ff;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        setEntry.getChildren().addAll(exerciseLabel, detailsLabel, spacer, volumeLabel);

        // Add to top of exercise log
        exerciseLogBox.getChildren().add(0, setEntry);

        VBox.setMargin(setEntry, new Insets(5));
    }
    private void updateCurrentWorkoutDisplay() {
        if (currentWorkout == null) return;

        StringBuilder display = new StringBuilder();
        display.append(String.format("📊 Current Workout Stats:\n\n"));
        display.append(String.format("Sets: %d\n", currentWorkout.getTotalSets()));
        display.append(String.format("Reps: %d\n", currentWorkout.getTotalReps()));
        display.append(String.format("Volume: %,.0f lbs\n\n", currentWorkout.getTotalVolume()));
        display.append("Recent Sets:\n");

        List<WorkoutSet> sets = currentWorkout.getSets();
        int displayCount = Math.min(5, sets.size());
        for (int i = sets.size() - displayCount; i < sets.size(); i++) {
            WorkoutSet set = sets.get(i);
            display.append(String.format("• %s: %d reps @ %.0f lbs\n",
                    set.getExercise().getName(), set.getReps(), set.getWeight()));
        }

        currentWorkoutStatsLabel.setText(display.toString());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dbHelper = DatabaseHelper.getInstance();
        sessionManager = SessionManager.getInstance();
        exercises = FXCollections.observableArrayList();
        loadExerciseDatabase();
        // Initialize all tabs
        initializeHomeTab();
        initializeMuscleMapTab();
        initializeWorkoutTab();
        initializeProgressTab();
        // LOGOUT BUTTON STYLING
        if (logoutButton != null) {
            logoutButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ff5555, #ff3333); " +
                            "-fx-text-fill: #FFFFFF; " +
                            "-fx-font-size: 16px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 10px 24px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-border-color: #FFFFFF; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0, 0, 3);"
            );

            logoutButton.setOnMouseEntered(e -> {
                logoutButton.setStyle(
                        "-fx-background-color: linear-gradient(to right, #ff3333, #ff0000); " +
                                "-fx-text-fill: #FFFFFF; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 10px 24px; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-border-color: #00f5ff; " +
                                "-fx-border-width: 3px; " +
                                "-fx-border-radius: 8px; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(255,0,0,0.6), 10, 0, 0, 5);"
                );
            });

            logoutButton.setOnMouseExited(e -> {
                logoutButton.setStyle(
                        "-fx-background-color: linear-gradient(to right, #ff5555, #ff3333); " +
                                "-fx-text-fill: #FFFFFF; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 10px 24px; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-border-color: #FFFFFF; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 8px; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0, 0, 3);"
                );
            });

            logoutButton.setOnAction(e -> handleLogout());
        }

        // TAB SWITCHING LISTENER
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && newTab.getText() != null) {
                    System.out.println("📍 Switched to tab: " + newTab.getText());

                    if (newTab.getText().contains("Progress") && currentUser != null) {
                        System.out.println("🔄 Refreshing Progress tab...");
                        loadProgressCharts();
                    }

                    if (newTab.getText().contains("Home") && currentUser != null) {
                        System.out.println("🔄 Refreshing Home tab...");
                        loadUserWorkouts();
                        updateUserStats();
                    }
                }
            });

        }
        // ✅ ADD THIS AT THE END - Initialize Progress Controller
        progressController = new ProgressController();
        progressController.initializeComponents(
                this.progressChartsBox,
                this.exerciseFilterCombo,
                this.fromDatePicker,
                this.toDatePicker);
    }



    /**
     * Load the ProgressController dynamically
     */
    private void loadProgressData() {
        System.out.println("📊 loadProgressData() called");
        loadProgressCharts();  // Call existing method
    }

    // Add this NEW method to set user after login:
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ User set in MainController: " + (user != null ? user.getUsername() : "null"));

        if (user != null) {
            // Update UI
            updateWelcomeLabel();
            updateUserStats();
            loadUserWorkouts();

            // Load progress data
            System.out.println("🔄 Loading initial progress data");
            loadProgressCharts();
        }
    }
    private void updateWelcomeLabel() {
        if (currentUser != null && welcomeLabel != null) {
            welcomeLabel.setText("Welcome back, " + currentUser.getUsername() + "! 💪");
        }
    }


    // Add logout handler:
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Logout");
        confirmAlert.setHeaderText("Are you sure you want to logout?");
        confirmAlert.setContentText("Your progress will be saved.");

        DialogPane dialogPane = confirmAlert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #2b2b2b;");
        dialogPane.lookup(".content.label").setStyle("-fx-text-fill: white;");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            sessionManager.logout();
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            Stage stage = (Stage) mainTabPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("💪 MuscleMap - Login");
            stage.centerOnScreen();

        } catch (Exception e) {
            System.err.println("❌ Error loading login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUserWorkouts() {
        if (currentUser != null && currentUser.getId() != null) {
            // NEW
            List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId());


            displayRecentWorkouts(workouts);
        }
    }

    private void updateUserStats() {
        if (currentUser != null) {
            streakLabel.setText("Current Streak: " + currentUser.getWorkoutStreak() + " days 🔥");

            if (currentUser.getId() != null) {
                List<Workout> recentWorkouts = dbHelper.getWorkoutsByUser(currentUser.getId());
                if (!recentWorkouts.isEmpty()) {
                    recentWorkouts.sort((w1, w2) -> w2.getStartTime().compareTo(w1.getStartTime()));
                    Workout lastWorkout = recentWorkouts.get(0);
                    lastWorkoutLabel.setText("Last Workout: " +
                            lastWorkout.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                } else {
                    lastWorkoutLabel.setText("Last Workout: Start your first workout! 🚀");
                }
            }
        }
    }

    private void displayRecentWorkouts(List<Workout> workouts) {
        recentWorkoutsBox.getChildren().clear();

        if (workouts.isEmpty()) {
            Label noWorkoutsLabel = new Label("No workouts yet. Start your first workout! 💪");
            noWorkoutsLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-padding: 8px;");
            recentWorkoutsBox.getChildren().add(noWorkoutsLabel);
            return;
        }

        workouts.sort((w1, w2) -> w2.getStartTime().compareTo(w1.getStartTime()));

        int count = 0;
        for (Workout workout : workouts) {
            if (count >= 4) break;

            String duration = workout.getFormattedDuration();
            String timeAgo = getTimeAgo(workout.getStartTime());

            Label workoutLabel = new Label(
                    "💪 " + workout.getName() + " - " + duration + " - " + timeAgo
            );
            workoutLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-padding: 8px; " +
                    "-fx-background-color: #3a3a3a; -fx-background-radius: 8px; " +
                    "-fx-border-color: #555555; -fx-border-radius: 8px;");
            VBox.setMargin(workoutLabel, new Insets(2));
            recentWorkoutsBox.getChildren().add(workoutLabel);
            count++;
        }
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        long days = java.time.Duration.between(dateTime, LocalDateTime.now()).toDays();
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    private void initializeHomeTab() {
        if (currentUser != null) {
            welcomeLabel.setText("Welcome back, " + currentUser.getUsername() + "! 💪");
        } else {
            welcomeLabel.setText("Welcome to MuscleMap! 💪");
        }

        streakLabel.setText("Current Streak: 7 days 🔥");
        lastWorkoutLabel.setText("Last Workout: " + LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + " 🚀");

        loadRecentWorkouts();

        // FIXED START QUICK WORKOUT BUTTON - MAXIMUM VISIBILITY
        if (startQuickWorkoutBtn != null) {
            startQuickWorkoutBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #00d4ff, #00a0cc); " +
                            "-fx-text-fill: #FFFFFF; " +  // PURE WHITE TEXT
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 14px 28px; " +
                            "-fx-background-radius: 12px; " +
                            "-fx-border-color: #FFFFFF; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 12px; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.6), 12, 0, 0, 6);"
            );

            // HOVER EFFECT
            startQuickWorkoutBtn.setOnMouseEntered(e -> {
                startQuickWorkoutBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #00f5ff, #00c4dd); " +
                                "-fx-text-fill: #000000; " +  // BLACK TEXT ON HOVER
                                "-fx-font-size: 19px; " +  // SLIGHTLY LARGER
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 14px 28px; " +
                                "-fx-background-radius: 12px; " +
                                "-fx-border-color: #FFFFFF; " +
                                "-fx-border-width: 3px; " +
                                "-fx-border-radius: 12px; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.9), 16, 0, 0, 8);"
                );
            });

            startQuickWorkoutBtn.setOnMouseExited(e -> {
                startQuickWorkoutBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #00d4ff, #00a0cc); " +
                                "-fx-text-fill: #FFFFFF; " +
                                "-fx-font-size: 18px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 14px 28px; " +
                                "-fx-background-radius: 12px; " +
                                "-fx-border-color: #FFFFFF; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 12px; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.6), 12, 0, 0, 6);"
                );
            });

            startQuickWorkoutBtn.setOnAction(e -> startQuickWorkout());
        }
    }


    private void initializeMuscleMapTab() {
        selectedMuscleLabel.setText("🎯 Click on a muscle group to see exercises");
        selectedMuscleLabel.setStyle(
                "-fx-text-fill: #FFFFFF; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 4, 0, 0, 2);"
        );

        exerciseDescriptionArea.setEditable(false);
        UIStyler.styleTextArea(exerciseDescriptionArea);

        createMuscleMap();

        exerciseListView.setCellFactory(listView -> new ExerciseListCell());

        // FIXED LISTVIEW BACKGROUND - DARK WITH PROPER STYLING
        exerciseListView.setStyle(
                "-fx-background-color: #0a0a0a; " +  // VERY DARK BACKGROUND
                        "-fx-border-color: #00f5ff; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.3), 12, 0, 0, 6);"
        );

        exerciseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showExerciseDetails(newSelection);
            }
        });
    }


    private void showExerciseDetails(Exercise newSelection) {
    }

    private void initializeWorkoutTab() {
        routineSelector.setItems(FXCollections.observableArrayList(
                "Push Day", "Pull Day", "Leg Day", "Upper Body", "Full Body", "Custom"
        ));

        // ✅ CRITICAL FIX - Initialize exercise combo box with exercises from database
        if (exerciseComboBox != null && exercises != null && !exercises.isEmpty()) {
            exerciseComboBox.setItems(exercises); // Use the loaded exercises

            // Custom cell factory for display
            exerciseComboBox.setCellFactory(param -> new ListCell<Exercise>() {
                @Override
                protected void updateItem(Exercise item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });

            exerciseComboBox.setButtonCell(new ListCell<Exercise>() {
                @Override
                protected void updateItem(Exercise item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Select exercise...");
                    } else {
                        setText(item.getName());
                    }
                }
            });

            System.out.println("✅ Exercise ComboBox loaded with " + exercises.size() + " exercises");
        } else {
            System.err.println("❌ ERROR: exercises list is null or empty!");
        }

        // Button styling
        startWorkoutBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #00d4ff, #00a0cc);" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14px 28px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: #FFFFFF;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.6), 12, 0, 0, 6);"
        );

        endWorkoutBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff5555, #ff3333);" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14px 28px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: #FFFFFF;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,85,85,0.6), 12, 0, 0, 6);"
        );

        endWorkoutBtn.setDisable(true);

        if (workoutTimerLabel != null) {
            workoutTimerLabel.setText("⏱️ Timer: 00:00:00");
        }

        if (currentWorkoutStatsLabel != null) {
            currentWorkoutStatsLabel.setText("Start a workout to begin logging!");
        }
    }



    private void initializeProgressTab() {
        System.out.println("📊 Initializing Progress Tab");

        // Set default date range (last 3 months)
        if (fromDatePicker != null) {
            fromDatePicker.setValue(LocalDate.now().minusMonths(3));
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(LocalDate.now());
        }

        // Add listeners for date changes
        if (fromDatePicker != null) {
            fromDatePicker.setOnAction(e -> {
                System.out.println("📅 From date changed");
                loadProgressCharts();
            });
        }
        if (toDatePicker != null) {
            toDatePicker.setOnAction(e -> {
                System.out.println("📅 To date changed");
                loadProgressCharts();
            });
        }

        // Initialize exercise filter combo
        if (exerciseFilterCombo != null) {
            exerciseFilterCombo.getItems().add("All Exercises");
            exerciseFilterCombo.setValue("All Exercises");
        }

        System.out.println("✅ Progress tab initialized successfully");
    }

    // In MainController.java - Add this to your workout completion method
    // In MainController.java - Add this to your workout completion method
    private void finishWorkout() {
        if (currentWorkout != null) {
            currentWorkout.completeWorkout(); // Sets end time

            // CRITICAL: Save to database with user ID
            int workoutId = dbHelper.saveWorkout(currentWorkout, currentUser.getId());

            if (workoutId > 0) {
                showAlert("Success", "Workout saved! Total volume: " +
                        String.format("%,.0f lbs", currentWorkout.getTotalVolume()));

                // Refresh progress tab
                if (progressController != null) {
                    progressController.setCurrentUser(currentUser);
                }
            } else {
                showAlert("Error", "Failed to save workout");
            }

            currentWorkout = null;
            updateWorkoutDisplay();
        }
    }

    // Helper method for alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Update display after workout ends
    private void updateWorkoutDisplay() {
        // Clear workout UI or update status
        if (currentWorkout == null) {
            System.out.println("✅ Workout completed and cleared");
        }
    }


    private void debugLoadedWorkouts() {
        if (currentUser == null || currentUser.getId() == null) {
            System.out.println("❌ DEBUG: No current user!");
            return;
        }

        System.out.println("\n=== LOADING WORKOUTS FROM DB ===");
        System.out.println("Current User ID: " + currentUser.getId());

        List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId());
        System.out.println("Total workouts found: " + workouts.size());

        if (workouts.isEmpty()) {
            System.out.println("⚠️ No workouts found in database!");
        } else {
            for (int i = 0; i < workouts.size(); i++) {
                Workout w = workouts.get(i);
                System.out.println("\n📊 Workout #" + (i+1) + ":");
                System.out.println("   ID: " + w.getId());
                System.out.println("   Name: " + w.getName());
                System.out.println("   Start: " + w.getStartTime());
                System.out.println("   Sets Count: " + w.getSets().size());
                System.out.println("   Total Volume: " + w.getTotalVolume());

                if (w.getSets().isEmpty()) {
                    System.out.println("   ⚠️ WARNING: Workout has no sets loaded!");
                } else {
                    for (int j = 0; j < w.getSets().size(); j++) {
                        WorkoutSet set = w.getSets().get(j);
                        System.out.println("      Set " + (j+1) + ": " +
                                set.getExercise().getName() + " - " +
                                set.getReps() + " reps @ " + set.getWeight() + " lbs");
                    }
                }
            }
        }
        System.out.println("================================\n");
    }



    private void debugPrintWorkouts() {
        if (currentUser != null && currentUser.getId() != null) {
            List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId());
            System.out.println("=== DEBUG: User Workouts ===");
            System.out.println("User ID: " + currentUser.getId());
            System.out.println("Total Workouts: " + workouts.size());
            for (Workout w : workouts) {
                System.out.println("- " + w.getName() + " | " +
                        w.getStartTime() + " | Volume: " + w.getTotalVolume() +
                        " | Sets: " + w.getSets().size());
            }
            System.out.println("========================");
        }
    }

    private void createMuscleMap() {
        muscleMapPane.setPrefSize(700, 800);

        // Premium dark gradient background
        muscleMapPane.setStyle(
                "-fx-background-color: " +
                        "radial-gradient(center 50% 40%, radius 80%, " +
                        "rgba(10, 15, 25, 1) 0%, " +
                        "rgba(5, 8, 15, 1) 70%, " +
                        "rgba(2, 5, 10, 1) 100%); " +
                        "-fx-background-radius: 30px; " +
                        "-fx-border-color: linear-gradient(to right, #00f5ff, #ff6b6b, #4ecdc4); " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 30px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 25, 0, 0, 10);"
        );

        // Futuristic title with neon effect
        Label titleLabel = new Label("⚡ HEXAGONAL MUSCLE ATLAS");
        titleLabel.setLayoutX(150);
        titleLabel.setLayoutY(25);
        titleLabel.setStyle(
                "-fx-text-fill: linear-gradient(to right, #00f5ff, #ff6b6b); " +
                        "-fx-font-size: 28px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.8), 8, 0, 0, 0);"
        );
        muscleMapPane.getChildren().add(titleLabel);

        // Modern subtitle
        Label subtitleLabel = new Label("🎯 Interactive Hexagonal Body Mapping System");
        subtitleLabel.setLayoutX(180);
        subtitleLabel.setLayoutY(60);
        subtitleLabel.setStyle(
                "-fx-text-fill: #FFFFFF; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-style: italic; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 4, 0, 0, 2);"
        );
        muscleMapPane.getChildren().add(subtitleLabel);

        // ===== COMPACT HEXAGONAL BODY LAYOUT =====

        // HEAD REGION - Top hexagon
        createHexagonMuscle("🧠 HEAD", 350, 100, "neck", "#8E24AA", "NEURAL");

        // UPPER BODY HEXAGON ROW 1
        createHexagonMuscle("🤸 SHOULDERS", 220, 150, "shoulders", "#FF9800", "DELTOID");
        createHexagonMuscle("💪 TRAPS", 350, 150, "traps", "#5E35B1", "TRAPEZIUS");
        createHexagonMuscle("🏋️ UPPER BACK", 480, 150, "back", "#7B1FA2", "RHOMBOID");

        // UPPER BODY HEXAGON ROW 2
        createHexagonMuscle("💪 BICEPS", 150, 210, "biceps", "#4CAF50", "BRACHII");
        createHexagonMuscle("❤️ CHEST", 270, 210, "chest", "#E53935", "PECTORAL");
        createHexagonMuscle("🏋️ LATS", 430, 210, "back", "#8E24AA", "LATISSIMUS");
        createHexagonMuscle("💪 TRICEPS", 550, 210, "triceps", "#2196F3", "BRACHII");

        // MID BODY HEXAGON ROW
        createHexagonMuscle("⚡ CORE", 270, 270, "abs", "#EC407A", "RECTUS");
        createHexagonMuscle("⚡ OBLIQUES", 430, 270, "abs", "#F06292", "LATERAL");

        // LOWER BODY HEXAGON ROW 1
        createHexagonMuscle("🍑 GLUTES", 350, 330, "glutes", "#607D8B", "MAXIMUS");

        // LOWER BODY HEXAGON ROW 2
        createHexagonMuscle("🦵 QUADS", 270, 390, "quadriceps", "#FF5722", "FEMORIS");
        createHexagonMuscle("🦵 HAMSTRINGS", 430, 390, "hamstrings", "#FFC107", "BICEPS");

        // LOWER LEG HEXAGON
        createHexagonMuscle("🦵 CALVES", 350, 450, "calves", "#8D6E63", "GASTRO");

        // ===== PERFECT POSITIONED INFO PANELS - DO NOT CHANGE =====

        // Left Panel - System Stats - PERFECT POSITION - DO NOT CHANGE
        VBox leftPanel = createClearPanel(
                "🔬 SYSTEM ANALYTICS",
                "• 12 Primary Muscle Groups\n• Hexagonal Mapping System\n• Interactive Neural Network\n• Real-time Exercise Database",
                30, 500, 300, "#00f5ff"
        );
        muscleMapPane.getChildren().add(leftPanel);

        // Right Panel - Training Protocol - PERFECT POSITION - DO NOT CHANGE
        VBox rightPanel = createClearPanel(
                "🚀 TRAINING PROTOCOL",
                "• Hover: View muscle data\n• Click: Access exercise library\n• Progressive overload system\n• Anatomical precision training",
                370, 500, 300, "#ff6b6b"
        );
        muscleMapPane.getChildren().add(rightPanel);
    }

    // ANTI-FLICKER Hexagonal Muscle Button - Smooth and Stable
    private void createHexagonMuscle(String muscleName, double x, double y, String muscleGroup, String color, String scientificTag) {
        // Create hexagonal muscle button
        javafx.scene.shape.Polygon hexagon = new javafx.scene.shape.Polygon();
        hexagon.getPoints().addAll(new Double[]{
                0.0, -20.0,    // Top (smaller hexagon)
                18.0, -10.0,   // Top right
                18.0, 10.0,    // Bottom right
                0.0, 20.0,     // Bottom
                -18.0, 10.0,   // Bottom left
                -18.0, -10.0   // Top left
        });

        hexagon.setFill(Color.web(color));
        hexagon.setStroke(Color.web("#ffffff"));
        hexagon.setStrokeWidth(2);
        hexagon.setLayoutX(x);
        hexagon.setLayoutY(y);

        // STABLE glow effect - no flickering
        hexagon.setStyle(
                "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,0,0,0.6), 6, 0, 0, 3), " +
                        "innershadow(gaussian, rgba(255,255,255,0.2), 2, 0, 0, 1);"
        );

        // Create STABLE label container - positioned ABOVE hexagon
        VBox labelContainer = new VBox(3);
        labelContainer.setLayoutX(x - 70);
        labelContainer.setLayoutY(y - 70);
        labelContainer.setPrefWidth(140);
        labelContainer.setAlignment(Pos.CENTER);
        labelContainer.setVisible(false);
        labelContainer.setMouseTransparent(true);

        // Scientific tag label - FIXED VISIBILITY WITH BLACK TEXT ON COLORED BACKGROUND
        Label scientificLabel = new Label(scientificTag);
        scientificLabel.setMouseTransparent(true);
        scientificLabel.setStyle(
                "-fx-text-fill: #000000; " +  // CHANGED TO BLACK FOR VISIBILITY
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-padding: 4px 10px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: #FFFFFF; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0, 0, 3);"
        );
        scientificLabel.setAlignment(Pos.CENTER);

        // Muscle name label - FIXED WITH WHITE TEXT ON SOLID BLACK BACKGROUND
        Label muscleLabel = new Label(muscleName);
        muscleLabel.setMouseTransparent(true);
        muscleLabel.setStyle(
                "-fx-text-fill: #FFFFFF; " +  // WHITE TEXT
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: rgba(0, 0, 0, 1); " +  // SOLID BLACK BACKGROUND
                        "-fx-padding: 8px 15px; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-border-color: #FFFFFF; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 8, 0, 0, 4);"
        );
        muscleLabel.setAlignment(Pos.CENTER);

        // Add labels to container
        labelContainer.getChildren().addAll(scientificLabel, muscleLabel);

        // Click handler
        hexagon.setOnMouseClicked(e -> selectMuscleGroup(muscleName, muscleGroup));

        // FIXED HOVER EFFECTS WITH PROPER VISIBILITY
        hexagon.setOnMouseEntered(e -> {
            labelContainer.setVisible(true);
            labelContainer.toFront();

            javafx.animation.Timeline scaleUp = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(150),
                            new javafx.animation.KeyValue(hexagon.scaleXProperty(), 1.2),
                            new javafx.animation.KeyValue(hexagon.scaleYProperty(), 1.2)
                    )
            );
            scaleUp.play();

            hexagon.setStroke(Color.web("#00f5ff"));
            hexagon.setStrokeWidth(3);

            hexagon.setStyle(
                    "-fx-effect: " +
                            "dropshadow(gaussian, #00f5ff, 8, 0.3, 0, 0), " +
                            "dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0, 0, 3), " +
                            "innershadow(gaussian, rgba(255,255,255,0.4), 3, 0, 0, 1);"
            );

            // HOVER STATE - BLACK TEXT ON CYAN BACKGROUND FOR MAXIMUM CONTRAST
            scientificLabel.setStyle(
                    "-fx-text-fill: #000000; " +  // BLACK TEXT
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-color: #00f5ff; " +  // BRIGHT CYAN BACKGROUND
                            "-fx-padding: 5px 12px; " +
                            "-fx-background-radius: 12px; " +
                            "-fx-border-color: #FFFFFF; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 12px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 8, 0, 0, 4);"
            );

            muscleLabel.setStyle(
                    "-fx-text-fill: #00f5ff; " +  // CYAN TEXT
                            "-fx-font-size: 16px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-color: rgba(0, 0, 0, 1); " +  // SOLID BLACK
                            "-fx-padding: 10px 18px; " +
                            "-fx-background-radius: 15px; " +
                            "-fx-border-color: #00f5ff; " +  // CYAN BORDER
                            "-fx-border-width: 3px; " +
                            "-fx-border-radius: 15px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.6), 12, 0, 0, 6);"
            );
        });

        hexagon.setOnMouseExited(e -> {
            labelContainer.setVisible(false);

            javafx.animation.Timeline scaleDown = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(150),
                            new javafx.animation.KeyValue(hexagon.scaleXProperty(), 1.0),
                            new javafx.animation.KeyValue(hexagon.scaleYProperty(), 1.0)
                    )
            );
            scaleDown.play();

            hexagon.setStroke(Color.web("#ffffff"));
            hexagon.setStrokeWidth(2);

            hexagon.setStyle(
                    "-fx-effect: " +
                            "dropshadow(gaussian, rgba(0,0,0,0.6), 6, 0, 0, 3), " +
                            "innershadow(gaussian, rgba(255,255,255,0.2), 2, 0, 0, 1);"
            );

            // RESET TO BLACK TEXT ON COLORED BACKGROUND
            scientificLabel.setStyle(
                    "-fx-text-fill: #000000; " +
                            "-fx-font-size: 11px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-color: " + color + "; " +
                            "-fx-padding: 4px 10px; " +
                            "-fx-background-radius: 10px; " +
                            "-fx-border-color: #FFFFFF; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 10px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0, 0, 3);"
            );

            muscleLabel.setStyle(
                    "-fx-text-fill: #FFFFFF; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-color: rgba(0, 0, 0, 1); " +
                            "-fx-padding: 8px 15px; " +
                            "-fx-background-radius: 12px; " +
                            "-fx-border-color: #FFFFFF; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 12px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 8, 0, 0, 4);"
            );
        });

        muscleMapPane.getChildren().addAll(hexagon, labelContainer);
    }


    // Crystal Clear Info Panel Creator - POSITIONS UNCHANGED
    private VBox createClearPanel(String title, String content, double x, double y, double width, String accentColor) {
        VBox panel = new VBox(15);
        panel.setLayoutX(x);
        panel.setLayoutY(y);
        panel.setPrefWidth(width);
        panel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.95); " +  // MORE OPAQUE BLACK
                        "-fx-padding: 20px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-border-color: " + accentColor + "; " +
                        "-fx-border-width: 3px; " +  // THICKER BORDER
                        "-fx-border-radius: 20px; " +
                        "-fx-effect: dropshadow(gaussian, " + accentColor + ", 20, 0.7, 0, 8);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: " + accentColor + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 20px; " +  // LARGER FONT
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 4, 0, 0, 2);"
        );

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                "-fx-text-fill: #FFFFFF; " +  // PURE WHITE
                        "-fx-font-size: 15px; " +  // LARGER FONT
                        "-fx-font-weight: 600; " +  // BOLDER
                        "-fx-line-spacing: 6px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 3, 0, 0, 1);"
        );

        panel.getChildren().addAll(titleLabel, contentLabel);
        return panel;
    }


    private void selectMuscleGroup(String muscleName, String muscleGroup) {
        selectedMuscleLabel.setText("🎯 Selected: " + muscleName);
        selectedMuscleLabel.setStyle(
                "-fx-text-fill: #00f5ff; " +  // BRIGHT CYAN
                        "-fx-font-size: 20px; " +  // LARGER
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 4, 0, 0, 2);"
        );

        List<Exercise> filteredExercises = exercises.stream()
                .filter(ex -> ex.getMuscleGroup().toLowerCase().contains(muscleGroup.toLowerCase()) ||
                        ex.getName().toLowerCase().contains(muscleGroup.toLowerCase()))
                .sorted((e1, e2) -> e1.getDifficulty().compareTo(e2.getDifficulty()))
                .toList();

        exerciseListView.setItems(FXCollections.observableArrayList(filteredExercises));

        if (filteredExercises.isEmpty()) {
            exerciseDescriptionArea.setText(
                    "🚧 No exercises found for " + muscleName + "\n\n" +
                            "💡 Try selecting a different muscle group!\n\n" +
                            "🎯 Available muscle groups:\n" +
                            "• Chest • Back • Shoulders • Biceps • Triceps\n" +
                            "• Quadriceps • Hamstrings • Glutes • Calves • Abs"
            );
        } else {
            exerciseDescriptionArea.setText(
                    "💪 " + muscleName.toUpperCase() + " SELECTED!\n" +
                            "═══════════════════════════════\n\n" +
                            "🎯 Found " + filteredExercises.size() + " exercises\n" +
                            "📊 Difficulty levels: Beginner to Advanced\n" +
                            "🏋️ Equipment: Bodyweight, Dumbbells, Barbells, Machines\n\n" +
                            "👆 Click on an exercise from the list above to see:\n" +
                            "• Detailed form instructions\n" +
                            "• Proper breathing technique\n" +
                            "• Common mistakes to avoid\n" +
                            "• Progressive variations\n\n" +
                            "🚀 Ready to build stronger " + muscleName.toLowerCase() + "?"
            );
        }

        // FIXED TEXT AREA STYLING WITH HIGH CONTRAST
        exerciseDescriptionArea.setStyle(
                "-fx-text-fill: #FFFFFF; " +  // PURE WHITE TEXT
                        "-fx-control-inner-background: rgba(10, 10, 10, 0.98); " +  // DARKER BACKGROUND
                        "-fx-background-color: rgba(10, 10, 10, 0.98); " +
                        "-fx-background-radius: 12px; " +
                        "-fx-border-color: #00f5ff; " +  // CYAN BORDER
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-font-size: 15px; " +  // LARGER FONT
                        "-fx-font-weight: 600; " +  // BOLDER
                        "-fx-font-family: 'Segoe UI', 'Arial', sans-serif; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.3), 10, 0, 0, 4);"
        );
    }


    // Dramatically expanded exercise database
    private void loadExerciseDatabase() {
        exercises.addAll(Arrays.asList(
                // CHEST EXERCISES (Enhanced)
                new Exercise("Bench Press", "Chest", "Barbell", "Intermediate", "Lie on bench, lower bar to chest with control, press up powerfully"),
                new Exercise("Incline Bench Press", "Chest", "Barbell", "Intermediate", "30-45 degree incline, targets upper chest fibers"),
                new Exercise("Decline Bench Press", "Chest", "Barbell", "Intermediate", "Decline angle targets lower chest, safer for shoulders"),
                new Exercise("Push-ups", "Chest", "Bodyweight", "Beginner", "Classic bodyweight exercise, modify angle for difficulty"),
                new Exercise("Incline Push-ups", "Chest", "Bodyweight", "Beginner", "Feet elevated, targets upper chest"),
                new Exercise("Diamond Push-ups", "Chest", "Bodyweight", "Advanced", "Hands in diamond shape, emphasizes triceps and inner chest"),
                new Exercise("Dumbbell Flyes", "Chest", "Dumbbell", "Intermediate", "Wide arc motion, excellent for chest isolation"),
                new Exercise("Incline Dumbbell Press", "Chest", "Dumbbell", "Intermediate", "Upper chest focus with dumbbells"),
                new Exercise("Chest Dips", "Chest", "Bodyweight", "Intermediate", "Lean forward for chest emphasis"),
                new Exercise("Cable Crossovers", "Chest", "Cable", "Intermediate", "Constant tension throughout range of motion"),

                // BACK EXERCISES (Enhanced)
                new Exercise("Deadlifts", "Back", "Barbell", "Advanced", "King of exercises, full posterior chain activation"),
                new Exercise("Pull-ups", "Back", "Bodyweight", "Intermediate", "Wide grip for lats, narrow for rhomboids"),
                new Exercise("Chin-ups", "Back", "Bodyweight", "Intermediate", "Underhand grip, more bicep involvement"),
                new Exercise("Bent-over Rows", "Back", "Barbell", "Intermediate", "Hip hinge position, row to lower chest"),
                new Exercise("T-Bar Rows", "Back", "Machine", "Intermediate", "Thick grip, mid-trap and rhomboid focus"),
                new Exercise("Seated Cable Rows", "Back", "Cable", "Beginner", "Controlled movement, squeeze shoulder blades"),
                new Exercise("Lat Pulldowns", "Back", "Cable", "Beginner", "Wide grip, pull to upper chest"),
                new Exercise("Single-arm Dumbbell Rows", "Back", "Dumbbell", "Beginner", "Unilateral training, core stability"),
                new Exercise("Face Pulls", "Back", "Cable", "Beginner", "Rear delt and upper back, posture correction"),
                new Exercise("Inverted Rows", "Back", "Bodyweight", "Beginner", "Bodyweight rowing movement"),

                // SHOULDER EXERCISES (Enhanced)
                new Exercise("Overhead Press", "Shoulders", "Barbell", "Intermediate", "Standing press, full shoulder development"),
                new Exercise("Dumbbell Shoulder Press", "Shoulders", "Dumbbell", "Intermediate", "Seated or standing, unilateral movement"),
                new Exercise("Lateral Raises", "Shoulders", "Dumbbell", "Beginner", "Side deltoid isolation, control the negative"),
                new Exercise("Front Raises", "Shoulders", "Dumbbell", "Beginner", "Front deltoid focus, alternate arms"),
                new Exercise("Rear Delt Flyes", "Shoulders", "Dumbbell", "Beginner", "Bent over, rear deltoid isolation"),
                new Exercise("Arnold Press", "Shoulders", "Dumbbell", "Intermediate", "Full range rotation, all deltoid heads"),
                new Exercise("Pike Push-ups", "Shoulders", "Bodyweight", "Intermediate", "Inverted position, shoulder press movement"),
                new Exercise("Handstand Push-ups", "Shoulders", "Bodyweight", "Advanced", "Advanced vertical pushing movement"),

                // ARM EXERCISES (Enhanced)
                new Exercise("Bicep Curls", "Biceps", "Dumbbell", "Beginner", "Classic bicep exercise, control the movement"),
                new Exercise("Hammer Curls", "Biceps", "Dumbbell", "Beginner", "Neutral grip, targets brachialis"),
                new Exercise("Concentration Curls", "Biceps", "Dumbbell", "Intermediate", "Seated, isolated bicep movement"),
                new Exercise("Cable Curls", "Biceps", "Cable", "Intermediate", "Constant tension throughout rep"),
                new Exercise("21s", "Biceps", "Barbell", "Advanced", "7 bottom half + 7 top half + 7 full reps"),
                new Exercise("Tricep Dips", "Triceps", "Bodyweight", "Intermediate", "Compound tricep exercise"),
                new Exercise("Close-Grip Bench Press", "Triceps", "Barbell", "Intermediate", "Narrow grip emphasizes triceps"),
                new Exercise("Overhead Tricep Extension", "Triceps", "Dumbbell", "Beginner", "Seated or standing, full stretch"),
                new Exercise("Tricep Pushdowns", "Triceps", "Cable", "Beginner", "Keep elbows stable, focus on triceps"),
                new Exercise("Diamond Push-ups", "Triceps", "Bodyweight", "Advanced", "Bodyweight tricep emphasis"),

                // LEG EXERCISES (Enhanced)
                new Exercise("Squats", "Quadriceps", "Barbell", "Intermediate", "King of leg exercises, full quad development"),
                new Exercise("Front Squats", "Quadriceps", "Barbell", "Advanced", "Front-loaded, more quad emphasis"),
                new Exercise("Goblet Squats", "Quadriceps", "Dumbbell", "Beginner", "Dumbbell front squat variation"),
                new Exercise("Leg Press", "Quadriceps", "Machine", "Beginner", "Machine-based quad exercise"),
                new Exercise("Lunges", "Quadriceps", "Bodyweight", "Beginner", "Unilateral leg exercise"),
                new Exercise("Bulgarian Split Squats", "Quadriceps", "Bodyweight", "Intermediate", "Rear foot elevated lunges"),
                new Exercise("Romanian Deadlifts", "Hamstrings", "Barbell", "Intermediate", "Hip hinge movement, hamstring stretch"),
                new Exercise("Stiff Leg Deadlifts", "Hamstrings", "Dumbbell", "Beginner", "Straight leg hamstring exercise"),
                new Exercise("Leg Curls", "Hamstrings", "Machine", "Beginner", "Isolated hamstring exercise"),
                new Exercise("Walking Lunges", "Quadriceps", "Bodyweight", "Beginner", "Dynamic lunge variation"),

                // GLUTE EXERCISES (New category)
                new Exercise("Hip Thrusts", "Glutes", "Barbell", "Intermediate", "Primary glute exercise, full hip extension"),
                new Exercise("Glute Bridges", "Glutes", "Bodyweight", "Beginner", "Bodyweight glute activation"),
                new Exercise("Romanian Deadlifts", "Glutes", "Barbell", "Intermediate", "Hip hinge, glute and hamstring focus"),
                new Exercise("Step-ups", "Glutes", "Bodyweight", "Beginner", "Unilateral glute and quad exercise"),

                // CORE EXERCISES (Enhanced)
                new Exercise("Planks", "Abs", "Bodyweight", "Beginner", "Isometric core stability exercise"),
                new Exercise("Crunches", "Abs", "Bodyweight", "Beginner", "Classic abdominal exercise"),
                new Exercise("Russian Twists", "Abs", "Bodyweight", "Intermediate", "Rotational core movement"),
                new Exercise("Dead Bugs", "Abs", "Bodyweight", "Beginner", "Core stability and coordination"),
                new Exercise("Mountain Climbers", "Abs", "Bodyweight", "Intermediate", "Dynamic core and cardio"),
                new Exercise("Bicycle Crunches", "Abs", "Bodyweight", "Intermediate", "Rotational abdominal exercise"),
                new Exercise("Leg Raises", "Abs", "Bodyweight", "Intermediate", "Lower abdominal focus"),
                new Exercise("Hanging Leg Raises", "Abs", "Bodyweight", "Advanced", "Advanced lower ab exercise"),
                new Exercise("Ab Wheel Rollouts", "Abs", "Equipment", "Advanced", "Full core extension exercise"),
                new Exercise("L-Sits", "Abs", "Bodyweight", "Advanced", "Isometric core strength"),

                // CALF EXERCISES (Enhanced)
                new Exercise("Standing Calf Raises", "Calves", "Bodyweight", "Beginner", "Gastrocnemius focus"),
                new Exercise("Seated Calf Raises", "Calves", "Machine", "Beginner", "Soleus muscle focus"),
                new Exercise("Single-leg Calf Raises", "Calves", "Bodyweight", "Intermediate", "Unilateral calf training"),
                new Exercise("Jump Rope", "Calves", "Equipment", "Intermediate", "Dynamic calf training with cardio"),

                // CARDIO & CONDITIONING (New category)
                new Exercise("Burpees", "Full Body", "Bodyweight", "Advanced", "Full body cardio and strength exercise"),
                new Exercise("High Knees", "Full Body", "Bodyweight", "Beginner", "Cardio warm-up exercise"),
                new Exercise("Jumping Jacks", "Full Body", "Bodyweight", "Beginner", "Full body cardio movement"),
                new Exercise("Sprint Intervals", "Full Body", "Bodyweight", "Intermediate", "High intensity cardio training"),

                // FOREARM EXERCISES (New category)
                new Exercise("Wrist Curls", "Forearms", "Dumbbell", "Beginner", "Forearm flexor strengthening"),
                new Exercise("Reverse Wrist Curls", "Forearms", "Dumbbell", "Beginner", "Forearm extensor strengthening"),
                new Exercise("Farmer's Walks", "Forearms", "Dumbbell", "Intermediate", "Grip strength and forearm endurance"),
                new Exercise("Dead Hangs", "Forearms", "Bodyweight", "Intermediate", "Grip strength from pull-up bar")
        ));
    }

    private void loadRecentWorkouts() {
        recentWorkoutsBox.getChildren().clear();

        String[] recentWorkouts = {
                "💪 Push Day - 45 min - Yesterday 🔥",
                "🔙 Pull Day - 52 min - 2 days ago 💪",
                "🦵 Leg Day - 38 min - 4 days ago 🏆",
                "🎯 Full Body - 41 min - 6 days ago ⚡"
        };

        for (String workout : recentWorkouts) {
            Label workoutLabel = new Label(workout);
            workoutLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-padding: 8px; " +
                    "-fx-background-color: #3a3a3a; -fx-background-radius: 8px; " +
                    "-fx-border-color: #555555; -fx-border-radius: 8px;");
            VBox.setMargin(workoutLabel, new Insets(2));
            recentWorkoutsBox.getChildren().add(workoutLabel);
        }
    }

    private void startQuickWorkout() {
        mainTabPane.getSelectionModel().select(workoutTab);
        startWorkout();
    }

    private void startWorkout() {
        String routineName = routineSelector.getValue();
        if (routineName == null || routineName.isEmpty()) {
            showAlert("Please select a routine first!", Alert.AlertType.WARNING);
            return;
        }

        activeWorkout = new Workout(routineName);
        activeWorkout.setUserId(currentUser.getId());
        System.out.println("🏋️ Starting workout: " + routineName);

        startWorkoutBtn.setDisable(true);
        endWorkoutBtn.setDisable(false);

        // Start timer display
        workoutTimerLabel.setText("⏱️ Workout in progress: 00:00");
        workoutTimerLabel.setStyle(
                "-fx-text-fill: #00f5ff; " +
                        "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 4, 0, 0, 2);"
        );
        // ⏱️ START ACTUAL TIMER
        workoutStartTime = System.currentTimeMillis();
        startWorkoutTimer();

        // Load exercises FIRST
        loadWorkoutExercises();

        showAlert("Workout started! 💪\nLog your sets below.", Alert.AlertType.INFORMATION);
    }



    private void endWorkout() {
        // STOP TIMER FIRST
        if (workoutTimer != null) {
            workoutTimer.stop();
            workoutTimer = null;
        }
        if (activeWorkout != null && currentUser != null) {
            activeWorkout.completeWorkout();

            // Associate workout with user
            if (currentUser.getId() != null) {
                System.out.println("\n=== SAVING WORKOUT ===");
                System.out.println("Workout Name: " + activeWorkout.getName());
                System.out.println("User ID: " + currentUser.getId());
                System.out.println("Sets Count: " + activeWorkout.getSets().size());
                System.out.println("Total Volume: " + activeWorkout.getTotalVolume());

                // Print each set
                for (int i = 0; i < activeWorkout.getSets().size(); i++) {
                    WorkoutSet set = activeWorkout.getSets().get(i);
                    System.out.println("  Set " + (i+1) + ": " +
                            set.getExercise().getName() + " - " +
                            set.getReps() + " reps @ " + set.getWeight() + " lbs = " +
                            set.getVolume() + " lbs volume");
                }

                // Save to database
                int workoutId = dbHelper.saveWorkout(activeWorkout, currentUser.getId());
                System.out.println("✅ Saved with ID: " + workoutId);
                System.out.println("====================\n");

                showAlert("🎉 Workout Completed! Amazing job! 🏆\n" +
                                "Duration: " + activeWorkout.getFormattedDuration() + "\n" +
                                "Volume: " + String.format("%,.0f", activeWorkout.getTotalVolume()) + " lbs\n" +
                                "Sets: " + activeWorkout.getSets().size(),
                        Alert.AlertType.INFORMATION);

                startWorkoutBtn.setDisable(false);
                endWorkoutBtn.setDisable(true);
                workoutTimerLabel.setText("✅ Workout Complete! Great job! 🎯");
                workoutTimerLabel.setStyle(
                        "-fx-text-fill: #4CAF50; " +
                                "-fx-font-size: 20px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 4, 0, 0, 2);"
                );

                activeWorkout = null;
                exerciseLogBox.getChildren().clear();

                // Refresh user data
                System.out.println("🔄 Refreshing Home tab data...");
                loadUserWorkouts();
                updateUserStats();

                // Force refresh progress tab
                System.out.println("🔄 Refreshing Progress tab data...");
                loadProgressCharts();

                // Verify data was loaded
                System.out.println("🔍 Verifying saved data...");
                debugLoadedWorkouts();
            }
        }
    }



    private void loadWorkoutExercises() {
        exerciseLogBox.getChildren().clear();
        String routine = routineSelector.getValue();

        List<String> exerciseNames = switch (routine) {
            case "Push Day" ->  // REMOVED emoji
                    Arrays.asList("Bench Press", "Overhead Press", "Incline Dumbbell Press", "Tricep Dips", "Lateral Raises");
            case "Pull Day" ->  // REMOVED emoji
                    Arrays.asList("Pull-ups", "Bent-over Rows", "Deadlifts", "Bicep Curls", "Face Pulls");
            case "Leg Day" ->  // REMOVED emoji
                    Arrays.asList("Squats", "Romanian Deadlifts", "Leg Press", "Lunges", "Standing Calf Raises");
            case "Upper Body" ->  // REMOVED emoji
                    Arrays.asList("Bench Press", "Pull-ups", "Overhead Press", "Bent-over Rows");
            case "Full Body" ->  // REMOVED emoji
                    Arrays.asList("Squats", "Bench Press", "Pull-ups", "Overhead Press", "Planks");
            default -> Arrays.asList("Bench Press", "Squats", "Pull-ups", "Overhead Press");
        };

        for (String exerciseName : exerciseNames) {
            VBox exerciseBox = createExerciseLogBox(exerciseName);
            exerciseLogBox.getChildren().add(exerciseBox);
        }
    }


    private VBox createExerciseLogBox(String exerciseName) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: linear-gradient(to right, #3a3a3a, #2b2b2b); " +
                "-fx-padding: 15px; -fx-background-radius: 10px; " +
                "-fx-border-color: #555555; -fx-border-radius: 10px;");

        Label nameLabel = new Label("🏋️ " + exerciseName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox setBox = new HBox(12);
        setBox.setAlignment(Pos.CENTER_LEFT);

        // Input fields with styling
        TextField setsField = new TextField("3");
        setsField.setPromptText("Sets");
        setsField.setPrefWidth(70);
        UIStyler.styleTextField(setsField);

        TextField repsField = new TextField("10");
        repsField.setPromptText("Reps");
        repsField.setPrefWidth(70);
        UIStyler.styleTextField(repsField);

        TextField weightField = new TextField("135");
        weightField.setPromptText("Weight (lbs)");
        weightField.setPrefWidth(100);
        UIStyler.styleTextField(weightField);

        Button logSetBtn = new Button("✅ Log Set");
        logSetBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 6px; " +
                "-fx-font-weight: bold;");

        // Add functionality to log set button
        logSetBtn.setOnAction(e -> {
            try {
                int sets = Integer.parseInt(setsField.getText());
                int reps = Integer.parseInt(repsField.getText());
                double weight = Double.parseDouble(weightField.getText());

                // Find the exercise and create a set
                Exercise exercise = exercises.stream()
                        .filter(ex -> ex.getName().equals(exerciseName))
                        .findFirst()
                        .orElse(new Exercise(exerciseName, "Unknown", "Unknown", "Intermediate", ""));

                WorkoutSet workoutSet = new WorkoutSet(exercise, reps, weight);
                if (activeWorkout != null) {
                    activeWorkout.addSet(workoutSet);
                }

                showAlert("🎯 Set logged successfully!\n" + reps + " reps @ " + weight + " lbs", Alert.AlertType.INFORMATION);
                logSetBtn.setText("✅ Logged!");
                logSetBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 6px;");

            } catch (NumberFormatException ex) {
                showAlert("⚠️ Please enter valid numbers for sets, reps, and weight!", Alert.AlertType.ERROR);
            }
        });

        // Create labels
        Label setsLabel = new Label("Sets:");
        Label repsLabel = new Label("Reps:");
        Label weightLabel = new Label("Weight:");

        setsLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");
        repsLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");
        weightLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

        setBox.getChildren().addAll(
                setsLabel, setsField, repsLabel, repsField, weightLabel, weightField, logSetBtn
        );

        box.getChildren().addAll(nameLabel, setBox);
        return box;
    }
    private void displayBodyMetrics() {
        // For now, display placeholder - you can add body tracking later
        String bodyText =
                "⚖️ Weight: Track in settings\n" +
                        "💪 Muscle gain: Coming soon\n" +
                        "📉 Body fat: Coming soon\n" +
                        "🎯 Goal weight: Set in profile\n\n" +
                        "💡 Tip: Log body metrics weekly!";

        VBox bodyCard = createProgressCard("📏 Body Metrics", bodyText, "#9C27B0");
        progressChartsBox.getChildren().add(bodyCard);
    }


    public void loadProgressCharts() {
        System.out.println("📊 loadProgressCharts() called");

        if (currentUser == null) {
            System.out.println("⚠️ No current user - showing no data message");
            showNoProgressData();
            return;
        }

        if (progressChartsBox == null) {
            System.err.println("❌ progressChartsBox is null! FXML injection failed.");
            return;
        }

        progressChartsBox.getChildren().clear();

        // Get date range
        LocalDate fromDate = fromDatePicker != null ? fromDatePicker.getValue() : LocalDate.now().minusMonths(3);
        LocalDate toDate = toDatePicker != null ? toDatePicker.getValue() : LocalDate.now();

        LocalDateTime startDate = fromDate.atStartOfDay();
        LocalDateTime endDate = toDate.atTime(23, 59, 59);

        System.out.println("🔄 Loading workouts for user ID: " + currentUser.getId());
        System.out.println("📅 Date range: " + fromDate + " to " + toDate);

        // Load workouts from database
        List<Workout> allWorkouts = dbHelper.getWorkoutsByUserId(currentUser.getId());
        System.out.println("📊 Total workouts from DB: " + allWorkouts.size());

        // Filter by date range
        List<Workout> workouts = allWorkouts.stream()
                .filter(w -> w.getStartTime().isAfter(startDate) &&
                        w.getStartTime().isBefore(endDate))
                .collect(Collectors.toList());

        System.out.println("📊 Workouts in date range: " + workouts.size());

        // Debug: Show workout details
        if (!workouts.isEmpty()) {
            System.out.println("📋 Workout details:");
            for (Workout w : workouts) {
                System.out.println("  - " + w.getName() +
                        " | Date: " + w.getStartTime().toLocalDate() +
                        " | Sets: " + (w.getSets() != null ? w.getSets().size() : 0) +
                        " | Volume: " + w.getTotalVolume() + " lbs");
            }
        }

        if (workouts.isEmpty()) {
            showNoProgressData();
            return;
        }

        // Create progress cards
        createProgressCards(workouts);
    }

    private void showNoProgressData() {
        if (progressChartsBox == null) return;

        VBox noDataCard = createProgressCard(
                "📊 No Data Yet",
                "🏋️ Start logging workouts to see progress!\n\n" +
                        "💪 Complete a workout in the Workout tab\n" +
                        "📈 Track your strength and volume gains\n" +
                        "🔥 Build your workout streak!\n\n" +
                        "⚠️ Make sure to LOG SETS during your workout!",
                "#FF5722"
        );
        progressChartsBox.getChildren().add(noDataCard);
        System.out.println("📊 Displayed 'No Data Yet' message");
    }

    private void createProgressCards(List<Workout> workouts) {
        // Strength Progress Card
        VBox strengthCard = createStrengthCard(workouts);
        progressChartsBox.getChildren().add(strengthCard);

        // Volume Progress Card
        VBox volumeCard = createVolumeCard(workouts);
        progressChartsBox.getChildren().add(volumeCard);

        // Consistency Card
        VBox consistencyCard = createConsistencyCard(workouts);
        progressChartsBox.getChildren().add(consistencyCard);

        // Recent Workouts Card
        VBox recentCard = createRecentWorkoutsCard(workouts);
        progressChartsBox.getChildren().add(recentCard);

        System.out.println("✅ Created " + progressChartsBox.getChildren().size() + " progress cards");
    }

    private VBox createStrengthCard(List<Workout> workouts) {
        Map<String, Double> maxWeights = new HashMap<>();

        for (Workout workout : workouts) {
            if (workout.getSets() != null) {
                for (WorkoutSet set : workout.getSets()) {
                    String exerciseName = set.getExercise().getName();
                    double weight = set.getWeight();
                    maxWeights.put(exerciseName,
                            Math.max(maxWeights.getOrDefault(exerciseName, 0.0), weight));
                }
            }
        }

        StringBuilder strengthText = new StringBuilder();
        if (maxWeights.isEmpty()) {
            strengthText.append("💪 No strength data yet\n\n");
            strengthText.append("📊 Log sets with weights to track PRs!");
        } else {
            strengthText.append("🏆 Personal Records:\n\n");
            maxWeights.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> strengthText.append(String.format("🏋️ %s: %.0f lbs\n",
                            entry.getKey(), entry.getValue())));
            strengthText.append("\n💪 Keep pushing those PRs!");
        }

        return createProgressCard("💪 Strength Progress", strengthText.toString(), "#4CAF50");
    }

    private VBox createVolumeCard(List<Workout> workouts) {
        double totalVolume = workouts.stream().mapToDouble(Workout::getTotalVolume).sum();
        double avgVolume = totalVolume / workouts.size();
        double maxVolume = workouts.stream().mapToDouble(Workout::getTotalVolume).max().orElse(0.0);

        String volumeText = String.format(
                "📊 Total Volume: %,.0f lbs\n" +
                        "📈 Average/Workout: %,.0f lbs\n" +
                        "🏆 Best Session: %,.0f lbs\n" +
                        "🔥 Total Workouts: %d\n\n" +
                        "💪 Volume = Weight × Reps × Sets",
                totalVolume, avgVolume, maxVolume, workouts.size()
        );

        return createProgressCard("📊 Volume Stats", volumeText, "#2196F3");
    }

    private VBox createConsistencyCard(List<Workout> workouts) {
        int totalWorkouts = workouts.size();
        Set<LocalDate> workoutDates = workouts.stream()
                .map(w -> w.getStartTime().toLocalDate())
                .collect(Collectors.toSet());

        String consistencyText = String.format(
                "🔥 Current Streak: 0 days\n" +
                        "📅 Total Workouts: %d\n" +
                        "📊 Workout Days: %d\n\n" +
                        "🎯 Keep the momentum going!",
                totalWorkouts, workoutDates.size()
        );

        return createProgressCard("🔥 Consistency", consistencyText, "#FF5722");
    }

    private VBox createRecentWorkoutsCard(List<Workout> workouts) {
        StringBuilder recentText = new StringBuilder("📋 Last 5 Workouts:\n\n");

        workouts.stream()
                .limit(5)
                .forEach(w -> {
                    recentText.append(String.format("💪 %s\n", w.getName()));
                    recentText.append(String.format("📅 %s\n", w.getStartTime().toLocalDate()));
                    recentText.append(String.format("⏱️ %d min | 📊 %.0f lbs\n\n",
                            w.getDurationMinutes(), w.getTotalVolume()));
                });

        return createProgressCard("📋 Recent Activity", recentText.toString(), "#9C27B0");
    }

    private VBox createProgressCard(String title, String content, String color) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + color + ", " +
                        adjustBrightness(color) + "); " +
                        "-fx-padding: 20px; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 5);"
        );
        card.setPrefWidth(350);
        card.setMinHeight(200);
        VBox.setMargin(card, new Insets(10));

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 22px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 2);"
        );

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-line-spacing: 5px; " +
                        "-fx-font-weight: 500; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 2, 0, 0, 1);"
        );

        card.getChildren().addAll(titleLabel, contentLabel);
        return card;
    }

    private String adjustBrightness(String color) {
        return switch (color) {
            case "#4CAF50" -> "#2E7D32";
            case "#2196F3" -> "#1565C0";
            case "#FF5722" -> "#D84315";
            case "#9C27B0" -> "#7B1FA2";
            default -> color;
        };
    }

    private String adjustBrightness(String color, double factor) {
        // Simple color adjustment - in a real app you'd use proper color manipulation
        return color.replace("4CAF50", "2E7D2E")
                .replace("2196F3", "1565C0")
                .replace("FF5722", "D84315")
                .replace("9C27B0", "7B1FA2");
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("💪 Muscle Map");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the alert dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #2b2b2b;");
        dialogPane.lookup(".content.label").setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        alert.showAndWait();
    }

    // Custom list cell for exercises with rich formatting
    private class ExerciseListCell extends ListCell<Exercise> {
        @Override
        protected void updateItem(Exercise exercise, boolean empty) {
            super.updateItem(exercise, empty);

            if (empty || exercise == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                VBox content = new VBox(6);
                content.setStyle("-fx-padding: 12px;");

                // EXERCISE NAME - PURE WHITE WITH BLACK SHADOW
                Label nameLabel = new Label(exercise.getEquipmentEmoji() + " " + exercise.getName());
                nameLabel.setStyle(
                        "-fx-font-weight: bold; " +
                                "-fx-text-fill: #FFFFFF; " +  // PURE WHITE
                                "-fx-font-size: 16px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 3, 0, 0, 2);"
                );

                // DETAILS - BRIGHT CYAN FOR VISIBILITY
                Label detailLabel = new Label(
                        "💪 " + exercise.getMuscleGroup() + " • " +
                                exercise.getDifficulty() + " " + exercise.getDifficultyEmoji()
                );
                detailLabel.setStyle(
                        "-fx-text-fill: #00f5ff; " +  // BRIGHT CYAN
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);"
                );

                content.getChildren().addAll(nameLabel, detailLabel);
                setGraphic(content);
                setText(null);

                // NORMAL STATE - DARK BACKGROUND
                setStyle(
                        "-fx-background-color: #1a1a1a; " +
                                "-fx-background-radius: 10px; " +
                                "-fx-padding: 4px; " +
                                "-fx-border-color: rgba(255,255,255,0.1); " +
                                "-fx-border-width: 1px; " +
                                "-fx-border-radius: 10px;"
                );

                // HOVER STATE - BRIGHT CYAN BACKGROUND WITH BLACK TEXT
                setOnMouseEntered(e -> {
                    setStyle(
                            "-fx-background-color: #00f5ff; " +  // BRIGHT CYAN
                                    "-fx-background-radius: 10px; " +
                                    "-fx-padding: 4px; " +
                                    "-fx-border-color: #FFFFFF; " +
                                    "-fx-border-width: 2px; " +
                                    "-fx-border-radius: 10px; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.8), 12, 0, 0, 6);"
                    );

                    // CHANGE TEXT COLORS ON HOVER
                    nameLabel.setStyle(
                            "-fx-font-weight: bold; " +
                                    "-fx-text-fill: #000000; " +  // BLACK TEXT
                                    "-fx-font-size: 17px; " +  // SLIGHTLY LARGER
                                    "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.8), 2, 0, 0, 1);"
                    );

                    detailLabel.setStyle(
                            "-fx-text-fill: #1a1a1a; " +  // DARK TEXT
                                    "-fx-font-size: 14px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.6), 1, 0, 0, 1);"
                    );
                });

                setOnMouseExited(e -> {
                    setStyle(
                            "-fx-background-color: #1a1a1a; " +
                                    "-fx-background-radius: 10px; " +
                                    "-fx-padding: 4px; " +
                                    "-fx-border-color: rgba(255,255,255,0.1); " +
                                    "-fx-border-width: 1px; " +
                                    "-fx-border-radius: 10px;"
                    );

                    // RESET TEXT COLORS
                    nameLabel.setStyle(
                            "-fx-font-weight: bold; " +
                                    "-fx-text-fill: #FFFFFF; " +
                                    "-fx-font-size: 16px; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,1), 3, 0, 0, 2);"
                    );

                    detailLabel.setStyle(
                            "-fx-text-fill: #00f5ff; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-font-weight: 600; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);"
                    );
                });
            }
        }
    }
}

