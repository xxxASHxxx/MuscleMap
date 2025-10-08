package com.musclemmap.controllers;

import com.musclemmap.utils.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import java.util.LinkedHashMap;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.concurrent.Task;
import java.net.URL;

import javafx.scene.layout.*;
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
    @FXML private VBox imageLoadingIndicator;
    @FXML private StackPane exerciseImageContainer;

    @FXML
    private TextArea exerciseDescriptionArea;

    // Workout Tab Components
    @FXML
    private ComboBox<String> routineSelector;
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
        verifyDatabaseExercises();
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

        if (progressController != null) {
            System.out.println("✅ ProgressController loaded from FXML");
        }

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
                    "-fx-background-color: linear-gradient(to right, #00d4ff, #00a0cc); " +
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
                        "-fx-background-color: linear-gradient(to right, #00f5ff, #00c4dd); " +
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
                        "-fx-background-color: linear-gradient(to right, #00d4ff, #00a0cc); " +
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
                        "-fx-background-color: linear-gradient(135deg, #ff6ec7 0%, #ff3d5c 100%);" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(255,110,199,0.9), 30, 0, 0, 12);" +
                                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-width: 3px; -fx-border-radius: 25px;" +
                                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
                );
            });

            quickSetCard.setOnMouseExited(e -> {
                quickSetCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(240,147,251,0.6), 20, 0, 0, 10);" +
                                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 2px; -fx-border-radius: 25px;"
                );
            });
        }

        // Progress Card
        if (progressCard != null) {
            progressCard.setOnMouseEntered(e -> {
                progressCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #00d4ff 0%, #00c6ff 100%);" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.9), 30, 0, 0, 12);" +
                                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-width: 3px; -fx-border-radius: 25px;" +
                                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
                );
            });

            progressCard.setOnMouseExited(e -> {
                progressCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(79,172,254,0.6), 20, 0, 0, 10);" +
                                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 2px; -fx-border-radius: 25px;"
                );
            });
        }

        // Muscle Map Card
        if (muscleMapCard != null) {
            muscleMapCard.setOnMouseEntered(e -> {
                muscleMapCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #ff8fb3 0%, #ffd700 100%);" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(255,143,179,0.9), 30, 0, 0, 12);" +
                                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-width: 3px; -fx-border-radius: 25px;" +
                                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
                );
            });

            muscleMapCard.setOnMouseExited(e -> {
                muscleMapCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #fa709a 0%, #fee140 100%);" +
                                "-fx-padding: 35px; -fx-background-radius: 25px; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(250,112,154,0.6), 20, 0, 0, 10);" +
                                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 2px; -fx-border-radius: 25px;"
                );
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
        createMuscleMap();

        // Exercise list selection handler
        if (exerciseListView != null) {
            exerciseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    displayExerciseDetails(newVal);
                    loadExerciseImage(newVal); // ✅ NEW: Load image when exercise selected
                }
            });
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

        System.out.println("✅ Muscle map created with proper spacing");
    }

    // Helper method to add muscle region with proper layering
    private void addMuscleRegion(Pane pane, String muscleName, double x, double y, double width, double height, String color, int layer) {
        Rectangle muscleShape = new Rectangle(x, y, width, height);
        muscleShape.setFill(Color.web(color).deriveColor(0, 1, 1, 0.80));
        muscleShape.setStroke(Color.web(color).brighter());
        muscleShape.setStrokeWidth(2.5);
        muscleShape.setArcWidth(20);
        muscleShape.setArcHeight(20);
        muscleShape.setEffect(new javafx.scene.effect.DropShadow(8, Color.BLACK));
        muscleShape.setCursor(javafx.scene.Cursor.HAND);

        // Set Z-order for proper layering
        muscleShape.setViewOrder(-layer);

        // Hover effects
        muscleShape.setOnMouseEntered(e -> {
            muscleShape.setFill(Color.web(color).brighter().brighter());
            muscleShape.setScaleX(1.08);
            muscleShape.setScaleY(1.08);
            muscleShape.setEffect(new javafx.scene.effect.DropShadow(18, Color.web(color)));
        });

        muscleShape.setOnMouseExited(e -> {
            muscleShape.setFill(Color.web(color).deriveColor(0, 1, 1, 0.80));
            muscleShape.setScaleX(1.0);
            muscleShape.setScaleY(1.0);
            muscleShape.setEffect(new javafx.scene.effect.DropShadow(8, Color.BLACK));
        });

        muscleShape.setOnMouseClicked(e -> handleMuscleClick(muscleName));

        pane.getChildren().add(muscleShape);

        // Add label with proper visibility
        Label label = new Label(getMuscleEmoji(muscleName));
        label.setLayoutX(x + width / 2 - 12);
        label.setLayoutY(y + height / 2 - 12);
        label.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-effect: dropshadow(gaussian, black, 4, 0, 0, 2);");
        label.setMouseTransparent(true);
        label.setViewOrder(-layer - 1); // Labels always on top of their muscle
        pane.getChildren().add(label);
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
                            "-fx-background-color: linear-gradient(to right, #00d4ff, #0099cc); " +
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
                "-fx-background-color: linear-gradient(to bottom, " + color + ", " + adjustBrightness(color) + ");" +
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
            message.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

            Label suggestion = new Label("Complete some workouts to see your progress!");
            suggestion.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14px;");

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
                "-fx-background-color: linear-gradient(135deg, #2b2b2b 0%, #1f1f1f 100%);" +
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
                "-fx-background-color: linear-gradient(to right, #00d4ff, #0099cc);" +
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
                "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");
        VBox volumeCard = createStatCard("🏋️", String.format("%.0fK", totalVolume / 1000), "Total Volume",
                "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)");
        VBox setsCard = createStatCard("📊", String.valueOf(totalSets), "Total Sets",
                "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)");
        VBox durationCard = createStatCard("⏱️", avgDuration + "m", "Avg Duration",
                "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)");

        statsCardsContainer.getChildren().addAll(workoutsCard, volumeCard, setsCard, durationCard);
    }

    private VBox createStatCard(String icon, String value, String label, String gradient) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));
        card.setStyle(
                "-fx-background: " + gradient + ";" +
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
                "-fx-background-color: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);" +
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
                                "• Animated demonstration (GIF)\n" +
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
    private String toProperCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // ✅ CRITICAL: Single backslash for regex (not double!)
        String[] words = str.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            // Capitalize first letter of each word
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }

        return result.toString();
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

        System.out.println("🖼️ Attempting to load image for: " + exercise.getName());

        // Show loading indicator on UI thread
        javafx.application.Platform.runLater(() -> {
            if (imagePlaceholder != null) imagePlaceholder.setVisible(false);
            if (exerciseImageView != null) exerciseImageView.setVisible(false);
            if (imageLoadingIndicator != null) imageLoadingIndicator.setVisible(true);
        });

        // Load image in background thread
        Task<Image> imageLoadTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                // ✅ CHANGED: Use exercise.getGifUrl() instead of getExerciseImageUrl()
                String imageUrl = exercise.getGifUrl();

                if (imageUrl == null || imageUrl.isEmpty()) {
                    System.err.println("❌ No GIF URL found for: " + exercise.getName());
                    throw new Exception("No image URL available");
                }

                System.out.println("📥 Loading image from URL: " + imageUrl);

                // Load with background loading enabled
                Image image = new Image(imageUrl, true);

                // Wait for image to finish loading
                int attempts = 0;
                while (image.getProgress() < 1.0 && attempts < 50) {
                    Thread.sleep(100);
                    attempts++;
                }

                if (image.isError()) {
                    System.err.println("❌ Image error: " + image.getException());
                    throw new Exception("Failed to load image");
                }

                System.out.println("✅ Image loaded successfully! Size: " + image.getWidth() + "x" + image.getHeight());
                return image;
            }
        };

        imageLoadTask.setOnSucceeded(e -> {
            Image image = imageLoadTask.getValue();
            if (image != null && !image.isError()) {
                javafx.application.Platform.runLater(() -> {
                    if (exerciseImageView != null) {
                        exerciseImageView.setImage(image);
                        exerciseImageView.setVisible(true);
                    }
                    if (imageLoadingIndicator != null) {
                        imageLoadingIndicator.setVisible(false);
                    }
                    System.out.println("✅ Image displayed successfully for: " + exercise.getName());
                });
            } else {
                showImageError();
            }
        });

        imageLoadTask.setOnFailed(e -> {
            System.err.println("❌ Failed to load image: " + imageLoadTask.getException().getMessage());
            imageLoadTask.getException().printStackTrace();
            showImageError();
        });

        // Start loading in background thread
        Thread imageThread = new Thread(imageLoadTask);
        imageThread.setDaemon(true);
        imageThread.start();
    }

    // ✅ ADD THIS METHOD if it doesn't exist
    private void showImagePlaceholder() {
        javafx.application.Platform.runLater(() -> {
            if (exerciseImageView != null) {
                exerciseImageView.setVisible(false);
                exerciseImageView.setImage(null);
            }
            if (imageLoadingIndicator != null) {
                imageLoadingIndicator.setVisible(false);
            }
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisible(true);
            }
            System.out.println("📭 Showing image placeholder");
        });
    }

    // ✅ ADD THIS METHOD if it doesn't exist
    private void showImageError() {
        javafx.application.Platform.runLater(() -> {
            if (exerciseImageView != null) {
                exerciseImageView.setVisible(false);
            }
            if (imageLoadingIndicator != null) {
                imageLoadingIndicator.setVisible(false);
            }
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisible(true);
            }
            System.err.println("⚠️ Showing placeholder due to image error");
        });
    }
    private void handleExerciseSelection(Exercise exercise) {
        if (exercise == null) {
            System.err.println("⚠️ handleExerciseSelection called with null exercise");
            return;
        }

        System.out.println("✅ Exercise selected: " + exercise.getName());

        // Display exercise details in the text area
        displayExerciseDetails(exercise);

        // ✅ Load the exercise GIF
        loadExerciseImage(exercise);
    }


    private String getExerciseImageUrl(Exercise exercise) {
        String exerciseName = exercise.getName().toLowerCase().trim();

        // Comprehensive exercise image database - ALL 69 EXERCISES
        Map<String, String> exerciseImages = new HashMap<>();

        // ==================== CHEST EXERCISES (9) ====================
        exerciseImages.put("bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Bench-Press.gif");
        exerciseImages.put("incline bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Incline-Bench-Press.gif");
        exerciseImages.put("decline bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Decline-Barbell-Bench-Press.gif");
        exerciseImages.put("dumbbell press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Bench-Press.gif");
        exerciseImages.put("incline dumbbell press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Incline-Dumbbell-Press.gif");
        exerciseImages.put("chest fly", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Fly.gif");
        exerciseImages.put("cable fly", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Cross-over-Variation.gif");
        exerciseImages.put("push-ups", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Push-up.gif");
        exerciseImages.put("chest dips", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Chest-Dips.gif");

        // ==================== BACK EXERCISES (7) ====================
        exerciseImages.put("deadlifts", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Deadlift.gif");
        exerciseImages.put("barbell row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Row.gif");
        exerciseImages.put("pull-ups", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Pull-up.gif");
        exerciseImages.put("lat pulldown", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Lat-Pulldown.gif");
        exerciseImages.put("seated cable row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Seated-Cable-Row.gif");
        exerciseImages.put("t-bar row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/T-Bar-Row.gif");
        exerciseImages.put("one-arm dumbbell row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/One-Arm-Dumbbell-Row.gif");

        // ==================== SHOULDER EXERCISES (7) ====================
        exerciseImages.put("overhead press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/barbell-standing-military-press.gif");
        exerciseImages.put("dumbbell shoulder press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Shoulder-Press.gif");
        exerciseImages.put("lateral raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Lateral-Raise.gif");
        exerciseImages.put("front raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Front-Raise.gif");
        exerciseImages.put("rear delt fly", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Rear-Delt-Fly.gif");
        exerciseImages.put("face pull", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Face-Pull.gif");
        exerciseImages.put("arnold press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Arnold-Press.gif");

        // ==================== BICEPS EXERCISES (6) ====================
        exerciseImages.put("bicep curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Curl.gif");
        exerciseImages.put("barbell curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Curl.gif");
        exerciseImages.put("hammer curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hammer-Curl.gif");
        exerciseImages.put("preacher curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Preacher-Curl.gif");
        exerciseImages.put("cable curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Curl.gif");
        exerciseImages.put("concentration curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Concentration-Curl.gif");

        // ==================== TRICEPS EXERCISES (6) ====================
        exerciseImages.put("tricep dips", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Bench-Dips.gif");
        exerciseImages.put("close-grip bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Close-grip-Bench-Press.gif");
        exerciseImages.put("tricep pushdown", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Triceps-Pushdown.gif");
        exerciseImages.put("overhead tricep extension", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Overhead-Triceps-Extension.gif");
        exerciseImages.put("skull crushers", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Lying-Triceps-Extension.gif");
        exerciseImages.put("diamond push-ups", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Diamond-Push-up.gif");

        // ==================== FOREARMS EXERCISES (3) ====================
        exerciseImages.put("wrist curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Wrist-Curl.gif");
        exerciseImages.put("reverse wrist curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Reverse-Wrist-Curl.gif");
        exerciseImages.put("farmer's walk", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Farmers-Walk.gif");

        // ==================== ABS EXERCISES (6) ====================
        exerciseImages.put("planks", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Plank.gif");
        exerciseImages.put("crunches", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Crunch.gif");
        exerciseImages.put("leg raises", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Raise.gif");
        exerciseImages.put("russian twists", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Russian-Twist.gif");
        exerciseImages.put("cable crunch", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Crunch.gif");
        exerciseImages.put("hanging knee raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hanging-Leg-Raise.gif");

        // ==================== OBLIQUES EXERCISES (3) ====================
        exerciseImages.put("side plank", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Side-Plank.gif");
        exerciseImages.put("bicycle crunches", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Bicycle-Crunch.gif");
        exerciseImages.put("wood chops", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Wood-Chop.gif");

        // ==================== LOWER BACK EXERCISES (3) ====================
        exerciseImages.put("back extensions", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hyperextension.gif");
        exerciseImages.put("good mornings", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Good-Morning.gif");
        exerciseImages.put("superman", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Superman-Exercise.gif");

        // ==================== GLUTES EXERCISES (4) ====================
        exerciseImages.put("hip thrusts", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Hip-Thrust.gif");
        exerciseImages.put("glute bridges", "https://fitnessprogramer.com/wp-content/uploads/2021/02/glute-bridge.gif");
        exerciseImages.put("bulgarian split squats", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Bulgarian-Split-Squat.gif");
        exerciseImages.put("cable kickbacks", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Kickback.gif");

        // ==================== QUADRICEPS EXERCISES (6) ====================
        exerciseImages.put("squats", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Squat.gif");
        exerciseImages.put("front squats", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Front-Squat.gif");
        exerciseImages.put("leg press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/LEG-PRESS.gif");
        exerciseImages.put("lunges", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Lunge.gif");
        exerciseImages.put("leg extensions", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-extension.gif");
        exerciseImages.put("walking lunges", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Walking-Lunge.gif");

        // ==================== HAMSTRINGS EXERCISES (4) ====================
        exerciseImages.put("romanian deadlift", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Romanian-Deadlift.gif");
        exerciseImages.put("leg curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Curl.gif");
        exerciseImages.put("nordic curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Nordic-Hamstring-Curl.gif");
        exerciseImages.put("stiff-leg deadlift", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Stiff-Leg-Barbell-Deadlift.gif");

        // ==================== CALVES EXERCISES (3) ====================
        exerciseImages.put("standing calf raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Calf-Raise.gif");
        exerciseImages.put("seated calf raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Seated-Calf-Raise.gif");
        exerciseImages.put("jump rope", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Jump-Rope.gif");

        // ==================== NECK EXERCISES (2) ====================
        exerciseImages.put("neck curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Neck-Curl.gif");
        exerciseImages.put("neck extensions", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Neck-Extension.gif");

        // Try exact match first
        if (exerciseImages.containsKey(exerciseName)) {
            System.out.println("✅ Found exact match for: " + exerciseName);
            return exerciseImages.get(exerciseName);
        }

        // Try partial match (handles variations like "Dumbbell Curls" vs "Bicep Curls")
        for (Map.Entry<String, String> entry : exerciseImages.entrySet()) {
            if (exerciseName.contains(entry.getKey()) || entry.getKey().contains(exerciseName)) {
                System.out.println("✅ Found partial match: " + exerciseName + " -> " + entry.getKey());
                return entry.getValue();
            }
        }

        // Fallback: Use muscle group placeholder
        System.out.println("⚠️ No image found for: " + exerciseName + " - using fallback");
        return getPlaceholderImageByMuscleGroup(exercise.getMuscleGroup());
    }

    // Fallback placeholder images by muscle group
    private String getPlaceholderImageByMuscleGroup(String muscleGroup) {
        return switch (muscleGroup.toLowerCase()) {
            case "chest" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Bench-Press.gif";
            case "back" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Row.gif";
            case "shoulders" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Shoulder-Press.gif";
            case "biceps" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Curl.gif";
            case "triceps" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Triceps-Pushdown.gif";
            case "forearms" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Wrist-Curl.gif";
            case "abs" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Crunch.gif";
            case "obliques" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Bicycle-Crunch.gif";
            case "lower back" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hyperextension.gif";
            case "glutes" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Hip-Thrust.gif";
            case "quadriceps" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Squat.gif";
            case "hamstrings" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Curl.gif";
            case "calves" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Calf-Raise.gif";
            case "neck" -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Neck-Curl.gif";
            default -> "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Bench-Press.gif";
        };
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

        String formattedInstructions = String.format(
                "🏋️ EXERCISE: %s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "📋 INSTRUCTIONS:\n%s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "🎯 MUSCLE GROUP: %s\n" +
                        "🔧 EQUIPMENT: %s %s\n" +
                        "⭐ DIFFICULTY: %s %s",
                exercise.getName().toUpperCase(),
                exercise.getInstructions(),
                exercise.getMuscleGroup(),
                exercise.getEquipmentEmoji(),
                exercise.getEquipment(),
                exercise.getDifficultyEmoji(),
                exercise.getDifficulty()
        );

        exerciseDescriptionArea.setText(formattedInstructions);
        exerciseDescriptionArea.setStyle(
                "-fx-font-size: 15px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-control-inner-background: rgba(10, 10, 10, 0.98); " +
                        "-fx-background-color: rgba(10, 10, 10, 0.98); " +
                        "-fx-background-radius: 12px; " +
                        "-fx-border-color: #00f5ff; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-padding: 10px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.3), 10, 0, 0, 4);"
        );

        System.out.println("📝 Displayed details for: " + exercise.getName());
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

}

