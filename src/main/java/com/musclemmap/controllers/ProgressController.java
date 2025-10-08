package com.musclemmap.controllers;

import com.musclemmap.models.*;
import com.musclemmap.utils.DatabaseHelper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ProgressController {

    @FXML private VBox progressChartsBox;
    @FXML private ComboBox<String> exerciseFilterCombo;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    private DatabaseHelper dbHelper;
    private User currentUser;

    public void initializeComponents(VBox progressChartsBox,
                                     ComboBox<String> exerciseFilterCombo,
                                     DatePicker fromDatePicker,
                                     DatePicker toDatePicker) {
        this.progressChartsBox = progressChartsBox;
        this.exerciseFilterCombo = exerciseFilterCombo;
        this.fromDatePicker = fromDatePicker;
        this.toDatePicker = toDatePicker;

        // Now initialize with received components
        initialize();
    }
    public void initialize() {
        dbHelper = DatabaseHelper.getInstance();

        // Set default date range (last 3 months)
        if (fromDatePicker != null) {
            fromDatePicker.setValue(LocalDate.now().minusMonths(3));
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(LocalDate.now());
        }

        // Add listeners
        if (exerciseFilterCombo != null) {
            exerciseFilterCombo.setOnAction(e -> loadProgressData());
        }
        if (fromDatePicker != null) {
            fromDatePicker.setOnAction(e -> loadProgressData());
        }
        if (toDatePicker != null) {
            toDatePicker.setOnAction(e -> loadProgressData());
        }
    }
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("🔄 ProgressController: User set - " +
                (user != null ? user.getUsername() + " (ID: " + user.getId() + ")" : "null"));

        if (user != null && user.getId() != null) {
            loadProgressData();
        } else {
            System.out.println("⚠️ Cannot load progress: User or User ID is null");
            showNoDataMessage();
        }
    }
    private void displayWorkoutFrequency(List<Workout> workouts) {
        // Group by day of week
        Map<DayOfWeek, Long> frequencyMap = workouts.stream()
                .collect(Collectors.groupingBy(
                        w -> w.getStartTime().getDayOfWeek(),
                        Collectors.counting()
                ));

        StringBuilder frequencyText = new StringBuilder();
        frequencyText.append("📅 Most Active Days:\n\n");

        frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<DayOfWeek, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    frequencyText.append(String.format("%s: %d workouts\n",
                            entry.getKey(), entry.getValue()));
                });

        int avgPerWeek = workouts.size() /
                Math.max(1, (int)java.time.temporal.ChronoUnit.WEEKS.between(
                        workouts.get(workouts.size()-1).getStartTime(),
                        LocalDateTime.now()));

        frequencyText.append(String.format("\n📊 Average: %d workouts/week", avgPerWeek));

        VBox frequencyCard = createProgressCard(
                "📆 Workout Schedule",
                frequencyText.toString(),
                "#FFA726"
        );
        progressChartsBox.getChildren().add(frequencyCard);
    }
    private void displayVolumeProgression(List<Workout> workouts) {
        if (workouts.size() < 2) return;

        // Group by week
        Map<LocalDate, Double> weeklyVolume = new TreeMap<>();

        for (Workout w : workouts) {
            LocalDate weekStart = w.getStartTime().toLocalDate()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeklyVolume.merge(weekStart, w.getTotalVolume(), Double::sum);
        }

        StringBuilder progressText = new StringBuilder();
        progressText.append("📈 Last 4 Weeks:\n\n");

        weeklyVolume.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Double>comparingByKey().reversed())
                .limit(4)
                .forEach(entry -> {
                    progressText.append(String.format("Week of %s: %,.0f lbs\n",
                            entry.getKey().format(DateTimeFormatter.ofPattern("MMM dd")),
                            entry.getValue()));
                });

        // Calculate trend
        List<Double> volumes = new ArrayList<>(weeklyVolume.values());
        double trend = volumes.size() > 1 ?
                ((volumes.get(0) - volumes.get(volumes.size()-1)) / volumes.get(volumes.size()-1) * 100) : 0;

        progressText.append(String.format("\n%s Overall trend: %+.1f%%",
                trend > 0 ? "📈" : "📉", trend));

        VBox progressCard = createProgressCard(
                "📊 Volume Progression",
                progressText.toString(),
                "#7E57C2"
        );
        progressChartsBox.getChildren().add(progressCard);
    }

    private void loadProgressData() {
        if (currentUser == null || currentUser.getId() == null) {
            System.out.println("⚠️ No current user set in ProgressController");
            showNoDataMessage();
            return;
        }

        progressChartsBox.getChildren().clear();

        // Get date range
        LocalDateTime startDate = fromDatePicker.getValue().atStartOfDay();
        LocalDateTime endDate = toDatePicker.getValue().atTime(23, 59, 59);

        // Load workouts in date range
        System.out.println("🔄 Loading workouts for user ID: " + currentUser.getId());
        System.out.println("📅 Date range: " + startDate.toLocalDate() + " to " + endDate.toLocalDate());

        List<Workout> allWorkouts = dbHelper.getWorkoutsByUserId(currentUser.getId());
        System.out.println("📊 Total workouts from DB: " + allWorkouts.size());

        List<Workout> workouts = allWorkouts.stream()
                .filter(w -> w.getStartTime().isAfter(startDate) &&
                        w.getStartTime().isBefore(endDate))
                .collect(Collectors.toList());

        System.out.println("📊 Found " + workouts.size() + " workouts in date range");

        // ⭐ ENHANCED DEBUG - Show workout details
        if (!workouts.isEmpty()) {
            System.out.println("📋 Workout details:");
            for (Workout w : workouts) {
                System.out.println("  - " + w.getName() +
                        " | Date: " + w.getStartTime().toLocalDate() +
                        " | Sets: " + w.getSets().size() +
                        " | Volume: " + w.getTotalVolume() + " lbs");
            }
        } else {
            System.out.println("⚠️ No workouts found!");
            System.out.println("   Check if workouts exist with dates between " +
                    startDate.toLocalDate() + " and " + endDate.toLocalDate());
        }

        if (workouts.isEmpty()) {
            showNoDataMessage();
            return;
        }

        // Calculate real statistics
        try {
            displayStrengthProgress(workouts);
            displayVolumeProgress(workouts);
            displayConsistencyStats(workouts);
            displayRecentWorkouts(workouts);
            System.out.println("✅ Progress data displayed successfully");
        } catch (Exception e) {
            System.err.println("❌ Error displaying progress: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showNoDataMessage() {
        VBox noDataCard = createProgressCard(
                "📊 No Data Yet",
                "🏋️ Start logging workouts to see progress!\n\n" +
                        "💪 Complete a workout in the Workout tab\n" +
                        "📈 Track your strength and volume gains\n" +
                        "🔥 Build your workout streak!\n\n" +
                        "⚠️ Make sure to LOG SETS during workout!",
                "#FF5722"
        );
        progressChartsBox.getChildren().add(noDataCard);
        System.out.println("📊 Showing 'No Data Yet' message");
    }

    private void displayStrengthProgress(List<Workout> workouts) {
        Map<String, Double> maxWeights = calculateMaxWeights(workouts);
        StringBuilder strengthText = new StringBuilder();

        if (maxWeights.isEmpty()) {
            strengthText.append("💪 No strength data yet\n\n");
            strengthText.append("📊 Log sets with weights to track PRs!");
        } else {
            strengthText.append("🏆 Personal Records:\n\n");
            int count = 0;
            for (Map.Entry<String, Double> entry : maxWeights.entrySet()) {
                if (count >= 5) break; // Show top 5
                strengthText.append(String.format("🏋️ %s: %.0f lbs\n",
                        entry.getKey(), entry.getValue()));
                count++;
            }
            strengthText.append("\n💪 Keep pushing those PRs!");
        }

        VBox strengthCard = createProgressCard("💪 Strength Progress",
                strengthText.toString(), "#4CAF50");
        progressChartsBox.getChildren().add(strengthCard);
    }

    private void displayVolumeProgress(List<Workout> workouts) {
        // Calculate total volume
        double totalVolume = workouts.stream()
                .mapToDouble(Workout::getTotalVolume)
                .sum();

        // Calculate average per workout
        double avgVolume = totalVolume / workouts.size();

        // Find best workout
        double maxVolume = workouts.stream()
                .mapToDouble(Workout::getTotalVolume)
                .max().orElse(0.0);

        String volumeText = String.format(
                "📊 Total Volume: %,.0f lbs\n" +
                        "📈 Average/Workout: %,.0f lbs\n" +
                        "🏆 Best Session: %,.0f lbs\n" +
                        "🔥 Total Workouts: %d\n\n" +
                        "💪 Volume = Weight × Reps × Sets",
                totalVolume, avgVolume, maxVolume, workouts.size()
        );

        VBox volumeCard = createProgressCard("📊 Volume Stats", volumeText, "#2196F3");
        progressChartsBox.getChildren().add(volumeCard);
    }

    private void displayConsistencyStats(List<Workout> workouts) {
        int totalWorkouts = workouts.size();

        // Calculate days with workouts
        Set<LocalDate> workoutDates = workouts.stream()
                .map(w -> w.getStartTime().toLocalDate())
                .collect(Collectors.toSet());

        // Calculate streak (simplified)
        int currentStreak = calculateCurrentStreak(workouts);

        String consistencyText = String.format(
                "🔥 Current Streak: %d days\n" +
                        "📅 Total Workouts: %d\n" +
                        "📊 Workout Days: %d\n" +
                        "⏱️ Avg Duration: %s\n\n" +
                        "🎯 Keep the momentum going!",
                currentStreak, totalWorkouts, workoutDates.size(),
                calculateAvgDuration(workouts)
        );

        VBox streakCard = createProgressCard("🔥 Consistency", consistencyText, "#FF5722");
        progressChartsBox.getChildren().add(streakCard);
    }

    private void displayRecentWorkouts(List<Workout> workouts) {
        StringBuilder recentText = new StringBuilder("📋 Last 5 Workouts:\n\n");

        workouts.stream()
                .limit(5)
                .forEach(w -> {
                    recentText.append(String.format("💪 %s\n", w.getName()));
                    recentText.append(String.format("📅 %s\n",
                            w.getStartTime().toLocalDate()));
                    recentText.append(String.format("⏱️ %s | 📊 %.0f lbs\n\n",
                            w.getFormattedDuration(), w.getTotalVolume()));
                });

        VBox recentCard = createProgressCard("📋 Recent Activity",
                recentText.toString(), "#9C27B0");
        progressChartsBox.getChildren().add(recentCard);
    }

    private Map<String, Double> calculateMaxWeights(List<Workout> workouts) {
        Map<String, Double> maxWeights = new HashMap<>();

        for (Workout workout : workouts) {
            List<WorkoutSet> sets = workout.getSets();
            if (sets != null) {
                for (WorkoutSet set : sets) {
                    String exerciseName = set.getExercise().getName();
                    double weight = set.getWeight();
                    maxWeights.put(exerciseName,
                            Math.max(maxWeights.getOrDefault(exerciseName, 0.0), weight));
                }
            }
        }

        // Return sorted by weight (highest first)
        return maxWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
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

    private String calculateAvgDuration(List<Workout> workouts) {
        double avgMinutes = workouts.stream()
                .filter(w -> w.getEndTime() != null)
                .mapToLong(Workout::getDurationMinutes)
                .average()
                .orElse(0.0);

        return String.format("%.0f min", avgMinutes);
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

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

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
}
