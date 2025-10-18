package com.musclemmap.controllers;

import com.musclemmap.models.*;
import com.musclemmap.utils.DatabaseHelper;
// Using AIInsightsWindow for UI; services are referenced there
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ProgressController {
    @FXML private ScrollPane progressScrollPane;
    @FXML private VBox progressMainContainer;
    @FXML private ComboBox<String> exerciseFilterCombo;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button refreshButton;

    // Charts
    @FXML private LineChart<String, Number> volumeChart;
    @FXML private BarChart<String, Number> exerciseChart;
    @FXML private AreaChart<String, Number> progressChart;

    // Stats containers
    @FXML private VBox statsCardsContainer;
    @FXML private VBox personalRecordsContainer;
    @FXML private VBox bodyMetricsContainer;

    // New components
    @FXML private TableView<PersonalRecordEntry> prTableView;
    @FXML private TableColumn<PersonalRecordEntry, String> exerciseColumn;
    @FXML private TableColumn<PersonalRecordEntry, String> weightColumn;
    @FXML private TableColumn<PersonalRecordEntry, String> dateColumn;

    private DatabaseHelper dbHelper;
    private User currentUser;

    @FXML
    public void initialize() {
        dbHelper = DatabaseHelper.getInstance();

        // ✅ Style ComboBox for visibility
        if (exerciseFilterCombo != null) {
            com.musclemmap.utils.UIStyler.styleComboBox(exerciseFilterCombo);
        }

        // Set default date range (last 3 months)
        fromDatePicker.setValue(LocalDate.now().minusMonths(3));
        toDatePicker.setValue(LocalDate.now());

        // Setup listeners
        exerciseFilterCombo.setOnAction(e -> loadProgressData());
        fromDatePicker.setOnAction(e -> loadProgressData());
        toDatePicker.setOnAction(e -> loadProgressData());
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> loadProgressData());
        }

        // Setup table columns
        setupPersonalRecordsTable();

        // Add AI features action bar
        addIntelligenceActionsBar();

        System.out.println("✅ ProgressController initialized!");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadExerciseFilter();
        loadProgressData();
    }

    private void loadExerciseFilter() {
        if (exerciseFilterCombo != null) {
            List<Exercise> exercises = dbHelper.getAllExercises();
            ObservableList<String> exerciseNames = FXCollections.observableArrayList();
            exerciseNames.add("All Exercises");
            exerciseNames.addAll(exercises.stream()
                    .map(Exercise::getName)
                    .sorted()
                    .collect(Collectors.toList()));
            exerciseFilterCombo.setItems(exerciseNames);
            exerciseFilterCombo.setValue("All Exercises");
        }
    }

    private void loadProgressData() {
        if (currentUser == null || currentUser.getId() == null) {
            showNoDataMessage();
            return;
        }

        // Clear existing data
        if (statsCardsContainer != null) statsCardsContainer.getChildren().clear();

        // Get date range
        LocalDateTime startDate = fromDatePicker.getValue().atStartOfDay();
        LocalDateTime endDate = toDatePicker.getValue().atTime(23, 59, 59);

        // Load workouts in date range
        List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId())
                .stream()
                .filter(w -> w.getStartTime().isAfter(startDate) && w.getStartTime().isBefore(endDate))
                .collect(Collectors.toList());

        if (workouts.isEmpty()) {
            showNoDataMessage();
            return;
        }

        // Display all progress sections
        displayOverviewStats(workouts);
        displayVolumeChart(workouts);
        displayExerciseChart(workouts);
        displayProgressTimeline(workouts);
        displayPersonalRecords();
        displayStrengthProgress(workouts);
        displayConsistencyStats(workouts);
    }

    private void displayOverviewStats(List<Workout> workouts) {
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.setPadding(new Insets(20));

        // Total Workouts
        VBox totalWorkouts = createStatCard("💪", String.valueOf(workouts.size()), "Total Workouts", "#4CAF50");

        // Total Volume
        double totalVolume = workouts.stream().mapToDouble(Workout::getTotalVolume).sum();
        VBox volumeCard = createStatCard("🏋️", String.format("%,.0f", totalVolume), "Total Volume (lbs)", "#2196F3");

        // Total Sets
        int totalSets = workouts.stream().mapToInt(Workout::getTotalSets).sum();
        VBox setsCard = createStatCard("📊", String.valueOf(totalSets), "Total Sets", "#FF9800");

        // Average Duration
        long avgDuration = (long) workouts.stream()
                .mapToLong(Workout::getDurationMinutes)
                .average()
                .orElse(0);
        VBox durationCard = createStatCard("⏱️", avgDuration + "min", "Avg Duration", "#9C27B0");

        statsRow.getChildren().addAll(totalWorkouts, volumeCard, setsCard, durationCard);

        if (statsCardsContainer != null) {
            statsCardsContainer.getChildren().add(statsRow);
        }
    }

    private void addIntelligenceActionsBar() {
        if (statsCardsContainer == null) return;

        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 20, 0, 20));

        Button btnPlan = new Button("🧠 Generate Plan");
        Button btnRecommend = new Button("✨ Exercise Suggestions");
        Button btnWeakness = new Button("⚠️ Detect Weaknesses");
        Button btnDiet = new Button("🥗 Diet Plan");

        String btnStyle = "-fx-background-color: #00d4ff; -fx-text-fill: #0b1020; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 14;";
        btnPlan.setStyle(btnStyle);
        btnRecommend.setStyle(btnStyle);
        btnWeakness.setStyle(btnStyle);
        btnDiet.setStyle(btnStyle);

        btnPlan.setOnAction(e -> onGeneratePlan());
        btnRecommend.setOnAction(e -> onRecommendExercises());
        btnWeakness.setOnAction(e -> onDetectWeaknesses());
        btnDiet.setOnAction(e -> onDietPlan());

        bar.getChildren().addAll(btnPlan, btnRecommend, btnWeakness, btnDiet);
        statsCardsContainer.getChildren().add(0, bar);
    }

    private void onGeneratePlan() {
        if (currentUser == null || currentUser.getId() == null) return;
        new AIInsightsWindow(currentUser.getId(), dbHelper).show("Plan");
    }

    private void onRecommendExercises() {
        if (currentUser == null || currentUser.getId() == null) return;
        new AIInsightsWindow(currentUser.getId(), dbHelper).show("Recommendations");
    }

    private void onDetectWeaknesses() {
        if (currentUser == null || currentUser.getId() == null) return;
        new AIInsightsWindow(currentUser.getId(), dbHelper).show("Weaknesses");
    }

    private void onDietPlan() {
        new AIInsightsWindow(currentUser.getId(), dbHelper).show("Diet");
    }

    

    private VBox createStatCard(String icon, String value, String label, String color) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + color + ", " + adjustColor(color) + ");" +
                        "-fx-padding: 20px 30px;" +
                        "-fx-background-radius: 15px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);"
        );
        card.setMinWidth(180);
        card.setMinHeight(120);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 36px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label descLabel = new Label(label);
        descLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        descLabel.setWrapText(true);

        card.getChildren().addAll(iconLabel, valueLabel, descLabel);

        // Hover animation
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }

    private void displayVolumeChart(List<Workout> workouts) {
        if (volumeChart == null) return;

        volumeChart.getData().clear();
        volumeChart.setTitle("📈 Volume Progress Over Time");
        volumeChart.setLegendVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Total Volume");

        // Group workouts by week
        Map<String, Double> weeklyData = workouts.stream()
                .collect(Collectors.groupingBy(
                        w -> "Week " + getWeekNumber(w.getStartTime()),
                        Collectors.summingDouble(Workout::getTotalVolume)
                ));

        weeklyData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

        volumeChart.getData().add(series);
        applyChartStyle(volumeChart);
    }

    private void displayExerciseChart(List<Workout> workouts) {
        if (exerciseChart == null) return;

        exerciseChart.getData().clear();
        exerciseChart.setTitle("🏆 Top Exercises by Volume");
        exerciseChart.setLegendVisible(false);

        // Get top 5 exercises by volume
        Map<String, Double> exerciseVolumes = new HashMap<>();

        for (Workout workout : workouts) {
            for (WorkoutSet set : workout.getSets()) {
                String exerciseName = set.getExercise().getName();
                double volume = set.getVolume();
                exerciseVolumes.merge(exerciseName, volume, Double::sum);
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        exerciseVolumes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

        exerciseChart.getData().add(series);
        applyChartStyle(exerciseChart);
    }

    private void displayProgressTimeline(List<Workout> workouts) {
        if (progressChart == null) return;

        progressChart.getData().clear();
        progressChart.setTitle("📅 Workout Frequency Timeline");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Workouts per Week");

        Map<String, Long> weeklyWorkouts = workouts.stream()
                .collect(Collectors.groupingBy(
                        w -> "W" + getWeekNumber(w.getStartTime()),
                        Collectors.counting()
                ));

        weeklyWorkouts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

        progressChart.getData().add(series);
        applyChartStyle(progressChart);
    }

    private void displayPersonalRecords() {
        if (currentUser == null || prTableView == null) return;

        Map<String, Double> prs = dbHelper.getPersonalRecords(currentUser.getId());

        ObservableList<PersonalRecordEntry> prEntries = FXCollections.observableArrayList();

        prs.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    prEntries.add(new PersonalRecordEntry(
                            entry.getKey(),
                            String.format("%.1f lbs", entry.getValue()),
                            "Recent"
                    ));
                });

        prTableView.setItems(prEntries);
    }

    private void setupPersonalRecordsTable() {
        if (prTableView == null) return;

        exerciseColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExercise()));

        weightColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getWeight()));

        dateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));

        prTableView.setPlaceholder(new Label("No personal records yet. Start lifting! 💪"));
    }

    private void displayStrengthProgress(List<Workout> workouts) {
        Map<String, Double> maxWeights = calculateMaxWeights(workouts);
        Map<String, Double> previousMaxWeights = calculatePreviousMaxWeights(workouts);

        if (personalRecordsContainer != null) {
            personalRecordsContainer.getChildren().clear();

            if (maxWeights.isEmpty()) {
                Label noData = new Label("🏋️ No strength data yet!\n\nStart logging workouts to track your progress!");
                noData.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 20px;");
                noData.setWrapText(true);
                personalRecordsContainer.getChildren().add(noData);
                return;
            }

            for (Map.Entry<String, Double> entry : maxWeights.entrySet()) {
                String exercise = entry.getKey();
                double currentMax = entry.getValue();
                double previousMax = previousMaxWeights.getOrDefault(exercise, 0.0);
                double improvement = currentMax - previousMax;

                HBox prCard = createPRCard(exercise, currentMax, improvement);
                personalRecordsContainer.getChildren().add(prCard);
            }
        }
    }

    private HBox createPRCard(String exercise, double weight, double improvement) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle(
                "-fx-background-color: #2b2b2b;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: " + (improvement > 0 ? "#4CAF50" : "#2196F3") + ";" +
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

        Label improvementLabel = new Label(
                improvement > 0 ? String.format("⬆️ +%.1f lbs", improvement) : "New PR! 🎉"
        );
        improvementLabel.setStyle(
                "-fx-text-fill: " + (improvement > 0 ? "#4CAF50" : "#FFD700") + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;"
        );

        card.getChildren().addAll(exerciseLabel, weightLabel, spacer, improvementLabel);
        return card;
    }

    private void displayConsistencyStats(List<Workout> workouts) {
        int currentStreak = calculateCurrentStreak(workouts);
        int longestStreak = calculateLongestStreak(workouts);
        int workoutsThisMonth = (int) workouts.stream()
                .filter(w -> w.getStartTime().isAfter(LocalDateTime.now().minusMonths(1)))
                .count();

        if (bodyMetricsContainer != null) {
            bodyMetricsContainer.getChildren().clear();

            VBox streakCard = createConsistencyCard(
                    "🔥 Current Streak",
                    currentStreak + " days",
                    "#FF5722"
            );

            VBox longestCard = createConsistencyCard(
                    "🏆 Longest Streak",
                    longestStreak + " days",
                    "#FFD700"
            );

            VBox monthCard = createConsistencyCard(
                    "📅 This Month",
                    workoutsThisMonth + " workouts",
                    "#4CAF50"
            );

            HBox consistencyRow = new HBox(15, streakCard, longestCard, monthCard);
            consistencyRow.setAlignment(Pos.CENTER);
            bodyMetricsContainer.getChildren().add(consistencyRow);
        }
    }

    private VBox createConsistencyCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " + color + ", " + adjustColor(color) + ");" +
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

    // Helper methods

    private Map<String, Double> calculateMaxWeights(List<Workout> workouts) {
        Map<String, Double> maxWeights = new HashMap<>();

        for (Workout workout : workouts) {
            for (WorkoutSet set : workout.getSets()) {
                String exerciseName = set.getExercise().getName();
                double weight = set.getWeight();
                maxWeights.put(exerciseName, Math.max(maxWeights.getOrDefault(exerciseName, 0.0), weight));
            }
        }

        return maxWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Double> calculatePreviousMaxWeights(List<Workout> workouts) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        return workouts.stream()
                .filter(w -> w.getStartTime().isBefore(monthAgo))
                .flatMap(w -> w.getSets().stream())
                .collect(Collectors.groupingBy(
                        set -> set.getExercise().getName(),
                        Collectors.mapping(WorkoutSet::getWeight, Collectors.maxBy(Double::compare))
                ))
                .entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private int calculateCurrentStreak(List<Workout> workouts) {
        if (workouts.isEmpty()) return 0;

        List<LocalDate> workoutDates = workouts.stream()
                .map(w -> w.getStartTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int streak = 0;
        LocalDate checkDate = LocalDate.now();

        for (LocalDate date : workoutDates) {
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

        List<LocalDate> workoutDates = workouts.stream()
                .map(w -> w.getStartTime().toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < workoutDates.size(); i++) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(workoutDates.get(i - 1), workoutDates.get(i));

            if (daysBetween <= 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    private int getWeekNumber(LocalDateTime date) {
        return date.toLocalDate().get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
    }

    private void applyChartStyle(Chart chart) {
        chart.setStyle(
                "-fx-background-color: #2b2b2b;" +
                        "-fx-border-color: #444444;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;"
        );
    }

    private String adjustColor(String color) {
        return switch (color) {
            case "#4CAF50" -> "#2E7D32";
            case "#2196F3" -> "#1565C0";
            case "#FF9800" -> "#E65100";
            case "#9C27B0" -> "#6A1B9A";
            case "#FF5722" -> "#D84315";
            case "#FFD700" -> "#FFA000";
            default -> color;
        };
    }

    private void showNoDataMessage() {
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
        public String getDate() { return date; }
    }
}
