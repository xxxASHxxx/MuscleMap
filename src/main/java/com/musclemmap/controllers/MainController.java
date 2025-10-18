package com.musclemmap.controllers;

import com.musclemmap.utils.SessionManager;
import com.musclemmap.services.IntelligentWorkoutGenerator;
import com.musclemmap.services.ExerciseRecommendationEngine;
import com.musclemmap.services.WeaknessDetectionService;
import com.musclemmap.services.IntelligentDietGenerator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Path;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.effect.DropShadow;
import java.util.LinkedHashMap;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.concurrent.Task;
import java.net.URL;

import javafx.scene.layout.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import com.musclemmap.models.*;
import com.musclemmap.utils.DatabaseHelper;
import com.musclemmap.utils.UIStyler;

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

    private void handleAddSet() {
        if (currentWorkout == null) {
            showAlert("Please start a workout first!", Alert.AlertType.WARNING);
            return;
        }

        Exercise selectedExercise = exerciseComboBox.getValue();
        if (selectedExercise == null) {
            showAlert("Please select an exercise!", Alert.AlertType.WARNING);
            return;
        }

        try {
            int sets = Integer.parseInt(setsField.getText().trim());
            int reps = Integer.parseInt(repsField.getText().trim());
            double weight = Double.parseDouble(weightField.getText().trim());

            if (sets <= 0 || reps <= 0 || weight < 0) {
                showAlert("Please enter valid positive numbers!", Alert.AlertType.WARNING);
                return;
            }

            // Create workout set
            WorkoutSet workoutSet = new WorkoutSet(selectedExercise, reps, weight);
            workoutSet.setCompleted(true);

            // Save to database
            dbHelper.addWorkoutSet(workoutSet);

            // Add to current workout
            currentWorkout.getSets().add(workoutSet);

            // Update exercise log display
            addExerciseLogEntry(selectedExercise.getName(), sets, reps, weight);

            // Clear input fields
            setsField.clear();
            repsField.clear();
            weightField.clear();

            showAlert("Set added successfully! 💪", Alert.AlertType.INFORMATION);

        } catch (NumberFormatException e) {
            showAlert("Please enter valid numbers for sets, reps, and weight!", Alert.AlertType.ERROR);
        }
    }

    private void addExerciseLogEntry(String exerciseName, int sets, int reps, double weight) {
        if (exerciseLogBox == null) return;

        HBox logEntry = new HBox(15);
        logEntry.setStyle(
                "-fx-background-color: #353535; " +
                "-fx-padding: 12px; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: rgba(0,212,255,0.3); " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 10px;"
        );

        Label exerciseLabel = new Label(exerciseName);
        exerciseLabel.setStyle("-fx-text-fill: #00d4ff; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label setsLabel = new Label(sets + " sets");
        setsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        Label repsLabel = new Label(reps + " reps");
        repsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        Label weightLabel = new Label(weight + " kg");
        weightLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        logEntry.getChildren().addAll(exerciseLabel, setsLabel, repsLabel, weightLabel, spacer);
        exerciseLogBox.getChildren().add(logEntry);
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
    @FXML
    private Tab aiTab;
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
    @FXML private VBox progressStatsContainer;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    // ========== HOME TAB ENHANCED FIELDS ==========
    @FXML private Label motivationLabel;
    @FXML private HBox achievementsBox;
    @FXML private VBox statsCardsContainer;

    @FXML private VBox progressChartsBox;
    @FXML
    private ListView<Exercise> exerciseListView;
    @FXML
    private Label selectedMuscleLabel;
    // Exercise Image Display
    @FXML private ImageView exerciseImageView;
    @FXML private VBox imagePlaceholder;
    @FXML private StackPane exerciseImageContainer;

    @FXML
    private TextArea exerciseDescriptionArea;
    @FXML
    private VBox stepImagesPanel;

    // Workout Tab Components
    @FXML
    private ComboBox<String> routineSelector;
    @FXML
    private ComboBox<Exercise> exerciseComboBox;
    @FXML
    private TextField setsField, repsField, weightField;
    @FXML
    private Button addSetBtn;
    @FXML
    private Button startWorkoutBtn, endWorkoutBtn;
    // ========== PROGRESS TAB FIELDS ==========
    @FXML private Button refreshProgressButton;
    @FXML private LineChart<String, Number> volumeChart;
    @FXML private BarChart<String, Number> exerciseChart;
    @FXML private AreaChart<String, Number> progressChart;
    @FXML private VBox personalRecordsContainer;
    @FXML private TableView<ProgressController.PersonalRecordEntry> prTableView;
    @FXML private TableColumn<ProgressController.PersonalRecordEntry, String> exerciseColumn;
    @FXML private TableColumn<ProgressController.PersonalRecordEntry, String> weightColumn;
    @FXML private TableColumn<ProgressController.PersonalRecordEntry, String> dateColumn;
    @FXML private VBox bodyMetricsContainer;
    // Quick Action Cards
    @FXML private VBox quickSetCard;
    @FXML private VBox progressCard;
    @FXML private VBox muscleMapCard;
    @FXML private VBox aiCard;
    @FXML private VBox aiContentContainer;


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
    
    // AI Service instances
    private IntelligentWorkoutGenerator workoutGenerator;
    private ExerciseRecommendationEngine recommendationEngine;
    private WeaknessDetectionService weaknessService;
    private IntelligentDietGenerator dietGenerator;
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
                "-fx-background-color: #2b2b2b;" +
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
        verifyDatabaseExercises();
        
        // ✅ STYLE ALL FXML COMBOBOXES FOR VISIBILITY
        styleAllComboBoxes();
        
        // Initialize all tabs
        initializeHomeTab();
        initializeMuscleMapTab();
        initializeWorkoutTab();
        initializeProgressTab();
        // LOGOUT BUTTON STYLING
        if (logoutButton != null) {
            logoutButton.setStyle(
                    "-fx-background-color: #ff5555; " +
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
                        "-fx-background-color: #ff3333; " +
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
                        "-fx-background-color: #ff5555; " +
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

        if (progressController != null) {
            System.out.println("✅ ProgressController loaded from FXML");
        }

        // ✅ FIXED: Ensure app starts on Home tab
        if (mainTabPane != null && homeTab != null) {
            javafx.application.Platform.runLater(() -> {
                mainTabPane.getSelectionModel().select(homeTab);
                System.out.println("🏠 App started on Home tab");
            });
        }

        // AI tab will be initialized after user login

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
            // Initialize AI services
            this.workoutGenerator = new IntelligentWorkoutGenerator();
            this.recommendationEngine = new ExerciseRecommendationEngine();
            this.weaknessService = new WeaknessDetectionService();
            this.dietGenerator = new IntelligentDietGenerator();
            System.out.println("🤖 AI services initialized");
            
            // Update UI
            updateWelcomeLabel();
            updateUserStats();
            loadUserWorkouts();

            // Load progress data
            System.out.println("🔄 Loading initial progress data");
            loadProgressCharts();
            
            // Initialize AI tab content after user is set
            System.out.println("🤖 Initializing AI tab content");
            updateAITabContent("Plan");
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
        if (currentUser == null) {
            System.out.println("⚠️ No user logged in");
            return;
        }

        System.out.println("📊 Loading user workouts for: " + currentUser.getUsername());

        // Update welcome label
        if (welcomeLabel != null) {
            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();
            String greeting = hour < 12 ? "Good Morning" : hour < 18 ? "Good Afternoon" : "Good Evening";
            welcomeLabel.setText(greeting + ", " + currentUser.getUsername() + "! 💪");
        }

        // Load workouts
        List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId());
        System.out.println("✅ Loaded " + workouts.size() + " workouts");

        // Display recent workouts
        displayRecentWorkouts(workouts);

        // Update stats cards
        updateStatsCards(workouts);

        // Display achievements
        displayAchievements(workouts);
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


    private String getTimeAgo(LocalDateTime dateTime) {
        long days = java.time.Duration.between(dateTime, LocalDateTime.now()).toDays();
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    private void initializeHomeTab() {
        System.out.println("🏠 Initializing Enhanced Home Tab");

        // Dynamic greeting based on time of day
        if (currentUser != null) {
            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();
            String greeting = hour < 12 ? "Good Morning" : hour < 18 ? "Good Afternoon" : "Good Evening";
            welcomeLabel.setText(greeting + ", " + currentUser.getUsername() + "! 💪");
        } else {
            welcomeLabel.setText("Welcome to MuscleMap! 💪");
        }

        // Calculate and display actual streak
        if (currentUser != null) {
            List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId());
            int streak = calculateCurrentStreak(workouts);
            streakLabel.setText("Current Streak: " + streak + " days 🔥");

            // Show last workout date
            if (!workouts.isEmpty()) {
                Workout lastWorkout = workouts.stream()
                        .max(Comparator.comparing(Workout::getStartTime))
                        .orElse(null);
                if (lastWorkout != null) {
                    String lastDate = lastWorkout.getStartTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                    lastWorkoutLabel.setText("Last Workout: " + lastDate);
                } else {
                    lastWorkoutLabel.setText("No workouts yet - Start today! 🚀");
                }
            } else {
                lastWorkoutLabel.setText("No workouts yet - Start today! 🚀");
            }
        } else {
            streakLabel.setText("Current Streak: 0 days 🔥");
            lastWorkoutLabel.setText("Start your fitness journey today! 🚀");
        }

        // Load recent workouts
        loadRecentWorkouts();

        // Display motivation quote
        displayMotivationQuote();

        // ENHANCED START QUICK WORKOUT BUTTON
        if (startQuickWorkoutBtn != null) {
            startQuickWorkoutBtn.setStyle(
                    "-fx-background-color: #00d4ff; " +
                            "-fx-text-fill: #FFFFFF; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 15px 35px; " +
                            "-fx-background-radius: 15px; " +
                            "-fx-border-color: #FFFFFF; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 15px; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.6), 12, 0, 0, 6);"
            );

            // HOVER EFFECT
            startQuickWorkoutBtn.setOnMouseEntered(e -> {
                startQuickWorkoutBtn.setStyle(
                        "-fx-background-color: #00f5ff; " +
                                "-fx-text-fill: #000000; " +
                                "-fx-font-size: 19px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 15px 35px; " +
                                "-fx-background-radius: 15px; " +
                                "-fx-border-color: #FFFFFF; " +
                                "-fx-border-width: 3px; " +
                                "-fx-border-radius: 15px; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.9), 16, 0, 0, 8);"
                );
            });

            startQuickWorkoutBtn.setOnMouseExited(e -> {
                startQuickWorkoutBtn.setStyle(
                        "-fx-background-color: #00d4ff; " +
                                "-fx-text-fill: #FFFFFF; " +
                                "-fx-font-size: 18px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 15px 35px; " +
                                "-fx-background-radius: 15px; " +
                                "-fx-border-color: #FFFFFF; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 15px; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.6), 12, 0, 0, 6);"
                );
            });

            startQuickWorkoutBtn.setOnAction(e -> {
                // Switch to workout tab and start workout
                mainTabPane.getSelectionModel().select(workoutTab);
                handleStartWorkout();
            });
        }
        // Setup Quick Action Card Hover Effects


        System.out.println("✅ Home tab initialized successfully");
        setupQuickActionHoverEffects();
    }
    private void setupQuickActionHoverEffects() {
        // Quick Set Card
        if (quickSetCard != null) {
            quickSetCard.setOnMouseEntered(e -> {
                quickSetCard.setStyle(
                        "-fx-background-color: #ff6ec7;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(255,110,199,0.9), 30, 0, 0, 12);" +
                                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-width: 3px; -fx-border-radius: 25px;" +
                                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
                );
            });

            quickSetCard.setOnMouseExited(e -> {
                quickSetCard.setStyle(
                        "-fx-background-color: #f093fb;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(240,147,251,0.6), 20, 0, 0, 10);" +
                                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 2px; -fx-border-radius: 25px;"
                );
            });

            // Add click handler for Quick Set card
            quickSetCard.setOnMouseClicked(e -> {
                System.out.println("⚡ Quick Set card clicked - switching to Workout tab");
                if (mainTabPane != null && workoutTab != null) {
                    mainTabPane.getSelectionModel().select(workoutTab);
                }
            });
        }

        // Progress Card
        if (progressCard != null) {
            progressCard.setOnMouseEntered(e -> {
                progressCard.setStyle(
                        "-fx-background-color: #00d4ff;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.9), 30, 0, 0, 12);" +
                                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-width: 3px; -fx-border-radius: 25px;" +
                                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
                );
            });

            progressCard.setOnMouseExited(e -> {
                progressCard.setStyle(
                        "-fx-background-color: #4facfe;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(79,172,254,0.6), 20, 0, 0, 10);" +
                                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 2px; -fx-border-radius: 25px;"
                );
            });

            // Add click handler for Progress card
            progressCard.setOnMouseClicked(e -> {
                System.out.println("📈 Progress card clicked - switching to Progress tab");
                if (mainTabPane != null && progressTab != null) {
                    mainTabPane.getSelectionModel().select(progressTab);
                }
            });
        }

        // Muscle Map Card
        if (muscleMapCard != null) {
            muscleMapCard.setOnMouseEntered(e -> {
                muscleMapCard.setStyle(
                        "-fx-background-color: #ff8fb3;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(255,143,179,0.9), 30, 0, 0, 12);" +
                                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-width: 3px; -fx-border-radius: 25px;" +
                                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
                );
            });

            muscleMapCard.setOnMouseExited(e -> {
                muscleMapCard.setStyle(
                        "-fx-background-color: #fa709a;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(250,112,154,0.6), 20, 0, 0, 10);" +
                                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 2px; -fx-border-radius: 25px;"
                );
            });

            // Add click handler for Muscle Map card
            muscleMapCard.setOnMouseClicked(e -> {
                System.out.println("🗺️ Muscle Map card clicked - switching to Muscle Map tab");
                if (mainTabPane != null && muscleMapTab != null) {
                    mainTabPane.getSelectionModel().select(muscleMapTab);
                }
            });
        }

        // AI Card
        if (aiCard != null) {
            aiCard.setOnMouseEntered(e -> {
                aiCard.setStyle(
                        "-fx-background-color: #FFD700;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.9), 30, 0, 0, 12);" +
                                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-width: 3px; -fx-border-radius: 25px;" +
                                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
                );
            });

            aiCard.setOnMouseExited(e -> {
                aiCard.setStyle(
                        "-fx-background-color: #FFA500;" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(255,165,0,0.6), 20, 0, 0, 10);" +
                                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 2px; -fx-border-radius: 25px;"
                );
            });

            // Add click handler for AI card
            aiCard.setOnMouseClicked(e -> {
                System.out.println("🤖 AI card clicked - switching to AI tab");
                if (mainTabPane != null && aiTab != null) {
                    mainTabPane.getSelectionModel().select(aiTab);
                }
            });
        }
    }

    @FXML
    private void handleMuscleMapClick() {
        System.out.println("🗺️ Switching to Muscle Map tab");

        if (mainTabPane != null && muscleMapTab != null) {
            mainTabPane.getSelectionModel().select(muscleMapTab);
        }
    }


    private void initializeMuscleMapTab() {
        System.out.println("🗺️ Initializing Muscle Map Tab");

        // Setup header label with cyan styling
        if (selectedMuscleLabel != null) {
            selectedMuscleLabel.setText("🎯 Click on a muscle group to see exercises");
            selectedMuscleLabel.setStyle(
                    "-fx-font-size: 20px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #00d4ff; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.6), 4, 0, 0, 2);"
            );
        }

        // Setup exercise description area with UIStyler
        if (exerciseDescriptionArea != null) {
            exerciseDescriptionArea.setEditable(false);
            exerciseDescriptionArea.setWrapText(true);
            UIStyler.styleTextArea(exerciseDescriptionArea);
        }


        // Create the interactive muscle map with all muscle regions
        createMuscleMap();

        // Setup exercise list view with complete functionality
        if (exerciseListView != null) {
            // Set custom cell factory for beautifully styled exercise cells
            exerciseListView.setCellFactory(listView -> new ExerciseListCell());

            // Set preferred height for scrollable list
            exerciseListView.setPrefHeight(240);

            // Apply enhanced dark theme styling with cyan borders and glow effect
            exerciseListView.setStyle(
                    "-fx-background-color: #1f1f1f; " +              // Outer dark background
                            "-fx-control-inner-background: #2b2b2b; " +      // Inner dark background for cells
                            "-fx-border-color: #00d4ff; " +                  // Cyan border matching app theme
                            "-fx-border-radius: 10px; " +                    // Rounded corners
                            "-fx-background-radius: 10px; " +                // Match border radius
                            "-fx-border-width: 2px; " +                      // Medium border width
                            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.3), 10, 0, 0, 5);"  // Cyan glow effect
            );

            // ✅ FIXED: Add selection listener that calls handleExerciseSelection()
            exerciseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    System.out.println("📋 Exercise selected from list: " + newSelection.getName());
                    handleExerciseSelection(newSelection);  // ✅ Use the dedicated method
                }
            });

            System.out.println("✅ Exercise ListView configured with enhanced styling");
        }

        System.out.println("✅ Muscle Map tab fully initialized and ready");
    }


    private void showExerciseDetails(Exercise newSelection) {
    }

    /**
     * Style all FXML ComboBoxes for maximum visibility
     */
    private void styleAllComboBoxes() {
        // Style workout tab ComboBoxes
        if (routineSelector != null) {
            UIStyler.styleComboBox(routineSelector);
        }
        if (exerciseComboBox != null) {
            UIStyler.styleComboBox(exerciseComboBox);
        }
        if (exerciseFilterCombo != null) {
            UIStyler.styleComboBox(exerciseFilterCombo);
        }
        System.out.println("✅ All FXML ComboBoxes styled for visibility");
    }

    private void initializeWorkoutTab() {
        routineSelector.setItems(FXCollections.observableArrayList(
                "Push Day", "Pull Day", "Leg Day", "Upper Body", "Full Body", "Custom"
        ));

        // ✅ CRITICAL FIX - Initialize exercise combo box with exercises from database
        if (exerciseComboBox != null && exercises != null && !exercises.isEmpty()) {
            exerciseComboBox.setItems(exercises); // Use the loaded exercises

            // Custom cell factory for display with proper styling
            exerciseComboBox.setCellFactory(param -> new ListCell<Exercise>() {
                @Override
                protected void updateItem(Exercise item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle(null);
                    } else {
                        setText(item.getName());
                        // Apply styling to ensure white text
                        setStyle("-fx-text-fill: #FFFFFF !important; " +
                                "-fx-background-color: #1a1a1a; " +
                                "-fx-padding: 12px 16px; " +
                                "-fx-font-size: 15px; " +
                                "-fx-font-weight: 500;");
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
                    // Apply styling to button cell
                    setStyle("-fx-text-fill: #FFFFFF !important; " +
                            "-fx-background-color: #353535;");
                }
            });

            System.out.println("✅ Exercise ComboBox loaded with " + exercises.size() + " exercises");
        } else {
            System.err.println("❌ ERROR: exercises list is null or empty!");
        }
        createMuscleMap();

        // Exercise list selection handler
        if (exerciseListView != null) {
            exerciseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    handleExerciseSelection(newVal); // Use dedicated method to avoid duplicate calls
                }
            });
        }
        // Button styling
        startWorkoutBtn.setStyle(
                "-fx-background-color: #00d4ff;" +
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
                "-fx-background-color: #ff5555;" +
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

        // Add event handlers for workout tab buttons
        if (startWorkoutBtn != null) {
            startWorkoutBtn.setOnAction(e -> handleStartWorkout());
        }
        if (endWorkoutBtn != null) {
            endWorkoutBtn.setOnAction(e -> handleEndWorkout());
        }
        if (addSetBtn != null) {
            addSetBtn.setOnAction(e -> handleAddSet());
        }
    }

    @FXML
    private void handleRefreshProgress() {
        System.out.println("🔄 Manual refresh triggered");
        loadProgressCharts();
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

        // Setup exercise filter
        if (exerciseFilterCombo != null) {
            List<Exercise> exercises = dbHelper.getAllExercises();
            exerciseFilterCombo.getItems().add("All Exercises");
            exercises.forEach(ex -> exerciseFilterCombo.getItems().add(ex.getName()));
            exerciseFilterCombo.setValue("All Exercises");

            // Add listener
            exerciseFilterCombo.setOnAction(e -> loadProgressCharts());
        }

        // Setup date picker listeners
        if (fromDatePicker != null) {
            fromDatePicker.setOnAction(e -> loadProgressCharts());
        }
        if (toDatePicker != null) {
            toDatePicker.setOnAction(e -> loadProgressCharts());
        }

        // Setup table columns
        if (prTableView != null && exerciseColumn != null && weightColumn != null && dateColumn != null) {
            exerciseColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExercise()));
            weightColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getWeight()));
            dateColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));
        }

        System.out.println("✅ Progress tab initialized");
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
        System.out.println("🗺️ Creating anatomical muscle map...");

        if (muscleMapPane == null) {
            System.err.println("❌ muscleMapPane is null!");
            return;
        }

        muscleMapPane.getChildren().clear();

        // LAYER 1: Background body outline
        createRealisticBodyOutline(muscleMapPane);

        // LAYER 2: Back muscles (deepest)
        addMuscleRegion(muscleMapPane, "Back", 238, 218, 48, 92, "#FF5722", 1);
        addMuscleRegion(muscleMapPane, "Back", 364, 218, 48, 92, "#FF5722", 1);
        addMuscleRegion(muscleMapPane, "Lower Back", 282, 348, 86, 58, "#FF6F00", 1);

        // LAYER 3: Core muscles
        addMuscleRegion(muscleMapPane, "Abs", 297, 272, 56, 102, "#4CAF50", 2);
        addMuscleRegion(muscleMapPane, "Obliques", 262, 282, 28, 82, "#8BC34A", 2);
        addMuscleRegion(muscleMapPane, "Obliques", 360, 282, 28, 82, "#8BC34A", 2);

        // LAYER 4: Chest muscles
        addMuscleRegion(muscleMapPane, "Chest", 258, 182, 40, 70, "#E91E63", 3);
        addMuscleRegion(muscleMapPane, "Chest", 352, 182, 40, 70, "#E91E63", 3);

        // LAYER 5: Shoulder muscles
        addMuscleRegion(muscleMapPane, "Shoulders", 228, 145, 65, 60, "#9C27B0", 4);
        addMuscleRegion(muscleMapPane, "Shoulders", 357, 145, 65, 60, "#9C27B0", 4);

        // LAYER 6: Neck
        addMuscleRegion(muscleMapPane, "Neck", 307, 98, 36, 40, "#9C27B0", 5);

        // LAYER 7: Glutes
        addMuscleRegion(muscleMapPane, "Glutes", 282, 412, 86, 68, "#F44336", 3);

        // LAYER 8: Upper legs
        addMuscleRegion(muscleMapPane, "Quadriceps", 272, 492, 40, 122, "#FFC107", 4);
        addMuscleRegion(muscleMapPane, "Quadriceps", 338, 492, 40, 122, "#FFC107", 4);
        addMuscleRegion(muscleMapPane, "Hamstrings", 272, 502, 40, 112, "#FF9800", 3);
        addMuscleRegion(muscleMapPane, "Hamstrings", 338, 502, 40, 112, "#FF9800", 3);

        // LAYER 9: Lower legs
        addMuscleRegion(muscleMapPane, "Calves", 277, 632, 31, 82, "#795548", 5);
        addMuscleRegion(muscleMapPane, "Calves", 342, 632, 31, 82, "#795548", 5);

        // LAYER 10: Triceps (WIDER SPACING - moved out)
        addMuscleRegion(muscleMapPane, "Triceps", 185, 212, 30, 73, "#00BCD4", 6);
        addMuscleRegion(muscleMapPane, "Triceps", 435, 212, 30, 73, "#00BCD4", 6);

        // LAYER 11: Biceps (SIDE BY SIDE with triceps, not overlapping)
        addMuscleRegion(muscleMapPane, "Biceps", 218, 208, 30, 76, "#2196F3", 7);
        addMuscleRegion(muscleMapPane, "Biceps", 402, 208, 30, 76, "#2196F3", 7);

        // LAYER 12: Forearms (top layer for arms)
        addMuscleRegion(muscleMapPane, "Forearms", 188, 290, 24, 66, "#03A9F4", 8);
        addMuscleRegion(muscleMapPane, "Forearms", 438, 290, 24, 66, "#03A9F4", 8);

        System.out.println("✅ Muscle map created with original spacing");
    }

    // ==================== PREMIUM MUSCLE REGION CREATOR ====================
    
    private void addMuscleRegion(Pane pane, String muscleName, double x, double y, double width, double height, String color, int layer) {
        // Create muscle-specific styling
        MuscleStyle style = getMuscleStyle(muscleName, color);
        
        // Create the main muscle shape with enhanced styling
        Rectangle muscleShape = new Rectangle(x, y, width, height);
        muscleShape.setFill(style.fillColor);
        muscleShape.setStroke(style.strokeColor);
        muscleShape.setStrokeWidth(style.strokeWidth);
        muscleShape.setArcWidth(style.cornerRadius);
        muscleShape.setArcHeight(style.cornerRadius);
        muscleShape.setEffect(style.shadowEffect);
        muscleShape.setCursor(javafx.scene.Cursor.HAND);

        // Set Z-order for proper layering
        muscleShape.setViewOrder(-layer);

        // Enhanced hover effects with muscle-specific animations
        muscleShape.setOnMouseEntered(e -> {
            muscleShape.setFill(style.hoverFillColor);
            muscleShape.setScaleX(style.hoverScale);
            muscleShape.setScaleY(style.hoverScale);
            muscleShape.setEffect(style.hoverEffect);
            
            // Add pulsing animation for certain muscle groups
            if (style.hasPulseEffect) {
                addPulseAnimation(muscleShape);
            }
        });

        muscleShape.setOnMouseExited(e -> {
            muscleShape.setFill(style.fillColor);
            muscleShape.setScaleX(1.0);
            muscleShape.setScaleY(1.0);
            muscleShape.setEffect(style.shadowEffect);
        });

        muscleShape.setOnMouseClicked(e -> handleMuscleClick(muscleName));

        pane.getChildren().add(muscleShape);

        // Add premium label with muscle-specific styling
        Label label = new Label(getMuscleEmoji(muscleName));
        label.setLayoutX(x + width / 2 - 15);
        label.setLayoutY(y + height / 2 - 15);
        label.setStyle(style.labelStyle);
        label.setMouseTransparent(true);
        label.setViewOrder(-layer - 1);
        pane.getChildren().add(label);
        
        // No muscle name labels below shapes; keep the map clean and original
    }
    
    // ==================== MUSCLE STYLING SYSTEM ====================
    
    private static class MuscleStyle {
        Color fillColor;
        Color strokeColor;
        Color hoverFillColor;
        double strokeWidth;
        double cornerRadius;
        double hoverScale;
        javafx.scene.effect.Effect shadowEffect;
        javafx.scene.effect.Effect hoverEffect;
        String labelStyle;
        String nameLabelStyle;
        boolean hasPulseEffect;
        
        MuscleStyle(Color fillColor, Color strokeColor, Color hoverFillColor, double strokeWidth, 
                   double cornerRadius, double hoverScale, javafx.scene.effect.Effect shadowEffect,
                   javafx.scene.effect.Effect hoverEffect, String labelStyle, String nameLabelStyle, boolean hasPulseEffect) {
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            this.hoverFillColor = hoverFillColor;
            this.strokeWidth = strokeWidth;
            this.cornerRadius = cornerRadius;
            this.hoverScale = hoverScale;
            this.shadowEffect = shadowEffect;
            this.hoverEffect = hoverEffect;
            this.labelStyle = labelStyle;
            this.nameLabelStyle = nameLabelStyle;
            this.hasPulseEffect = hasPulseEffect;
        }
    }
    
    private MuscleStyle getMuscleStyle(String muscleName, String baseColor) {
        Color base = Color.web(baseColor);
        
        switch (muscleName.toLowerCase()) {
            case "chest":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.85),
                    base.brighter().brighter(),
                    base.brighter().brighter().brighter(),
                    3.0, 25, 1.12,
                    new javafx.scene.effect.DropShadow(12, Color.rgb(233, 30, 99, 0.6)),
                    new javafx.scene.effect.DropShadow(20, Color.rgb(233, 30, 99, 0.8)),
                    "-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(233,30,99,0.8), 6, 0, 0, 3);",
                    "-fx-font-size: 12px; -fx-text-fill: #E91E63; -fx-font-weight: bold;",
                    true
                );
                
            case "back":
            case "lower back":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.80),
                    base.brighter(),
                    base.brighter().brighter(),
                    2.5, 20, 1.10,
                    new javafx.scene.effect.DropShadow(10, Color.rgb(255, 87, 34, 0.5)),
                    new javafx.scene.effect.DropShadow(18, Color.rgb(255, 87, 34, 0.7)),
                    "-fx-font-size: 22px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(255,87,34,0.7), 5, 0, 0, 2);",
                    "-fx-font-size: 11px; -fx-text-fill: #FF5722; -fx-font-weight: bold;",
                    false
                );
                
            case "shoulders":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.88),
                    base.brighter().brighter(),
                    base.brighter().brighter().brighter(),
                    3.5, 30, 1.15,
                    new javafx.scene.effect.DropShadow(15, Color.rgb(156, 39, 176, 0.6)),
                    new javafx.scene.effect.DropShadow(25, Color.rgb(156, 39, 176, 0.8)),
                    "-fx-font-size: 26px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(156,39,176,0.8), 7, 0, 0, 3);",
                    "-fx-font-size: 12px; -fx-text-fill: #9C27B0; -fx-font-weight: bold;",
                    true
                );
                
            case "biceps":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.90),
                    base.brighter().brighter(),
                    base.brighter().brighter().brighter(),
                    3.0, 22, 1.13,
                    new javafx.scene.effect.DropShadow(13, Color.rgb(33, 150, 243, 0.6)),
                    new javafx.scene.effect.DropShadow(22, Color.rgb(33, 150, 243, 0.8)),
                    "-fx-font-size: 25px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.8), 6, 0, 0, 3);",
                    "-fx-font-size: 11px; -fx-text-fill: #2196F3; -fx-font-weight: bold;",
                    true
                );
                
            case "triceps":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.88),
                    base.brighter().brighter(),
                    base.brighter().brighter().brighter(),
                    3.0, 20, 1.11,
                    new javafx.scene.effect.DropShadow(12, Color.rgb(0, 188, 212, 0.6)),
                    new javafx.scene.effect.DropShadow(20, Color.rgb(0, 188, 212, 0.8)),
                    "-fx-font-size: 23px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,188,212,0.8), 6, 0, 0, 3);",
                    "-fx-font-size: 11px; -fx-text-fill: #00BCD4; -fx-font-weight: bold;",
                    false
                );
                
            case "abs":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.87),
                    base.brighter().brighter(),
                    base.brighter().brighter().brighter(),
                    3.5, 25, 1.14,
                    new javafx.scene.effect.DropShadow(14, Color.rgb(76, 175, 80, 0.6)),
                    new javafx.scene.effect.DropShadow(24, Color.rgb(76, 175, 80, 0.8)),
                    "-fx-font-size: 27px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.8), 7, 0, 0, 3);",
                    "-fx-font-size: 12px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;",
                    true
                );
                
            case "obliques":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.85),
                    base.brighter(),
                    base.brighter().brighter(),
                    2.5, 18, 1.09,
                    new javafx.scene.effect.DropShadow(11, Color.rgb(139, 195, 74, 0.5)),
                    new javafx.scene.effect.DropShadow(19, Color.rgb(139, 195, 74, 0.7)),
                    "-fx-font-size: 21px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(139,195,74,0.7), 5, 0, 0, 2);",
                    "-fx-font-size: 10px; -fx-text-fill: #8BC34A; -fx-font-weight: bold;",
                    false
                );
                
            case "quadriceps":
            case "hamstrings":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.82),
                    base.brighter(),
                    base.brighter().brighter(),
                    2.8, 20, 1.10,
                    new javafx.scene.effect.DropShadow(12, Color.rgb(255, 193, 7, 0.5)),
                    new javafx.scene.effect.DropShadow(20, Color.rgb(255, 193, 7, 0.7)),
                    "-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(255,193,7,0.7), 6, 0, 0, 3);",
                    "-fx-font-size: 11px; -fx-text-fill: #FFC107; -fx-font-weight: bold;",
                    false
                );
                
            case "glutes":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.86),
                    base.brighter().brighter(),
                    base.brighter().brighter().brighter(),
                    3.2, 28, 1.13,
                    new javafx.scene.effect.DropShadow(13, Color.rgb(244, 67, 54, 0.6)),
                    new javafx.scene.effect.DropShadow(22, Color.rgb(244, 67, 54, 0.8)),
                    "-fx-font-size: 25px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(244,67,54,0.8), 6, 0, 0, 3);",
                    "-fx-font-size: 12px; -fx-text-fill: #F44336; -fx-font-weight: bold;",
                    true
                );
                
            case "calves":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.80),
                    base.brighter(),
                    base.brighter().brighter(),
                    2.5, 15, 1.08,
                    new javafx.scene.effect.DropShadow(10, Color.rgb(121, 85, 72, 0.5)),
                    new javafx.scene.effect.DropShadow(18, Color.rgb(121, 85, 72, 0.7)),
                    "-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(121,85,72,0.7), 5, 0, 0, 2);",
                    "-fx-font-size: 10px; -fx-text-fill: #795548; -fx-font-weight: bold;",
                    false
                );
                
            case "forearms":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.83),
                    base.brighter(),
                    base.brighter().brighter(),
                    2.8, 16, 1.09,
                    new javafx.scene.effect.DropShadow(11, Color.rgb(3, 169, 244, 0.5)),
                    new javafx.scene.effect.DropShadow(19, Color.rgb(3, 169, 244, 0.7)),
                    "-fx-font-size: 22px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(3,169,244,0.7), 5, 0, 0, 2);",
                    "-fx-font-size: 10px; -fx-text-fill: #03A9F4; -fx-font-weight: bold;",
                    false
                );
                
            case "neck":
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.88),
                    base.brighter().brighter(),
                    base.brighter().brighter().brighter(),
                    3.0, 25, 1.12,
                    new javafx.scene.effect.DropShadow(12, Color.rgb(156, 39, 176, 0.6)),
                    new javafx.scene.effect.DropShadow(20, Color.rgb(156, 39, 176, 0.8)),
                    "-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(156,39,176,0.8), 6, 0, 0, 3);",
                    "-fx-font-size: 11px; -fx-text-fill: #9C27B0; -fx-font-weight: bold;",
                    false
                );
                
            default:
                return new MuscleStyle(
                    base.deriveColor(0, 1, 1, 0.80),
                    base.brighter(),
                    base.brighter().brighter(),
                    2.5, 20, 1.10,
                    new javafx.scene.effect.DropShadow(10, Color.BLACK),
                    new javafx.scene.effect.DropShadow(18, Color.web(baseColor)),
                    "-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, black, 4, 0, 0, 2);",
                    "-fx-font-size: 10px; -fx-text-fill: " + baseColor + "; -fx-font-weight: bold;",
                    false
                );
        }
    }
    
    // ==================== ANIMATION HELPERS ====================
    
    private void addPulseAnimation(Rectangle muscleShape) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), muscleShape);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
    }

    // Create realistic body outline with better visibility
    private void createRealisticBodyOutline(Pane pane) {
        // Head circle - MORE VISIBLE
        Circle head = new Circle(325, 75, 40);
        head.setFill(Color.TRANSPARENT);
        head.setStroke(Color.web("#808080"));
        head.setStrokeWidth(3);
        pane.getChildren().add(head);

        // Neck trapezoid
        Polygon neck = new Polygon();
        neck.getPoints().addAll(
                310.0, 115.0,
                340.0, 115.0,
                348.0, 140.0,
                302.0, 140.0
        );
        neck.setFill(Color.TRANSPARENT);
        neck.setStroke(Color.web("#808080"));
        neck.setStrokeWidth(3);
        pane.getChildren().add(neck);

        // Torso outline - CURVED and VISIBLE
        Path torsoPath = new Path();
        MoveTo moveTo = new MoveTo(228, 158);

        CubicCurveTo leftSide = new CubicCurveTo();
        leftSide.setControlX1(222);
        leftSide.setControlY1(275);
        leftSide.setControlX2(232);
        leftSide.setControlY2(395);
        leftSide.setX(278);
        leftSide.setY(413);

        LineTo bottomLine = new LineTo(372, 413);

        CubicCurveTo rightSide = new CubicCurveTo();
        rightSide.setControlX1(418);
        rightSide.setControlY1(395);
        rightSide.setControlX2(428);
        rightSide.setControlY2(275);
        rightSide.setX(422);
        rightSide.setY(158);

        LineTo topLine = new LineTo(228, 158);

        torsoPath.getElements().addAll(moveTo, leftSide, bottomLine, rightSide, topLine);
        torsoPath.setFill(Color.TRANSPARENT);
        torsoPath.setStroke(Color.web("#808080"));
        torsoPath.setStrokeWidth(3);
        pane.getChildren().add(torsoPath);

        // Arms - VISIBLE CURVES
        QuadCurve leftArm = new QuadCurve();
        leftArm.setStartX(223);
        leftArm.setStartY(158);
        leftArm.setEndX(193);
        leftArm.setEndY(365);
        leftArm.setControlX(202);
        leftArm.setControlY(255);
        leftArm.setFill(Color.TRANSPARENT);
        leftArm.setStroke(Color.web("#808080"));
        leftArm.setStrokeWidth(3);
        pane.getChildren().add(leftArm);

        QuadCurve rightArm = new QuadCurve();
        rightArm.setStartX(427);
        rightArm.setStartY(158);
        rightArm.setEndX(457);
        rightArm.setEndY(365);
        rightArm.setControlX(448);
        rightArm.setControlY(255);
        rightArm.setFill(Color.TRANSPARENT);
        rightArm.setStroke(Color.web("#808080"));
        rightArm.setStrokeWidth(3);
        pane.getChildren().add(rightArm);

        // Legs - TAPERED SHAPE
        QuadCurve leftLeg = new QuadCurve();
        leftLeg.setStartX(283);
        leftLeg.setStartY(413);
        leftLeg.setEndX(288);
        leftLeg.setEndY(720);
        leftLeg.setControlX(278);
        leftLeg.setControlY(560);
        leftLeg.setFill(Color.TRANSPARENT);
        leftLeg.setStroke(Color.web("#808080"));
        leftLeg.setStrokeWidth(3);
        pane.getChildren().add(leftLeg);

        QuadCurve rightLeg = new QuadCurve();
        rightLeg.setStartX(367);
        rightLeg.setStartY(413);
        rightLeg.setEndX(362);
        rightLeg.setEndY(720);
        rightLeg.setControlX(372);
        rightLeg.setControlY(560);
        rightLeg.setFill(Color.TRANSPARENT);
        rightLeg.setStroke(Color.web("#808080"));
        rightLeg.setStrokeWidth(3);
        pane.getChildren().add(rightLeg);
    }
    // Create curved muscle shape for organic look
    private Rectangle createCurvedMuscleShape(MuscleMapRegion region) {
        Rectangle rect = new Rectangle(region.x, region.y, region.width, region.height);
        rect.setFill(Color.web(region.color).deriveColor(0, 1, 1, 0.75));
        rect.setStroke(Color.web(region.color).brighter());
        rect.setStrokeWidth(2.5);
        rect.setArcWidth(25);  // More curved corners
        rect.setArcHeight(25);
        rect.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        rect.setCursor(javafx.scene.Cursor.HAND);
        return rect;
    }


    // Helper method to create body outline
    private void createBodyOutline(Pane pane, double width, double height) {
        // Create simple body silhouette using shapes

        // Head circle
        Circle head = new Circle(325, 60, 35);
        head.setFill(Color.TRANSPARENT);
        head.setStroke(Color.web("#404040"));
        head.setStrokeWidth(2);
        pane.getChildren().add(head);

        // Neck line
        Line neck = new Line(325, 95, 325, 120);
        neck.setStroke(Color.web("#404040"));
        neck.setStrokeWidth(3);
        pane.getChildren().add(neck);

        // Torso outline (body shape)
        Polygon torso = new Polygon();
        torso.getPoints().addAll(
                250.0, 140.0,  // Left shoulder
                400.0, 140.0,  // Right shoulder
                420.0, 400.0,  // Right hip
                230.0, 400.0   // Left hip
        );
        torso.setFill(Color.TRANSPARENT);
        torso.setStroke(Color.web("#404040"));
        torso.setStrokeWidth(2);
        pane.getChildren().add(torso);

        // Arms outlines
        Line leftArm1 = new Line(240, 150, 210, 280);
        Line leftArm2 = new Line(210, 280, 200, 350);
        Line rightArm1 = new Line(410, 150, 440, 280);
        Line rightArm2 = new Line(440, 280, 450, 350);

        leftArm1.setStroke(Color.web("#404040"));
        leftArm2.setStroke(Color.web("#404040"));
        rightArm1.setStroke(Color.web("#404040"));
        rightArm2.setStroke(Color.web("#404040"));

        leftArm1.setStrokeWidth(2);
        leftArm2.setStrokeWidth(2);
        rightArm1.setStrokeWidth(2);
        rightArm2.setStrokeWidth(2);

        pane.getChildren().addAll(leftArm1, leftArm2, rightArm1, rightArm2);

        // Legs outlines
        Line leftLeg1 = new Line(280, 400, 280, 620);
        Line leftLeg2 = new Line(280, 620, 280, 720);
        Line rightLeg1 = new Line(370, 400, 370, 620);
        Line rightLeg2 = new Line(370, 620, 370, 720);

        leftLeg1.setStroke(Color.web("#404040"));
        leftLeg2.setStroke(Color.web("#404040"));
        rightLeg1.setStroke(Color.web("#404040"));
        rightLeg2.setStroke(Color.web("#404040"));

        leftLeg1.setStrokeWidth(3);
        leftLeg2.setStrokeWidth(2);
        rightLeg1.setStrokeWidth(3);
        rightLeg2.setStrokeWidth(2);

        pane.getChildren().addAll(leftLeg1, leftLeg2, rightLeg1, rightLeg2);
    }
    // Create muscle shape (rounded rectangle for organic look)
    private Rectangle createMuscleShape(MuscleMapRegion region) {
        Rectangle rect = new Rectangle(region.x, region.y, region.width, region.height);
        rect.setFill(Color.web(region.color).deriveColor(0, 1, 1, 0.7));
        rect.setStroke(Color.web(region.color));
        rect.setStrokeWidth(2);
        rect.setArcWidth(15);  // Rounded corners for organic look
        rect.setArcHeight(15);
        rect.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        rect.setCursor(javafx.scene.Cursor.HAND);
        return rect;
    }

    // Get emoji for muscle group
    private String getMuscleEmoji(String muscleName) {
        return switch (muscleName.toUpperCase()) {
            case "CHEST" -> "💪";
            case "BACK" -> "🔱";
            case "SHOULDERS" -> "🏋️";
            case "BICEPS" -> "💪";
            case "TRICEPS" -> "💪";
            case "FOREARMS" -> "✊";
            case "ABS" -> "📊";
            case "OBLIQUES" -> "⚡";
            case "LOWER BACK" -> "🔱";
            case "GLUTES" -> "🍑";
            case "QUADRICEPS" -> "🦵";
            case "HAMSTRINGS" -> "🦵";
            case "CALVES" -> "👟";
            case "NECK" -> "⚡";
            default -> "💪";
        };
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


    // Load exercises from database
    private void loadExerciseDatabase() {
        // Load exercises from database instead of hardcoded list
        List<Exercise> dbExercises = dbHelper.getAllExercises();
        if (dbExercises != null && !dbExercises.isEmpty()) {
            exercises.addAll(dbExercises);
            System.out.println("✅ Loaded " + exercises.size() + " exercises from database");
        } else {
            // Fallback to hardcoded exercises if database is empty
            System.out.println("⚠️ Database empty, using fallback exercises");
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
        box.setStyle("-fx-background-color: #3a3a3a; " +
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
        System.out.println("📊 Loading progress charts...");

        if (currentUser == null) {
            showNoProgressData();
            return;
        }

        // Clear existing data
        if (statsCardsContainer != null) statsCardsContainer.getChildren().clear();
        if (volumeChart != null) volumeChart.getData().clear();
        if (exerciseChart != null) exerciseChart.getData().clear();
        if (progressChart != null) progressChart.getData().clear();
        if (personalRecordsContainer != null) personalRecordsContainer.getChildren().clear();
        if (bodyMetricsContainer != null) bodyMetricsContainer.getChildren().clear();

        // Get date range
        LocalDateTime startDate = fromDatePicker.getValue().atStartOfDay();
        LocalDateTime endDate = toDatePicker.getValue().atTime(23, 59, 59);

        // Load workouts
        List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId())
                .stream()
                .filter(w -> w.getStartTime().isAfter(startDate) && w.getStartTime().isBefore(endDate))
                .collect(Collectors.toList());

        System.out.println("📊 Loaded " + workouts.size() + " workouts for progress analysis");

        if (workouts.isEmpty()) {
            showNoProgressData();
            return;
        }

        // Display all sections
        displayOverviewStats(workouts);
        displayVolumeChart(workouts);
        displayExerciseChart(workouts);
        displayProgressTimeline(workouts);
        displayPersonalRecords();
        displayStrengthProgress(workouts);
        displayConsistencyStats(workouts);

        System.out.println("✅ Progress charts loaded successfully!");
    }
    private VBox createProgressCard(String title, String content, String color) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: " + color + "; " +
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
                setStyle("-fx-background-color: transparent;");
            } else {
                // Create custom cell layout
                HBox cellContent = new HBox(12);
                cellContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                cellContent.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));

                // Exercise emoji/icon
                Label icon = new Label(getExerciseIcon(exercise.getEquipment()));
                icon.setStyle("-fx-font-size: 20px;");

                // Exercise name and difficulty
                VBox textContent = new VBox(3);

                Label nameLabel = new Label(exercise.getName());
                nameLabel.setStyle(
                        "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-text-fill: white;"
                );

                Label detailsLabel = new Label(
                        exercise.getDifficulty() + " • " + exercise.getEquipment()
                );
                detailsLabel.setStyle(
                        "-fx-font-size: 11px; " +
                                "-fx-text-fill: #00d4ff;"
                );

                textContent.getChildren().addAll(nameLabel, detailsLabel);

                // Difficulty badge
                Label difficultyBadge = new Label(getDifficultyEmoji(exercise.getDifficulty()));
                difficultyBadge.setStyle("-fx-font-size: 16px;");

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                cellContent.getChildren().addAll(icon, textContent, spacer, difficultyBadge);

                setGraphic(cellContent);
                setText(null);

                // Styling for normal and selected states
                setStyle(
                        "-fx-background-color: #2b2b2b; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-padding: 4px;"
                );

                // Hover effect
                setOnMouseEntered(e -> {
                    if (!isSelected()) {
                        setStyle(
                                "-fx-background-color: #3a3a3a; " +
                                        "-fx-background-radius: 8px; " +
                                        "-fx-padding: 4px; " +
                                        "-fx-cursor: hand;"
                        );
                    }
                });

                setOnMouseExited(e -> {
                    if (!isSelected()) {
                        setStyle(
                                "-fx-background-color: #2b2b2b; " +
                                        "-fx-background-radius: 8px; " +
                                        "-fx-padding: 4px;"
                        );
                    }
                });

                // Selected state
                if (isSelected()) {
                    setStyle(
                            "-fx-background-color: #00d4ff; " +
                                    "-fx-background-radius: 8px; " +
                                    "-fx-padding: 4px;"
                    );
                    nameLabel.setStyle(
                            "-fx-font-size: 14px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-text-fill: white;"
                    );
                    detailsLabel.setStyle(
                            "-fx-font-size: 11px; " +
                                    "-fx-text-fill: rgba(255,255,255,0.9);"
                    );
                }
            }
        }

        private String getExerciseIcon(String equipment) {
            return switch (equipment.toLowerCase()) {
                case "barbell" -> "🏋️";
                case "dumbbell" -> "💪";
                case "cable" -> "🔗";
                case "machine" -> "⚙️";
                case "bodyweight" -> "🤸";
                default -> "🏋️";
            };
        }

        private String getDifficultyEmoji(String difficulty) {
            return switch (difficulty.toLowerCase()) {
                case "beginner" -> "🟢";
                case "intermediate" -> "🟡";
                case "advanced" -> "🔴";
                default -> "⚪";
            };
        }
    }
    // ==================== PROGRESS CHART HELPER METHODS ====================

    private void displayOverviewStats(List<Workout> workouts) {
        if (statsCardsContainer == null) return;

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);

        // Total Workouts
        VBox totalCard = createStatCard("💪", String.valueOf(workouts.size()), "Total Workouts", "#4CAF50");

        // Total Volume
        double totalVolume = workouts.stream().mapToDouble(Workout::getTotalVolume).sum();
        VBox volumeCard = createStatCard("🏋️", String.format("%,.0f lbs", totalVolume), "Total Volume", "#2196F3");

        // Total Sets
        int totalSets = workouts.stream().mapToInt(Workout::getTotalSets).sum();
        VBox setsCard = createStatCard("📊", String.valueOf(totalSets), "Total Sets", "#FF9800");

        // Average Duration
        long avgDuration = (long) workouts.stream()
                .mapToLong(Workout::getDurationMinutes)
                .average()
                .orElse(0);
        VBox durationCard = createStatCard("⏱️", avgDuration + " min", "Avg Duration", "#9C27B0");

        statsRow.getChildren().addAll(totalCard, volumeCard, setsCard, durationCard);
        statsCardsContainer.getChildren().add(statsRow);
    }


    private void displayVolumeChart(List<Workout> workouts) {
        if (volumeChart == null) return;

        volumeChart.setTitle("📈 Volume Progress Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Total Volume");

        // Group by week
        Map<String, Double> weeklyData = new LinkedHashMap<>();
        for (Workout w : workouts) {
            String week = "W" + w.getStartTime().toLocalDate().get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
            weeklyData.merge(week, w.getTotalVolume(), Double::sum);
        }

        weeklyData.forEach((week, volume) -> series.getData().add(new XYChart.Data<>(week, volume)));
        volumeChart.getData().add(series);
    }

    private void displayExerciseChart(List<Workout> workouts) {
        if (exerciseChart == null) return;

        exerciseChart.setTitle("🏆 Top Exercises by Volume");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        Map<String, Double> exerciseVolumes = new HashMap<>();
        for (Workout w : workouts) {
            for (WorkoutSet set : w.getSets()) {
                exerciseVolumes.merge(set.getExercise().getName(), set.getVolume(), Double::sum);
            }
        }

        exerciseVolumes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

        exerciseChart.getData().add(series);
    }

    private void displayProgressTimeline(List<Workout> workouts) {
        if (progressChart == null) return;

        progressChart.setTitle("📅 Workout Frequency Timeline");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Workouts per Week");

        Map<String, Long> weeklyCount = workouts.stream()
                .collect(Collectors.groupingBy(
                        w -> "W" + w.getStartTime().toLocalDate().get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()),
                        Collectors.counting()
                ));

        weeklyCount.forEach((week, count) -> series.getData().add(new XYChart.Data<>(week, count)));
        progressChart.getData().add(series);
    }

    private void displayPersonalRecords() {
        if (prTableView == null) return;

        Map<String, Double> prs = dbHelper.getPersonalRecords(currentUser.getId());

        javafx.collections.ObservableList<ProgressController.PersonalRecordEntry> entries =
                javafx.collections.FXCollections.observableArrayList();

        prs.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> entries.add(new ProgressController.PersonalRecordEntry(
                        entry.getKey(),
                        String.format("%.1f lbs", entry.getValue()),
                        "Recent"
                )));

        prTableView.setItems(entries);
    }

    private void displayStrengthProgress(List<Workout> workouts) {
        if (personalRecordsContainer == null) return;

        Map<String, Double> maxWeights = calculateMaxWeights(workouts);

        if (maxWeights.isEmpty()) {
            Label noData = new Label("🏋️ No strength data yet!\n\nStart logging workouts to track your progress!");
            noData.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 20px;");
            noData.setWrapText(true);
            personalRecordsContainer.getChildren().add(noData);
            return;
        }

        for (Map.Entry<String, Double> entry : maxWeights.entrySet()) {
            HBox prCard = createPRCard(entry.getKey(), entry.getValue());
            personalRecordsContainer.getChildren().add(prCard);
        }
    }

    private HBox createPRCard(String exercise, double weight) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle(
                "-fx-background-color: #2b2b2b;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: #4CAF50;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;"
        );

        Label exerciseLabel = new Label(exercise);
        exerciseLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        exerciseLabel.setMinWidth(150);

        Label weightLabel = new Label(String.format("%.1f lbs", weight));
        weightLabel.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("PR! 🎉");
        badge.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px; -fx-font-weight: bold;");

        card.getChildren().addAll(exerciseLabel, weightLabel, spacer, badge);
        return card;
    }

    private void displayConsistencyStats(List<Workout> workouts) {
        if (bodyMetricsContainer == null) return;

        int currentStreak = calculateCurrentStreak(workouts);
        int longestStreak = calculateLongestStreak(workouts);
        int monthWorkouts = (int) workouts.stream()
                .filter(w -> w.getStartTime().isAfter(LocalDateTime.now().minusMonths(1)))
                .count();

        VBox streakCard = createConsistencyCard("🔥 Current Streak", currentStreak + " days", "#FF5722");
        VBox longestCard = createConsistencyCard("🏆 Longest Streak", longestStreak + " days", "#FFD700");
        VBox monthCard = createConsistencyCard("📅 This Month", monthWorkouts + " workouts", "#4CAF50");

        HBox row = new HBox(15, streakCard, longestCard, monthCard);
        row.setAlignment(Pos.CENTER);
        bodyMetricsContainer.getChildren().add(row);
    }

    private VBox createConsistencyCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 15px;" +
                        "-fx-min-width: 180px;" +
                        "-fx-min-height: 120px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 5);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private Map<String, Double> calculateMaxWeights(List<Workout> workouts) {
        Map<String, Double> maxWeights = new HashMap<>();

        for (Workout w : workouts) {
            for (WorkoutSet set : w.getSets()) {
                maxWeights.merge(set.getExercise().getName(), set.getWeight(), Double::max);
            }
        }

        return maxWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private int calculateCurrentStreak(List<Workout> workouts) {
        if (workouts.isEmpty()) return 0;

        List<LocalDate> dates = workouts.stream()
                .map(w -> w.getStartTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int streak = 0;
        LocalDate checkDate = LocalDate.now();

        for (LocalDate date : dates) {
            if (date.equals(checkDate) || date.equals(checkDate.minusDays(1))) {
                streak++;
                checkDate = date.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }

    private int calculateLongestStreak(List<Workout> workouts) {
        if (workouts.isEmpty()) return 0;

        List<LocalDate> dates = workouts.stream()
                .map(w -> w.getStartTime().toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        int maxStreak = 1, currentStreak = 1;

        for (int i = 1; i < dates.size(); i++) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));

            if (daysBetween <= 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    private void showNoProgressData() {
        if (statsCardsContainer != null) {
            statsCardsContainer.getChildren().clear();

            VBox noDataBox = new VBox(20);
            noDataBox.setAlignment(Pos.CENTER);
            noDataBox.setPadding(new Insets(50));

            Label icon = new Label("📊");
            icon.setStyle("-fx-font-size: 72px;");

            Label message = new Label("No workout data available for this period");
            message.setStyle("-fx-text-fill: #FFFFFF !important; " +
                           "-fx-font-size: 22px; " +
                           "-fx-font-weight: bold; " +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 1);");

            Label suggestion = new Label("Complete some workouts to see your progress!");
            suggestion.setStyle("-fx-text-fill: #E0E0E0 !important; " +
                              "-fx-font-size: 16px; " +
                              "-fx-font-weight: 500; " +
                              "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 2, 0, 0, 1);");

            noDataBox.getChildren().addAll(icon, message, suggestion);
            statsCardsContainer.getChildren().add(noDataBox);
        }
    }

    // Inner class for Personal Records Table
    public static class PersonalRecordEntry {
        private final String exercise;
        private final String weight;
        private final String date;

        public PersonalRecordEntry(String exercise, String weight, String date) {
            this.exercise = exercise;
            this.weight = weight;
            this.date = date;
        }

        public String getExercise() { return exercise; }
        public String getWeight() { return weight; }
        public String getDate() { return date;  }
    }
    // ==================== ENHANCED HOME TAB METHODS ====================

    private void displayRecentWorkouts(List<Workout> workouts) {
        if (recentWorkoutsBox == null) return;

        recentWorkoutsBox.getChildren().clear();

        if (workouts.isEmpty()) {
            VBox emptyState = createEmptyWorkoutState();
            recentWorkoutsBox.getChildren().add(emptyState);
            return;
        }

        // Show last 5 workouts
        workouts.stream()
                .sorted((w1, w2) -> w2.getStartTime().compareTo(w1.getStartTime()))
                .limit(5)
                .forEach(workout -> {
                    HBox workoutCard = createWorkoutCard(workout);
                    recentWorkoutsBox.getChildren().add(workoutCard);
                });
    }

    private HBox createWorkoutCard(Workout workout) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 25, 20, 25));
        card.setStyle(
                "-fx-background-color: #2b2b2b;" +
                        "-fx-background-radius: 15px;" +
                        "-fx-border-color: rgba(0,212,255,0.3);" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 15px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 5);" +
                        "-fx-cursor: hand;"
        );

        // Workout icon
        Label icon = new Label("🏋️");
        icon.setStyle("-fx-font-size: 32px;");

        // Workout details
        VBox details = new VBox(5);

        Label nameLabel = new Label(workout.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label dateLabel = new Label(workout.getStartTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a")));
        dateLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 13px;");

        Label statsLabel = new Label(String.format("%d sets • %.0f lbs volume • %d min",
                workout.getTotalSets(),
                workout.getTotalVolume(),
                workout.getDurationMinutes()));
        statsLabel.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 13px; -fx-font-weight: 600;");

        details.getChildren().addAll(nameLabel, dateLabel, statsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // View button
        Button viewBtn = new Button("View →");
        viewBtn.setStyle(
                "-fx-background-color: #00d4ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 20px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-cursor: hand;"
        );

        card.getChildren().addAll(icon, details, spacer, viewBtn);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-scale-x: 1.02; -fx-scale-y: 1.02;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-scale-x: 1.02; -fx-scale-y: 1.02;", ""));
        });

        return card;
    }

    private VBox createEmptyWorkoutState() {
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(40));
        emptyState.setStyle(
                "-fx-background-color: rgba(43,43,43,0.5);" +
                        "-fx-background-radius: 15px;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-style: dashed;" +
                        "-fx-border-radius: 15px;"
        );

        Label icon = new Label("💪");
        icon.setStyle("-fx-font-size: 64px;");

        Label message = new Label("No workouts yet!");
        message.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitle = new Label("Start your first workout to see it here");
        subtitle.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14px;");

        Button startBtn = new Button("🚀 Start Your First Workout");
        startBtn.setStyle(
                "-fx-background-color: #00d4ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12px 30px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-cursor: hand;"
        );
        startBtn.setOnAction(e -> {
            mainTabPane.getSelectionModel().select(workoutTab);
            handleStartWorkout();
        });

        emptyState.getChildren().addAll(icon, message, subtitle, startBtn);
        return emptyState;
    }

    private void updateStatsCards(List<Workout> workouts) {
        if (statsCardsContainer == null) return;

        statsCardsContainer.getChildren().clear();

        // Calculate stats
        int totalWorkouts = workouts.size();
        double totalVolume = workouts.stream().mapToDouble(Workout::getTotalVolume).sum();
        int totalSets = workouts.stream().mapToInt(Workout::getTotalSets).sum();
        long avgDuration = workouts.isEmpty() ? 0 :
                (long) workouts.stream().mapToLong(Workout::getDurationMinutes).average().orElse(0);

        // Create stat cards
        VBox workoutsCard = createStatCard("💪", String.valueOf(totalWorkouts), "Total Workouts",
                "#667eea");
        VBox volumeCard = createStatCard("🏋️", String.format("%.0fK", totalVolume / 1000), "Total Volume",
                "#f093fb");
        VBox setsCard = createStatCard("📊", String.valueOf(totalSets), "Total Sets",
                "#4facfe");
        VBox durationCard = createStatCard("⏱️", avgDuration + "m", "Avg Duration",
                "#43e97b");

        statsCardsContainer.getChildren().addAll(workoutsCard, volumeCard, setsCard, durationCard);
    }

    private VBox createStatCard(String icon, String value, String label, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 18px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 6);" +
                        "-fx-cursor: hand;"
        );
        card.setMinWidth(160);
        card.setMinHeight(140);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 42px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label descLabel = new Label(label);
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13px; -fx-font-weight: 600;");

        card.getChildren().addAll(iconLabel, valueLabel, descLabel);

        // Hover animation
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.08);
            card.setScaleY(1.08);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }

    private void displayMotivationQuote() {
        if (motivationLabel == null) return;

        String[] quotes = {
                "The only bad workout is the one that didn't happen. Let's make today count! 💪",
                "Push yourself, because no one else is going to do it for you. You got this! 🔥",
                "Success starts with self-discipline. Every rep brings you closer to your goals! 🎯",
                "Your body can stand almost anything. It's your mind you have to convince. Keep pushing! 🧠",
                "The pain you feel today will be the strength you feel tomorrow. Stay strong! ⚡",
                "Don't limit your challenges. Challenge your limits! You're unstoppable! 🚀"
        };

        int randomIndex = (int) (Math.random() * quotes.length);
        motivationLabel.setText(quotes[randomIndex]);
    }

    private void displayAchievements(List<Workout> workouts) {
        if (achievementsBox == null) return;

        achievementsBox.getChildren().clear();

        // Check for achievements
        if (workouts.size() >= 1) {
            achievementsBox.getChildren().add(createAchievementBadge("🎉", "First Steps", "Completed your first workout!"));
        }
        if (workouts.size() >= 10) {
            achievementsBox.getChildren().add(createAchievementBadge("🔥", "On Fire", "10 workouts completed!"));
        }
        if (workouts.size() >= 50) {
            achievementsBox.getChildren().add(createAchievementBadge("💯", "Dedicated", "50 workouts milestone!"));
        }
        if (workouts.size() >= 100) {
            achievementsBox.getChildren().add(createAchievementBadge("👑", "Champion", "100 workouts! Legendary!"));
        }
    }

    private VBox createAchievementBadge(String icon, String title, String description) {
        VBox badge = new VBox(8);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(20));
        badge.setStyle(
                "-fx-background-color: #FFD700;" +
                        "-fx-background-radius: 15px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.5), 12, 0, 0, 6);"
        );
        badge.setMinWidth(140);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 36px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 11px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(120);
        descLabel.setAlignment(Pos.CENTER);

        badge.getChildren().addAll(iconLabel, titleLabel, descLabel);

        return badge;
    }
    // ==================== QUICK ACTION HANDLERS ====================

    @FXML
    private void handleQuickSetClick() {
        System.out.println("🏋️ Quick Set clicked - switching to Workout tab");

        // Animate card
        animateCardClick(quickSetCard);

        // Switch to workout tab after brief delay
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(150);
                mainTabPane.getSelectionModel().select(workoutTab);

                // Auto-start workout if not started
                if (currentWorkout == null) {
                    handleStartWorkout();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleProgressClick() {
        System.out.println("📊 Progress clicked - switching to Progress tab");

        // Animate card
        animateCardClick(progressCard);

        // Switch to progress tab after brief delay
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(150);
                mainTabPane.getSelectionModel().select(progressTab);
                loadProgressCharts();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    // Call this from initialize() to verify database
    private void verifyDatabaseExercises() {
        System.out.println("\n=== DATABASE VERIFICATION ===");

        String[] muscleGroups = {
                "Chest", "Back", "Shoulders", "Biceps", "Triceps",
                "Forearms", "Abs", "Obliques", "Lower Back", "Glutes",
                "Quadriceps", "Hamstrings", "Calves", "Neck"
        };

        int totalExercises = 0;
        for (String muscle : muscleGroups) {
            List<Exercise> exercises = dbHelper.getExercisesByMuscleGroup(muscle);
            int count = exercises != null ? exercises.size() : 0;
            totalExercises += count;
            System.out.println(muscle + ": " + count + " exercises");
        }

        System.out.println("TOTAL: " + totalExercises + " exercises in database");
        System.out.println("=============================\n");
    }

    @FXML
    private void handleMuscleClick(String muscleName) {
        System.out.println("💪 Muscle clicked: " + muscleName);

        // Update selected muscle label
        if (selectedMuscleLabel != null) {
            selectedMuscleLabel.setText("🎯 Selected: " + muscleName.toUpperCase());
            selectedMuscleLabel.setStyle(
                    "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00d4ff;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.6), 4, 0, 0, 2);"
            );
        }

        // ✅ FIX: Convert muscle name to proper case for database query
        String properMuscleName = toProperCase(muscleName);
        System.out.println("🔍 Querying database with proper case: '" + properMuscleName + "'");

        // Load exercises for this muscle group from database
        List<Exercise> exercises = dbHelper.getExercisesByMuscleGroup(properMuscleName);

        System.out.println("🔍 Database query returned: " + (exercises != null ? exercises.size() : 0) + " exercises");

        if (exercises != null && !exercises.isEmpty()) {
            System.out.println("✅ Found " + exercises.size() + " exercises for " + muscleName);

            // Clear and update exercise list
            if (exerciseListView != null) {
                exerciseListView.getItems().clear();
                exerciseListView.getItems().addAll(exercises);
                exerciseListView.refresh();

                System.out.println("📋 Exercise list populated with:");
                for (Exercise ex : exercises) {
                    System.out.println("  - " + ex.getName() + " (" + ex.getDifficulty() + ")");
                }
            }

            // Update instruction area with helpful message
            if (exerciseDescriptionArea != null) {
                exerciseDescriptionArea.setText(
                        "💡 " + exercises.size() + " EXERCISES LOADED FOR " + muscleName.toUpperCase() + "\n\n" +
                                "✨ SELECT AN EXERCISE FROM THE LIST ABOVE\n\n" +
                                "You will see:\n" +
                                "• Professional exercise information\n" +
                                "• Step-by-step instructions\n" +
                                "• Equipment needed\n" +
                                "• Difficulty level\n\n" +
                                "Click any exercise to begin!"
                );
                exerciseDescriptionArea.setStyle(
                        "-fx-font-size: 15px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-text-fill: #FFFFFF; " +
                                "-fx-control-inner-background: #2b2b2b;"
                );
            }

            // Show image placeholder
            showImagePlaceholder();

        } else {
            // No exercises found
            System.out.println("⚠️ No exercises found for: " + muscleName);
            System.out.println("⚠️ Database might be empty - check insertSampleExercises()");

            if (exerciseListView != null) {
                exerciseListView.getItems().clear();
            }

            if (exerciseDescriptionArea != null) {
                exerciseDescriptionArea.setText(
                        "❌ NO EXERCISES FOUND FOR " + muscleName.toUpperCase() + "\n\n" +
                                "⚠️ DATABASE ISSUE DETECTED\n\n" +
                                "Possible causes:\n" +
                                "1. Database file not loaded\n" +
                                "2. Exercises not inserted\n" +
                                "3. Muscle group name mismatch\n\n" +
                                "Solution: Check DatabaseHelper.insertSampleExercises()\n" +
                                "Make sure musclemap.db exists in project root."
                );
                exerciseDescriptionArea.setStyle(
                        "-fx-font-size: 14px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-text-fill: #ff6b6b; " +
                                "-fx-control-inner-background: #2b2b2b;"
                );
            }

            showImagePlaceholder();
        }
    }

// ✅ ADD THIS NEW HELPER METHOD AT THE END OF YOUR CLASS (before the last closing brace)
    /**
     * Converts any string to proper case (First Letter Uppercase)
     * Examples: "GLUTES" -> "Glutes", "LOWER BACK" -> "Lower Back"
     */
    /**
     * Converts any string to proper case (First Letter Uppercase)
     * Examples: "FOREARMS" -> "Forearms", "LOWER BACK" -> "Lower Back"
     */
    /**
     * Converts muscle names to proper case for database queries
     * Examples: "FOREARMS" -> "Forearms", "LOWER BACK" -> "Lower Back"
     */
    /**
     * Converts muscle names to proper case for database queries
     * Examples: "FOREARMS" -> "Forearms", "LOWER BACK" -> "Lower Back"
     */
    /**
     * Converts string to proper case for database queries
     * Examples: "BICEPS" -> "Biceps", "biceps" -> "Biceps", "Biceps" -> "Biceps"
     */
    private String toProperCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Trim whitespace
        input = input.trim();

        // Convert first letter to uppercase, rest to lowercase
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

// ✅ ADD THIS NEW HELPER METHOD TO YOUR CLASS
    /**
     * Converts any string to proper case (First Letter Uppercase)
     * Examples: "CHEST" -> "Chest", "lower back" -> "Lower Back"
     */
    private void animateCardClick(VBox card) {
        if (card == null) return;

        // Scale down animation
        javafx.animation.ScaleTransition scaleDown = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100), card);
        scaleDown.setToX(0.95);
        scaleDown.setToY(0.95);

        // Scale up animation
        javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100), card);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        // Play animations
        scaleDown.setOnFinished(e -> scaleUp.play());
        scaleDown.play();
    }
    // ==================== EXERCISE IMAGE LOADING ====================

    private void loadExerciseImage(Exercise exercise) {
        if (exercise == null) {
            showImagePlaceholder();
            return;
        }

        System.out.println("📋 Loading exercise details for: " + exercise.getName());
        
        // Show comprehensive exercise information instead of visuals
        showComprehensiveExerciseDetails(exercise);
    }

    private void showComprehensiveExerciseDetails(Exercise exercise) {
        System.out.println("📋 Creating comprehensive details for: " + exercise.getName());
        
        // Hide the image area and placeholder since we're showing rich text details
        if (exerciseImageContainer != null) {
            exerciseImageContainer.setVisible(false);
            exerciseImageContainer.setManaged(false);
        }
        if (imagePlaceholder != null) {
            imagePlaceholder.setVisible(false);
            imagePlaceholder.setManaged(false);
        }
        if (exerciseImageView != null) {
            exerciseImageView.setVisible(false);
            exerciseImageView.setManaged(false);
        }
        
        // Render static, high-quality text details in the description area only
        displayExerciseDetails(exercise);
        System.out.println("✅ Static details displayed for: " + exercise.getName());
    }


    // ✅ ADD THIS METHOD if it doesn't exist
    private void showImagePlaceholder() {
        javafx.application.Platform.runLater(() -> {
            if (exerciseImageView != null) {
                exerciseImageView.setImage(null);
                exerciseImageView.setVisible(false);
            }
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisible(true);
            }
        });
    }

    // ✅ ADD THIS METHOD if it doesn't exist
    private void showImageError() {
        javafx.application.Platform.runLater(() -> {
            if (exerciseImageView != null) {
                exerciseImageView.setImage(null);
                exerciseImageView.setVisible(false);
            }
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisible(true);
            }
            
            System.err.println("⚠️ Showing placeholder due to image load failure");
        });
    }


    private void showExerciseImage(Image image) {
        javafx.application.Platform.runLater(() -> {
            System.out.println("🖼️ Displaying exercise image: " + (image != null ? "Image loaded" : "Image is null"));
            if (image != null) {
                System.out.println("🖼️ Image dimensions: " + image.getWidth() + "x" + image.getHeight());
            }
            
            if (exerciseImageView != null) {
                exerciseImageView.setImage(image);
                exerciseImageView.setVisible(true);
                exerciseImageView.setFitWidth(200.0);
                exerciseImageView.setFitHeight(200.0);
                exerciseImageView.setPreserveRatio(true);
                exerciseImageView.setSmooth(true);
                exerciseImageView.setCache(true);
                System.out.println("✅ ImageView configured and made visible");
            } else {
                System.out.println("❌ exerciseImageView is null!");
            }
            
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisible(false);
                System.out.println("✅ Placeholder hidden");
            }
            
        });
    }

    private void handleExerciseSelection(Exercise exercise) {
        if (exercise == null) {
            System.err.println("⚠️ handleExerciseSelection called with null exercise");
            return;
        }

        System.out.println("✅ Exercise selected: " + exercise.getName());
        System.out.println("🔍 Exercise GIF URL: " + exercise.getGifUrl());
        System.out.println("🔍 Exercise details: " + exercise.getName() + " - " + exercise.getMuscleGroup());

        // Display exercise details in the text area
        displayExerciseDetails(exercise);

        // Load the exercise image (placeholder) - called from selection listener
        loadExerciseImage(exercise);
    }


    // ✅ REMOVED: Now using Exercise.getGifUrl() method instead of duplicate mapping

    // ✅ REMOVED: No more fallback image URLs - using professional placeholders only
    private String getPlaceholderImageByMuscleGroup(String muscleGroup) {
        return null; // No images - use professional placeholders
    }



    /**
     * Displays detailed information about an exercise in the description area
     */
    private void displayExerciseDetails(Exercise exercise) {
        if (exercise == null) {
            System.err.println("⚠️ displayExerciseDetails called with null exercise");
            return;
        }

        if (exerciseDescriptionArea == null) {
            System.err.println("⚠️ exerciseDescriptionArea is null");
            return;
        }

        // Create premium exercise details with custom styling per exercise
        StringBuilder details = new StringBuilder();
        
        // Get exercise-specific theme
        ExerciseTheme theme = getExerciseTheme(exercise);
        
        // Premium header with custom styling
        details.append(theme.headerIcon).append(" ").append(exercise.getName().toUpperCase()).append("\n");
        details.append("═".repeat(50)).append("\n\n");
        
        // Enhanced overview with custom icons and colors
        details.append("📊 EXERCISE OVERVIEW:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(theme.targetIcon).append(" Target: ").append(exercise.getMuscleGroup()).append("\n");
        details.append(theme.equipmentIcon).append(" Equipment: ").append(exercise.getEquipment()).append("\n");
        details.append(theme.difficultyIcon).append(" Difficulty: ").append(exercise.getDifficulty()).append("\n");
        details.append("⏱️ Sets: ").append(getRecommendedSets(exercise)).append(" | Reps: ").append(getRecommendedReps(exercise)).append("\n");
        details.append("🔥 Intensity: ").append(getExerciseIntensity(exercise)).append("\n");
        details.append("💪 Muscle Activation: ").append(getMuscleActivationPercentage(exercise)).append("\n\n");
        
        // Premium form instructions with visual guides
        details.append("📋 PROPER FORM & TECHNIQUE:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getPremiumFormInstructions(exercise)).append("\n\n");
        
        // Visual step-by-step guide
        details.append("👁️ VISUAL STEP-BY-STEP GUIDE:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getVisualStepGuide(exercise)).append("\n\n");
        
        // Load and display exercise step images
        loadExerciseStepImages(exercise);
        
        // Enhanced muscle activation
        details.append("💪 MUSCLE ACTIVATION:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getPremiumMuscleActivation(exercise)).append("\n\n");
        
        // Premium tips and mistakes
        details.append("⚠️ COMMON MISTAKES TO AVOID:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getPremiumCommonMistakes(exercise)).append("\n\n");
        
        // Enhanced progression
        details.append("📈 PROGRESSION & VARIATIONS:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getPremiumProgression(exercise)).append("\n\n");
        
        // Premium benefits
        details.append("🎯 BENEFITS:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getPremiumBenefits(exercise)).append("\n\n");
        
        // Enhanced safety
        details.append("🛡️ SAFETY TIPS:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getPremiumSafetyTips(exercise)).append("\n\n");
        
        // Posture and form checklist
        details.append("🎯 POSTURE & FORM CHECKLIST:\n");
        details.append("━".repeat(30)).append("\n");
        details.append(getPostureVisualGuide(exercise)).append("\n");

        exerciseDescriptionArea.setText(details.toString());
        
        // Apply premium styling with exercise-specific theme
        String premiumStyle = String.format(
                "-fx-font-size: 15px; " +
                "-fx-font-weight: 600; " +
                        "-fx-text-fill: #FFFFFF; " +
                "-fx-control-inner-background: %s; " +
                "-fx-background-color: %s; " +
                "-fx-background-radius: 20px; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 3px; " +
                "-fx-border-radius: 20px; " +
                "-fx-padding: 20px; " +
                "-fx-effect: dropshadow(gaussian, %s, 25, 0, 0, 8);",
                theme.backgroundColor,
                theme.backgroundColor,
                theme.borderColor,
                theme.shadowColor
        );
        
        exerciseDescriptionArea.setStyle(premiumStyle);

        // Ensure description area is visible when details are shown
        exerciseDescriptionArea.setVisible(true);
        exerciseDescriptionArea.setManaged(true);

        System.out.println("📝 Displayed enhanced details for: " + exercise.getName());
    }
    
    // ==================== PREMIUM EXERCISE THEME SYSTEM ====================
    
    private static class ExerciseTheme {
        String headerIcon;
        String targetIcon;
        String equipmentIcon;
        String difficultyIcon;
        String backgroundColor;
        String borderColor;
        String shadowColor;
        
        ExerciseTheme(String headerIcon, String targetIcon, String equipmentIcon, String difficultyIcon,
                     String backgroundColor, String borderColor, String shadowColor) {
            this.headerIcon = headerIcon;
            this.targetIcon = targetIcon;
            this.equipmentIcon = equipmentIcon;
            this.difficultyIcon = difficultyIcon;
            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            this.shadowColor = shadowColor;
        }
    }
    
    private ExerciseTheme getExerciseTheme(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        
        // Exercise-specific themes
        if (exerciseName.contains("bench press")) {
            return new ExerciseTheme("🏋️‍♂️", "🎯", "🏋️", "💪", 
                "rgba(139, 0, 0, 0.95)", "#8B0000", "rgba(139, 0, 0, 0.6)");
        } else if (exerciseName.contains("squat")) {
            return new ExerciseTheme("🦵", "🎯", "🏋️", "💪", 
                "rgba(255, 165, 0, 0.95)", "#FFA500", "rgba(255, 165, 0, 0.6)");
        } else if (exerciseName.contains("deadlift")) {
            return new ExerciseTheme("⚡", "🎯", "🏋️", "💪", 
                "rgba(255, 69, 0, 0.95)", "#FF4500", "rgba(255, 69, 0, 0.6)");
        } else if (exerciseName.contains("curl")) {
            return new ExerciseTheme("💪", "🎯", "🏋️", "💪", 
                "rgba(0, 191, 255, 0.95)", "#00BFFF", "rgba(0, 191, 255, 0.6)");
        } else if (exerciseName.contains("press") || exerciseName.contains("shoulder")) {
            return new ExerciseTheme("🔥", "🎯", "🏋️", "💪", 
                "rgba(138, 43, 226, 0.95)", "#8A2BE2", "rgba(138, 43, 226, 0.6)");
        } else if (exerciseName.contains("row") || exerciseName.contains("pull")) {
            return new ExerciseTheme("🦴", "🎯", "🏋️", "💪", 
                "rgba(255, 87, 34, 0.95)", "#FF5722", "rgba(255, 87, 34, 0.6)");
        } else if (exerciseName.contains("plank") || exerciseName.contains("crunch")) {
            return new ExerciseTheme("🔥", "🎯", "🏋️", "💪", 
                "rgba(76, 175, 80, 0.95)", "#4CAF50", "rgba(76, 175, 80, 0.6)");
        } else if (exerciseName.contains("dip")) {
            return new ExerciseTheme("💧", "🎯", "🏋️", "💪", 
                "rgba(33, 150, 243, 0.95)", "#2196F3", "rgba(33, 150, 243, 0.6)");
        } else if (exerciseName.contains("fly")) {
            return new ExerciseTheme("🦋", "🎯", "🏋️", "💪", 
                "rgba(233, 30, 99, 0.95)", "#E91E63", "rgba(233, 30, 99, 0.6)");
        } else {
            // Default theme based on muscle group
            if (muscleGroup.contains("chest")) {
                return new ExerciseTheme("🫁", "🎯", "🏋️", "💪", 
                    "rgba(233, 30, 99, 0.95)", "#E91E63", "rgba(233, 30, 99, 0.6)");
            } else if (muscleGroup.contains("back")) {
                return new ExerciseTheme("🦴", "🎯", "🏋️", "💪", 
                    "rgba(255, 87, 34, 0.95)", "#FF5722", "rgba(255, 87, 34, 0.6)");
            } else if (muscleGroup.contains("shoulder")) {
                return new ExerciseTheme("🔥", "🎯", "🏋️", "💪", 
                    "rgba(138, 43, 226, 0.95)", "#8A2BE2", "rgba(138, 43, 226, 0.6)");
            } else if (muscleGroup.contains("bicep") || muscleGroup.contains("tricep")) {
                return new ExerciseTheme("💪", "🎯", "🏋️", "💪", 
                    "rgba(0, 191, 255, 0.95)", "#00BFFF", "rgba(0, 191, 255, 0.6)");
            } else if (muscleGroup.contains("leg")) {
                return new ExerciseTheme("🦵", "🎯", "🏋️", "💪", 
                    "rgba(255, 165, 0, 0.95)", "#FFA500", "rgba(255, 165, 0, 0.6)");
            } else if (muscleGroup.contains("abs") || muscleGroup.contains("core")) {
                return new ExerciseTheme("🔥", "🎯", "🏋️", "💪", 
                    "rgba(76, 175, 80, 0.95)", "#4CAF50", "rgba(76, 175, 80, 0.6)");
            } else {
                return new ExerciseTheme("🏋️", "🎯", "🏋️", "💪", 
                    "rgba(0, 212, 255, 0.95)", "#00D4FF", "rgba(0, 212, 255, 0.6)");
            }
        }
    }
    
    // ==================== PREMIUM CONTENT GENERATORS ====================
    
    private String getPremiumFormInstructions(Exercise exercise) {
        // Comprehensive form instructions covering all 69 exercises with intelligent fallbacks
        return switch (exercise.getName()) {
            // CHEST
            case "Bench Press" -> "1. 🛏️ Lie flat on bench with feet planted firmly, shoulder blades retracted\n" +
                   "2. 🤏 Grip bar slightly wider than shoulders with thumbs around bar\n" +
                   "3. ⬇️ Lower bar to mid-chest with controlled descent (2-3 seconds)\n" +
                   "4. ⬆️ Press up explosively but controlled, driving through chest\n" +
                   "5. 🔒 Keep core tight and maintain natural arch throughout";
            case "Incline Bench Press" -> "1. 🛏️ Set bench to 30-45° incline, lie back with feet flat\n" +
                   "2. 🤏 Grip bar slightly wider than shoulders\n" +
                   "3. ⬇️ Lower bar to upper chest below collarbone\n" +
                   "4. ⬆️ Press up toward ceiling focusing on upper chest\n" +
                   "5. 🔒 Keep elbows at 45° angle, avoid flaring";
            case "Decline Bench Press" -> "1. 🛏️ Set bench to 15-30° decline, secure legs properly\n" +
                   "2. 🤏 Grip bar with hands shoulder-width or wider\n" +
                   "3. ⬇️ Lower bar to lower chest with control\n" +
                   "4. ⬆️ Press up forcefully, engaging lower pecs\n" +
                   "5. 🔒 Maintain tight core throughout movement";
            case "Dumbbell Press", "Incline Dumbbell Press" -> "1. 💪 Start with dumbbells at chest level, palms forward\n" +
                   "2. 🦶 Plant feet firmly, engage core and retract scapula\n" +
                   "3. ⬆️ Press dumbbells up and slightly together\n" +
                   "4. 🔝 Squeeze chest at top, dumbbells nearly touching\n" +
                   "5. ⬇️ Lower with control back to starting position";
            case "Chest Fly", "Cable Fly" -> "1. 💪 Start with arms extended, slight elbow bend maintained\n" +
                   "2. ⬇️ Lower weights/handles in wide arc motion\n" +
                   "3. 🔥 Feel deep stretch in chest, don't go past shoulder line\n" +
                   "4. ⬆️ Bring weights together using chest, not arms\n" +
                   "5. 💪 Squeeze chest hard at peak contraction";
            case "Push-ups" -> "1. 🤸 Start in plank, hands shoulder-width, body straight\n" +
                   "2. 🔒 Engage core and glutes, don't let hips sag\n" +
                   "3. ⬇️ Lower chest to ground, elbows at 45° angle\n" +
                   "4. ⬆️ Press through palms to return to start\n" +
                   "5. 🎯 Maintain straight body line throughout";
            case "Chest Dips" -> "1. 💪 Grip bars, jump to support position, arms locked\n" +
                   "2. 🔄 Lean torso forward 20-30° for chest focus\n" +
                   "3. ⬇️ Lower until shoulders slightly below elbows\n" +
                   "4. ⬆️ Press up powerfully to starting position\n" +
                   "5. 🔒 Keep core tight, control the movement";
            
            // BACK
            case "Deadlifts" -> "1. 🦶 Bar over mid-foot, feet hip-width, toes slightly out\n" +
                   "2. 🤏 Grip bar outside legs, mixed or double overhand\n" +
                   "3. 💪 Drop hips, chest up, shoulders over bar, neutral spine\n" +
                   "4. 🫁 Big breath, brace core, engage lats\n" +
                   "5. ⬆️ Drive through floor, extend hips and knees together";
            case "Barbell Row", "T-Bar Row" -> "1. 🔄 Hinge at hips to 45°, keep back straight and chest up\n" +
                   "2. 🤏 Grip bar/handles, arms fully extended\n" +
                   "3. ⬆️ Pull to lower chest/upper abdomen, lead with elbows\n" +
                   "4. 💪 Retract shoulder blades, squeeze for 1 second\n" +
                   "5. ⬇️ Lower with control, maintain position";
            case "Pull-ups" -> "1. 🤏 Overhand grip, hands shoulder-width apart\n" +
                   "2. 💪 Dead hang with shoulders engaged, not relaxed\n" +
                   "3. ⬆️ Pull shoulder blades down, drive elbows to sides\n" +
                   "4. 🔝 Chin above bar, chest near bar, squeeze back\n" +
                   "5. ⬇️ Control descent back to dead hang";
            case "Lat Pulldown" -> "1. 💺 Sit with thighs secured under pad\n" +
                   "2. 🤏 Grip bar wide, lean back slightly 10-15°\n" +
                   "3. ⬆️ Pull bar to upper chest, drive elbows down\n" +
                   "4. 💪 Squeeze lats at bottom, hold contraction\n" +
                   "5. ⬆️ Control bar back up with full arm extension";
            case "Seated Cable Row", "One-Arm Dumbbell Row" -> "1. 💺 Sit/position with stable base, chest up\n" +
                   "2. 🤏 Grip handle, arms extended, slight lean\n" +
                   "3. ⬆️ Pull to torso/hip, keep elbows close\n" +
                   "4. 💪 Retract shoulder blades maximally\n" +
                   "5. ⬇️ Extend arms forward with control";
            
            // SHOULDERS
            case "Overhead Press", "Dumbbell Shoulder Press" -> "1. 🦶 Stand/sit with feet shoulder-width, core braced\n" +
                   "2. 🤏 Weight at shoulder height, elbows slightly forward\n" +
                   "3. 🫁 Take breath, engage core and glutes\n" +
                   "4. ⬆️ Press straight up overhead, avoid arching back\n" +
                   "5. 🔝 Lock arms at top, lower with control";
            case "Lateral Raise" -> "1. 🦶 Stand with feet hip-width, slight forward lean\n" +
                   "2. 🤏 Dumbbells at sides, slight elbow bend\n" +
                   "3. ⬆️ Raise to sides until arms parallel to ground\n" +
                   "4. 🔝 Lead with elbows, not hands\n" +
                   "5. ⬇️ Lower slowly with control";
            case "Front Raise" -> "1. 🦶 Stand with dumbbells at thighs\n" +
                   "2. 🔒 Slight elbow bend, core engaged\n" +
                   "3. ⬆️ Raise forward to shoulder height\n" +
                   "4. 🚫 Don't use momentum or swing\n" +
                   "5. ⬇️ Lower with control back to thighs";
            case "Rear Delt Fly" -> "1. 🔄 Hinge at hips to 45°, dumbbells hanging\n" +
                   "2. 💪 Chest up, back flat, slight elbow bend\n" +
                   "3. ⬆️ Raise dumbbells to sides, lead with elbows\n" +
                   "4. 💪 Squeeze shoulder blades together\n" +
                   "5. ⬇️ Lower with control";
            case "Face Pull" -> "1. 🤏 Grip rope, step back for tension\n" +
                   "2. 🦶 Staggered stance, slight lean back\n" +
                   "3. ⬆️ Pull rope to face, elbows flare high\n" +
                   "4. 💪 Hands by ears, squeeze shoulder blades\n" +
                   "5. ⬇️ Control rope back to start";
            case "Arnold Press" -> "1. 💺 Sit with dumbbells at chest, palms facing you\n" +
                   "2. 🔄 Begin press while rotating palms forward\n" +
                   "3. ⬆️ Continue pressing and rotating simultaneously\n" +
                   "4. 🔝 Finish with arms overhead, palms forward\n" +
                   "5. ⬇️ Reverse rotation while lowering";
            
            // BICEPS
            case "Bicep Curls", "Barbell Curl", "Hammer Curl", "Cable Curl" -> "1. 🦶 Stand with feet hip-width, weight in hands\n" +
                   "2. 🔒 Keep elbows close to body, upper arms stationary\n" +
                   "3. ⬆️ Curl weights up to shoulders with control\n" +
                   "4. 💪 Squeeze biceps at top, hold for 1 second\n" +
                   "5. ⬇️ Lower with control to full extension";
            case "Preacher Curl" -> "1. 💺 Sit at preacher bench, armpits at top of pad\n" +
                   "2. 🤏 Grip weight, arms fully extended on pad\n" +
                   "3. ⬆️ Curl up keeping arms on pad\n" +
                   "4. 💪 Squeeze biceps at top\n" +
                   "5. ⬇️ Lower slowly, don't lock out completely";
            case "Concentration Curl" -> "1. 💺 Sit with elbow braced on inner thigh\n" +
                   "2. 🤏 Dumbbell hanging straight down\n" +
                   "3. ⬆️ Curl up focusing only on bicep\n" +
                   "4. 💪 Squeeze hard at top\n" +
                   "5. ⬇️ Lower slowly with full stretch";
            
            // TRICEPS
            case "Tricep Dips" -> "1. 💪 Grip bars, support body with locked arms\n" +
                   "2. 🦶 Body upright (not leaning for triceps)\n" +
                   "3. ⬇️ Lower until upper arms parallel to ground\n" +
                   "4. ⬆️ Press through palms to extend arms\n" +
                   "5. 🔝 Lock out at top, repeat";
            case "Close-Grip Bench Press" -> "1. 🛏️ Lie on bench, grip bar hands shoulder-width\n" +
                   "2. ⬇️ Lower to lower chest, elbows close to body\n" +
                   "3. ⬆️ Press up focusing on triceps\n" +
                   "4. 💪 Extend arms fully, squeeze triceps\n" +
                   "5. 🔒 Keep elbows tucked throughout";
            case "Tricep Pushdown" -> "1. 🦶 Stand facing cable, grip attachment\n" +
                   "2. 🔒 Elbows at sides, slight forward lean\n" +
                   "3. ⬇️ Push down until arms fully extended\n" +
                   "4. 💪 Squeeze triceps at bottom\n" +
                   "5. ⬆️ Control weight back up, elbows stationary";
            case "Overhead Tricep Extension", "Skull Crushers" -> "1. 💪 Position weight overhead or above chest\n" +
                   "2. 🔒 Keep upper arms stationary throughout\n" +
                   "3. ⬇️ Lower weight behind head/to forehead\n" +
                   "4. 💪 Feel stretch in triceps\n" +
                   "5. ⬆️ Extend arms back to starting position";
            case "Diamond Push-ups" -> "1. 🤸 Plank position, hands forming diamond shape\n" +
                   "2. 🔒 Body straight, core engaged\n" +
                   "3. ⬇️ Lower chest to hands, elbows close\n" +
                   "4. ⬆️ Press up to starting position\n" +
                   "5. 💪 Focus on triceps throughout";
            
            // FOREARMS
            case "Wrist Curls", "Reverse Wrist Curls" -> "1. 💺 Sit with forearms on thighs, wrists over knees\n" +
                   "2. 🤏 Hold weight in hands, palms up or down\n" +
                   "3. ⬆️ Curl/extend wrists upward\n" +
                   "4. 💪 Squeeze forearms at top\n" +
                   "5. ⬇️ Lower slowly for full stretch";
            case "Farmer's Walk" -> "1. 🤏 Grip heavy weights firmly at sides\n" +
                   "2. 🦶 Stand tall, chest up, shoulders back\n" +
                   "3. 🚶 Walk forward with controlled steps\n" +
                   "4. 🔒 Keep weights from swinging\n" +
                   "5. 💪 Maintain good posture throughout";
            
            // ABS
            case "Planks", "Side Plank" -> "1. 💪 Position on forearms and toes, body straight\n" +
                   "2. 🔒 Engage abs, glutes, and core\n" +
                   "3. 🚫 Don't let hips sag or pike up\n" +
                   "4. 🫁 Breathe steadily, don't hold breath\n" +
                   "5. ⏱️ Hold for prescribed time";
            case "Crunches", "Cable Crunch" -> "1. 🛏️ Lie on back or kneel, hands positioned\n" +
                   "2. 🔒 Press lower back down, engage abs\n" +
                   "3. ⬆️ Lift shoulders/crunch down toward knees\n" +
                   "4. 💪 Squeeze abs at top, hold 1 second\n" +
                   "5. ⬇️ Lower with control";
            case "Leg Raises", "Hanging Knee Raise" -> "1. 🛏️ Lie flat or hang from bar\n" +
                   "2. 🔒 Press lower back down or engage shoulders\n" +
                   "3. ⬆️ Raise legs/knees toward chest\n" +
                   "4. 💪 Curl pelvis up, squeeze abs\n" +
                   "5. ⬇️ Lower with control";
            case "Russian Twists", "Bicycle Crunches" -> "1. 💺 Sit with feet elevated, lean back 45°\n" +
                   "2. 🔒 Engage core for stability\n" +
                   "3. 🔄 Rotate torso side to side with control\n" +
                   "4. 💪 Touch weight/elbow to sides\n" +
                   "5. ⚡ Maintain constant tension";
            case "Wood Chops" -> "1. 🦶 Stand sideways to cable, feet wide\n" +
                   "2. 🤏 Grip handle with both hands\n" +
                   "3. 🔄 Pull cable diagonally across body\n" +
                   "4. 💪 Rotate torso, pivot back foot\n" +
                   "5. ⬇️ Control return to start";
            
            // LOWER BACK & GLUTES
            case "Back Extensions" -> "1. 💺 Position on hyperextension bench properly\n" +
                   "2. ⬇️ Bend forward at waist, arms crossed\n" +
                   "3. ⬆️ Raise torso until body forms straight line\n" +
                   "4. 💪 Squeeze lower back and glutes at top\n" +
                   "5. ⬇️ Lower with control";
            case "Good Mornings" -> "1. 💪 Bar on upper back, feet shoulder-width\n" +
                   "2. 🔄 Hinge at hips, slight knee bend\n" +
                   "3. ⬇️ Lower until torso near parallel\n" +
                   "4. 💪 Feel hamstring stretch, back straight\n" +
                   "5. ⬆️ Drive hips forward to stand";
            case "Superman" -> "1. 🛏️ Lie face down, arms overhead\n" +
                   "2. 💪 Engage core and glutes\n" +
                   "3. ⬆️ Lift arms, chest, and legs simultaneously\n" +
                   "4. ⏱️ Hold for 2-3 seconds\n" +
                   "5. ⬇️ Lower with control";
            case "Hip Thrusts", "Glute Bridges" -> "1. 🛏️ Position with back against bench or on ground\n" +
                   "2. 🦶 Feet flat, knees bent, core braced\n" +
                   "3. ⬆️ Drive through heels, lift hips up\n" +
                   "4. 💪 Squeeze glutes at top, body straight\n" +
                   "5. ⬇️ Lower with control";
            case "Bulgarian Split Squats" -> "1. 🦶 Stand facing away from bench, one foot elevated\n" +
                   "2. 💪 Front foot forward enough for deep lunge\n" +
                   "3. ⬇️ Lower back knee toward ground\n" +
                   "4. 📐 Front thigh parallel to ground\n" +
                   "5. ⬆️ Drive through front heel to stand";
            case "Cable Kickbacks" -> "1. 💪 Attach ankle cuff, face machine\n" +
                   "2. 🦶 Hold machine for balance, slight lean forward\n" +
                   "3. ⬆️ Kick leg back, squeezing glutes\n" +
                   "4. 💪 Contract glute at full extension\n" +
                   "5. ⬇️ Control leg back to start";
            
            // LEGS
            case "Squats", "Front Squats" -> "1. 🦶 Feet shoulder-width, toes slightly out\n" +
                   "2. 💪 Bar positioned properly, core braced\n" +
                   "3. ⬇️ Push hips back, bend knees, chest up\n" +
                   "4. 📐 Lower until thighs parallel or below\n" +
                   "5. ⬆️ Drive through heels, extend hips and knees";
            case "Leg Press" -> "1. 💺 Sit with back flat against pad\n" +
                   "2. 🦶 Feet on platform shoulder-width apart\n" +
                   "3. ⬇️ Lower platform by bending knees\n" +
                   "4. 📐 Knees to 90° or slightly below\n" +
                   "5. ⬆️ Press through heels to extend";
            case "Lunges", "Walking Lunges" -> "1. 🦶 Stand tall, feet hip-width apart\n" +
                   "2. 🚶 Step forward with large step\n" +
                   "3. ⬇️ Lower back knee toward ground\n" +
                   "4. 📐 Both knees at 90°, front knee over ankle\n" +
                   "5. ⬆️ Drive through front heel to return/continue";
            case "Leg Extensions" -> "1. 💺 Sit at machine, adjust pad to ankles\n" +
                   "2. 🔒 Back against pad, knees at pivot point\n" +
                   "3. ⬆️ Extend legs until fully straight\n" +
                   "4. 💪 Squeeze quads at top\n" +
                   "5. ⬇️ Lower with control";
            case "Romanian Deadlift", "Stiff-Leg Deadlift" -> "1. 🦶 Feet hip-width, bar at thighs\n" +
                   "2. 🔄 Hinge at hips, slight/minimal knee bend\n" +
                   "3. ⬇️ Lower bar down legs, keep back straight\n" +
                   "4. 💪 Feel deep hamstring stretch\n" +
                   "5. ⬆️ Drive hips forward to stand";
            case "Leg Curls" -> "1. 🛏️ Lie face down on machine\n" +
                   "2. 💪 Pad on back of ankles, knees off bench\n" +
                   "3. ⬆️ Curl legs up toward glutes\n" +
                   "4. 💪 Squeeze hamstrings at top\n" +
                   "5. ⬇️ Lower with control";
            case "Nordic Curls" -> "1. 🦵 Kneel with ankles secured\n" +
                   "2. 💪 Body upright, core braced\n" +
                   "3. ⬇️ Lower torso forward keeping body straight\n" +
                   "4. 🔒 Control descent with hamstrings\n" +
                   "5. ⬆️ Pull back up or push off floor";
            
            // CALVES
            case "Standing Calf Raise", "Seated Calf Raise" -> "1. 🦶 Balls of feet on platform, heels hanging\n" +
                   "2. ⬇️ Start with heels below toes for stretch\n" +
                   "3. ⬆️ Push through balls of feet, raise heels high\n" +
                   "4. 💪 Rise as high as possible, squeeze calves\n" +
                   "5. ⬇️ Lower heels below platform for stretch";
            case "Jump Rope" -> "1. 🦶 Hold rope handles, rope behind heels\n" +
                   "2. 💪 Stand tall, elbows close to sides\n" +
                   "3. 🔄 Rotate rope forward with wrists\n" +
                   "4. 🦘 Jump as rope approaches, land on balls of feet\n" +
                   "5. ⚡ Maintain rhythm, stay light on feet";
            
            // NECK
            case "Neck Curls", "Neck Extensions" -> "1. 🛏️ Lie on bench with head hanging off\n" +
                   "2. 🤏 Place hands on head for resistance\n" +
                   "3. ⬆️ Lift/extend head against resistance\n" +
                   "4. 💪 Control movement, don't jerk\n" +
                   "5. ⬇️ Lower with control";
            
            default -> "1. 💪 Set up in proper starting position with good alignment\n" +
                   "2. 🔒 Engage core and stabilize body throughout movement\n" +
                   "3. ⚡ Perform exercise with controlled, deliberate motion\n" +
                   "4. 💪 Focus on target muscle, maintain proper form\n" +
                   "5. 🫁 Breathe properly, don't hold breath";
        };
    }
    
    private String getPremiumMuscleActivation(Exercise exercise) {
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        
        if (muscleGroup.contains("chest")) {
            return "🔥 PRIMARY: Pectoralis Major & Minor (95%)\n" +
                   "💪 SECONDARY: Anterior Deltoids (75%)\n" +
                   "⚡ STABILIZERS: Triceps, Core, Serratus Anterior";
        } else if (muscleGroup.contains("back")) {
            return "🔥 PRIMARY: Latissimus Dorsi, Rhomboids (90%)\n" +
                   "💪 SECONDARY: Middle Trapezius, Posterior Deltoids (80%)\n" +
                   "⚡ STABILIZERS: Core, Lower Back, Biceps";
        } else if (muscleGroup.contains("shoulder")) {
            return "🔥 PRIMARY: Deltoids - All Heads (95%)\n" +
                   "💪 SECONDARY: Upper Trapezius, Supraspinatus (70%)\n" +
                   "⚡ STABILIZERS: Core, Rotator Cuff, Serratus";
        } else if (muscleGroup.contains("bicep")) {
            return "🔥 PRIMARY: Biceps Brachii (98%)\n" +
                   "💪 SECONDARY: Brachialis, Brachioradialis (85%)\n" +
                   "⚡ STABILIZERS: Core, Shoulder Girdle, Forearms";
        } else if (muscleGroup.contains("tricep")) {
            return "🔥 PRIMARY: Triceps Brachii (95%)\n" +
                   "💪 SECONDARY: Anconeus (60%)\n" +
                   "⚡ STABILIZERS: Core, Shoulder Girdle, Chest";
        } else if (muscleGroup.contains("leg")) {
            return "🔥 PRIMARY: Quadriceps/Hamstrings (92%)\n" +
                   "💪 SECONDARY: Glutes, Calves (80%)\n" +
                   "⚡ STABILIZERS: Core, Hip Flexors, Ankle Stabilizers";
        } else if (muscleGroup.contains("abs")) {
            return "🔥 PRIMARY: Rectus Abdominis (95%)\n" +
                   "💪 SECONDARY: Obliques, Transverse Abdominis (80%)\n" +
                   "⚡ STABILIZERS: Hip Flexors, Lower Back";
        } else {
            return "Primary: " + exercise.getMuscleGroup() + "\nFocus on proper muscle engagement and controlled movement";
        }
    }
    
    private String getPremiumCommonMistakes(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        
        if (exerciseName.contains("bench press")) {
            return "❌ Bouncing bar off chest\n" +
                   "❌ Flaring elbows too wide\n" +
                   "❌ Arching back excessively\n" +
                   "❌ Not controlling descent\n" +
                   "❌ Feet not stable on ground";
        } else if (exerciseName.contains("squat")) {
            return "❌ Knees caving inward\n" +
                   "❌ Leaning too far forward\n" +
                   "❌ Not going deep enough\n" +
                   "❌ Lifting heels off ground\n" +
                   "❌ Rushing the movement";
        } else if (exerciseName.contains("curl")) {
            return "❌ Swinging the weights\n" +
                   "❌ Using momentum\n" +
                   "❌ Not full range of motion\n" +
                   "❌ Elbows moving forward\n" +
                   "❌ Rushing the movement";
        } else {
            return "❌ Using momentum instead of muscle\n" +
                   "❌ Poor posture and form\n" +
                   "❌ Not controlling the weight\n" +
                   "❌ Rushing through reps\n" +
                   "❌ Not engaging core";
        }
    }
    
    private String getPremiumProgression(Exercise exercise) {
        String difficulty = exercise.getDifficulty().toLowerCase();
        
        if (difficulty.contains("beginner")) {
            return "🌱 Start with bodyweight or light weights\n" +
                   "🎯 Focus on perfect form first\n" +
                   "📈 Increase reps before weight\n" +
                   "🏆 Master the movement pattern\n" +
                   "📅 Build consistency with regular practice";
        } else if (difficulty.contains("intermediate")) {
            return "💪 Gradually increase weight\n" +
                   "🔄 Add variations to challenge yourself\n" +
                   "⏱️ Focus on tempo and control\n" +
                   "📊 Track your progress\n" +
                   "🎯 Include both strength and endurance";
        } else if (difficulty.contains("advanced")) {
            return "⚡ Use advanced techniques (dropsets, supersets)\n" +
                   "🔥 Increase intensity and volume\n" +
                   "🎯 Focus on weak points\n" +
                   "📈 Periodize your training\n" +
                   "🚀 Consider advanced variations";
        } else {
            return "🌱 Start with proper form\n" +
                   "📈 Gradually increase intensity\n" +
                   "📊 Track your progress\n" +
                   "👂 Listen to your body\n" +
                   "📅 Stay consistent";
        }
    }
    
    private String getPremiumBenefits(Exercise exercise) {
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        
        if (muscleGroup.contains("chest")) {
            return "💪 Builds upper body strength and power\n" +
                   "🏋️ Develops pushing strength\n" +
                   "🔥 Improves muscle definition\n" +
                   "⚡ Enhances athletic performance\n" +
                   "🎯 Increases functional strength";
        } else if (muscleGroup.contains("back")) {
            return "🦴 Improves posture and spinal health\n" +
                   "💪 Builds pulling strength\n" +
                   "🔥 Develops V-taper physique\n" +
                   "⚡ Reduces risk of back pain\n" +
                   "🎯 Enhances grip strength";
        } else if (muscleGroup.contains("leg")) {
            return "🦵 Builds lower body power\n" +
                   "💪 Improves athletic performance\n" +
                   "🔥 Increases muscle mass\n" +
                   "⚡ Enhances bone density\n" +
                   "🎯 Boosts metabolism";
        } else if (muscleGroup.contains("abs")) {
            return "🔥 Develops core strength\n" +
                   "💪 Improves stability\n" +
                   "⚡ Enhances posture\n" +
                   "🎯 Reduces back pain risk\n" +
                   "🏋️ Increases functional movement";
        } else {
            return "💪 Builds muscle strength and endurance\n" +
                   "🔥 Improves muscle definition\n" +
                   "⚡ Enhances athletic performance\n" +
                   "🎯 Increases functional strength\n" +
                   "📈 Boosts overall fitness";
        }
    }
    
    private String getPremiumSafetyTips(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        
        if (exerciseName.contains("bench press")) {
            return "🛡️ Always use a spotter with heavy weights\n" +
                   "🛡️ Ensure proper bench stability\n" +
                   "🛡️ Start with lighter weights to warm up\n" +
                   "🛡️ Keep core tight throughout movement\n" +
                   "🛡️ Don't lock elbows at the top";
        } else if (exerciseName.contains("squat")) {
            return "🛡️ Start with bodyweight to master form\n" +
                   "🛡️ Keep knees tracking over toes\n" +
                   "🛡️ Maintain neutral spine position\n" +
                   "🛡️ Use proper footwear with grip\n" +
                   "🛡️ Progress weight gradually";
        } else if (exerciseName.contains("deadlift")) {
            return "🛡️ Start with lighter weights\n" +
                   "🛡️ Keep bar close to body\n" +
                   "🛡️ Maintain neutral spine\n" +
                   "🛡️ Use proper lifting shoes\n" +
                   "🛡️ Don't round your back";
        } else {
            return "🛡️ Start with lighter weights\n" +
                   "🛡️ Focus on proper form\n" +
                   "🛡️ Warm up before exercising\n" +
                   "🛡️ Listen to your body\n" +
                   "🛡️ Progress gradually";
        }
    }
    
    private String getExerciseIntensity(Exercise exercise) {
        String difficulty = exercise.getDifficulty().toLowerCase();
        if (difficulty.contains("beginner")) return "Moderate";
        if (difficulty.contains("intermediate")) return "High";
        if (difficulty.contains("advanced")) return "Maximum";
        return "Moderate";
    }
    
    private String getMuscleActivationPercentage(Exercise exercise) {
        String difficulty = exercise.getDifficulty().toLowerCase();
        if (difficulty.contains("beginner")) return "85-90%";
        if (difficulty.contains("intermediate")) return "90-95%";
        if (difficulty.contains("advanced")) return "95-98%";
        return "90%";
    }
    
    // ==================== VISUAL STEP-BY-STEP GUIDE SYSTEM ====================
    
    private String getVisualStepGuide(Exercise exercise) {
        return switch (exercise.getName()) {
            // CHEST
            case "Bench Press" -> "🖼️ STEP 1: Starting Position - Lie flat on bench, feet planted firmly on floor\n" +
                   "🖼️ STEP 2: Grip Setup - Grip bar slightly wider than shoulder-width, thumbs wrapped\n" +
                   "🖼️ STEP 3: Unrack - Lift bar off rack with locked arms, position over chest\n" +
                   "🖼️ STEP 4: Lower Phase - Lower bar to mid-chest with controlled descent (2-3 sec)\n" +
                   "🖼️ STEP 5: Press Phase - Press bar up explosively, driving through chest\n" +
                   "🖼️ STEP 6: Lockout - Extend arms fully, maintain scapular retraction throughout";
            
            case "Incline Bench Press" -> "🖼️ STEP 1: Setup - Set bench to 30-45 degree incline, lie back with feet flat\n" +
                   "🖼️ STEP 2: Positioning - Position yourself so bar is above upper chest\n" +
                   "🖼️ STEP 3: Grip - Grasp bar slightly wider than shoulders, unrack safely\n" +
                   "🖼️ STEP 4: Lower - Lower bar to upper chest area below collarbone\n" +
                   "🖼️ STEP 5: Press - Drive bar up toward ceiling, engaging upper chest\n" +
                   "🖼️ STEP 6: Control - Keep elbows at 45-degree angle, avoid flaring wide";
            
            case "Decline Bench Press" -> "🖼️ STEP 1: Setup - Set bench to 15-30 degree decline, secure legs\n" +
                   "🖼️ STEP 2: Position - Lie back with head lower than hips, feet secured\n" +
                   "🖼️ STEP 3: Grip - Grip bar with hands shoulder-width or wider\n" +
                   "🖼️ STEP 4: Lower - Lower bar to lower chest with control\n" +
                   "🖼️ STEP 5: Press - Press up forcefully, focusing on lower pecs\n" +
                   "🖼️ STEP 6: Complete - Lock out arms, maintain tight core throughout";
            
            case "Dumbbell Press" -> "🖼️ STEP 1: Starting - Sit on bench with dumbbells on thighs, lie back carefully\n" +
                   "🖼️ STEP 2: Position - Hold dumbbells at chest level, palms forward, elbows bent 90°\n" +
                   "🖼️ STEP 3: Setup - Plant feet firmly, retract shoulder blades\n" +
                   "🖼️ STEP 4: Press - Press dumbbells up and slightly together\n" +
                   "🖼️ STEP 5: Peak - Dumbbells nearly touch at top, squeeze chest\n" +
                   "🖼️ STEP 6: Lower - Control dumbbells back to starting position";
            
            case "Incline Dumbbell Press" -> "🖼️ STEP 1: Setup - Set bench to 30-45 degrees, sit with dumbbells on thighs\n" +
                   "🖼️ STEP 2: Position - Lie back, bringing dumbbells to shoulders\n" +
                   "🖼️ STEP 3: Starting Position - Dumbbells at upper chest, elbows at 45°\n" +
                   "🖼️ STEP 4: Press Up - Drive dumbbells up and slightly inward\n" +
                   "🖼️ STEP 5: Top Position - Fully extend arms, don't clang dumbbells\n" +
                   "🖼️ STEP 6: Lower - Control descent back to shoulder level";
            
            case "Chest Fly" -> "🖼️ STEP 1: Setup - Lie flat on bench with dumbbells above chest\n" +
                   "🖼️ STEP 2: Starting Position - Arms extended, slight elbow bend, palms facing\n" +
                   "🖼️ STEP 3: Arc Motion - Lower dumbbells out to sides in wide arc\n" +
                   "🖼️ STEP 4: Stretch - Feel deep stretch in chest, don't go past shoulder level\n" +
                   "🖼️ STEP 5: Squeeze - Bring dumbbells together using chest, not arms\n" +
                   "🖼️ STEP 6: Peak Contraction - Touch dumbbells at top, squeeze chest hard";
            
            case "Cable Fly" -> "🖼️ STEP 1: Setup - Set pulleys to chest height, grab handles with palms forward\n" +
                   "🖼️ STEP 2: Position - Step forward, lean slightly, one foot forward for balance\n" +
                   "🖼️ STEP 3: Starting - Arms extended to sides, slight elbow bend\n" +
                   "🖼️ STEP 4: Fly Motion - Bring handles together in front of chest\n" +
                   "🖼️ STEP 5: Squeeze - Cross hands slightly at midline, contract chest\n" +
                   "🖼️ STEP 6: Return - Control cables back to starting position with tension";
            
            case "Push-ups" -> "🖼️ STEP 1: Setup - Start in plank position, hands shoulder-width apart\n" +
                   "🖼️ STEP 2: Alignment - Body forms straight line from head to heels\n" +
                   "🖼️ STEP 3: Core - Engage core and glutes, don't let hips sag\n" +
                   "🖼️ STEP 4: Lower - Lower chest toward ground, elbows at 45°\n" +
                   "🖼️ STEP 5: Bottom - Chest nearly touches floor, hold briefly\n" +
                   "🖼️ STEP 6: Push - Press through palms to return to start";
            
            case "Chest Dips" -> "🖼️ STEP 1: Setup - Grip parallel bars, jump up to support position\n" +
                   "🖼️ STEP 2: Starting Position - Arms locked, body vertical, feet crossed\n" +
                   "🖼️ STEP 3: Lean Forward - Lean torso forward 20-30 degrees for chest focus\n" +
                   "🖼️ STEP 4: Descent - Lower body by bending elbows, go until chest stretch\n" +
                   "🖼️ STEP 5: Bottom - Shoulders slightly below elbows, don't bounce\n" +
                   "🖼️ STEP 6: Press Up - Push through palms, return to starting position";
            
            // BACK
            case "Deadlifts" -> "🖼️ STEP 1: Setup - Bar over mid-foot, feet hip-width apart, toes slightly out\n" +
                   "🖼️ STEP 2: Grip - Bend at hips, grip bar just outside legs, mixed or double overhand\n" +
                   "🖼️ STEP 3: Position - Drop hips, chest up, shoulders over bar, neutral spine\n" +
                   "🖼️ STEP 4: Brace - Take big breath, brace core, engage lats\n" +
                   "🖼️ STEP 5: Lift - Drive through floor, extend hips and knees simultaneously\n" +
                   "🖼️ STEP 6: Lockout - Stand tall, shoulders back, then lower with control";
            
            case "Barbell Row" -> "🖼️ STEP 1: Setup - Stand with feet hip-width, bend at hips to 45°\n" +
                   "🖼️ STEP 2: Grip - Grip bar slightly wider than shoulders, overhand grip\n" +
                   "🖼️ STEP 3: Position - Keep back straight, chest up, knees slightly bent\n" +
                   "🖼️ STEP 4: Pull - Pull bar to lower chest/upper abdomen\n" +
                   "🖼️ STEP 5: Squeeze - Retract shoulder blades, hold for 1 second\n" +
                   "🖼️ STEP 6: Lower - Control bar back down, fully extend arms";
            
            case "Pull-ups" -> "🖼️ STEP 1: Grip - Hang from bar with overhand grip, hands shoulder-width\n" +
                   "🖼️ STEP 2: Dead Hang - Arms fully extended, shoulders engaged\n" +
                   "🖼️ STEP 3: Initiate - Pull shoulder blades down and together\n" +
                   "🖼️ STEP 4: Pull - Drive elbows down, pull chin above bar\n" +
                   "🖼️ STEP 5: Top - Chest near bar, squeeze back muscles\n" +
                   "🖼️ STEP 6: Lower - Control descent back to dead hang";
            
            case "Lat Pulldown" -> "🖼️ STEP 1: Setup - Sit at machine, adjust thigh pad, grab bar wide\n" +
                   "🖼️ STEP 2: Position - Lean back slightly (10-15°), chest up\n" +
                   "🖼️ STEP 3: Initiate - Pull shoulder blades down, engage lats\n" +
                   "🖼️ STEP 4: Pull - Pull bar down to upper chest, drive elbows down\n" +
                   "🖼️ STEP 5: Squeeze - Hold contraction, feel lats working\n" +
                   "🖼️ STEP 6: Release - Control bar back up, full arm extension";
            
            case "Seated Cable Row" -> "🖼️ STEP 1: Setup - Sit at cable machine, feet on platform, knees bent\n" +
                   "🖼️ STEP 2: Grip - Grasp handle with both hands, arms extended\n" +
                   "🖼️ STEP 3: Position - Sit upright, slight lean back, chest out\n" +
                   "🖼️ STEP 4: Pull - Pull handle to lower chest, keep elbows close\n" +
                   "🖼️ STEP 5: Squeeze - Retract shoulder blades maximally, pause\n" +
                   "🖼️ STEP 6: Return - Extend arms forward with control, feel stretch";
            
            case "T-Bar Row" -> "🖼️ STEP 1: Setup - Straddle bar, feet shoulder-width apart\n" +
                   "🖼️ STEP 2: Grip - Bend at hips, grip handles with both hands\n" +
                   "🖼️ STEP 3: Position - Back straight, chest up, knees bent\n" +
                   "🖼️ STEP 4: Pull - Pull handles to chest, lead with elbows\n" +
                   "🖼️ STEP 5: Squeeze - Retract shoulder blades, hold peak\n" +
                   "🖼️ STEP 6: Lower - Lower weight with control, arms fully extended";
            
            case "One-Arm Dumbbell Row" -> "🖼️ STEP 1: Setup - Place knee and hand on bench, other foot on floor\n" +
                   "🖼️ STEP 2: Position - Back parallel to ground, dumbbell hanging straight down\n" +
                   "🖼️ STEP 3: Brace - Engage core, keep neutral spine\n" +
                   "🖼️ STEP 4: Pull - Pull dumbbell to hip, lead with elbow\n" +
                   "🖼️ STEP 5: Squeeze - Retract shoulder blade, hold briefly\n" +
                   "🖼️ STEP 6: Lower - Control dumbbell back down, full extension";
            
            // SHOULDERS
            case "Overhead Press" -> "🖼️ STEP 1: Setup - Stand with feet shoulder-width, bar at collarbone\n" +
                   "🖼️ STEP 2: Grip - Grip bar slightly wider than shoulders, elbows forward\n" +
                   "🖼️ STEP 3: Brace - Take breath, brace core, squeeze glutes\n" +
                   "🖼️ STEP 4: Press - Press bar straight up past face\n" +
                   "🖼️ STEP 5: Lockout - Lock arms overhead, shrug at top\n" +
                   "🖼️ STEP 6: Lower - Lower bar to collarbone with control";
            
            case "Dumbbell Shoulder Press" -> "🖼️ STEP 1: Setup - Sit or stand with dumbbells at shoulder height\n" +
                   "🖼️ STEP 2: Position - Palms forward, elbows at 90°, core tight\n" +
                   "🖼️ STEP 3: Brace - Engage core and glutes for stability\n" +
                   "🖼️ STEP 4: Press - Press dumbbells straight up overhead\n" +
                   "🖼️ STEP 5: Top - Extend arms fully, dumbbells nearly touch\n" +
                   "🖼️ STEP 6: Lower - Control dumbbells back to shoulders";
            
            case "Lateral Raise" -> "🖼️ STEP 1: Setup - Stand with feet hip-width, dumbbells at sides\n" +
                   "🖼️ STEP 2: Position - Slight bend in elbows, lean forward 10°\n" +
                   "🖼️ STEP 3: Initiate - Keep core tight, shoulders down\n" +
                   "🖼️ STEP 4: Raise - Lift dumbbells to sides until arms parallel\n" +
                   "🖼️ STEP 5: Peak - Hold briefly at shoulder height\n" +
                   "🖼️ STEP 6: Lower - Control dumbbells back down slowly";
            
            case "Front Raise" -> "🖼️ STEP 1: Setup - Stand with feet hip-width, dumbbells at thighs\n" +
                   "🖼️ STEP 2: Grip - Palms facing down or toward each other\n" +
                   "🖼️ STEP 3: Position - Slight elbow bend, engage core\n" +
                   "🖼️ STEP 4: Raise - Lift dumbbells forward to shoulder height\n" +
                   "🖼️ STEP 5: Top - Arms parallel to ground, don't swing\n" +
                   "🖼️ STEP 6: Lower - Lower with control back to thighs";
            
            case "Rear Delt Fly" -> "🖼️ STEP 1: Setup - Hinge at hips to 45°, dumbbells hanging down\n" +
                   "🖼️ STEP 2: Position - Chest up, back flat, slight elbow bend\n" +
                   "🖼️ STEP 3: Brace - Engage core, neutral neck position\n" +
                   "🖼️ STEP 4: Fly - Raise dumbbells out to sides, lead with elbows\n" +
                   "🖼️ STEP 5: Squeeze - Retract shoulder blades, feel rear delts\n" +
                   "🖼️ STEP 6: Lower - Control dumbbells back to hanging position";
            
            case "Face Pull" -> "🖼️ STEP 1: Setup - Set cable at face height, attach rope\n" +
                   "🖼️ STEP 2: Grip - Grip rope with overhand grip, step back for tension\n" +
                   "🖼️ STEP 3: Position - Feet staggered, slight lean back\n" +
                   "🖼️ STEP 4: Pull - Pull rope toward face, elbows flare out high\n" +
                   "🖼️ STEP 5: Peak - Hands by ears, squeeze shoulder blades\n" +
                   "🖼️ STEP 6: Return - Control rope back to starting position";
            
            case "Arnold Press" -> "🖼️ STEP 1: Setup - Sit with dumbbells at chest, palms facing you\n" +
                   "🖼️ STEP 2: Starting - Dumbbells in front of shoulders, like top of curl\n" +
                   "🖼️ STEP 3: Rotate - Begin press while rotating palms forward\n" +
                   "🖼️ STEP 4: Press - Continue pressing up as palms rotate\n" +
                   "🖼️ STEP 5: Lockout - Arms extended overhead, palms forward\n" +
                   "🖼️ STEP 6: Reverse - Rotate palms back in while lowering";
            
            // BICEPS
            case "Bicep Curls" -> "🖼️ STEP 1: Setup - Stand with feet hip-width, dumbbells at sides\n" +
                   "🖼️ STEP 2: Position - Palms forward, elbows close to torso\n" +
                   "🖼️ STEP 3: Stabilize - Keep upper arms stationary, core engaged\n" +
                   "🖼️ STEP 4: Curl - Curl weights up to shoulders, squeeze biceps\n" +
                   "🖼️ STEP 5: Peak - Hold contraction at top for 1 second\n" +
                   "🖼️ STEP 6: Lower - Control weights down to full extension";
            
            case "Barbell Curl" -> "🖼️ STEP 1: Setup - Stand with feet shoulder-width, grip barbell underhand\n" +
                   "🖼️ STEP 2: Position - Bar at thighs, elbows at sides, chest up\n" +
                   "🖼️ STEP 3: Stabilize - Keep shoulders back, avoid swinging\n" +
                   "🖼️ STEP 4: Curl - Curl bar to chest level, elbows stationary\n" +
                   "🖼️ STEP 5: Squeeze - Contract biceps at top, hold briefly\n" +
                   "🖼️ STEP 6: Lower - Lower bar with control, fully extend arms";
            
            case "Hammer Curl" -> "🖼️ STEP 1: Setup - Stand with dumbbells at sides, neutral grip\n" +
                   "🖼️ STEP 2: Position - Palms facing each other (hammer grip)\n" +
                   "🖼️ STEP 3: Stabilize - Keep elbows close, upper arms still\n" +
                   "🖼️ STEP 4: Curl - Curl dumbbells up maintaining neutral grip\n" +
                   "🖼️ STEP 5: Peak - Dumbbells at shoulders, squeeze biceps and forearms\n" +
                   "🖼️ STEP 6: Lower - Control descent back to sides";
            
            case "Preacher Curl" -> "🖼️ STEP 1: Setup - Sit at preacher bench, adjust height properly\n" +
                   "🖼️ STEP 2: Position - Arms over pad, armpits at top of pad\n" +
                   "🖼️ STEP 3: Grip - Grip bar or dumbbells with underhand grip\n" +
                   "🖼️ STEP 4: Curl - Curl weight up, keeping arms on pad\n" +
                   "🖼️ STEP 5: Squeeze - Contract biceps at top of movement\n" +
                   "🖼️ STEP 6: Lower - Lower weight slowly until arms nearly straight";
            
            case "Cable Curl" -> "🖼️ STEP 1: Setup - Stand facing cable machine, attach straight bar\n" +
                   "🖼️ STEP 2: Grip - Grip bar underhand, hands shoulder-width\n" +
                   "🖼️ STEP 3: Position - Step back slightly, elbows at sides\n" +
                   "🖼️ STEP 4: Curl - Curl bar up to shoulders, constant tension\n" +
                   "🖼️ STEP 5: Squeeze - Contract biceps fully at top\n" +
                   "🖼️ STEP 6: Lower - Resist cable back to starting position";
            
            case "Concentration Curl" -> "🖼️ STEP 1: Setup - Sit on bench, spread legs, dumbbell in one hand\n" +
                   "🖼️ STEP 2: Position - Rest elbow on inner thigh, arm hanging straight\n" +
                   "🖼️ STEP 3: Stabilize - Other hand on other knee for support\n" +
                   "🖼️ STEP 4: Curl - Curl dumbbell up focusing solely on bicep\n" +
                   "🖼️ STEP 5: Peak - Bring weight to shoulder, squeeze hard\n" +
                   "🖼️ STEP 6: Lower - Lower slowly with full control and stretch";
            
            // TRICEPS
            case "Tricep Dips" -> "🖼️ STEP 1: Setup - Grip parallel bars, jump to support position\n" +
                   "🖼️ STEP 2: Position - Arms locked, body upright (not leaning)\n" +
                   "🖼️ STEP 3: Descent - Lower body by bending elbows\n" +
                   "🖼️ STEP 4: Depth - Go until upper arms parallel to ground\n" +
                   "🖼️ STEP 5: Press - Push through palms to extend arms\n" +
                   "🖼️ STEP 6: Lockout - Fully extend arms at top, repeat";
            
            case "Close-Grip Bench Press" -> "🖼️ STEP 1: Setup - Lie on bench, grip bar with hands shoulder-width\n" +
                   "🖼️ STEP 2: Position - Hands closer than normal bench press\n" +
                   "🖼️ STEP 3: Unrack - Lift bar off rack, position over chest\n" +
                   "🖼️ STEP 4: Lower - Lower bar to lower chest, elbows close to body\n" +
                   "🖼️ STEP 5: Press - Press bar up, focusing on triceps\n" +
                   "🖼️ STEP 6: Lockout - Extend arms fully, squeeze triceps";
            
            case "Tricep Pushdown" -> "🖼️ STEP 1: Setup - Stand facing cable machine, attach bar or rope\n" +
                   "🖼️ STEP 2: Grip - Grip attachment, elbows at sides\n" +
                   "🖼️ STEP 3: Position - Lean forward slightly, core engaged\n" +
                   "🖼️ STEP 4: Push - Push bar/rope down until arms fully extended\n" +
                   "🖼️ STEP 5: Squeeze - Contract triceps at bottom, hold\n" +
                   "🖼️ STEP 6: Return - Control weight back up, elbows stationary";
            
            case "Overhead Tricep Extension" -> "🖼️ STEP 1: Setup - Stand or sit with dumbbell held overhead\n" +
                   "🖼️ STEP 2: Grip - Both hands under top plate of dumbbell\n" +
                   "🖼️ STEP 3: Position - Arms extended overhead, elbows close to head\n" +
                   "🖼️ STEP 4: Lower - Lower dumbbell behind head, elbows stationary\n" +
                   "🖼️ STEP 5: Stretch - Feel stretch in triceps, don't flare elbows\n" +
                   "🖼️ STEP 6: Extend - Extend arms back to starting position";
            
            case "Skull Crushers" -> "🖼️ STEP 1: Setup - Lie on bench with barbell held above chest\n" +
                   "🖼️ STEP 2: Position - Arms perpendicular to body, hands shoulder-width\n" +
                   "🖼️ STEP 3: Stabilize - Keep upper arms still throughout movement\n" +
                   "🖼️ STEP 4: Lower - Lower bar toward forehead, bend only at elbows\n" +
                   "🖼️ STEP 5: Stretch - Bar near forehead/behind head, feel stretch\n" +
                   "🖼️ STEP 6: Extend - Extend arms back to starting position";
            
            case "Diamond Push-ups" -> "🖼️ STEP 1: Setup - Start in push-up position, hands together\n" +
                   "🖼️ STEP 2: Hand Position - Form diamond shape with index fingers and thumbs\n" +
                   "🖼️ STEP 3: Alignment - Body straight from head to heels\n" +
                   "🖼️ STEP 4: Lower - Lower chest to hands, elbows close to body\n" +
                   "🖼️ STEP 5: Bottom - Chest nearly touches hands\n" +
                   "🖼️ STEP 6: Push - Press up to starting position";
            
            // FOREARMS
            case "Wrist Curls" -> "🖼️ STEP 1: Setup - Sit on bench with forearms on thighs, palms up\n" +
                   "🖼️ STEP 2: Grip - Hold dumbbell or barbell in hands\n" +
                   "🖼️ STEP 3: Position - Wrists hanging over knees, hands relaxed\n" +
                   "🖼️ STEP 4: Curl - Curl wrists upward, flexing forearms\n" +
                   "🖼️ STEP 5: Squeeze - Contract forearms at top of movement\n" +
                   "🖼️ STEP 6: Lower - Lower wrists back down, feel stretch";
            
            case "Reverse Wrist Curls" -> "🖼️ STEP 1: Setup - Sit with forearms on thighs, palms down\n" +
                   "🖼️ STEP 2: Grip - Hold dumbbell or barbell with overhand grip\n" +
                   "🖼️ STEP 3: Position - Wrists hanging over knees\n" +
                   "🖼️ STEP 4: Curl - Extend wrists upward toward ceiling\n" +
                   "🖼️ STEP 5: Peak - Lift hands as high as possible\n" +
                   "🖼️ STEP 6: Lower - Lower hands back down with control";
            
            case "Farmer's Walk" -> "🖼️ STEP 1: Setup - Select heavy dumbbells or kettlebells\n" +
                   "🖼️ STEP 2: Grip - Grip weights firmly at sides, stand tall\n" +
                   "🖼️ STEP 3: Posture - Chest up, shoulders back, core tight\n" +
                   "🖼️ STEP 4: Walk - Walk forward with controlled steps\n" +
                   "🖼️ STEP 5: Maintain - Keep weights from swinging, good posture\n" +
                   "🖼️ STEP 6: Complete - Walk for distance or time, set weights down safely";
            
            // ABS
            case "Planks" -> "🖼️ STEP 1: Setup - Start on forearms and toes\n" +
                   "🖼️ STEP 2: Alignment - Elbows under shoulders, body straight line\n" +
                   "🖼️ STEP 3: Engage - Tighten abs, squeeze glutes\n" +
                   "🖼️ STEP 4: Position - Don't let hips sag or pike up\n" +
                   "🖼️ STEP 5: Hold - Maintain position, breathe steadily\n" +
                   "🖼️ STEP 6: Endure - Hold for prescribed time, keep form perfect";
            
            case "Crunches" -> "🖼️ STEP 1: Setup - Lie on back, knees bent, feet flat on floor\n" +
                   "🖼️ STEP 2: Hand Position - Hands behind head or across chest\n" +
                   "🖼️ STEP 3: Prepare - Press lower back to floor, engage abs\n" +
                   "🖼️ STEP 4: Crunch - Lift shoulders off ground, curl toward knees\n" +
                   "🖼️ STEP 5: Squeeze - Contract abs at top, hold 1 second\n" +
                   "🖼️ STEP 6: Lower - Lower shoulders back down with control";
            
            case "Leg Raises" -> "🖼️ STEP 1: Setup - Lie flat on back, hands under hips or at sides\n" +
                   "🖼️ STEP 2: Position - Legs extended, feet together\n" +
                   "🖼️ STEP 3: Engage - Press lower back to floor, tighten core\n" +
                   "🖼️ STEP 4: Raise - Lift legs to vertical position, keep them straight\n" +
                   "🖼️ STEP 5: Top - Legs perpendicular to floor, squeeze abs\n" +
                   "🖼️ STEP 6: Lower - Lower legs slowly without touching floor";
            
            case "Russian Twists" -> "🖼️ STEP 1: Setup - Sit with knees bent, feet elevated off floor\n" +
                   "🖼️ STEP 2: Position - Lean back 45°, hold weight at chest\n" +
                   "🖼️ STEP 3: Balance - Engage core for stability\n" +
                   "🖼️ STEP 4: Twist Right - Rotate torso to right, touch weight to floor\n" +
                   "🖼️ STEP 5: Twist Left - Rotate to left side, touch weight down\n" +
                   "🖼️ STEP 6: Continue - Alternate sides with controlled movement";
            
            case "Cable Crunch" -> "🖼️ STEP 1: Setup - Kneel facing cable machine, rope attachment overhead\n" +
                   "🖼️ STEP 2: Grip - Hold rope beside head/neck area\n" +
                   "🖼️ STEP 3: Position - Hips stationary, slight forward lean\n" +
                   "🖼️ STEP 4: Crunch - Crunch down, bringing elbows toward knees\n" +
                   "🖼️ STEP 5: Squeeze - Contract abs maximally at bottom\n" +
                   "🖼️ STEP 6: Return - Control return to starting position";
            
            case "Hanging Knee Raise" -> "🖼️ STEP 1: Setup - Hang from pull-up bar, overhand grip\n" +
                   "🖼️ STEP 2: Position - Dead hang, shoulders engaged\n" +
                   "🖼️ STEP 3: Stabilize - Minimize swinging, control body\n" +
                   "🖼️ STEP 4: Raise - Lift knees toward chest, curl pelvis up\n" +
                   "🖼️ STEP 5: Peak - Knees at chest level, squeeze abs\n" +
                   "🖼️ STEP 6: Lower - Lower legs with control to dead hang";
            
            // OBLIQUES
            case "Side Plank" -> "🖼️ STEP 1: Setup - Lie on side, forearm on ground, elbow under shoulder\n" +
                   "🖼️ STEP 2: Lift - Lift hips off ground, body in straight line\n" +
                   "🖼️ STEP 3: Alignment - Feet stacked or staggered, free arm up or on hip\n" +
                   "🖼️ STEP 4: Engage - Tighten obliques, squeeze glutes\n" +
                   "🖼️ STEP 5: Hold - Maintain position without letting hips sag\n" +
                   "🖼️ STEP 6: Breathe - Hold for time, then switch sides";
            
            case "Bicycle Crunches" -> "🖼️ STEP 1: Setup - Lie on back, hands behind head, legs elevated\n" +
                   "🖼️ STEP 2: Position - Lift shoulders off ground, engage core\n" +
                   "🖼️ STEP 3: Right Twist - Bring right elbow to left knee\n" +
                   "🖼️ STEP 4: Extend - Extend right leg straight while twisting\n" +
                   "🖼️ STEP 5: Left Twist - Bring left elbow to right knee\n" +
                   "🖼️ STEP 6: Continue - Alternate sides in cycling motion";
            
            case "Wood Chops" -> "🖼️ STEP 1: Setup - Stand sideways to cable machine, handle at shoulder height\n" +
                   "🖼️ STEP 2: Grip - Grasp handle with both hands, arms extended\n" +
                   "🖼️ STEP 3: Position - Feet wider than shoulders, knees soft\n" +
                   "🖼️ STEP 4: Chop - Pull cable diagonally across body\n" +
                   "🖼️ STEP 5: Rotate - Rotate torso, pivot back foot\n" +
                   "🖼️ STEP 6: Return - Control cable back to start, switch sides";
            
            // LOWER BACK
            case "Back Extensions" -> "🖼️ STEP 1: Setup - Position on hyperextension bench, ankles secured\n" +
                   "🖼️ STEP 2: Starting - Cross arms over chest, bend forward at waist\n" +
                   "🖼️ STEP 3: Position - Torso perpendicular to legs at start\n" +
                   "🖼️ STEP 4: Extend - Raise torso up until body forms straight line\n" +
                   "🖼️ STEP 5: Squeeze - Contract lower back and glutes at top\n" +
                   "🖼️ STEP 6: Lower - Lower torso back down with control";
            
            case "Good Mornings" -> "🖼️ STEP 1: Setup - Place bar on upper back, feet shoulder-width\n" +
                   "🖼️ STEP 2: Position - Stand tall, slight knee bend\n" +
                   "🖼️ STEP 3: Hinge - Push hips back, hinge at waist\n" +
                   "🖼️ STEP 4: Depth - Lower until torso near parallel to ground\n" +
                   "🖼️ STEP 5: Stretch - Feel stretch in hamstrings, keep back straight\n" +
                   "🖼️ STEP 6: Return - Drive hips forward to return to standing";
            
            case "Superman" -> "🖼️ STEP 1: Setup - Lie face down on floor, arms extended overhead\n" +
                   "🖼️ STEP 2: Position - Legs straight, forehead on ground\n" +
                   "🖼️ STEP 3: Engage - Engage core and glutes\n" +
                   "🖼️ STEP 4: Lift - Simultaneously lift arms, chest, and legs off ground\n" +
                   "🖼️ STEP 5: Hold - Extend as if flying, hold for 2-3 seconds\n" +
                   "🖼️ STEP 6: Lower - Lower back down with control, repeat";
            
            // GLUTES
            case "Hip Thrusts" -> "🖼️ STEP 1: Setup - Sit on ground, upper back against bench\n" +
                   "🖼️ STEP 2: Position - Place barbell over hips, feet flat, knees bent\n" +
                   "🖼️ STEP 3: Brace - Engage core, chin tucked\n" +
                   "🖼️ STEP 4: Thrust - Drive through heels, lift hips up\n" +
                   "🖼️ STEP 5: Top - Body forms straight line from shoulders to knees\n" +
                   "🖼️ STEP 6: Lower - Lower hips back down, repeat";
            
            case "Glute Bridges" -> "🖼️ STEP 1: Setup - Lie on back, knees bent, feet flat near glutes\n" +
                   "🖼️ STEP 2: Position - Arms at sides, palms down\n" +
                   "🖼️ STEP 3: Engage - Brace core, squeeze glutes\n" +
                   "🖼️ STEP 4: Lift - Drive through heels, lift hips toward ceiling\n" +
                   "🖼️ STEP 5: Top - Body straight from shoulders to knees, squeeze\n" +
                   "🖼️ STEP 6: Lower - Lower hips back down with control";
            
            case "Bulgarian Split Squats" -> "🖼️ STEP 1: Setup - Stand facing away from bench, one foot elevated behind\n" +
                   "🖼️ STEP 2: Position - Front foot forward enough for deep lunge\n" +
                   "🖼️ STEP 3: Balance - Hold dumbbells at sides, stand tall\n" +
                   "🖼️ STEP 4: Lower - Lower back knee toward ground\n" +
                   "🖼️ STEP 5: Depth - Front thigh parallel to ground\n" +
                   "🖼️ STEP 6: Drive - Push through front heel to stand";
            
            case "Cable Kickbacks" -> "🖼️ STEP 1: Setup - Attach ankle cuff to cable, face machine\n" +
                   "🖼️ STEP 2: Position - Hold machine for balance, slight forward lean\n" +
                   "🖼️ STEP 3: Starting - Working leg slightly back, tension on cable\n" +
                   "🖼️ STEP 4: Kick - Extend leg back, squeezing glutes\n" +
                   "🖼️ STEP 5: Squeeze - Contract glute maximally at full extension\n" +
                   "🖼️ STEP 6: Return - Control leg back to starting position";
            
            // QUADRICEPS
            case "Squats" -> "🖼️ STEP 1: Setup - Stand with feet shoulder-width apart, toes slightly out\n" +
                   "🖼️ STEP 2: Bar Position - Bar on upper back (high bar) or rear delts (low bar)\n" +
                   "🖼️ STEP 3: Descent - Push hips back, bend knees, keep chest up\n" +
                   "🖼️ STEP 4: Depth - Lower until thighs parallel or below\n" +
                   "🖼️ STEP 5: Ascent - Drive through heels, extend hips and knees\n" +
                   "🖼️ STEP 6: Lockout - Stand fully upright, repeat";
            
            case "Front Squats" -> "🖼️ STEP 1: Setup - Bar rests on front of shoulders, fingers under bar\n" +
                   "🖼️ STEP 2: Position - Elbows high, feet shoulder-width\n" +
                   "🖼️ STEP 3: Brace - Deep breath, core tight, chest up\n" +
                   "🖼️ STEP 4: Descent - Lower down, keep elbows high, torso upright\n" +
                   "🖼️ STEP 5: Depth - Thighs parallel or below, knees forward\n" +
                   "🖼️ STEP 6: Ascent - Drive up through heels, maintain upright torso";
            
            case "Leg Press" -> "🖼️ STEP 1: Setup - Sit in machine, feet on platform shoulder-width\n" +
                   "🖼️ STEP 2: Position - Back flat against pad, grip handles\n" +
                   "🖼️ STEP 3: Unrack - Press platform up, release safety\n" +
                   "🖼️ STEP 4: Lower - Lower platform by bending knees\n" +
                   "🖼️ STEP 5: Depth - Knees to 90° or slightly below\n" +
                   "🖼️ STEP 6: Press - Press platform back up through heels";
            
            case "Lunges" -> "🖼️ STEP 1: Setup - Stand tall, feet hip-width apart\n" +
                   "🖼️ STEP 2: Step - Take large step forward with one leg\n" +
                   "🖼️ STEP 3: Lower - Lower back knee toward ground\n" +
                   "🖼️ STEP 4: Depth - Both knees at 90°, front knee over ankle\n" +
                   "🖼️ STEP 5: Push - Drive through front heel to return\n" +
                   "🖼️ STEP 6: Alternate - Return to start, repeat with other leg";
            
            case "Leg Extensions" -> "🖼️ STEP 1: Setup - Sit at machine, adjust pad to ankle level\n" +
                   "🖼️ STEP 2: Position - Back against pad, knees at pivot point\n" +
                   "🖼️ STEP 3: Grip - Hold handles at sides\n" +
                   "🖼️ STEP 4: Extend - Extend legs until fully straight\n" +
                   "🖼️ STEP 5: Squeeze - Contract quads at top, hold briefly\n" +
                   "🖼️ STEP 6: Lower - Lower weight with control, don't let plates touch";
            
            case "Walking Lunges" -> "🖼️ STEP 1: Setup - Stand with dumbbells at sides or no weight\n" +
                   "🖼️ STEP 2: Step - Take large step forward with right leg\n" +
                   "🖼️ STEP 3: Lower - Lower until both knees at 90°\n" +
                   "🖼️ STEP 4: Push - Push through front heel\n" +
                   "🖼️ STEP 5: Step Through - Bring back leg forward into next lunge\n" +
                   "🖼️ STEP 6: Continue - Walk forward alternating legs";
            
            // HAMSTRINGS
            case "Romanian Deadlift" -> "🖼️ STEP 1: Setup - Stand with feet hip-width, bar at thighs\n" +
                   "🖼️ STEP 2: Grip - Overhand grip, slightly wider than shoulders\n" +
                   "🖼️ STEP 3: Hinge - Push hips back, slight knee bend, keep back straight\n" +
                   "🖼️ STEP 4: Lower - Lower bar down legs until hamstring stretch\n" +
                   "🖼️ STEP 5: Stretch - Bar at mid-shin level, feel hamstring stretch\n" +
                   "🖼️ STEP 6: Return - Drive hips forward to return to standing";
            
            case "Leg Curls" -> "🖼️ STEP 1: Setup - Lie face down on leg curl machine\n" +
                   "🖼️ STEP 2: Position - Pad on back of ankles, knees just off bench\n" +
                   "🖼️ STEP 3: Grip - Hold handles, keep hips down\n" +
                   "🖼️ STEP 4: Curl - Curl legs up toward glutes\n" +
                   "🖼️ STEP 5: Squeeze - Contract hamstrings at top, hold\n" +
                   "🖼️ STEP 6: Lower - Lower legs with control, full extension";
            
            case "Nordic Curls" -> "🖼️ STEP 1: Setup - Kneel on pad, ankles secured under bar or partner\n" +
                   "🖼️ STEP 2: Position - Knees bent 90°, body upright, arms crossed\n" +
                   "🖼️ STEP 3: Engage - Brace core, squeeze hamstrings and glutes\n" +
                   "🖼️ STEP 4: Lower - Slowly lower torso forward, keeping body straight\n" +
                   "🖼️ STEP 5: Control - Lower as far as possible with control\n" +
                   "🖼️ STEP 6: Return - Push off floor if needed, pull back with hamstrings";
            
            case "Stiff-Leg Deadlift" -> "🖼️ STEP 1: Setup - Stand with feet hip-width, bar at thighs\n" +
                   "🖼️ STEP 2: Position - Knees slightly bent, locked position\n" +
                   "🖼️ STEP 3: Hinge - Push hips back, lower bar down legs\n" +
                   "🖼️ STEP 4: Depth - Lower until deep hamstring stretch\n" +
                   "🖼️ STEP 5: Stretch - Keep legs nearly straight throughout\n" +
                   "🖼️ STEP 6: Return - Contract hamstrings and glutes to stand";
            
            // CALVES
            case "Standing Calf Raise" -> "🖼️ STEP 1: Setup - Stand at calf raise machine, shoulders under pads\n" +
                   "🖼️ STEP 2: Position - Balls of feet on platform, heels hanging off\n" +
                   "🖼️ STEP 3: Starting - Start with heels below toes, stretch calves\n" +
                   "🖼️ STEP 4: Raise - Push through balls of feet, raise heels high\n" +
                   "🖼️ STEP 5: Peak - Rise as high as possible, squeeze calves\n" +
                   "🖼️ STEP 6: Lower - Lower heels below platform level, full stretch";
            
            case "Seated Calf Raise" -> "🖼️ STEP 1: Setup - Sit at seated calf machine, balls of feet on platform\n" +
                   "🖼️ STEP 2: Position - Place pads on thighs, release safety\n" +
                   "🖼️ STEP 3: Starting - Heels below toes, calves stretched\n" +
                   "🖼️ STEP 4: Raise - Press through balls of feet, lift heels\n" +
                   "🖼️ STEP 5: Squeeze - Raise heels as high as possible\n" +
                   "🖼️ STEP 6: Lower - Lower heels for full stretch";
            
            case "Jump Rope" -> "🖼️ STEP 1: Setup - Hold rope handles, rope behind heels\n" +
                   "🖼️ STEP 2: Position - Stand tall, elbows close to sides\n" +
                   "🖼️ STEP 3: Swing - Rotate rope forward with wrists\n" +
                   "🖼️ STEP 4: Jump - Jump as rope approaches feet\n" +
                   "🖼️ STEP 5: Land - Land on balls of feet, knees soft\n" +
                   "🖼️ STEP 6: Continue - Maintain rhythm, quick repetitions";
            
            // NECK
            case "Neck Curls" -> "🖼️ STEP 1: Setup - Lie on back on bench, head hanging off end\n" +
                   "🖼️ STEP 2: Position - Place hands on forehead for resistance\n" +
                   "🖼️ STEP 3: Starting - Let head hang back gently\n" +
                   "🖼️ STEP 4: Curl - Lift head toward chest against resistance\n" +
                   "🖼️ STEP 5: Flex - Bring chin toward chest, squeeze\n" +
                   "🖼️ STEP 6: Lower - Lower head back down with control";
            
            case "Neck Extensions" -> "🖼️ STEP 1: Setup - Lie face down on bench, head hanging off end\n" +
                   "🖼️ STEP 2: Position - Place hands on back of head for resistance\n" +
                   "🖼️ STEP 3: Starting - Let head hang down gently\n" +
                   "🖼️ STEP 4: Extend - Lift head up against resistance\n" +
                   "🖼️ STEP 5: Peak - Look straight ahead at top position\n" +
                   "🖼️ STEP 6: Lower - Lower head back down with control";
            
            default -> "🖼️ STEP 1: Starting Position - Set up in proper stance with correct alignment\n" +
                   "🖼️ STEP 2: Preparation - Engage core and stabilize body position\n" +
                   "🖼️ STEP 3: Execution - Perform movement with control and proper form\n" +
                   "🖼️ STEP 4: Peak Contraction - Hold briefly at peak of movement\n" +
                   "🖼️ STEP 5: Return - Control back to starting position smoothly\n" +
                   "🖼️ STEP 6: Repeat - Maintain perfect form throughout all repetitions";
        };
    }
    
    // ==================== EXERCISE IMAGE LOADING SYSTEM ====================
    
    private void loadExerciseStepImages(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        
        // Create image paths for step-by-step guide
        List<String> stepImagePaths = getExerciseStepImagePaths(exerciseName, muscleGroup);
        
        // Display images if they exist, otherwise show placeholder
        displayExerciseStepImages(stepImagePaths, exercise);
    }
    
    private List<String> getExerciseStepImagePaths(String exerciseName, String muscleGroup) {
        List<String> imagePaths = new ArrayList<>();
        
        // Generate step image paths based on exercise
        String basePath = "/images/exercises/" + muscleGroup + "/" + 
                         exerciseName.replaceAll("\\s+", "-") + "/";
        
        for (int i = 1; i <= 6; i++) {
            imagePaths.add(basePath + "step-" + i + ".jpg");
        }
        
        return imagePaths;
    }
    
    private void displayExerciseStepImages(List<String> imagePaths, Exercise exercise) {
        if (stepImagesPanel == null) {
            System.err.println("[-] stepImagesPanel is null!");
            return;
        }
        
        // Clear previous images
        stepImagesPanel.getChildren().clear();
        
        // Add title with better styling
        Label titleLabel = new Label("📸 Step-by-Step Visual Guide");
        titleLabel.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #00d4ff; " +
            "-fx-padding: 5 0 15 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.4), 3, 0, 0, 1);"
        );
        stepImagesPanel.getChildren().add(titleLabel);
        
        // Create grid for images (3 columns) with better spacing
        GridPane imageGrid = new GridPane();
        imageGrid.setHgap(12);
        imageGrid.setVgap(12);
        imageGrid.setAlignment(javafx.geometry.Pos.CENTER);
        imageGrid.setStyle("-fx-padding: 5px;");
        
        int loadedCount = 0;
        System.out.println("[*] Loading step images for " + exercise.getName() + ":");
        
        for (int i = 0; i < imagePaths.size() && i < 6; i++) {
            String path = imagePaths.get(i);
            System.out.println("   [*] " + path);
            
            try {
                java.io.InputStream stream = getClass().getResourceAsStream(path);
                if (stream != null) {
                    Image image = new Image(stream);
                    stream.close();
                    
                    if (!image.isError()) {
                        // Create compact image container
                        VBox imageContainer = new VBox(8);
                        imageContainer.setAlignment(javafx.geometry.Pos.CENTER);
                        imageContainer.setStyle(
                            "-fx-background-color: #2b2b2b; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-border-color: #00d4ff; " +
                            "-fx-border-width: 1.5px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-padding: 8px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.2), 8, 0, 0, 3);"
                        );
                        
                        // Step number label - compact
                        Label stepLabel = new Label("STEP " + (i + 1));
                        stepLabel.setStyle(
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #00d4ff; " +
                            "-fx-padding: 3px;"
                        );
                        
                        // Image view - optimized smaller size
                        ImageView imageView = new ImageView(image);
                        imageView.setFitWidth(150);
                        imageView.setFitHeight(110);
                        imageView.setPreserveRatio(true);
                        imageView.setSmooth(true);
                        imageView.setStyle(
                            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.3), 5, 0, 0, 2);"
                        );
                        
                        imageContainer.getChildren().addAll(stepLabel, imageView);
                        
                        // Add to grid (3 columns)
                        int col = i % 3;
                        int row = i / 3;
                        imageGrid.add(imageContainer, col, row);
                        
                        loadedCount++;
                        System.out.println("   [+] Image loaded and displayed: step-" + (i + 1));
                    } else {
                        System.out.println("   [-] Image error for: " + path);
                    }
                } else {
                    System.out.println("   [-] Image not found: " + path);
                }
            } catch (Exception e) {
                System.out.println("   [-] Error loading image: " + e.getMessage());
            }
        }
        
        if (loadedCount > 0) {
            stepImagesPanel.getChildren().add(imageGrid);
            stepImagesPanel.setVisible(true);
            stepImagesPanel.setManaged(true);
            System.out.println("[+] Displayed " + loadedCount + " step images");
        } else {
            stepImagesPanel.setVisible(false);
            stepImagesPanel.setManaged(false);
            System.out.println("[-] No images loaded for display");
        }
    }
    
    // ==================== EXERCISE STEP IMAGE PANEL CREATION ====================
    
    private VBox createExerciseStepImagePanel(Exercise exercise) {
        VBox imagePanel = new VBox(10);
        imagePanel.setPadding(new Insets(15));
        imagePanel.setStyle(
            "-fx-background-color: rgba(20, 20, 20, 0.95); " +
                        "-fx-background-radius: 15px; " +
                        "-fx-border-color: #00d4ff; " +
                        "-fx-border-width: 2px; " +
            "-fx-border-radius: 15px;"
        );
        
        // Title
        Label title = new Label("📸 Step-by-Step Visual Guide");
        title.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #00d4ff;"
        );
        imagePanel.getChildren().add(title);
        
        // Create step image grid
        GridPane stepGrid = new GridPane();
        stepGrid.setHgap(10);
        stepGrid.setVgap(10);
        
        // Add placeholder step images
        for (int i = 1; i <= 6; i++) {
            VBox stepContainer = createStepImageContainer(i, exercise);
            int col = (i - 1) % 3;
            int row = (i - 1) / 3;
            stepGrid.add(stepContainer, col, row);
        }
        
        imagePanel.getChildren().add(stepGrid);
        
        return imagePanel;
    }
    
    private VBox createStepImageContainer(int stepNumber, Exercise exercise) {
        VBox container = new VBox(5);
        container.setAlignment(Pos.CENTER);
        
        // Step number label
        Label stepLabel = new Label("STEP " + stepNumber);
        stepLabel.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        // Placeholder image (would be replaced with actual step image)
        Rectangle placeholder = new Rectangle(120, 90);
        placeholder.setFill(Color.rgb(40, 40, 40));
        placeholder.setStroke(Color.rgb(0, 212, 255));
        placeholder.setStrokeWidth(2);
        placeholder.setArcWidth(10);
        placeholder.setArcHeight(10);
        
        // Step description
        Label description = new Label(getStepDescription(stepNumber, exercise));
        description.setStyle(
            "-fx-font-size: 10px; " +
            "-fx-text-fill: #CCCCCC; " +
            "-fx-wrap-text: true;"
        );
        description.setMaxWidth(120);
        
        container.getChildren().addAll(stepLabel, placeholder, description);
        
        return container;
    }
    
    private String getStepDescription(int stepNumber, Exercise exercise) {
        // Map comprehensive exercise-specific step descriptions
        String[] steps = switch (exercise.getName()) {
            case "Bench Press" -> new String[]{"Starting Position", "Grip Setup", "Unrack", "Lower Phase", "Press Phase", "Lockout"};
            case "Incline Bench Press" -> new String[]{"Setup", "Positioning", "Grip", "Lower", "Press", "Control"};
            case "Decline Bench Press" -> new String[]{"Setup", "Position", "Grip", "Lower", "Press", "Complete"};
            case "Dumbbell Press" -> new String[]{"Starting", "Position", "Setup", "Press", "Peak", "Lower"};
            case "Incline Dumbbell Press" -> new String[]{"Setup", "Position", "Starting Position", "Press Up", "Top Position", "Lower"};
            case "Chest Fly" -> new String[]{"Setup", "Starting Position", "Arc Motion", "Stretch", "Squeeze", "Peak Contraction"};
            case "Cable Fly" -> new String[]{"Setup", "Position", "Starting", "Fly Motion", "Squeeze", "Return"};
            case "Push-ups" -> new String[]{"Setup", "Alignment", "Core", "Lower", "Bottom", "Push"};
            case "Chest Dips" -> new String[]{"Setup", "Starting Position", "Lean Forward", "Descent", "Bottom", "Press Up"};
            case "Deadlifts" -> new String[]{"Setup", "Grip", "Position", "Brace", "Lift", "Lockout"};
            case "Barbell Row" -> new String[]{"Setup", "Grip", "Position", "Pull", "Squeeze", "Lower"};
            case "Pull-ups" -> new String[]{"Grip", "Dead Hang", "Initiate", "Pull", "Top", "Lower"};
            case "Lat Pulldown" -> new String[]{"Setup", "Position", "Initiate", "Pull", "Squeeze", "Release"};
            case "Seated Cable Row" -> new String[]{"Setup", "Grip", "Position", "Pull", "Squeeze", "Return"};
            case "T-Bar Row" -> new String[]{"Setup", "Grip", "Position", "Pull", "Squeeze", "Lower"};
            case "One-Arm Dumbbell Row" -> new String[]{"Setup", "Position", "Brace", "Pull", "Squeeze", "Lower"};
            case "Overhead Press" -> new String[]{"Setup", "Grip", "Brace", "Press", "Lockout", "Lower"};
            case "Dumbbell Shoulder Press" -> new String[]{"Setup", "Position", "Brace", "Press", "Top", "Lower"};
            case "Lateral Raise" -> new String[]{"Setup", "Position", "Initiate", "Raise", "Peak", "Lower"};
            case "Front Raise" -> new String[]{"Setup", "Grip", "Position", "Raise", "Top", "Lower"};
            case "Rear Delt Fly" -> new String[]{"Setup", "Position", "Brace", "Fly", "Squeeze", "Lower"};
            case "Face Pull" -> new String[]{"Setup", "Grip", "Position", "Pull", "Peak", "Return"};
            case "Arnold Press" -> new String[]{"Setup", "Starting", "Rotate", "Press", "Lockout", "Reverse"};
            case "Bicep Curls" -> new String[]{"Setup", "Position", "Stabilize", "Curl", "Peak", "Lower"};
            case "Barbell Curl" -> new String[]{"Setup", "Position", "Stabilize", "Curl", "Squeeze", "Lower"};
            case "Hammer Curl" -> new String[]{"Setup", "Position", "Stabilize", "Curl", "Peak", "Lower"};
            case "Preacher Curl" -> new String[]{"Setup", "Position", "Grip", "Curl", "Squeeze", "Lower"};
            case "Cable Curl" -> new String[]{"Setup", "Grip", "Position", "Curl", "Squeeze", "Lower"};
            case "Concentration Curl" -> new String[]{"Setup", "Position", "Stabilize", "Curl", "Peak", "Lower"};
            case "Tricep Dips" -> new String[]{"Setup", "Position", "Descent", "Depth", "Press", "Lockout"};
            case "Close-Grip Bench Press" -> new String[]{"Setup", "Position", "Unrack", "Lower", "Press", "Lockout"};
            case "Tricep Pushdown" -> new String[]{"Setup", "Grip", "Position", "Push", "Squeeze", "Return"};
            case "Overhead Tricep Extension" -> new String[]{"Setup", "Grip", "Position", "Lower", "Stretch", "Extend"};
            case "Skull Crushers" -> new String[]{"Setup", "Position", "Stabilize", "Lower", "Stretch", "Extend"};
            case "Diamond Push-ups" -> new String[]{"Setup", "Hand Position", "Alignment", "Lower", "Bottom", "Push"};
            case "Wrist Curls" -> new String[]{"Setup", "Grip", "Position", "Curl", "Squeeze", "Lower"};
            case "Reverse Wrist Curls" -> new String[]{"Setup", "Grip", "Position", "Curl", "Peak", "Lower"};
            case "Farmer's Walk" -> new String[]{"Setup", "Grip", "Posture", "Walk", "Maintain", "Complete"};
            case "Planks" -> new String[]{"Setup", "Alignment", "Engage", "Position", "Hold", "Endure"};
            case "Crunches" -> new String[]{"Setup", "Hand Position", "Prepare", "Crunch", "Squeeze", "Lower"};
            case "Leg Raises" -> new String[]{"Setup", "Position", "Engage", "Raise", "Top", "Lower"};
            case "Russian Twists" -> new String[]{"Setup", "Position", "Balance", "Twist Right", "Twist Left", "Continue"};
            case "Cable Crunch" -> new String[]{"Setup", "Grip", "Position", "Crunch", "Squeeze", "Return"};
            case "Hanging Knee Raise" -> new String[]{"Setup", "Position", "Stabilize", "Raise", "Peak", "Lower"};
            case "Side Plank" -> new String[]{"Setup", "Lift", "Alignment", "Engage", "Hold", "Breathe"};
            case "Bicycle Crunches" -> new String[]{"Setup", "Position", "Right Twist", "Extend", "Left Twist", "Continue"};
            case "Wood Chops" -> new String[]{"Setup", "Grip", "Position", "Chop", "Rotate", "Return"};
            case "Back Extensions" -> new String[]{"Setup", "Starting", "Position", "Extend", "Squeeze", "Lower"};
            case "Good Mornings" -> new String[]{"Setup", "Position", "Hinge", "Depth", "Stretch", "Return"};
            case "Superman" -> new String[]{"Setup", "Position", "Engage", "Lift", "Hold", "Lower"};
            case "Hip Thrusts" -> new String[]{"Setup", "Position", "Brace", "Thrust", "Top", "Lower"};
            case "Glute Bridges" -> new String[]{"Setup", "Position", "Engage", "Lift", "Top", "Lower"};
            case "Bulgarian Split Squats" -> new String[]{"Setup", "Position", "Balance", "Lower", "Depth", "Drive"};
            case "Cable Kickbacks" -> new String[]{"Setup", "Position", "Starting", "Kick", "Squeeze", "Return"};
            case "Squats" -> new String[]{"Setup", "Bar Position", "Descent", "Depth", "Ascent", "Lockout"};
            case "Front Squats" -> new String[]{"Setup", "Position", "Brace", "Descent", "Depth", "Ascent"};
            case "Leg Press" -> new String[]{"Setup", "Position", "Unrack", "Lower", "Depth", "Press"};
            case "Lunges" -> new String[]{"Setup", "Step", "Lower", "Depth", "Push", "Alternate"};
            case "Leg Extensions" -> new String[]{"Setup", "Position", "Grip", "Extend", "Squeeze", "Lower"};
            case "Walking Lunges" -> new String[]{"Setup", "Step", "Lower", "Push", "Step Through", "Continue"};
            case "Romanian Deadlift" -> new String[]{"Setup", "Grip", "Hinge", "Lower", "Stretch", "Return"};
            case "Leg Curls" -> new String[]{"Setup", "Position", "Grip", "Curl", "Squeeze", "Lower"};
            case "Nordic Curls" -> new String[]{"Setup", "Position", "Engage", "Lower", "Control", "Return"};
            case "Stiff-Leg Deadlift" -> new String[]{"Setup", "Position", "Hinge", "Depth", "Stretch", "Return"};
            case "Standing Calf Raise" -> new String[]{"Setup", "Position", "Starting", "Raise", "Peak", "Lower"};
            case "Seated Calf Raise" -> new String[]{"Setup", "Position", "Starting", "Raise", "Squeeze", "Lower"};
            case "Jump Rope" -> new String[]{"Setup", "Position", "Swing", "Jump", "Land", "Continue"};
            case "Neck Curls" -> new String[]{"Setup", "Position", "Starting", "Curl", "Flex", "Lower"};
            case "Neck Extensions" -> new String[]{"Setup", "Position", "Starting", "Extend", "Peak", "Lower"};
            default -> new String[]{"Setup", "Prepare", "Execute", "Peak", "Return", "Repeat"};
        };
        
        if (stepNumber > 0 && stepNumber <= steps.length) {
            return steps[stepNumber - 1];
        }
        return "Step " + stepNumber;
    }
    
    // ==================== POSTURE AND FORM VISUAL GUIDES ====================
    
    private String getPostureVisualGuide(Exercise exercise) {
        // Comprehensive posture guidance for all 69 exercises
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        String exerciseName = exercise.getName().toLowerCase();
        
        // Specific exercise posture guides
        if (exerciseName.contains("bench press")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Feet flat on ground, shoulder-width apart\n" +
                   "✓ Shoulders retracted and depressed\n" +
                   "✓ Natural arch in lower back\n" +
                   "✓ Core engaged throughout\n" +
                   "✓ Eyes looking at ceiling\n" +
                   "✓ Bar path straight over chest";
        } else if (exerciseName.contains("squat")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Feet shoulder-width, toes slightly out\n" +
                   "✓ Weight distributed through full foot\n" +
                   "✓ Chest up, shoulders back\n" +
                   "✓ Core braced tight\n" +
                   "✓ Knees tracking over toes\n" +
                   "✓ Neutral spine maintained";
        } else if (exerciseName.contains("deadlift")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Bar over mid-foot\n" +
                   "✓ Shoulders slightly in front of bar\n" +
                   "✓ Neutral spine - no rounding\n" +
                   "✓ Chest up, lats engaged\n" +
                   "✓ Core maximally braced\n" +
                   "✓ Hips and shoulders rise together";
        } else if (exerciseName.contains("pull-up") || exerciseName.contains("pulldown")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Shoulders engaged, not relaxed\n" +
                   "✓ Chest up and proud\n" +
                   "✓ Core tight\n" +
                   "✓ Avoid excessive swinging\n" +
                   "✓ Full scapular retraction\n" +
                   "✓ Controlled tempo";
        } else if (exerciseName.contains("row")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Back flat, neutral spine\n" +
                   "✓ Hinge at hips properly\n" +
                   "✓ Core engaged\n" +
                   "✓ Shoulder blades retract fully\n" +
                   "✓ Elbows stay close to body\n" +
                   "✓ No rotation in torso";
        } else if (exerciseName.contains("press") && muscleGroup.contains("shoulder")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Feet shoulder-width apart\n" +
                   "✓ Core tight, glutes engaged\n" +
                   "✓ Avoid excessive back arch\n" +
                   "✓ Press straight overhead\n" +
                   "✓ Full lockout at top\n" +
                   "✓ Controlled descent";
        } else if (exerciseName.contains("curl")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Feet hip-width apart\n" +
                   "✓ Shoulders back and down\n" +
                   "✓ Elbows locked at sides\n" +
                   "✓ No body sway or momentum\n" +
                   "✓ Full range of motion\n" +
                   "✓ Controlled throughout";
        } else if (exerciseName.contains("tricep") || exerciseName.contains("pushdown") || exerciseName.contains("extension")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Upright posture\n" +
                   "✓ Elbows stationary\n" +
                   "✓ Core engaged\n" +
                   "✓ No elbow flaring\n" +
                   "✓ Full extension at bottom\n" +
                   "✓ Controlled movement";
        } else if (exerciseName.contains("lunge")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Torso upright\n" +
                   "✓ Front knee over ankle\n" +
                   "✓ Back knee toward ground\n" +
                   "✓ Core engaged\n" +
                   "✓ Weight through front heel\n" +
                   "✓ Balanced and controlled";
        } else if (exerciseName.contains("plank")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Body in perfect straight line\n" +
                   "✓ Elbows under shoulders\n" +
                   "✓ Core maximally tight\n" +
                   "✓ Glutes squeezed\n" +
                   "✓ Head neutral, looking down\n" +
                   "✓ Steady breathing";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("leg raise")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Lower back pressed to floor\n" +
                   "✓ Neck neutral, not pulled\n" +
                   "✓ Core engaged throughout\n" +
                   "✓ Controlled movement\n" +
                   "✓ Full contraction at peak\n" +
                   "✓ No momentum or swinging";
        } else if (exerciseName.contains("fly")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Slight elbow bend maintained\n" +
                   "✓ Shoulders stable on bench\n" +
                   "✓ Chest up\n" +
                   "✓ Don't go past shoulder level\n" +
                   "✓ Controlled arc motion\n" +
                   "✓ Focus on chest squeeze";
        } else if (exerciseName.contains("raise") && muscleGroup.contains("shoulder")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Slight forward lean\n" +
                   "✓ Slight elbow bend\n" +
                   "✓ No swinging or momentum\n" +
                   "✓ Lead with elbows\n" +
                   "✓ Controlled tempo\n" +
                   "✓ Don't raise past shoulder height";
        } else if (exerciseName.contains("calf")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Balls of feet on platform\n" +
                   "✓ Full stretch at bottom\n" +
                   "✓ Maximum contraction at top\n" +
                   "✓ Controlled movement\n" +
                   "✓ No bouncing\n" +
                   "✓ Core stable";
        } else if (exerciseName.contains("hip thrust") || exerciseName.contains("bridge")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Shoulders on bench/floor\n" +
                   "✓ Feet flat, hip-width\n" +
                   "✓ Knees at 90° at top\n" +
                   "✓ Core braced\n" +
                   "✓ Chin tucked\n" +
                   "✓ Maximum glute squeeze at top";
        } else if (muscleGroup.contains("back") || muscleGroup.contains("lower back")) {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Neutral spine maintained\n" +
                   "✓ Core engaged\n" +
                   "✓ Shoulder blades controlled\n" +
                   "✓ No excessive rounding\n" +
                   "✓ Controlled movement\n" +
                   "✓ Breathe properly";
        } else {
            return "🎯 POSTURE CHECKLIST:\n" +
                   "✓ Maintain proper alignment\n" +
                   "✓ Core engaged throughout\n" +
                   "✓ Controlled movement tempo\n" +
                   "✓ Full range of motion\n" +
                   "✓ Focus on target muscle\n" +
                   "✓ Breathe steadily - don't hold breath";
        }
    }

    private String getDetailedFormInstructions(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        
        if (exerciseName.contains("squat")) {
            return "1. Stand with feet shoulder-width apart\n2. Keep chest up and core tight\n3. Lower down as if sitting in a chair\n4. Go down until thighs are parallel to floor\n5. Drive through heels to return to start";
        } else if (exerciseName.contains("deadlift")) {
            return "1. Stand with feet hip-width apart, bar over mid-foot\n2. Hinge at hips, keeping back straight\n3. Grip bar with hands just outside legs\n4. Drive hips forward while pulling bar up\n5. Stand tall at the top, then lower with control";
        } else if (exerciseName.contains("bench") || exerciseName.contains("press")) {
            return "1. Lie on bench with feet flat on floor\n2. Grip bar slightly wider than shoulders\n3. Lower bar to chest with control\n4. Press up explosively but controlled\n5. Keep core tight throughout movement";
        } else if (exerciseName.contains("curl")) {
            return "1. Stand with feet hip-width apart\n2. Hold weights with palms facing forward\n3. Keep elbows close to body\n4. Curl weights up to shoulders\n5. Lower with control, full range of motion";
        } else if (exerciseName.contains("row")) {
            return "1. Hinge at hips, keep back straight\n2. Pull weight to lower chest/upper abdomen\n3. Squeeze shoulder blades together\n4. Control the weight on the way down\n5. Keep core engaged throughout";
        } else {
            return "Focus on proper form, controlled movement, and full range of motion. Engage your core and maintain good posture throughout the exercise.";
        }
    }

    private String getMuscleActivationDetails(Exercise exercise) {
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        
        if (muscleGroup.contains("chest")) {
            return "Primary: Pectorals Major & Minor\nSecondary: Anterior Deltoids, Triceps\nStabilizers: Core, Serratus Anterior";
        } else if (muscleGroup.contains("back")) {
            return "Primary: Latissimus Dorsi, Rhomboids\nSecondary: Middle Trapezius, Posterior Deltoids\nStabilizers: Core, Lower Back";
        } else if (muscleGroup.contains("shoulder")) {
            return "Primary: Deltoids (All Heads)\nSecondary: Upper Trapezius, Supraspinatus\nStabilizers: Core, Rotator Cuff";
        } else if (muscleGroup.contains("bicep")) {
            return "Primary: Biceps Brachii\nSecondary: Brachialis, Brachioradialis\nStabilizers: Core, Shoulder Girdle";
        } else if (muscleGroup.contains("tricep")) {
            return "Primary: Triceps Brachii\nSecondary: Anconeus\nStabilizers: Core, Shoulder Girdle";
        } else if (muscleGroup.contains("leg") || muscleGroup.contains("quad")) {
            return "Primary: Quadriceps\nSecondary: Glutes, Hamstrings\nStabilizers: Core, Calves";
        } else {
            return "Primary: " + exercise.getMuscleGroup() + "\nFocus on proper muscle engagement and controlled movement";
        }
    }

    private String getCommonMistakes(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        
        if (exerciseName.contains("squat")) {
            return "• Knees caving inward\n• Leaning too far forward\n• Not going deep enough\n• Lifting heels off ground\n• Rushing the movement";
        } else if (exerciseName.contains("deadlift")) {
            return "• Rounding the back\n• Bar drifting away from body\n• Hyperextending at the top\n• Not engaging core\n• Using too much weight";
        } else if (exerciseName.contains("bench") || exerciseName.contains("press")) {
            return "• Bouncing bar off chest\n• Flaring elbows too wide\n• Arching back excessively\n• Not controlling descent\n• Feet not stable";
        } else if (exerciseName.contains("curl")) {
            return "• Swinging the weights\n• Using momentum\n• Not full range of motion\n• Elbows moving forward\n• Rushing the movement";
        } else {
            return "• Using momentum instead of muscle\n• Poor posture and form\n• Not controlling the weight\n• Rushing through reps\n• Not engaging core";
        }
    }

    private String getProgressionTips(Exercise exercise) {
        String difficulty = exercise.getDifficulty().toLowerCase();
        
        if (difficulty.contains("beginner")) {
            return "• Start with bodyweight or light weights\n• Focus on perfect form first\n• Increase reps before weight\n• Master the movement pattern\n• Build consistency with regular practice";
        } else if (difficulty.contains("intermediate")) {
            return "• Gradually increase weight\n• Add variations to challenge yourself\n• Focus on tempo and control\n• Track your progress\n• Include both strength and endurance";
        } else if (difficulty.contains("advanced")) {
            return "• Use advanced techniques (dropsets, supersets)\n• Increase intensity and volume\n• Focus on weak points\n• Periodize your training\n• Consider advanced variations";
        } else {
            return "• Start with proper form\n• Gradually increase intensity\n• Track your progress\n• Listen to your body\n• Stay consistent";
        }
    }

    private String getRecommendedSets(Exercise exercise) {
        String difficulty = exercise.getDifficulty().toLowerCase();
        
        if (difficulty.contains("beginner")) {
            return "2-3 sets";
        } else if (difficulty.contains("intermediate")) {
            return "3-4 sets";
        } else if (difficulty.contains("advanced")) {
            return "4-5 sets";
        } else {
            return "3-4 sets";
        }
    }

    private String getRecommendedReps(Exercise exercise) {
        String difficulty = exercise.getDifficulty().toLowerCase();
        
        if (difficulty.contains("beginner")) {
            return "8-12 reps";
        } else if (difficulty.contains("intermediate")) {
            return "6-10 reps";
        } else if (difficulty.contains("advanced")) {
            return "4-8 reps";
        } else {
            return "8-12 reps";
        }
    }


    // Helper class for muscle regions
    private static class MuscleMapRegion {
        String name;
        double x, y, width, height;
        String color;

        MuscleMapRegion(String name, double x, double y, double width, double height, String color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }
    }

    // Enhanced AI tab content updater
    private void updateAITabContent(String section) {
        if (currentUser == null || aiTab == null) {
            showAlert(Alert.AlertType.INFORMATION, "Login required", "Please login to use AI features.");
            return;
        }
        try {
            // Use the existing aiContentContainer from FXML
            if (aiContentContainer != null) {
                // Initialize AI tab content if not already done
                if (aiContentContainer.getChildren().size() <= 1) { // Only header exists
                    VBox aiContent = createEmbeddedAIContent(section);
                    aiContentContainer.getChildren().add(aiContent);
                } else {
                    // Update content area only
                    updateAIContentArea(section);
                }
                mainTabPane.getSelectionModel().select(aiTab);
                System.out.println("✅ AI tab updated: " + section);
            } else {
                System.err.println("❌ aiContentContainer not found in FXML");
            }
        } catch (Exception e) {
            System.err.println("Failed to update AI tab: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "AI Tab Error", "Failed to load AI content. Please try again.");
        }
    }

    // Create embedded AI content for the tab
    private VBox createEmbeddedAIContent(String initialSection) {
        VBox mainContainer = new VBox(15);
        mainContainer.setStyle("-fx-padding: 15px; -fx-background-color: transparent;");
        
        // AI Action Buttons (No duplicate header - using FXML header)
        HBox buttonRow = new HBox(15);
        buttonRow.setAlignment(Pos.CENTER);
        
        Button planBtn = new Button("📋 Workout Plan");
        planBtn.setStyle("-fx-background-color: #00d4ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 10px; -fx-cursor: hand;");
        planBtn.setOnAction(e -> {
            System.out.println("🤖 AI Plan button clicked");
            updateAIContentArea("Plan");
        });
        
        Button recsBtn = new Button("💡 Recommendations");
        recsBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 10px; -fx-cursor: hand;");
        recsBtn.setOnAction(e -> {
            System.out.println("🤖 AI Recommendations button clicked");
            updateAIContentArea("Recommendations");
        });
        
        Button weakBtn = new Button("🎯 Weaknesses");
        weakBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 10px; -fx-cursor: hand;");
        weakBtn.setOnAction(e -> {
            System.out.println("🤖 AI Weaknesses button clicked");
            updateAIContentArea("Weaknesses");
        });
        
        Button dietBtn = new Button("🥗 Diet Plan");
        dietBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 10px; -fx-cursor: hand;");
        dietBtn.setOnAction(e -> {
            System.out.println("🤖 AI Diet button clicked");
            updateAIContentArea("Diet");
        });
        
        buttonRow.getChildren().addAll(planBtn, recsBtn, weakBtn, dietBtn);
        
        // AI Content Area
        VBox contentArea = new VBox(15);
        contentArea.setStyle("-fx-background-color: #2b2b2b; " +
                           "-fx-padding: 25px; " +
                           "-fx-background-radius: 15px; " +
                           "-fx-min-height: 450px; " +
                           "-fx-border-color: rgba(0,212,255,0.2); " +
                           "-fx-border-width: 1px; " +
                           "-fx-border-radius: 15px; " +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);");
        
        // Add content based on section
        VBox sectionContent;
        switch (initialSection) {
            case "Plan" -> sectionContent = createWorkoutPlanContent();
            case "Recommendations" -> sectionContent = createRecommendationsContent();
            case "Weaknesses" -> sectionContent = createWeaknessesContent();
            case "Diet" -> sectionContent = createRealisticDietPlanForm();
            default -> sectionContent = createWorkoutPlanContent();
        }
        contentArea.getChildren().add(sectionContent);
        
        mainContainer.getChildren().addAll(buttonRow, contentArea);
        
        // Store reference to content area for updates
        mainContainer.getProperties().put("contentArea", contentArea);
        
        return mainContainer;
    }
    
    // Update only the content area without recreating the entire tab
    private void updateAIContentArea(String section) {
        try {
            if (aiContentContainer != null && aiContentContainer.getChildren().size() > 1) {
                // Find the AI content container (should be the last child added to aiContentContainer)
                VBox aiContentMain = (VBox) aiContentContainer.getChildren().get(aiContentContainer.getChildren().size() - 1);
                if (aiContentMain != null) {
                    // Get the content area from the stored properties
                    VBox contentArea = (VBox) aiContentMain.getProperties().get("contentArea");
                    if (contentArea != null) {
                        contentArea.getChildren().clear();
                        
                        // Add content based on section
                        VBox sectionContent;
                        switch (section) {
                            case "Plan" -> sectionContent = createWorkoutPlanContent();
                            case "Recommendations" -> sectionContent = createRecommendationsContent();
                            case "Weaknesses" -> sectionContent = createWeaknessesContent();
                            case "Diet" -> sectionContent = createRealisticDietPlanForm();
                            default -> sectionContent = createWorkoutPlanContent();
                        }
                        contentArea.getChildren().add(sectionContent);
                        System.out.println("✅ AI content area updated: " + section);
                    } else {
                        System.err.println("❌ Content area not found in properties");
                    }
                } else {
                    System.err.println("❌ AI content main container not found");
                }
            } else {
                System.err.println("❌ AI content container has insufficient children: " + 
                    (aiContentContainer != null ? aiContentContainer.getChildren().size() : "null"));
            }
        } catch (Exception e) {
            System.err.println("Failed to update AI content area: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create workout plan content
    private VBox createWorkoutPlanContent() {
        // Create a scrollable container for the workout plan form
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox content = new VBox(25);
        content.setStyle("-fx-padding: 30px; -fx-background-color: #2a2a2a; -fx-background-radius: 15px; -fx-border-color: #00d4ff; -fx-border-width: 2px; -fx-border-radius: 15px;");
        content.setMaxWidth(Double.MAX_VALUE);

        // Title with better styling
        Label title = new Label("📋 AI Workout Plan Generator");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #00d4ff; -fx-padding: 0 0 10px 0;");
        content.getChildren().add(title);

        // Subtitle
        Label subtitleLabel = new Label("Create a personalized workout plan tailored to your goals and schedule");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc; -fx-padding: 0 0 20px 0;");
        content.getChildren().add(subtitleLabel);

        // Create a form container for user preferences
        VBox formContainer = new VBox(20);
        formContainer.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a; -fx-background-radius: 10px;");

        // Workout Preferences Section
        Label preferencesLabel = new Label("🏋️ Workout Preferences");
        preferencesLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00d4ff; -fx-padding: 0 0 15px 0;");
        formContainer.getChildren().add(preferencesLabel);

        HBox preferencesRow = new HBox(20);
        preferencesRow.setStyle("-fx-padding: 10px 0;");

        VBox durationBox = new VBox(5);
        Label durationLabel = new Label("Duration (days):");
        durationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> durationCombo = new ComboBox<>();
        durationCombo.getItems().addAll("3", "5", "7", "10", "14");
        durationCombo.setValue("7");
        durationCombo.setPrefWidth(150);
        UIStyler.styleComboBox(durationCombo);  // ✅ Apply visibility styling
        durationBox.getChildren().addAll(durationLabel, durationCombo);

        VBox sessionBox = new VBox(5);
        Label sessionLabel = new Label("Session Length (min):");
        sessionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> sessionCombo = new ComboBox<>();
        sessionCombo.getItems().addAll("30", "45", "60", "75", "90");
        sessionCombo.setValue("60");
        sessionCombo.setPrefWidth(150);
        UIStyler.styleComboBox(sessionCombo);  // ✅ Apply visibility styling
        sessionBox.getChildren().addAll(sessionLabel, sessionCombo);

        VBox goalBox = new VBox(5);
        Label goalLabel = new Label("Goal:");
        goalLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> goalCombo = new ComboBox<>();
        goalCombo.getItems().addAll("Muscle Gain", "Strength", "Endurance", "Weight Loss", "General Fitness");
        goalCombo.setValue("Muscle Gain");
        goalCombo.setPrefWidth(150);
        UIStyler.styleComboBox(goalCombo);  // ✅ Apply visibility styling
        goalBox.getChildren().addAll(goalLabel, goalCombo);

        preferencesRow.getChildren().addAll(durationBox, sessionBox, goalBox);
        formContainer.getChildren().add(preferencesRow);

        // Generate button with better styling
        Button generateBtn = new Button("🚀 Generate My Workout Plan");
        generateBtn.setStyle(
            "-fx-background-color: #00d4ff;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15px 30px;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.3), 10, 0, 0, 5);"
        );
        generateBtn.setMaxWidth(Double.MAX_VALUE);

        // Results area with better styling
        VBox resultsArea = new VBox(15);
        resultsArea.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a; -fx-background-radius: 10px; -fx-border-color: #555555; -fx-border-width: 1px; -fx-border-radius: 10px;");

        Label statusLabel = new Label("Click 'Generate My Workout Plan' to create your personalized plan");
        statusLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
        
        // Generate plan button action
        generateBtn.setOnAction(e -> {
            System.out.println("🚀 Generate Workout Plan button clicked!");
            
            if (currentUser == null || workoutGenerator == null) {
                showAlert(Alert.AlertType.ERROR, "Service Error", "User not logged in or AI service not available");
                return;
            }
            
            try {
                generateBtn.setDisable(true);
                statusLabel.setText("🤖 Generating your personalized workout plan...");
                
                // Get user selections
                String selectedDuration = durationCombo.getValue();
                String selectedSession = sessionCombo.getValue();
                String selectedGoal = goalCombo.getValue();
                
                System.out.println("🚀 Selected Duration: " + selectedDuration);
                System.out.println("🚀 Selected Session: " + selectedSession);
                System.out.println("🚀 Selected Goal: " + selectedGoal);
                
                // Create async task for plan generation
                Task<IntelligentWorkoutGenerator.WorkoutPlan> generateTask = new Task<>() {
                    @Override
                    protected IntelligentWorkoutGenerator.WorkoutPlan call() throws Exception {
                        return workoutGenerator.generatePlanForUser(
                            currentUser.getId(), 
                            Integer.parseInt(selectedDuration), 
                            Integer.parseInt(selectedSession)
                        );
                    }
                };
                
                generateTask.setOnSucceeded(event -> {
                    IntelligentWorkoutGenerator.WorkoutPlan plan = generateTask.getValue();
                    displayWorkoutPlanResults(plan, resultsArea);
                    statusLabel.setText("✅ Workout plan generated successfully!");
                    generateBtn.setDisable(false);
                });
                
                generateTask.setOnFailed(event -> {
                    showAlert(Alert.AlertType.ERROR, "Generation Error", "Failed to generate workout plan. Please try again.");
                    statusLabel.setText("❌ Error generating plan. Please try again.");
                    generateBtn.setDisable(false);
                });
                
                new Thread(generateTask).start();
                
            } catch (Exception ex) {
                System.err.println("❌ Error generating workout plan: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Generation Error", "Failed to generate workout plan. Please try again.");
                generateBtn.setDisable(false);
            }
        });

        // Add form container and button to main content
        content.getChildren().addAll(formContainer, generateBtn, statusLabel, resultsArea);

        // Set the content as the content of the scroll pane
        scrollPane.setContent(content);
        
        // Create a container to hold the scroll pane
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        
        return container;
    }

    private void displayWorkoutPlanResults(IntelligentWorkoutGenerator.WorkoutPlan plan, VBox container) {
        System.out.println("🖼️ Displaying workout plan results...");
        container.getChildren().clear();

        // Success message
        Label successLabel = new Label("✅ Your personalized workout plan is ready!");
        successLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00d4ff; -fx-padding: 0 0 15px 0;");
        container.getChildren().add(successLabel);

        if (plan != null && !plan.sessions.isEmpty()) {
            Label headerLabel = new Label("🏋️ Your " + plan.sessions.size() + "-day workout plan:");
            headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-padding: 0 0 20px 0;");
            container.getChildren().add(headerLabel);

            for (int i = 0; i < plan.sessions.size(); i++) {
                IntelligentWorkoutGenerator.SessionPlan session = plan.sessions.get(i);
                
                VBox sessionBox = new VBox(15);
                sessionBox.setStyle("-fx-padding: 20px; -fx-background-color: #2a2a2a; -fx-background-radius: 10px; -fx-border-color: #00d4ff; -fx-border-width: 1px; -fx-border-radius: 10px;");
                
                Label sessionLabel = new Label("📅 " + session.name + " (" + session.targetDurationMinutes + " min)");
                sessionLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00d4ff;");
                
                VBox exercisesBox = new VBox(10);
                for (IntelligentWorkoutGenerator.PlannedExercise exercise : session.exercises) {
                    String weightText = exercise.suggestedWeight > 0 ? 
                        String.format("%.1f kg", exercise.suggestedWeight) : "Body weight";
                    
                    Label exerciseLabel = new Label("• " + exercise.exercise.getName() + 
                        ": " + exercise.sets + " sets x " + exercise.targetReps + " reps @ " + weightText);
                    exerciseLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF; -fx-padding: 5px 0;");
                    exercisesBox.getChildren().add(exerciseLabel);
                }
                
                sessionBox.getChildren().addAll(sessionLabel, exercisesBox);
                container.getChildren().add(sessionBox);
            }
        } else {
            Label noPlanLabel = new Label("No workout plan generated. Please try again.");
            noPlanLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF9800; -fx-padding: 20px; -fx-background-color: #2a2a2a; -fx-background-radius: 10px;");
            container.getChildren().add(noPlanLabel);
        }
    }

    // Create recommendations content
    private VBox createRecommendationsContent() {
        // Create a scrollable container for the recommendations form
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox content = new VBox(25);
        content.setStyle("-fx-padding: 30px; -fx-background-color: #2a2a2a; -fx-background-radius: 15px; -fx-border-color: #4CAF50; -fx-border-width: 2px; -fx-border-radius: 15px;");
        content.setMaxWidth(Double.MAX_VALUE);

        // Title with better styling
        Label title = new Label("💡 AI Exercise Recommendations");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-padding: 0 0 10px 0;");
        content.getChildren().add(title);

        // Subtitle
        Label subtitleLabel = new Label("Get personalized exercise suggestions based on your workout history");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc; -fx-padding: 0 0 20px 0;");
        content.getChildren().add(subtitleLabel);

        // Create a form container for user preferences
        VBox formContainer = new VBox(20);
        formContainer.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a; -fx-background-radius: 10px;");

        // Goal Selection Section
        Label goalLabel = new Label("🎯 Fitness Goal");
        goalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-padding: 0 0 15px 0;");
        formContainer.getChildren().add(goalLabel);

        HBox goalRow = new HBox(20);
        goalRow.setStyle("-fx-padding: 10px 0;");

        VBox goalBox = new VBox(5);
        Label goalSelectLabel = new Label("Goal:");
        goalSelectLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> goalCombo = new ComboBox<>();
        goalCombo.getItems().addAll("Hypertrophy", "Strength", "Endurance", "Weight Loss", "General Fitness");
        goalCombo.setValue("Hypertrophy");
        goalCombo.setPrefWidth(200);
        UIStyler.styleComboBox(goalCombo);  // ✅ Apply visibility styling
        goalBox.getChildren().addAll(goalSelectLabel, goalCombo);

        VBox muscleBox = new VBox(5);
        Label muscleLabel = new Label("Muscle Groups:");
        muscleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> muscleCombo = new ComboBox<>();
        muscleCombo.getItems().addAll("All", "Upper Body", "Lower Body", "Core", "Arms", "Legs");
        muscleCombo.setValue("All");
        muscleCombo.setPrefWidth(200);
        UIStyler.styleComboBox(muscleCombo);  // ✅ Apply visibility styling
        muscleBox.getChildren().addAll(muscleLabel, muscleCombo);

        VBox equipmentBox = new VBox(5);
        Label equipmentLabel = new Label("Equipment:");
        equipmentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> equipmentCombo = new ComboBox<>();
        equipmentCombo.getItems().addAll("All", "Bodyweight", "Dumbbells", "Barbell", "Machines", "Cables");
        equipmentCombo.setValue("All");
        equipmentCombo.setPrefWidth(200);
        UIStyler.styleComboBox(equipmentCombo);  // ✅ Apply visibility styling
        equipmentBox.getChildren().addAll(equipmentLabel, equipmentCombo);

        goalRow.getChildren().addAll(goalBox, muscleBox, equipmentBox);
        formContainer.getChildren().add(goalRow);

        // Generate button with better styling
        Button generateBtn = new Button("🎯 Get My Recommendations");
        generateBtn.setStyle(
            "-fx-background-color: #4CAF50;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15px 30px;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.3), 10, 0, 0, 5);"
        );
        generateBtn.setMaxWidth(Double.MAX_VALUE);

        // Results area with better styling
        VBox resultsArea = new VBox(15);
        resultsArea.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a; -fx-background-radius: 10px; -fx-border-color: #555555; -fx-border-width: 1px; -fx-border-radius: 10px;");

        Label statusLabel = new Label("Click 'Get My Recommendations' to analyze your workout patterns");
        statusLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
        
        // Generate recommendations button action
        generateBtn.setOnAction(e -> {
            System.out.println("🎯 Get Recommendations button clicked!");
            
            if (currentUser == null || recommendationEngine == null || dbHelper == null) {
                showAlert(Alert.AlertType.ERROR, "Service Error", "User not logged in or AI service not available");
                return;
            }
            
            try {
                generateBtn.setDisable(true);
                statusLabel.setText("🤖 Analyzing your workout patterns...");
                
                // Get user selections
                String selectedGoal = goalCombo.getValue();
                String selectedMuscle = muscleCombo.getValue();
                String selectedEquipment = equipmentCombo.getValue();
                
                System.out.println("🎯 Selected Goal: " + selectedGoal);
                System.out.println("🎯 Selected Muscle: " + selectedMuscle);
                System.out.println("🎯 Selected Equipment: " + selectedEquipment);
                
                // Create async task for recommendations
                Task<List<ExerciseRecommendationEngine.Recommendation>> task = new Task<>() {
                    @Override
                    protected List<ExerciseRecommendationEngine.Recommendation> call() throws Exception {
                        ExerciseRecommendationEngine.RecommendationRequest request = new ExerciseRecommendationEngine.RecommendationRequest(
                            currentUser.getId(),
                            selectedGoal.toLowerCase().replace(" ", "_"), // goal
                            Set.of(selectedMuscle.toLowerCase().replace(" ", "_")), // muscle groups
                            Set.of(selectedEquipment.toLowerCase().replace(" ", "_")), // equipment
                            10 // limit
                        );
                        return recommendationEngine.recommend(request);
                    }
                };
                
                task.setOnSucceeded(event -> {
                    List<ExerciseRecommendationEngine.Recommendation> recs = task.getValue();
                    displayRecommendationResults(recs, resultsArea);
                    statusLabel.setText("✅ Recommendations generated successfully!");
                    generateBtn.setDisable(false);
                });
                
                task.setOnFailed(event -> {
                    showAlert(Alert.AlertType.ERROR, "Generation Error", "Failed to generate recommendations. Please try again.");
                    statusLabel.setText("❌ Error generating recommendations. Please try again.");
                    generateBtn.setDisable(false);
                });
                
                new Thread(task).start();
                
            } catch (Exception ex) {
                System.err.println("❌ Error generating recommendations: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Generation Error", "Failed to generate recommendations. Please try again.");
                generateBtn.setDisable(false);
            }
        });

        // Add form container and button to main content
        content.getChildren().addAll(formContainer, generateBtn, statusLabel, resultsArea);

        // Set the content as the content of the scroll pane
        scrollPane.setContent(content);
        
        // Create a container to hold the scroll pane
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        
        return container;
    }

    private void displayRecommendationResults(List<ExerciseRecommendationEngine.Recommendation> recs, VBox container) {
        System.out.println("🖼️ Displaying recommendation results...");
        container.getChildren().clear();

        // Success message
        Label successLabel = new Label("✅ Your personalized exercise recommendations are ready!");
        successLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-padding: 0 0 15px 0;");
        container.getChildren().add(successLabel);

        if (recs != null && !recs.isEmpty()) {
            Label headerLabel = new Label("🎯 Based on your workout history and preferences:");
            headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-padding: 0 0 20px 0;");
            container.getChildren().add(headerLabel);

            for (int i = 0; i < recs.size(); i++) {
                ExerciseRecommendationEngine.Recommendation rec = recs.get(i);
                
                VBox recBox = new VBox(10);
                recBox.setStyle("-fx-padding: 15px; -fx-background-color: #2a2a2a; -fx-background-radius: 10px; -fx-border-color: #4CAF50; -fx-border-width: 1px; -fx-border-radius: 10px;");
                
                Label exerciseLabel = new Label((i + 1) + ". " + (rec.exercise != null ? rec.exercise.getName() : "Unknown Exercise"));
                exerciseLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                
                Label rationaleLabel = new Label("💡 " + rec.rationale);
                rationaleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF; -fx-wrap-text: true;");
                rationaleLabel.setWrapText(true);
                
                Label scoreLabel = new Label("⭐ Recommendation Score: " + String.format("%.1f", rec.score) + "/10");
                scoreLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E0E0E0;");
                
                recBox.getChildren().addAll(exerciseLabel, rationaleLabel, scoreLabel);
                container.getChildren().add(recBox);
            }
        } else {
            Label noRecsLabel = new Label("No recommendations available. Keep logging workouts to get personalized suggestions!");
            noRecsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF9800; -fx-padding: 20px; -fx-background-color: #2a2a2a; -fx-background-radius: 10px;");
            container.getChildren().add(noRecsLabel);
        }
    }

    // Create weaknesses content
    private VBox createWeaknessesContent() {
        VBox content = new VBox(15);
        
        Label title = new Label("🎯 Weakness Analysis");
        title.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Button analyzeBtn = new Button("🔍 Analyze Weaknesses");
        analyzeBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 8px;");
        
        TextArea weaknesses = new TextArea();
        weaknesses.setPrefHeight(350);
        weaknesses.setStyle("-fx-background-color: #1a1a1a; " +
                          "-fx-text-fill: #FFFFFF !important; " +
                          "-fx-background-radius: 10px; " +
                          "-fx-border-color: rgba(255,152,0,0.5); " +
                          "-fx-border-width: 2px; " +
                          "-fx-border-radius: 10px; " +
                          "-fx-font-size: 14px; " +
                          "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                          "-fx-control-inner-background: #1a1a1a;");
        weaknesses.setEditable(false);
        
        Label statusLabel = new Label("Click 'Analyze Weaknesses' to identify your training gaps");
        statusLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
        
        // Analyze weaknesses button action
        analyzeBtn.setOnAction(e -> {
            if (currentUser == null || weaknessService == null || dbHelper == null) {
                weaknesses.setText("❌ Error: User not logged in or AI service not available");
                return;
            }
            
            analyzeBtn.setDisable(true);
            statusLabel.setText("🔍 Analyzing your training patterns...");
            
            // Create async task for weakness analysis
            Task<List<WeaknessDetectionService.WeakArea>> task = new Task<>() {
                @Override
                protected List<WeaknessDetectionService.WeakArea> call() throws Exception {
                    return weaknessService.analyzeUser(currentUser.getId());
                }
            };
            
            task.setOnSucceeded(event -> {
                List<WeaknessDetectionService.WeakArea> weakAreas = task.getValue();
                StringBuilder weaknessText = new StringBuilder();
                weaknessText.append("🎯 Weakness Analysis for ").append(currentUser.getUsername()).append("\n\n");
                
                if (weakAreas != null && !weakAreas.isEmpty()) {
                    weaknessText.append("📊 Identified Weaknesses:\n\n");
                    
                    for (WeaknessDetectionService.WeakArea area : weakAreas) {
                        weaknessText.append("💪 ").append(area.muscleGroup).append(":\n");
                        weaknessText.append("   Weakness Score: ").append(String.format("%.1f", area.score)).append("\n");
                        weaknessText.append("   Analysis: ").append(area.note).append("\n");
                        weaknessText.append("\n");
                    }
                } else {
                    weaknessText.append("No weaknesses detected! Keep up the great work! 💪");
                }
                
                weaknesses.setText(weaknessText.toString());
                statusLabel.setText("✅ Weakness analysis completed!");
                analyzeBtn.setDisable(false);
            });
            
            task.setOnFailed(event -> {
                weaknesses.setText("❌ Failed to analyze weaknesses: " + task.getException().getMessage());
                statusLabel.setText("❌ Error analyzing weaknesses. Please try again.");
                analyzeBtn.setDisable(false);
            });
            
            new Thread(task).start();
        });
        
        content.getChildren().addAll(title, analyzeBtn, statusLabel, weaknesses);
        return content;
    }

    // Create diet content
    private VBox createDietContent() {
        VBox content = new VBox(15);
        
        Label title = new Label("🥗 AI Diet Plan");
        title.setStyle("-fx-text-fill: #9C27B0; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Button generateBtn = new Button("🍽️ Generate Diet Plan");
        generateBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 8px;");
        
        TextArea dietPlan = new TextArea();
        dietPlan.setPrefHeight(350);
        dietPlan.setStyle("-fx-background-color: #1a1a1a; " +
                        "-fx-text-fill: #FFFFFF !important; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: rgba(156,39,176,0.5); " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-control-inner-background: #1a1a1a;");
        dietPlan.setEditable(false);
        
        Label statusLabel = new Label("Click 'Generate Diet Plan' to create your personalized nutrition plan");
        statusLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
        
        // Generate diet plan button action
        generateBtn.setOnAction(e -> {
            if (currentUser == null || dietGenerator == null) {
                dietPlan.setText("❌ Error: User not logged in or AI service not available");
                return;
            }
            
            generateBtn.setDisable(true);
            statusLabel.setText("🍽️ Generating your personalized diet plan...");
            
            // Create async task for diet plan generation
            Task<IntelligentDietGenerator.MealPlan> task = new Task<>() {
                @Override
                protected IntelligentDietGenerator.MealPlan call() throws Exception {
                    IntelligentDietGenerator.DietRequest request = new IntelligentDietGenerator.DietRequest(
                        "male", // gender
                        25, // age
                        175.0, // height cm
                        70.0, // weight kg
                        "moderate", // activity level
                        "muscle_gain", // goal
                        List.of(), // dietary restrictions
                        List.of(), // preferences
                        50.0, // budget per day
                        3, // meals per day
                        60.0 // workout minutes per day
                    );
                    return dietGenerator.generateMealPlan(request);
                }
            };
            
            task.setOnSucceeded(event -> {
                IntelligentDietGenerator.MealPlan mealPlan = task.getValue();
                StringBuilder dietText = new StringBuilder();
                dietText.append("🥗 AI Diet Plan for ").append(currentUser.getUsername()).append("\n\n");
                
                if (mealPlan != null && mealPlan.targets != null) {
                    dietText.append("📊 Daily Macros:\n");
                    dietText.append("   Calories: ").append(mealPlan.targets.calories).append(" kcal\n");
                    dietText.append("   Protein: ").append(mealPlan.targets.proteinG).append("g\n");
                    dietText.append("   Carbs: ").append(mealPlan.targets.carbsG).append("g\n");
                    dietText.append("   Fats: ").append(mealPlan.targets.fatsG).append("g\n\n");
                    
                    dietText.append("🍽️ Meal Suggestions:\n\n");
                    
                    for (IntelligentDietGenerator.Meal meal : mealPlan.meals) {
                        dietText.append("🍴 ").append(meal.name).append(":\n");
                        if (meal.items != null && !meal.items.isEmpty()) {
                            for (IntelligentDietGenerator.MealItem item : meal.items) {
                                dietText.append("   • ").append(item.name);
                                if (item.grams > 0) {
                                    dietText.append(" (").append(String.format("%.0f", item.grams)).append("g)");
                                }
                                if (item.notes != null && !item.notes.isEmpty()) {
                                    dietText.append(" - ").append(item.notes);
                                }
                                dietText.append("\n");
                            }
                        }
                        dietText.append("\n");
                    }
                    
                    if (mealPlan.shoppingList != null && !mealPlan.shoppingList.isEmpty()) {
                        dietText.append("🛒 Shopping List:\n");
                        for (String item : mealPlan.shoppingList) {
                            dietText.append("   • ").append(item).append("\n");
                        }
                    }
                } else {
                    dietText.append("No diet plan generated. Please try again.");
                }
                
                dietPlan.setText(dietText.toString());
                statusLabel.setText("✅ Diet plan generated successfully!");
                generateBtn.setDisable(false);
            });
            
            task.setOnFailed(event -> {
                dietPlan.setText("❌ Failed to generate diet plan: " + task.getException().getMessage());
                statusLabel.setText("❌ Error generating diet plan. Please try again.");
                generateBtn.setDisable(false);
            });
            
            new Thread(task).start();
        });
        
        content.getChildren().addAll(title, generateBtn, statusLabel, dietPlan);
        return content;
    }


    @FXML
    private void handleOpenAIPlan() {
        updateAITabContent("Plan");
    }

    @FXML
    private void handleOpenAIRecs() {
        updateAITabContent("Recommendations");
    }

    @FXML
    private void handleOpenAIWeak() {
        updateAITabContent("Weaknesses");
    }

    @FXML
    private void handleOpenAIDiet() {
        updateAITabContent("Diet");
    }

    // Enhanced diet plan generation with user input
    private VBox createRealisticDietPlanForm() {
        // Create a scrollable container for the diet plan form
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox dietForm = new VBox(25);
        dietForm.setStyle("-fx-padding: 30px; -fx-background-color: #2a2a2a; -fx-background-radius: 15px; -fx-border-color: #00d4ff; -fx-border-width: 2px; -fx-border-radius: 15px;");
        dietForm.setMaxWidth(Double.MAX_VALUE);

        // Title with better styling
        Label titleLabel = new Label("🍎 Personalized Diet Plan Generator");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #00d4ff; -fx-padding: 0 0 10px 0;");
        dietForm.getChildren().add(titleLabel);

        // Subtitle
        Label subtitleLabel = new Label("Enter your details to get a customized nutrition plan");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc; -fx-padding: 0 0 20px 0;");
        dietForm.getChildren().add(subtitleLabel);

        // Create a horizontal layout for better space utilization
        VBox formContainer = new VBox(20);
        formContainer.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a; -fx-background-radius: 10px;");

        // Personal Information Section - Horizontal Layout
        Label personalInfoLabel = new Label("👤 Personal Information");
        personalInfoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-padding: 0 0 15px 0;");
        formContainer.getChildren().add(personalInfoLabel);

        // First row - Age, Weight, Height
        HBox row1 = new HBox(20);
        row1.setStyle("-fx-padding: 10px 0;");

        // Age
        VBox ageBox = new VBox(5);
        Label ageLabel = new Label("Age:");
        ageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        TextField ageField = new TextField();
        ageField.setPromptText("25");
        ageField.setPrefWidth(150);
        ageField.setStyle("-fx-background-color: #353535; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 8px; -fx-border-color: #555555; -fx-border-width: 1px; -fx-border-radius: 8px;");
        ageBox.getChildren().addAll(ageLabel, ageField);

        // Weight
        VBox weightBox = new VBox(5);
        Label weightLabel = new Label("Weight (kg):");
        weightLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        TextField weightField = new TextField();
        weightField.setPromptText("70");
        weightField.setPrefWidth(150);
        weightField.setStyle("-fx-background-color: #353535; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 8px; -fx-border-color: #555555; -fx-border-width: 1px; -fx-border-radius: 8px;");
        weightBox.getChildren().addAll(weightLabel, weightField);

        // Height
        VBox heightBox = new VBox(5);
        Label heightLabel = new Label("Height (cm):");
        heightLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        TextField heightField = new TextField();
        heightField.setPromptText("175");
        heightField.setPrefWidth(150);
        heightField.setStyle("-fx-background-color: #353535; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 8px; -fx-border-color: #555555; -fx-border-width: 1px; -fx-border-radius: 8px;");
        heightBox.getChildren().addAll(heightLabel, heightField);

        row1.getChildren().addAll(ageBox, weightBox, heightBox);
        formContainer.getChildren().add(row1);

        // Second row - Gender, Activity, Goal
        HBox row2 = new HBox(20);
        row2.setStyle("-fx-padding: 10px 0;");

        // Gender
        VBox genderBox = new VBox(5);
        Label genderLabel = new Label("Gender:");
        genderLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> genderCombo = new ComboBox<>();
        genderCombo.getItems().addAll("Male", "Female");
        genderCombo.setValue("Male");
        genderCombo.setPrefWidth(150);
        UIStyler.styleComboBox(genderCombo);  // ✅ Apply visibility styling
        genderBox.getChildren().addAll(genderLabel, genderCombo);

        // Activity Level
        VBox activityBox = new VBox(5);
        Label activityLabel = new Label("Activity Level:");
        activityLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> activityCombo = new ComboBox<>();
        activityCombo.getItems().addAll("Sedentary", "Light", "Moderate", "High", "Athlete");
        activityCombo.setValue("Moderate");
        activityCombo.setPrefWidth(150);
        UIStyler.styleComboBox(activityCombo);  // ✅ Apply visibility styling
        activityBox.getChildren().addAll(activityLabel, activityCombo);

        // Goal
        VBox goalBox = new VBox(5);
        Label goalLabel = new Label("Goal:");
        goalLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        ComboBox<String> goalCombo = new ComboBox<>();
        goalCombo.getItems().addAll("Muscle Gain", "Fat Loss", "Maintenance");
        goalCombo.setValue("Muscle Gain");
        goalCombo.setPrefWidth(150);
        UIStyler.styleComboBox(goalCombo);  // ✅ Apply visibility styling
        goalBox.getChildren().addAll(goalLabel, goalCombo);

        row2.getChildren().addAll(genderBox, activityBox, goalBox);
        formContainer.getChildren().add(row2);

        // Third row - Workout hours
        HBox row3 = new HBox(20);
        row3.setStyle("-fx-padding: 10px 0;");

        // Workout hours per week
        VBox workoutBox = new VBox(5);
        Label workoutLabel = new Label("Workout (hrs/week):");
        workoutLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        TextField workoutField = new TextField();
        workoutField.setPromptText("5");
        workoutField.setPrefWidth(150);
        workoutField.setStyle("-fx-background-color: #353535; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 8px; -fx-border-color: #555555; -fx-border-width: 1px; -fx-border-radius: 8px;");
        workoutBox.getChildren().addAll(workoutLabel, workoutField);

        row3.getChildren().add(workoutBox);
        formContainer.getChildren().add(row3);

        // Add the form container to the main form
        dietForm.getChildren().add(formContainer);

        // Generate button with better styling
        Button generateBtn = new Button("🍽️ Generate My Diet Plan");
        generateBtn.setStyle(
            "-fx-background-color: #00d4ff;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15px 30px;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.3), 10, 0, 0, 5);"
        );
        generateBtn.setMaxWidth(Double.MAX_VALUE);

        // Results area with better styling
        VBox resultsArea = new VBox(15);
        resultsArea.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a; -fx-background-radius: 10px; -fx-border-color: #555555; -fx-border-width: 1px; -fx-border-radius: 10px;");

        generateBtn.setOnAction(e -> {
            System.out.println("🍽️ Generate Diet Plan button clicked!");
            
            try {
                // Get user inputs with validation
                String ageText = ageField.getText().trim();
                String weightText = weightField.getText().trim();
                String heightText = heightField.getText().trim();
                String workoutText = workoutField.getText().trim();
                
                int age = ageText.isEmpty() ? 25 : Integer.parseInt(ageText);
                double weight = weightText.isEmpty() ? 70.0 : Double.parseDouble(weightText);
                double height = heightText.isEmpty() ? 175.0 : Double.parseDouble(heightText);
                double workoutHours = workoutText.isEmpty() ? 5.0 : Double.parseDouble(workoutText);
                
                String gender = genderCombo.getValue();
                String activity = activityCombo.getValue();
                String goal = goalCombo.getValue();
                
                System.out.println("📊 User inputs - Age: " + age + ", Weight: " + weight + ", Height: " + height);
                System.out.println("📊 Gender: " + gender + ", Activity: " + activity + ", Goal: " + goal);
                
                if (gender == null || activity == null || goal == null) {
                    showAlert(Alert.AlertType.WARNING, "Missing Information", "Please select options for Gender, Activity, and Goal.");
                    return;
                }

                // Create diet request
                IntelligentDietGenerator.DietRequest request = new IntelligentDietGenerator.DietRequest();
                request.gender = gender.toLowerCase();
                request.age = age;
                request.weightKg = weight;
                request.heightCm = height;
                request.activityLevel = activity.toLowerCase().replace(" ", "_");
                request.goal = goal.toLowerCase().replace(" ", "_");
                request.mealsPerDay = 4; // Default to 4 meals
                request.workoutMinutesPerDay = workoutHours * 60 / 7; // Convert weekly hours to daily minutes
                
                System.out.println("🔄 Generating diet plan...");
                
                // Generate diet plan
                IntelligentDietGenerator.MealPlan plan = dietGenerator.generateMealPlan(request);
                
                System.out.println("✅ Diet plan generated successfully!");
                System.out.println("📊 Target calories: " + (int)plan.targets.calories);
                System.out.println("📊 Target protein: " + (int)plan.targets.proteinG + "g");
                System.out.println("📊 Number of meals: " + plan.meals.size());
                
                // Display results
                displayDietPlanResults(plan, resultsArea);
                
            } catch (NumberFormatException ex) {
                System.err.println("❌ Number format error: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for Age, Weight, Height, and Workout hours.");
            } catch (Exception ex) {
                System.err.println("❌ Error generating diet plan: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Generation Error", "Failed to generate diet plan. Please try again.");
            }
        });

        // Add button and results area to the main form
        dietForm.getChildren().addAll(generateBtn, resultsArea);

        // Set the diet form as the content of the scroll pane
        scrollPane.setContent(dietForm);
        
        // Create a container to hold the scroll pane
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        
        return container;
    }

    private void displayDietPlanResults(IntelligentDietGenerator.MealPlan plan, VBox container) {
        System.out.println("🖼️ Displaying diet plan results...");
        container.getChildren().clear();

        // Success message
        Label successLabel = new Label("✅ Your personalized diet plan has been generated!");
        successLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        container.getChildren().add(successLabel);

        // Macro targets
        VBox macroBox = new VBox(10);
        macroBox.setStyle("-fx-padding: 15px; -fx-background-color: #353535; -fx-background-radius: 8px;");
        
        Label macroTitle = new Label("📊 Daily Macro Targets");
        macroTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00d4ff;");
        macroBox.getChildren().add(macroTitle);

        Label caloriesLabel = new Label("🔥 Calories: " + (int)plan.targets.calories + " kcal");
        caloriesLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");
        macroBox.getChildren().add(caloriesLabel);

        Label proteinLabel = new Label("🥩 Protein: " + (int)plan.targets.proteinG + "g");
        proteinLabel.setStyle("-fx-text-fill: #4ecdc4; -fx-font-size: 14px;");
        macroBox.getChildren().add(proteinLabel);

        Label carbsLabel = new Label("🍞 Carbs: " + (int)plan.targets.carbsG + "g");
        carbsLabel.setStyle("-fx-text-fill: #45b7d1; -fx-font-size: 14px;");
        macroBox.getChildren().add(carbsLabel);

        Label fatsLabel = new Label("🥑 Fats: " + (int)plan.targets.fatsG + "g");
        fatsLabel.setStyle("-fx-text-fill: #f9ca24; -fx-font-size: 14px;");
        macroBox.getChildren().add(fatsLabel);

        container.getChildren().add(macroBox);

        // Meals
        System.out.println("🍽️ Adding " + plan.meals.size() + " meals to display...");
        for (IntelligentDietGenerator.Meal meal : plan.meals) {
            VBox mealBox = new VBox(10);
            mealBox.setStyle("-fx-padding: 15px; -fx-background-color: #353535; -fx-background-radius: 8px;");

            Label mealTitle = new Label(meal.name);
            mealTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00d4ff;");
            mealBox.getChildren().add(mealTitle);

            System.out.println("🍽️ Adding meal: " + meal.name + " with " + meal.items.size() + " items");
            for (IntelligentDietGenerator.MealItem item : meal.items) {
                Label itemLabel = new Label("• " + item.name + " (" + (int)item.grams + "g) - " + item.notes);
                itemLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                mealBox.getChildren().add(itemLabel);
            }

            container.getChildren().add(mealBox);
        }

        // Shopping list
        if (!plan.shoppingList.isEmpty()) {
            VBox shoppingBox = new VBox(10);
            shoppingBox.setStyle("-fx-padding: 15px; -fx-background-color: #353535; -fx-background-radius: 8px;");
            
            Label shoppingTitle = new Label("🛒 Shopping List");
            shoppingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00d4ff;");
            shoppingBox.getChildren().add(shoppingTitle);

            System.out.println("🛒 Adding " + plan.shoppingList.size() + " items to shopping list");
            for (String item : plan.shoppingList) {
                Label itemLabel = new Label("• " + item);
                itemLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                shoppingBox.getChildren().add(itemLabel);
            }

            container.getChildren().add(shoppingBox);
        }
        
        System.out.println("✅ Diet plan results displayed successfully!");
    }
    
    // ==================== COMPREHENSIVE EXERCISE INFO PANEL ====================
    
    private void createExerciseInfoPanel(Exercise exercise) {
        if (exerciseImageView == null) {
            System.err.println("❌ exerciseImageView is null!");
            return;
        }
        
        System.out.println("📋 Creating premium info panel for: " + exercise.getName());
        
        // ==================== CREATE PREMIUM UI COMPONENTS ====================
        
        // Main container with modern styling
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(15);
        mainContainer.setPadding(new Insets(20, 25, 25, 25));
        mainContainer.setPrefWidth(420);
        mainContainer.setPrefHeight(380);
        
        // Get muscle group specific styling
        MuscleGroupTheme theme = getMuscleGroupTheme(exercise.getMuscleGroup().toLowerCase());
        
        // Apply gradient background
        LinearGradient backgroundGradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, theme.primaryGradient),
            new Stop(1, theme.secondaryGradient)
        );
        
        BackgroundFill backgroundFill = new BackgroundFill(backgroundGradient, 
            new CornerRadii(20), Insets.EMPTY);
        mainContainer.setBackground(new Background(backgroundFill));
        
        // Add subtle shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(theme.shadowColor);
        shadow.setRadius(25);
        shadow.setOffsetX(0);
        shadow.setOffsetY(8);
        mainContainer.setEffect(shadow);
        
        // ==================== EXERCISE HEADER SECTION ====================
        
        VBox headerSection = createHeaderSection(exercise, theme);
        mainContainer.getChildren().add(headerSection);
        
        // ==================== DETAILED INFO GRID ====================
        
        GridPane infoGrid = createInfoGrid(exercise, theme);
        mainContainer.getChildren().add(infoGrid);
        
        // ==================== TIPS SECTION ====================
        
        VBox tipsSection = createTipsSection(exercise, theme);
        mainContainer.getChildren().add(tipsSection);
        
        // ==================== CONVERT TO IMAGE AND DISPLAY ====================
        
        // Create a snapshot of the VBox immediately (no animations for image)
        WritableImage image = new WritableImage(420, 380);
        mainContainer.snapshot(null, image);
        
        exerciseImageView.setImage(image);
        exerciseImageView.setVisible(true);
        exerciseImageView.setFitWidth(420);
        exerciseImageView.setFitHeight(380);
        exerciseImageView.setPreserveRatio(true);
        exerciseImageView.setSmooth(true);
        exerciseImageView.setCache(true);
        
        // ==================== ANIMATIONS ====================
        
        // Fade in animation for the image view itself
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), exerciseImageView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // Scale animation for the image view
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), exerciseImageView);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);
        
        // Play animations in parallel
        ParallelTransition entranceAnimation = new ParallelTransition(fadeIn, scaleIn);
        entranceAnimation.play();
        
        System.out.println("📋 Premium info panel created for: " + exercise.getName());
    }
    
    // ==================== PREMIUM UI COMPONENT CREATORS ====================
    
    private VBox createHeaderSection(Exercise exercise, MuscleGroupTheme theme) {
        VBox headerSection = new VBox(8);
        
        // Exercise name with premium typography
        Label exerciseName = new Label(exercise.getName().toUpperCase());
        exerciseName.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 22));
        exerciseName.setTextFill(theme.accentColor);
        exerciseName.setWrapText(true);
        
        // Muscle group badge
        HBox muscleGroupContainer = new HBox(8);
        Label muscleIcon = new Label(getMuscleGroupIcon(exercise.getMuscleGroup()));
        muscleIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        muscleIcon.setTextFill(theme.highlightColor);
        
        Label muscleGroup = new Label(exercise.getMuscleGroup());
        muscleGroup.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        muscleGroup.setTextFill(Color.WHITE);
        
        muscleGroupContainer.getChildren().addAll(muscleIcon, muscleGroup);
        
        headerSection.getChildren().addAll(exerciseName, muscleGroupContainer);
        
        return headerSection;
    }
    
    private GridPane createInfoGrid(Exercise exercise, MuscleGroupTheme theme) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 10, 0));
        
        // Create info cards
        InfoCard difficultyCard = createInfoCard("DIFFICULTY", exercise.getDifficulty(), 
            getDifficultyIcon(exercise.getDifficulty()), theme);
        InfoCard equipmentCard = createInfoCard("EQUIPMENT", exercise.getEquipment(), 
            getEquipmentIcon(exercise.getEquipment()), theme);
        
        // Add muscle activation info
        String[] muscleInfo = getDetailedMuscleGroupInfo(exercise.getMuscleGroup().toLowerCase(), exercise.getName());
        InfoCard activationCard = createInfoCard("ACTIVATION", muscleInfo[6].split(": ")[1], 
            "⚡", theme);
        InfoCard effortCard = createInfoCard("EFFORT", muscleInfo[7].split(": ")[1], 
            "🔥", theme);
        
        // Add to grid
        grid.add(difficultyCard.container, 0, 0);
        grid.add(equipmentCard.container, 1, 0);
        grid.add(activationCard.container, 0, 1);
        grid.add(effortCard.container, 1, 1);
        
        return grid;
    }
    
    private VBox createTipsSection(Exercise exercise, MuscleGroupTheme theme) {
        VBox tipsSection = new VBox(8);
        
        // Tips header
        Label tipsHeader = new Label("💡 KEY TIPS");
        tipsHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tipsHeader.setTextFill(theme.highlightColor);
        
        // Tips content
        String[] exerciseTips = getExerciseSpecificTips(exercise.getName(), exercise.getMuscleGroup().toLowerCase());
        VBox tipsContent = new VBox(6);
        
        for (int i = 0; i < Math.min(exerciseTips.length, 3); i++) {
            Label tip = new Label("• " + exerciseTips[i]);
            tip.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            tip.setTextFill(Color.rgb(220, 220, 220));
            tip.setWrapText(true);
            tipsContent.getChildren().add(tip);
        }
        
        tipsSection.getChildren().addAll(tipsHeader, tipsContent);
        
        return tipsSection;
    }
    
    private InfoCard createInfoCard(String title, String value, String icon, MuscleGroupTheme theme) {
        InfoCard card = new InfoCard();
        
        VBox cardContainer = new VBox(6);
        cardContainer.setPadding(new Insets(12, 16, 12, 16));
        cardContainer.setPrefWidth(160);
        
        // Card background with subtle transparency
        BackgroundFill cardBackground = new BackgroundFill(
            Color.rgb(255, 255, 255, 0.1), 
            new CornerRadii(12), 
            Insets.EMPTY
        );
        cardContainer.setBackground(new Background(cardBackground));
        
        // Border
        BorderStroke borderStroke = new BorderStroke(
            theme.accentColor, 
            BorderStrokeStyle.SOLID, 
            new CornerRadii(12), 
            new BorderWidths(1)
        );
        cardContainer.setBorder(new Border(borderStroke));
        
        // Title
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        titleLabel.setTextFill(theme.secondaryTextColor);
        
        // Value with icon
        HBox valueContainer = new HBox(6);
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        iconLabel.setTextFill(theme.highlightColor);
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        valueLabel.setTextFill(Color.WHITE);
        
        valueContainer.getChildren().addAll(iconLabel, valueLabel);
        
        cardContainer.getChildren().addAll(titleLabel, valueContainer);
        card.container = cardContainer;
        
        return card;
    }
    
    // ==================== THEME AND STYLING CLASSES ====================
    
    private static class InfoCard {
        VBox container;
    }
    
    private static class MuscleGroupTheme {
        Color primaryGradient;
        Color secondaryGradient;
        Color accentColor;
        Color highlightColor;
        Color secondaryTextColor;
        Color shadowColor;
        
        MuscleGroupTheme(Color primaryGradient, Color secondaryGradient, Color accentColor, 
                        Color highlightColor, Color secondaryTextColor, Color shadowColor) {
            this.primaryGradient = primaryGradient;
            this.secondaryGradient = secondaryGradient;
            this.accentColor = accentColor;
            this.highlightColor = highlightColor;
            this.secondaryTextColor = secondaryTextColor;
            this.shadowColor = shadowColor;
        }
    }
    
    private MuscleGroupTheme getMuscleGroupTheme(String muscleGroup) {
        if (muscleGroup.contains("chest") || muscleGroup.contains("pectoral")) {
            return new MuscleGroupTheme(
                Color.rgb(139, 0, 0), Color.rgb(178, 34, 34),
                Color.rgb(255, 100, 100), Color.rgb(255, 69, 0),
                Color.rgb(255, 182, 193), Color.rgb(139, 0, 0, 0.3)
            );
        } else if (muscleGroup.contains("back") || muscleGroup.contains("lat")) {
            return new MuscleGroupTheme(
                Color.rgb(0, 100, 139), Color.rgb(0, 139, 139),
                Color.rgb(0, 191, 255), Color.rgb(0, 255, 255),
                Color.rgb(175, 238, 238), Color.rgb(0, 100, 139, 0.3)
            );
        } else if (muscleGroup.contains("shoulder") || muscleGroup.contains("deltoid")) {
            return new MuscleGroupTheme(
                Color.rgb(72, 61, 139), Color.rgb(106, 90, 205),
                Color.rgb(138, 43, 226), Color.rgb(186, 85, 211),
                Color.rgb(221, 160, 221), Color.rgb(72, 61, 139, 0.3)
            );
        } else if (muscleGroup.contains("bicep") || muscleGroup.contains("tricep") || muscleGroup.contains("arm")) {
            return new MuscleGroupTheme(
                Color.rgb(0, 100, 0), Color.rgb(34, 139, 34),
                Color.rgb(50, 205, 50), Color.rgb(255, 215, 0),
                Color.rgb(144, 238, 144), Color.rgb(0, 100, 0, 0.3)
            );
        } else if (muscleGroup.contains("abs") || muscleGroup.contains("core") || muscleGroup.contains("oblique")) {
            return new MuscleGroupTheme(
                Color.rgb(184, 134, 11), Color.rgb(255, 140, 0),
                Color.rgb(255, 215, 0), Color.rgb(255, 165, 0),
                Color.rgb(255, 228, 181), Color.rgb(184, 134, 11, 0.3)
            );
        } else if (muscleGroup.contains("leg") || muscleGroup.contains("quad") || muscleGroup.contains("hamstring") || muscleGroup.contains("glute") || muscleGroup.contains("calf")) {
            return new MuscleGroupTheme(
                Color.rgb(139, 69, 19), Color.rgb(160, 82, 45),
                Color.rgb(255, 69, 0), Color.rgb(255, 140, 0),
                Color.rgb(255, 218, 185), Color.rgb(139, 69, 19, 0.3)
            );
        } else {
            return new MuscleGroupTheme(
                Color.rgb(25, 25, 112), Color.rgb(72, 61, 139),
                Color.rgb(0, 191, 255), Color.rgb(255, 215, 0),
                Color.rgb(176, 224, 230), Color.rgb(25, 25, 112, 0.3)
            );
        }
    }
    
    // ==================== ICON HELPER METHODS ====================
    
    private String getMuscleGroupIcon(String muscleGroup) {
        String lower = muscleGroup.toLowerCase();
        if (lower.contains("chest") || lower.contains("pectoral")) return "🫁";
        if (lower.contains("back") || lower.contains("lat")) return "🦴";
        if (lower.contains("shoulder") || lower.contains("deltoid")) return "💪";
        if (lower.contains("bicep") || lower.contains("tricep") || lower.contains("arm")) return "💪";
        if (lower.contains("abs") || lower.contains("core") || lower.contains("oblique")) return "🎯";
        if (lower.contains("leg") || lower.contains("quad") || lower.contains("hamstring") || lower.contains("glute") || lower.contains("calf")) return "🦵";
        return "🏋️";
    }
    
    private String getDifficultyIcon(String difficulty) {
        String lower = difficulty.toLowerCase();
        if (lower.contains("beginner")) return "🟢";
        if (lower.contains("intermediate")) return "🟡";
        if (lower.contains("advanced")) return "🔴";
        return "⚪";
    }
    
    private String getEquipmentIcon(String equipment) {
        String lower = equipment.toLowerCase();
        if (lower.contains("dumbbell")) return "🏋️";
        if (lower.contains("barbell")) return "🏋️‍♂️";
        if (lower.contains("machine")) return "⚙️";
        if (lower.contains("bodyweight")) return "🧘";
        if (lower.contains("cable")) return "🔗";
        return "🏋️‍♀️";
    }
    
    // ==================== MUSCLE GROUP-SPECIFIC HELPER METHODS ====================
    
    private String[] getDetailedMuscleGroupInfo(String muscleGroup, String exerciseName) {
        if (muscleGroup.contains("chest") || muscleGroup.contains("pectoral")) {
            return new String[]{
                "🏋️ PRIMARY: Pectoralis Major",
                "💪 SECONDARY: Anterior Deltoids",
                "📊 SETS: 3-5 sets",
                "🔄 REPS: 6-12 reps",
                "⏱️ REST: 90-120 seconds",
                "🔥 INTENSITY: High",
                "📈 MUSCLE ACTIVATION: 95%",
                "💯 EFFORT: 9/10"
            };
        } else if (muscleGroup.contains("back") || muscleGroup.contains("lat")) {
            return new String[]{
                "🏋️ PRIMARY: Latissimus Dorsi",
                "💪 SECONDARY: Rhomboids, Traps",
                "📊 SETS: 4-6 sets",
                "🔄 REPS: 8-15 reps",
                "⏱️ REST: 60-90 seconds",
                "🔥 INTENSITY: High",
                "📈 MUSCLE ACTIVATION: 90%",
                "💯 EFFORT: 8/10"
            };
        } else if (muscleGroup.contains("shoulder") || muscleGroup.contains("deltoid")) {
            return new String[]{
                "🏋️ PRIMARY: Deltoids (All Heads)",
                "💪 SECONDARY: Trapezius, Serratus",
                "📊 SETS: 3-4 sets",
                "🔄 REPS: 10-15 reps",
                "⏱️ REST: 60-90 seconds",
                "🔥 INTENSITY: Moderate",
                "📈 MUSCLE ACTIVATION: 85%",
                "💯 EFFORT: 7/10"
            };
        } else if (muscleGroup.contains("bicep") || muscleGroup.contains("tricep") || muscleGroup.contains("arm")) {
            return new String[]{
                "🏋️ PRIMARY: Biceps/Triceps",
                "💪 SECONDARY: Forearms, Deltoids",
                "📊 SETS: 3-4 sets",
                "🔄 REPS: 8-12 reps",
                "⏱️ REST: 60-90 seconds",
                "🔥 INTENSITY: Moderate",
                "📈 MUSCLE ACTIVATION: 80%",
                "💯 EFFORT: 6/10"
            };
        } else if (muscleGroup.contains("abs") || muscleGroup.contains("core") || muscleGroup.contains("oblique")) {
            return new String[]{
                "🏋️ PRIMARY: Rectus Abdominis",
                "💪 SECONDARY: Obliques, Transverse",
                "📊 SETS: 3-5 sets",
                "🔄 REPS: 15-25 reps",
                "⏱️ REST: 30-60 seconds",
                "🔥 INTENSITY: High",
                "📈 MUSCLE ACTIVATION: 90%",
                "💯 EFFORT: 8/10"
            };
        } else if (muscleGroup.contains("leg") || muscleGroup.contains("quad") || muscleGroup.contains("hamstring") || muscleGroup.contains("glute") || muscleGroup.contains("calf")) {
            return new String[]{
                "🏋️ PRIMARY: Quadriceps/Hamstrings",
                "💪 SECONDARY: Glutes, Calves",
                "📊 SETS: 4-6 sets",
                "🔄 REPS: 8-15 reps",
                "⏱️ REST: 90-120 seconds",
                "🔥 INTENSITY: Maximum",
                "📈 MUSCLE ACTIVATION: 95%",
                "💯 EFFORT: 9/10"
            };
        } else {
            return new String[]{
                "🏋️ PRIMARY: Target Muscles",
                "💪 SECONDARY: Supporting Muscles",
                "📊 SETS: 3-4 sets",
                "🔄 REPS: 8-12 reps",
                "⏱️ REST: 60-90 seconds",
                "🔥 INTENSITY: Moderate",
                "📈 MUSCLE ACTIVATION: 85%",
                "💯 EFFORT: 7/10"
            };
        }
    }
    
    private String[] getExerciseSpecificTips(String exerciseName, String muscleGroup) {
        String exercise = exerciseName.toLowerCase();
        
        if (exercise.contains("bench") || exercise.contains("press")) {
            return new String[]{
                "Keep your core tight throughout",
                "Control the weight on the way down",
                "Squeeze at the top of the movement"
            };
        } else if (exercise.contains("curl") || exercise.contains("bicep")) {
            return new String[]{
                "Keep your elbows stationary",
                "Squeeze your biceps at the top",
                "Don't swing the weight"
            };
        } else if (exercise.contains("squat") || exercise.contains("leg")) {
            return new String[]{
                "Keep your knees behind your toes",
                "Drive through your heels",
                "Maintain a straight back"
            };
        } else if (exercise.contains("pull") || exercise.contains("row")) {
            return new String[]{
                "Pull your elbows back",
                "Squeeze your shoulder blades",
                "Keep your core engaged"
            };
        } else if (exercise.contains("plank") || exercise.contains("core")) {
            return new String[]{
                "Keep your body in a straight line",
                "Breathe normally",
                "Engage your core throughout"
            };
        } else {
            return new String[]{
                "Focus on proper form",
                "Control the movement",
                "Breathe consistently"
            };
        }
    }
    
    private String[] getMuscleGroupSpecificParams(String muscleGroup) {
        if (muscleGroup.contains("chest") || muscleGroup.contains("pectoral")) {
            return new String[]{
                "💪 SETS: 3-5",
                "🔄 REPS: 6-12", 
                "⏱️ REST: 90-120s",
                "🏋️ WEIGHT: Heavy"
            };
        } else if (muscleGroup.contains("back") || muscleGroup.contains("lat")) {
            return new String[]{
                "💪 SETS: 4-6",
                "🔄 REPS: 8-15",
                "⏱️ REST: 60-90s", 
                "🏋️ WEIGHT: Moderate-Heavy"
            };
        } else if (muscleGroup.contains("shoulder") || muscleGroup.contains("deltoid")) {
            return new String[]{
                "💪 SETS: 3-4",
                "🔄 REPS: 10-15",
                "⏱️ REST: 60-90s",
                "🏋️ WEIGHT: Light-Moderate"
            };
        } else if (muscleGroup.contains("bicep") || muscleGroup.contains("tricep") || muscleGroup.contains("arm")) {
            return new String[]{
                "💪 SETS: 3-4",
                "🔄 REPS: 8-12",
                "⏱️ REST: 60-90s",
                "🏋️ WEIGHT: Moderate"
            };
        } else if (muscleGroup.contains("abs") || muscleGroup.contains("core") || muscleGroup.contains("oblique")) {
            return new String[]{
                "💪 SETS: 3-5",
                "🔄 REPS: 15-25",
                "⏱️ REST: 30-60s",
                "🏋️ WEIGHT: Bodyweight"
            };
        } else if (muscleGroup.contains("leg") || muscleGroup.contains("quad") || muscleGroup.contains("hamstring") || muscleGroup.contains("glute") || muscleGroup.contains("calf")) {
            return new String[]{
                "💪 SETS: 4-6",
                "🔄 REPS: 8-15",
                "⏱️ REST: 90-120s",
                "🏋️ WEIGHT: Heavy"
            };
        } else {
            return new String[]{
                "💪 SETS: 3-4",
                "🔄 REPS: 8-12",
                "⏱️ REST: 60-90s",
                "🏋️ WEIGHT: Moderate"
            };
        }
    }
    
    private String[] getMuscleGroupIntensityMetrics(String muscleGroup) {
        if (muscleGroup.contains("chest") || muscleGroup.contains("pectoral")) {
            return new String[]{
                "⚡ INTENSITY: MAXIMUM",
                "🔥 CALORIES: 12-18/min",
                "📈 MUSCLE: 95%",
                "💯 EFFORT: 9/10"
            };
        } else if (muscleGroup.contains("back") || muscleGroup.contains("lat")) {
            return new String[]{
                "⚡ INTENSITY: HIGH",
                "🔥 CALORIES: 10-15/min",
                "📈 MUSCLE: 90%",
                "💯 EFFORT: 8/10"
            };
        } else if (muscleGroup.contains("shoulder") || muscleGroup.contains("deltoid")) {
            return new String[]{
                "⚡ INTENSITY: MODERATE",
                "🔥 CALORIES: 8-12/min",
                "📈 MUSCLE: 85%",
                "💯 EFFORT: 7/10"
            };
        } else if (muscleGroup.contains("bicep") || muscleGroup.contains("tricep") || muscleGroup.contains("arm")) {
            return new String[]{
                "⚡ INTENSITY: MODERATE",
                "🔥 CALORIES: 6-10/min",
                "📈 MUSCLE: 80%",
                "💯 EFFORT: 6/10"
            };
        } else if (muscleGroup.contains("abs") || muscleGroup.contains("core") || muscleGroup.contains("oblique")) {
            return new String[]{
                "⚡ INTENSITY: HIGH",
                "🔥 CALORIES: 8-15/min",
                "📈 MUSCLE: 90%",
                "💯 EFFORT: 8/10"
            };
        } else if (muscleGroup.contains("leg") || muscleGroup.contains("quad") || muscleGroup.contains("hamstring") || muscleGroup.contains("glute") || muscleGroup.contains("calf")) {
            return new String[]{
                "⚡ INTENSITY: MAXIMUM",
                "🔥 CALORIES: 15-25/min",
                "📈 MUSCLE: 95%",
                "💯 EFFORT: 9/10"
            };
        } else {
            return new String[]{
                "⚡ INTENSITY: MODERATE",
                "🔥 CALORIES: 8-12/min",
                "📈 MUSCLE: 85%",
                "💯 EFFORT: 7/10"
            };
        }
    }
    
    private String[] getMuscleGroupTipsAndMotivation(String muscleGroup) {
        if (muscleGroup.contains("chest") || muscleGroup.contains("pectoral")) {
            return new String[]{
                "💡 TIP: SQUEEZE AT TOP",
                "⚠️ SAFETY: KEEP ELBOWS IN",
                "🏆 PROGRESS: ADD WEIGHT",
                "🔥 FOCUS: FULL RANGE"
            };
        } else if (muscleGroup.contains("back") || muscleGroup.contains("lat")) {
            return new String[]{
                "💡 TIP: PULL ELBOWS BACK",
                "⚠️ SAFETY: KEEP CORE TIGHT",
                "🏆 PROGRESS: WIDE GRIP",
                "🔥 FOCUS: SQUEEZE LATS"
            };
        } else if (muscleGroup.contains("shoulder") || muscleGroup.contains("deltoid")) {
            return new String[]{
                "💡 TIP: CONTROL DESCENT",
                "⚠️ SAFETY: KEEP ELBOWS SOFT",
                "🏆 PROGRESS: INCREASE RANGE",
                "🔥 FOCUS: SMOOTH MOTION"
            };
        } else if (muscleGroup.contains("bicep") || muscleGroup.contains("tricep") || muscleGroup.contains("arm")) {
            return new String[]{
                "💡 TIP: SQUEEZE AT PEAK",
                "⚠️ SAFETY: NO SWINGING",
                "🏆 PROGRESS: SLOW TEMPO",
                "🔥 FOCUS: MIND-MUSCLE"
            };
        } else if (muscleGroup.contains("abs") || muscleGroup.contains("core") || muscleGroup.contains("oblique")) {
            return new String[]{
                "💡 TIP: BREATHE OUT UP",
                "⚠️ SAFETY: PROTECT NECK",
                "🏆 PROGRESS: HOLD LONGER",
                "🔥 FOCUS: CORE ENGAGED"
            };
        } else if (muscleGroup.contains("leg") || muscleGroup.contains("quad") || muscleGroup.contains("hamstring") || muscleGroup.contains("glute") || muscleGroup.contains("calf")) {
            return new String[]{
                "💡 TIP: DRIVE THROUGH HEELS",
                "⚠️ SAFETY: KEEP KNEES TRACKING",
                "🏆 PROGRESS: DEEPER SQUAT",
                "🔥 FOCUS: EXPLOSIVE UP"
            };
        } else {
            return new String[]{
                "💡 TIP: CONTROLLED MOVEMENT",
                "⚠️ SAFETY: PROPER FORM",
                "🏆 PROGRESS: INCREASE WEIGHT",
                "🔥 FOCUS: CONSISTENCY"
            };
        }
    }
    
    private String getExerciseSpecificInfo(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "Sets: 3-4\nReps: 8-12\nRest: 60-90s\nFocus: Bicep isolation\nForm: Controlled movement";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "Sets: 3-5\nReps: 5-8\nRest: 2-3min\nFocus: Chest strength\nForm: Full range motion";
        } else if (exerciseName.contains("squat")) {
            return "Sets: 3-4\nReps: 8-15\nRest: 2-3min\nFocus: Leg power\nForm: Deep squat";
        } else if (exerciseName.contains("deadlift")) {
            return "Sets: 3-5\nReps: 3-6\nRest: 3-5min\nFocus: Full body\nForm: Hip hinge";
        } else if (exerciseName.contains("row")) {
            return "Sets: 3-4\nReps: 8-12\nRest: 60-90s\nFocus: Back strength\nForm: Pull to chest";
        } else if (exerciseName.contains("raise")) {
            return "Sets: 3-4\nReps: 10-15\nRest: 45-60s\nFocus: Shoulder isolation\nForm: Slow controlled";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "Sets: 3-4\nReps: 15-25\nRest: 30-45s\nFocus: Core strength\nForm: Slow crunch";
        } else if (exerciseName.contains("plank")) {
            return "Sets: 3-4\nHold: 30-60s\nRest: 60s\nFocus: Core stability\nForm: Straight line";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "Sets: 3-5\nReps: 3-10\nRest: 2-3min\nFocus: Back & arms\nForm: Full range";
        } else if (exerciseName.contains("dip")) {
            return "Sets: 3-4\nReps: 6-12\nRest: 60-90s\nFocus: Triceps & chest\nForm: Controlled descent";
        } else if (exerciseName.contains("fly")) {
            return "Sets: 3-4\nReps: 10-15\nRest: 60s\nFocus: Chest stretch\nForm: Wide arc";
        } else if (exerciseName.contains("lunge")) {
            return "Sets: 3-4\nReps: 8-12 each\nRest: 60s\nFocus: Leg strength\nForm: Deep lunge";
        } else if (exerciseName.contains("extension")) {
            return "Sets: 3-4\nReps: 12-20\nRest: 45s\nFocus: Muscle isolation\nForm: Full extension";
        } else if (exerciseName.contains("curl") && exerciseName.contains("leg")) {
            return "Sets: 3-4\nReps: 10-15\nRest: 60s\nFocus: Hamstring\nForm: Controlled curl";
        } else if (exerciseName.contains("calf")) {
            return "Sets: 3-4\nReps: 15-25\nRest: 45s\nFocus: Calf strength\nForm: Full range";
        } else if (exerciseName.contains("twist")) {
            return "Sets: 3-4\nReps: 20-30\nRest: 30s\nFocus: Obliques\nForm: Rotational";
        } else if (exerciseName.contains("bridge")) {
            return "Sets: 3-4\nReps: 10-15\nRest: 60s\nFocus: Glutes\nForm: Hip thrust";
        } else if (exerciseName.contains("superman")) {
            return "Sets: 3-4\nReps: 10-15\nRest: 45s\nFocus: Lower back\nForm: Hold position";
        } else if (exerciseName.contains("good morning")) {
            return "Sets: 3-4\nReps: 8-12\nRest: 90s\nFocus: Hamstrings\nForm: Hip hinge";
        } else if (exerciseName.contains("wood chop")) {
            return "Sets: 3-4\nReps: 10-15 each\nRest: 60s\nFocus: Core rotation\nForm: Diagonal chop";
        } else if (exerciseName.contains("bicycle")) {
            return "Sets: 3-4\nReps: 20-30\nRest: 30s\nFocus: Obliques\nForm: Alternating";
        } else if (exerciseName.contains("side plank")) {
            return "Sets: 3-4\nHold: 30-45s each\nRest: 60s\nFocus: Obliques\nForm: Side hold";
        } else if (exerciseName.contains("jump rope")) {
            return "Sets: 3-5\nTime: 1-2min\nRest: 60s\nFocus: Cardio\nForm: Light on feet";
        } else if (exerciseName.contains("neck")) {
            return "Sets: 2-3\nReps: 10-15\nRest: 30s\nFocus: Neck strength\nForm: Controlled";
        } else {
            return "Sets: 3-4\nReps: 8-12\nRest: 60-90s\nFocus: Muscle group\nForm: Proper technique";
        }
    }
    
    private String getExerciseIcon(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "💪";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "🏋️";
        } else if (exerciseName.contains("squat")) {
            return "🦵";
        } else if (exerciseName.contains("deadlift")) {
            return "⚡";
        } else if (exerciseName.contains("row")) {
            return "🚣";
        } else if (exerciseName.contains("raise")) {
            return "⬆️";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "🔥";
        } else if (exerciseName.contains("plank")) {
            return "📏";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "🆙";
        } else if (exerciseName.contains("dip")) {
            return "⬇️";
        } else if (exerciseName.contains("fly")) {
            return "🦋";
        } else if (exerciseName.contains("lunge")) {
            return "🚶";
        } else if (exerciseName.contains("extension")) {
            return "🔗";
        } else if (exerciseName.contains("calf")) {
            return "🦶";
        } else if (exerciseName.contains("twist")) {
            return "🌀";
        } else if (exerciseName.contains("bridge")) {
            return "🌉";
        } else if (exerciseName.contains("superman")) {
            return "🦸";
        } else if (exerciseName.contains("good morning")) {
            return "🌅";
        } else if (exerciseName.contains("wood chop")) {
            return "🪓";
        } else if (exerciseName.contains("bicycle")) {
            return "🚴";
        } else if (exerciseName.contains("side plank")) {
            return "📐";
        } else if (exerciseName.contains("jump rope")) {
            return "🪢";
        } else if (exerciseName.contains("neck")) {
            return "🦒";
        } else {
            return "💪";
        }
    }
    
    // ==================== EXCITING NEW HELPER METHODS ====================
    
    private String getExerciseMotivation(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "BEAST MODE!";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "POWER UP!";
        } else if (exerciseName.contains("squat")) {
            return "LEG DAY!";
        } else if (exerciseName.contains("deadlift")) {
            return "KING OF LIFTS!";
        } else if (exerciseName.contains("row")) {
            return "BACK ATTACK!";
        } else if (exerciseName.contains("raise")) {
            return "SHOULDER POWER!";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "CORE BLASTER!";
        } else if (exerciseName.contains("plank")) {
            return "STEEL CORE!";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "BODYWEIGHT BEAST!";
        } else if (exerciseName.contains("dip")) {
            return "TRICEPS DESTROYER!";
        } else if (exerciseName.contains("fly")) {
            return "CHEST EXPANDER!";
        } else if (exerciseName.contains("lunge")) {
            return "LEG BLASTER!";
        } else if (exerciseName.contains("extension")) {
            return "MUSCLE ISOLATOR!";
        } else if (exerciseName.contains("calf")) {
            return "CALF CRUSHER!";
        } else if (exerciseName.contains("twist")) {
            return "CORE TWISTER!";
        } else if (exerciseName.contains("bridge")) {
            return "GLUTE ACTIVATOR!";
        } else if (exerciseName.contains("superman")) {
            return "BACK HERO!";
        } else if (exerciseName.contains("good morning")) {
            return "HAMSTRING HAMMER!";
        } else if (exerciseName.contains("wood chop")) {
            return "CORE CHOPPER!";
        } else if (exerciseName.contains("bicycle")) {
            return "ABS BIKER!";
        } else if (exerciseName.contains("side plank")) {
            return "OBLIQUE OBLITERATOR!";
        } else if (exerciseName.contains("jump rope")) {
            return "CARDIO BEAST!";
        } else if (exerciseName.contains("neck")) {
            return "NECK STRENGTH!";
        } else {
            return "FITNESS WARRIOR!";
        }
    }
    
    private String getExerciseFunFact(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "Biceps can generate 30% more force when fully contracted!";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "World record bench press is 1,100+ lbs!";
        } else if (exerciseName.contains("squat")) {
            return "Squats activate 200+ muscles simultaneously!";
        } else if (exerciseName.contains("deadlift")) {
            return "Deadlifts can increase testosterone by 20%!";
        } else if (exerciseName.contains("row")) {
            return "Rowing improves posture and reduces back pain!";
        } else if (exerciseName.contains("raise")) {
            return "Shoulder raises improve shoulder stability!";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "Core strength improves balance and coordination!";
        } else if (exerciseName.contains("plank")) {
            return "Planks activate 100+ muscles at once!";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "Pull-ups are the ultimate bodyweight test!";
        } else if (exerciseName.contains("dip")) {
            return "Dips can build massive triceps!";
        } else if (exerciseName.contains("fly")) {
            return "Flies create the perfect chest stretch!";
        } else if (exerciseName.contains("lunge")) {
            return "Lunges improve hip flexibility!";
        } else if (exerciseName.contains("extension")) {
            return "Extensions target specific muscle groups!";
        } else if (exerciseName.contains("calf")) {
            return "Calf raises improve ankle stability!";
        } else if (exerciseName.contains("twist")) {
            return "Twists improve rotational power!";
        } else if (exerciseName.contains("bridge")) {
            return "Bridges activate the entire posterior chain!";
        } else if (exerciseName.contains("superman")) {
            return "Superman strengthens the entire back!";
        } else if (exerciseName.contains("good morning")) {
            return "Good mornings improve hip hinge pattern!";
        } else if (exerciseName.contains("wood chop")) {
            return "Wood chops improve rotational strength!";
        } else if (exerciseName.contains("bicycle")) {
            return "Bicycle crunches target obliques perfectly!";
        } else if (exerciseName.contains("side plank")) {
            return "Side planks improve lateral stability!";
        } else if (exerciseName.contains("jump rope")) {
            return "Jump rope burns 10+ calories per minute!";
        } else if (exerciseName.contains("neck")) {
            return "Neck exercises improve posture!";
        } else {
            return "Every rep counts towards your goals!";
        }
    }
    
    private String getExerciseIntensity(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "MEDIUM";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "HIGH";
        } else if (exerciseName.contains("squat")) {
            return "HIGH";
        } else if (exerciseName.contains("deadlift")) {
            return "MAXIMUM";
        } else if (exerciseName.contains("row")) {
            return "MEDIUM";
        } else if (exerciseName.contains("raise")) {
            return "LOW";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "MEDIUM";
        } else if (exerciseName.contains("plank")) {
            return "HIGH";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "HIGH";
        } else if (exerciseName.contains("dip")) {
            return "HIGH";
        } else if (exerciseName.contains("fly")) {
            return "LOW";
        } else if (exerciseName.contains("lunge")) {
            return "MEDIUM";
        } else if (exerciseName.contains("extension")) {
            return "LOW";
        } else if (exerciseName.contains("calf")) {
            return "LOW";
        } else if (exerciseName.contains("twist")) {
            return "MEDIUM";
        } else if (exerciseName.contains("bridge")) {
            return "MEDIUM";
        } else if (exerciseName.contains("superman")) {
            return "LOW";
        } else if (exerciseName.contains("good morning")) {
            return "HIGH";
        } else if (exerciseName.contains("wood chop")) {
            return "MEDIUM";
        } else if (exerciseName.contains("bicycle")) {
            return "MEDIUM";
        } else if (exerciseName.contains("side plank")) {
            return "HIGH";
        } else if (exerciseName.contains("jump rope")) {
            return "HIGH";
        } else if (exerciseName.contains("neck")) {
            return "LOW";
        } else {
            return "MEDIUM";
        }
    }
    
    private String getMuscleActivation(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "95% BICEPS";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "90% CHEST";
        } else if (exerciseName.contains("squat")) {
            return "85% QUADS";
        } else if (exerciseName.contains("deadlift")) {
            return "80% FULL BODY";
        } else if (exerciseName.contains("row")) {
            return "90% BACK";
        } else if (exerciseName.contains("raise")) {
            return "95% SHOULDERS";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "90% CORE";
        } else if (exerciseName.contains("plank")) {
            return "85% CORE";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "90% BACK/ARMS";
        } else if (exerciseName.contains("dip")) {
            return "85% TRICEPS";
        } else if (exerciseName.contains("fly")) {
            return "80% CHEST";
        } else if (exerciseName.contains("lunge")) {
            return "85% LEGS";
        } else if (exerciseName.contains("extension")) {
            return "95% TARGET";
        } else if (exerciseName.contains("calf")) {
            return "95% CALVES";
        } else if (exerciseName.contains("twist")) {
            return "90% OBLIQUES";
        } else if (exerciseName.contains("bridge")) {
            return "90% GLUTES";
        } else if (exerciseName.contains("superman")) {
            return "85% LOWER BACK";
        } else if (exerciseName.contains("good morning")) {
            return "90% HAMSTRINGS";
        } else if (exerciseName.contains("wood chop")) {
            return "85% CORE";
        } else if (exerciseName.contains("bicycle")) {
            return "90% OBLIQUES";
        } else if (exerciseName.contains("side plank")) {
            return "95% OBLIQUES";
        } else if (exerciseName.contains("jump rope")) {
            return "80% FULL BODY";
        } else if (exerciseName.contains("neck")) {
            return "95% NECK";
        } else {
            return "80% TARGET";
        }
    }
    
    private String getCalorieBurn(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "5-8/min";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "8-12/min";
        } else if (exerciseName.contains("squat")) {
            return "10-15/min";
        } else if (exerciseName.contains("deadlift")) {
            return "12-18/min";
        } else if (exerciseName.contains("row")) {
            return "8-12/min";
        } else if (exerciseName.contains("raise")) {
            return "4-6/min";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "6-10/min";
        } else if (exerciseName.contains("plank")) {
            return "5-8/min";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "10-15/min";
        } else if (exerciseName.contains("dip")) {
            return "8-12/min";
        } else if (exerciseName.contains("fly")) {
            return "4-6/min";
        } else if (exerciseName.contains("lunge")) {
            return "8-12/min";
        } else if (exerciseName.contains("extension")) {
            return "4-6/min";
        } else if (exerciseName.contains("calf")) {
            return "3-5/min";
        } else if (exerciseName.contains("twist")) {
            return "6-10/min";
        } else if (exerciseName.contains("bridge")) {
            return "6-10/min";
        } else if (exerciseName.contains("superman")) {
            return "4-6/min";
        } else if (exerciseName.contains("good morning")) {
            return "8-12/min";
        } else if (exerciseName.contains("wood chop")) {
            return "8-12/min";
        } else if (exerciseName.contains("bicycle")) {
            return "6-10/min";
        } else if (exerciseName.contains("side plank")) {
            return "5-8/min";
        } else if (exerciseName.contains("jump rope")) {
            return "15-20/min";
        } else if (exerciseName.contains("neck")) {
            return "2-4/min";
        } else {
            return "6-10/min";
        }
    }
    
    private String getProgressionTip(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "Add weight";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "Increase weight";
        } else if (exerciseName.contains("squat")) {
            return "Add depth";
        } else if (exerciseName.contains("deadlift")) {
            return "Perfect form first";
        } else if (exerciseName.contains("row")) {
            return "Slow tempo";
        } else if (exerciseName.contains("raise")) {
            return "Hold at top";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "Slow & controlled";
        } else if (exerciseName.contains("plank")) {
            return "Add time";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "Assisted first";
        } else if (exerciseName.contains("dip")) {
            return "Full range";
        } else if (exerciseName.contains("fly")) {
            return "Wide stretch";
        } else if (exerciseName.contains("lunge")) {
            return "Deeper lunge";
        } else if (exerciseName.contains("extension")) {
            return "Hold squeeze";
        } else if (exerciseName.contains("calf")) {
            return "Single leg";
        } else if (exerciseName.contains("twist")) {
            return "Add weight";
        } else if (exerciseName.contains("bridge")) {
            return "Single leg";
        } else if (exerciseName.contains("superman")) {
            return "Hold longer";
        } else if (exerciseName.contains("good morning")) {
            return "Add weight";
        } else if (exerciseName.contains("wood chop")) {
            return "Heavier weight";
        } else if (exerciseName.contains("bicycle")) {
            return "Slower tempo";
        } else if (exerciseName.contains("side plank")) {
            return "Add leg lift";
        } else if (exerciseName.contains("jump rope")) {
            return "Faster pace";
        } else if (exerciseName.contains("neck")) {
            return "Add resistance";
        } else {
            return "Perfect form";
        }
    }
    
    private String getSafetyTip(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "Keep elbows still";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "Spotter needed";
        } else if (exerciseName.contains("squat")) {
            return "Knees over toes";
        } else if (exerciseName.contains("deadlift")) {
            return "Straight back";
        } else if (exerciseName.contains("row")) {
            return "Pull to chest";
        } else if (exerciseName.contains("raise")) {
            return "Don't swing";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "Don't pull neck";
        } else if (exerciseName.contains("plank")) {
            return "Straight line";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "Full range";
        } else if (exerciseName.contains("dip")) {
            return "Don't go too low";
        } else if (exerciseName.contains("fly")) {
            return "Slight bend";
        } else if (exerciseName.contains("lunge")) {
            return "Knee behind toe";
        } else if (exerciseName.contains("extension")) {
            return "Control weight";
        } else if (exerciseName.contains("calf")) {
            return "Full range";
        } else if (exerciseName.contains("twist")) {
            return "Slow rotation";
        } else if (exerciseName.contains("bridge")) {
            return "Squeeze glutes";
        } else if (exerciseName.contains("superman")) {
            return "Don't hyperextend";
        } else if (exerciseName.contains("good morning")) {
            return "Hinge at hips";
        } else if (exerciseName.contains("wood chop")) {
            return "Control rotation";
        } else if (exerciseName.contains("bicycle")) {
            return "Don't pull neck";
        } else if (exerciseName.contains("side plank")) {
            return "Hip alignment";
        } else if (exerciseName.contains("jump rope")) {
            return "Land softly";
        } else if (exerciseName.contains("neck")) {
            return "Slow movement";
        } else {
            return "Perfect form";
        }
    }
    
    
    private String getDifficultyStars(String difficulty) {
        if (difficulty.toLowerCase().contains("beginner")) {
            return "⭐⭐";
        } else if (difficulty.toLowerCase().contains("intermediate")) {
            return "⭐⭐⭐";
        } else if (difficulty.toLowerCase().contains("advanced")) {
            return "⭐⭐⭐⭐";
        } else {
            return "⭐⭐";
        }
    }
    
    
    private String getTimeEstimate(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "5-8 min";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "8-12 min";
        } else if (exerciseName.contains("squat")) {
            return "10-15 min";
        } else if (exerciseName.contains("deadlift")) {
            return "12-18 min";
        } else if (exerciseName.contains("row")) {
            return "8-12 min";
        } else if (exerciseName.contains("raise")) {
            return "5-8 min";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "6-10 min";
        } else if (exerciseName.contains("plank")) {
            return "3-5 min";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "8-12 min";
        } else if (exerciseName.contains("dip")) {
            return "6-10 min";
        } else if (exerciseName.contains("fly")) {
            return "5-8 min";
        } else if (exerciseName.contains("lunge")) {
            return "8-12 min";
        } else if (exerciseName.contains("extension")) {
            return "5-8 min";
        } else if (exerciseName.contains("calf")) {
            return "4-6 min";
        } else if (exerciseName.contains("twist")) {
            return "6-10 min";
        } else if (exerciseName.contains("bridge")) {
            return "6-10 min";
        } else if (exerciseName.contains("superman")) {
            return "4-6 min";
        } else if (exerciseName.contains("good morning")) {
            return "8-12 min";
        } else if (exerciseName.contains("wood chop")) {
            return "6-10 min";
        } else if (exerciseName.contains("bicycle")) {
            return "5-8 min";
        } else if (exerciseName.contains("side plank")) {
            return "4-6 min";
        } else if (exerciseName.contains("jump rope")) {
            return "10-15 min";
        } else if (exerciseName.contains("neck")) {
            return "3-5 min";
        } else {
            return "6-10 min";
        }
    }
    
    private String getMuscleGroupPercentage(String muscleGroup) {
        if (muscleGroup.toLowerCase().contains("chest")) {
            return "90% CHEST";
        } else if (muscleGroup.toLowerCase().contains("back")) {
            return "85% BACK";
        } else if (muscleGroup.toLowerCase().contains("shoulder")) {
            return "95% DELTS";
        } else if (muscleGroup.toLowerCase().contains("bicep")) {
            return "95% BICEPS";
        } else if (muscleGroup.toLowerCase().contains("tricep")) {
            return "90% TRICEPS";
        } else if (muscleGroup.toLowerCase().contains("forearm")) {
            return "90% FOREARMS";
        } else if (muscleGroup.toLowerCase().contains("abs")) {
            return "90% CORE";
        } else if (muscleGroup.toLowerCase().contains("oblique")) {
            return "95% OBLIQUES";
        } else if (muscleGroup.toLowerCase().contains("lower back")) {
            return "85% LOWER BACK";
        } else if (muscleGroup.toLowerCase().contains("glute")) {
            return "90% GLUTES";
        } else if (muscleGroup.toLowerCase().contains("quad")) {
            return "85% QUADS";
        } else if (muscleGroup.toLowerCase().contains("hamstring")) {
            return "90% HAMS";
        } else if (muscleGroup.toLowerCase().contains("calf")) {
            return "95% CALVES";
        } else if (muscleGroup.toLowerCase().contains("neck")) {
            return "95% NECK";
        } else {
            return "80% TARGET";
        }
    }
    
    private String getExerciseCategory(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "ISOLATION";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "COMPOUND";
        } else if (exerciseName.contains("squat")) {
            return "COMPOUND";
        } else if (exerciseName.contains("deadlift")) {
            return "COMPOUND";
        } else if (exerciseName.contains("row")) {
            return "COMPOUND";
        } else if (exerciseName.contains("raise")) {
            return "ISOLATION";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "ISOLATION";
        } else if (exerciseName.contains("plank")) {
            return "ISOMETRIC";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "COMPOUND";
        } else if (exerciseName.contains("dip")) {
            return "COMPOUND";
        } else if (exerciseName.contains("fly")) {
            return "ISOLATION";
        } else if (exerciseName.contains("lunge")) {
            return "COMPOUND";
        } else if (exerciseName.contains("extension")) {
            return "ISOLATION";
        } else if (exerciseName.contains("calf")) {
            return "ISOLATION";
        } else if (exerciseName.contains("twist")) {
            return "ROTATIONAL";
        } else if (exerciseName.contains("bridge")) {
            return "ISOLATION";
        } else if (exerciseName.contains("superman")) {
            return "ISOLATION";
        } else if (exerciseName.contains("good morning")) {
            return "COMPOUND";
        } else if (exerciseName.contains("wood chop")) {
            return "ROTATIONAL";
        } else if (exerciseName.contains("bicycle")) {
            return "ISOLATION";
        } else if (exerciseName.contains("side plank")) {
            return "ISOMETRIC";
        } else if (exerciseName.contains("jump rope")) {
            return "CARDIO";
        } else if (exerciseName.contains("neck")) {
            return "ISOLATION";
        } else {
            return "COMPOUND";
        }
    }
    
    private String getPopularityRating(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "95% POPULAR";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "98% POPULAR";
        } else if (exerciseName.contains("squat")) {
            return "99% POPULAR";
        } else if (exerciseName.contains("deadlift")) {
            return "97% POPULAR";
        } else if (exerciseName.contains("row")) {
            return "90% POPULAR";
        } else if (exerciseName.contains("raise")) {
            return "85% POPULAR";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "92% POPULAR";
        } else if (exerciseName.contains("plank")) {
            return "94% POPULAR";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "96% POPULAR";
        } else if (exerciseName.contains("dip")) {
            return "88% POPULAR";
        } else if (exerciseName.contains("fly")) {
            return "80% POPULAR";
        } else if (exerciseName.contains("lunge")) {
            return "89% POPULAR";
        } else if (exerciseName.contains("extension")) {
            return "75% POPULAR";
        } else if (exerciseName.contains("calf")) {
            return "70% POPULAR";
        } else if (exerciseName.contains("twist")) {
            return "82% POPULAR";
        } else if (exerciseName.contains("bridge")) {
            return "78% POPULAR";
        } else if (exerciseName.contains("superman")) {
            return "65% POPULAR";
        } else if (exerciseName.contains("good morning")) {
            return "72% POPULAR";
        } else if (exerciseName.contains("wood chop")) {
            return "68% POPULAR";
        } else if (exerciseName.contains("bicycle")) {
            return "85% POPULAR";
        } else if (exerciseName.contains("side plank")) {
            return "87% POPULAR";
        } else if (exerciseName.contains("jump rope")) {
            return "91% POPULAR";
        } else if (exerciseName.contains("neck")) {
            return "45% POPULAR";
        } else {
            return "80% POPULAR";
        }
    }
    
    private String getExerciseBenefits(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "ARM POWER";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "CHEST STRENGTH";
        } else if (exerciseName.contains("squat")) {
            return "LEG POWER";
        } else if (exerciseName.contains("deadlift")) {
            return "FULL BODY";
        } else if (exerciseName.contains("row")) {
            return "BACK STRENGTH";
        } else if (exerciseName.contains("raise")) {
            return "SHOULDER POWER";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "CORE STRENGTH";
        } else if (exerciseName.contains("plank")) {
            return "CORE STABILITY";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "UPPER BODY";
        } else if (exerciseName.contains("dip")) {
            return "TRICEPS POWER";
        } else if (exerciseName.contains("fly")) {
            return "CHEST STRETCH";
        } else if (exerciseName.contains("lunge")) {
            return "LEG STRENGTH";
        } else if (exerciseName.contains("extension")) {
            return "MUSCLE ISOLATION";
        } else if (exerciseName.contains("calf")) {
            return "CALF STRENGTH";
        } else if (exerciseName.contains("twist")) {
            return "CORE ROTATION";
        } else if (exerciseName.contains("bridge")) {
            return "GLUTE POWER";
        } else if (exerciseName.contains("superman")) {
            return "BACK STRENGTH";
        } else if (exerciseName.contains("good morning")) {
            return "HAMSTRING POWER";
        } else if (exerciseName.contains("wood chop")) {
            return "CORE ROTATION";
        } else if (exerciseName.contains("bicycle")) {
            return "OBLIQUE POWER";
        } else if (exerciseName.contains("side plank")) {
            return "LATERAL CORE";
        } else if (exerciseName.contains("jump rope")) {
            return "CARDIO POWER";
        } else if (exerciseName.contains("neck")) {
            return "NECK STRENGTH";
        } else {
            return "MUSCLE POWER";
        }
    }
    
    private String getExerciseTips(String exerciseName) {
        if (exerciseName.contains("curl") || exerciseName.contains("bicep")) {
            return "SQUEEZE AT TOP";
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            return "CONTROLLED DESCENT";
        } else if (exerciseName.contains("squat")) {
            return "DEEP SQUAT";
        } else if (exerciseName.contains("deadlift")) {
            return "HIP HINGE";
        } else if (exerciseName.contains("row")) {
            return "PULL TO CHEST";
        } else if (exerciseName.contains("raise")) {
            return "DON'T SWING";
        } else if (exerciseName.contains("crunch") || exerciseName.contains("abs")) {
            return "SLOW & CONTROLLED";
        } else if (exerciseName.contains("plank")) {
            return "STRAIGHT LINE";
        } else if (exerciseName.contains("pull") || exerciseName.contains("up")) {
            return "FULL RANGE";
        } else if (exerciseName.contains("dip")) {
            return "CONTROLLED DESCENT";
        } else if (exerciseName.contains("fly")) {
            return "WIDE STRETCH";
        } else if (exerciseName.contains("lunge")) {
            return "DEEP LUNGE";
        } else if (exerciseName.contains("extension")) {
            return "HOLD SQUEEZE";
        } else if (exerciseName.contains("calf")) {
            return "FULL RANGE";
        } else if (exerciseName.contains("twist")) {
            return "SLOW ROTATION";
        } else if (exerciseName.contains("bridge")) {
            return "SQUEEZE GLUTES";
        } else if (exerciseName.contains("superman")) {
            return "HOLD POSITION";
        } else if (exerciseName.contains("good morning")) {
            return "HINGE AT HIPS";
        } else if (exerciseName.contains("wood chop")) {
            return "CONTROL ROTATION";
        } else if (exerciseName.contains("bicycle")) {
            return "SLOW TEMPO";
        } else if (exerciseName.contains("side plank")) {
            return "HIP ALIGNMENT";
        } else if (exerciseName.contains("jump rope")) {
            return "LAND SOFTLY";
        } else if (exerciseName.contains("neck")) {
            return "SLOW MOVEMENT";
        } else {
            return "PERFECT FORM";
        }
    }
    
    
    
    // ==================== ENHANCED EXERCISE DETAILS HELPERS ====================
    
    private String getExerciseBenefits(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        
        StringBuilder benefits = new StringBuilder();
        
        if (muscleGroup.contains("chest") || exerciseName.contains("press")) {
            benefits.append("• Builds upper body strength and power\n");
            benefits.append("• Improves pushing movements and functional strength\n");
            benefits.append("• Enhances chest, shoulder, and tricep development\n");
            benefits.append("• Increases bone density in upper body\n");
        } else if (muscleGroup.contains("back") || exerciseName.contains("row") || exerciseName.contains("pull")) {
            benefits.append("• Strengthens posterior chain muscles\n");
            benefits.append("• Improves posture and spinal health\n");
            benefits.append("• Enhances pulling strength and grip\n");
            benefits.append("• Reduces risk of back pain and injury\n");
        } else if (muscleGroup.contains("leg") || exerciseName.contains("squat") || exerciseName.contains("deadlift")) {
            benefits.append("• Builds lower body strength and power\n");
            benefits.append("• Improves functional movement patterns\n");
            benefits.append("• Enhances athletic performance\n");
            benefits.append("• Increases bone density and joint stability\n");
        } else if (muscleGroup.contains("abs") || exerciseName.contains("crunch") || exerciseName.contains("raise")) {
            benefits.append("• Strengthens core muscles and improves stability\n");
            benefits.append("• Enhances posture and spinal alignment\n");
            benefits.append("• Improves athletic performance and balance\n");
            benefits.append("• Reduces risk of lower back injury\n");
        } else if (muscleGroup.contains("arm") || exerciseName.contains("curl")) {
            benefits.append("• Builds arm strength and muscle definition\n");
            benefits.append("• Improves grip strength and endurance\n");
            benefits.append("• Enhances functional pulling movements\n");
            benefits.append("• Supports daily activities and sports\n");
        } else {
            benefits.append("• Builds overall strength and muscle mass\n");
            benefits.append("• Improves functional movement patterns\n");
            benefits.append("• Enhances athletic performance\n");
            benefits.append("• Promotes better health and fitness\n");
        }
        
        return benefits.toString();
    }
    
    private String getSafetyTips(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase();
        String muscleGroup = exercise.getMuscleGroup().toLowerCase();
        
        StringBuilder safety = new StringBuilder();
        
        if (exerciseName.contains("deadlift") || exerciseName.contains("squat")) {
            safety.append("• Always warm up thoroughly before lifting\n");
            safety.append("• Maintain proper form throughout the movement\n");
            safety.append("• Start with lighter weights to master technique\n");
            safety.append("• Keep your core engaged and back straight\n");
            safety.append("• Use a spotter for heavy lifts\n");
        } else if (exerciseName.contains("press") || exerciseName.contains("bench")) {
            safety.append("• Use proper breathing technique (exhale on exertion)\n");
            safety.append("• Keep your feet flat on the ground\n");
            safety.append("• Maintain a slight arch in your lower back\n");
            safety.append("• Control the weight throughout the full range of motion\n");
            safety.append("• Use a spotter for heavy lifts\n");
        } else if (exerciseName.contains("row") || exerciseName.contains("pull")) {
            safety.append("• Keep your core engaged throughout the movement\n");
            safety.append("• Pull with your back muscles, not just your arms\n");
            safety.append("• Maintain proper posture and shoulder position\n");
            safety.append("• Control the weight on both the concentric and eccentric phases\n");
        } else if (muscleGroup.contains("abs") || exerciseName.contains("crunch")) {
            safety.append("• Avoid pulling on your neck during crunches\n");
            safety.append("• Keep your lower back pressed to the ground\n");
            safety.append("• Breathe properly throughout the movement\n");
            safety.append("• Start with bodyweight before adding resistance\n");
        } else {
            safety.append("• Always warm up before exercising\n");
            safety.append("• Use proper form and technique\n");
            safety.append("• Start with lighter weights and progress gradually\n");
            safety.append("• Listen to your body and rest when needed\n");
            safety.append("• Stay hydrated during your workout\n");
        }
        
        return safety.toString();
    }

}

