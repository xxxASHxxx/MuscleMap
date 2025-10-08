package com.musclemmap.controllers;

import com.musclemmap.models.*;
import com.musclemmap.utils.DatabaseHelper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
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

    public void initialize() {
        dbHelper = DatabaseHelper.getInstance();

        // Set default date range (last 3 months)
        fromDatePicker.setValue(LocalDate.now().minusMonths(3));
        toDatePicker.setValue(LocalDate.now());

        // Add listeners
        exerciseFilterCombo.setOnAction(e -> loadProgressData());
        fromDatePicker.setOnAction(e -> loadProgressData());
        toDatePicker.setOnAction(e -> loadProgressData());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadProgressData();
    }

    private void loadProgressData() {
        if (currentUser == null || currentUser.getId() == null) {
            return;
        }

        progressChartsBox.getChildren().clear();

        // Get date range
        LocalDateTime startDate = fromDatePicker.getValue().atStartOfDay();
        LocalDateTime endDate = toDatePicker.getValue().atTime(23, 59, 59);

        // Load workouts in date range
        List<Workout> workouts = dbHelper.getWorkoutsByUserId(currentUser.getId())
                .stream()
                .filter(w -> w.getStartTime().isAfter(startDate) &&
                        w.getStartTime().isBefore(endDate))
                .collect(Collectors.toList());

        // Calculate real statistics
        displayStrengthProgress(workouts);
        displayVolumeProgress(workouts);
        displayConsistencyStats(workouts);
        displayBodyMetrics();
    }

    private void displayStrengthProgress(List<Workout> workouts) {
        Map<String, Double> maxWeights = calculateMaxWeights(workouts);
        Map<String, Double> previousMaxWeights = calculatePreviousMaxWeights(workouts);

        StringBuilder strengthText = new StringBuilder();

        if (maxWeights.isEmpty()) {
            strengthText.append("💪 No strength data yet\n\n");
            strengthText.append("📊 Start logging workouts to track progress!\n");
            strengthText.append("🎯 Track: Bench Press, Squats, Deadlifts\n");
            strengthText.append("🔥 Watch your numbers grow!");
        } else {
            int prCount = 0;
            for (Map.Entry<String, Double> entry : maxWeights.entrySet()) {
                String exercise = entry.getKey();
                double currentMax = entry.getValue();
                double previousMax = previousMaxWeights.getOrDefault(exercise, 0.0);
                double improvement = currentMax - previousMax;

                strengthText.append(String.format("🏋️ %s: %.0f lbs", exercise, currentMax));

                if (improvement > 0) {
                    strengthText.append(String.format(" (+%.0f lbs) 🔥\n", improvement));
                    prCount++;
                } else {
                    strengthText.append("\n");
                }
            }
            strengthText.append(String.format("\n🏆 Total PRs this period: %d", prCount));
        }

        VBox strengthCard = createProgressCard(
                "💪 Strength Progress",
                strengthText.toString(),
                "#4CAF50"
        );
        progressChartsBox.getChildren().add(strengthCard);
    }

    private void displayVolumeProgress(List<Workout> workouts) {
        if (workouts.isEmpty()) {
            VBox volumeCard = createProgressCard(
                    "📊 Weekly Volume",
                    "📈 No volume data yet\n\n" +
                            "🎯 Complete workouts to track volume\n" +
                            "💪 Volume = Weight × Reps × Sets\n" +
                            "🔥 Higher volume = More gains!",
                    "#2196F3"
            );
            progressChartsBox.getChildren().add(volumeCard);
            return;
        }

        // Calculate current week volume
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        double currentWeekVolume = workouts.stream()
                .filter(w -> w.getStartTime().isAfter(weekStart))
                .mapToDouble(Workout::getTotalVolume)
                .sum();

        // Calculate previous week volume
        LocalDateTime prevWeekStart = LocalDateTime.now().minusDays(14);
        double previousWeekVolume = workouts.stream()
                .filter(w -> w.getStartTime().isAfter(prevWeekStart) &&
                        w.getStartTime().isBefore(weekStart))
                .mapToDouble(Workout::getTotalVolume)
                .sum();

        // Calculate average
        double avgVolume = workouts.stream()
                .mapToDouble(Workout::getTotalVolume)
                .average()
                .orElse(0.0);

        double improvement = previousWeekVolume > 0
                ? ((currentWeekVolume - previousWeekVolume) / previousWeekVolume * 100)
                : 0.0;

        String volumeText = String.format(
                "💪 This week: %,. 0f lbs\n" +
                        "📉 Last week: %,.0f lbs\n" +
                        "%s Improvement: %+.1f%%\n" +
                        "📊 Monthly average: %,.0f lbs",
                currentWeekVolume,
                previousWeekVolume,
                improvement >= 0 ? "🔥" : "⚠️",
                improvement,
                avgVolume
        );

        VBox volumeCard = createProgressCard("📊 Weekly Volume", volumeText, "#2196F3");
        progressChartsBox.getChildren().add(volumeCard);
    }

    private void displayConsistencyStats(List<Workout> workouts) {
        int currentStreak = calculateCurrentStreak(workouts);
        int longestStreak = calculateLongestStreak(workouts);
        int workoutsThisMonth = (int) workouts.stream()
                .filter(w -> w.getStartTime().isAfter(LocalDateTime.now().minusMonths(1)))
                .count();
        int goalWorkouts = 20;

        String consistencyText = String.format(
                "🔥 Current streak: %d days\n" +
                        "🏆 Longest streak: %d days\n" +
                        "📅 Workouts this month: %d\n" +
                        "🎯 Goal: %d workouts/month\n" +
                        "%s Progress: %.0f%%",
                currentStreak,
                longestStreak,
                workoutsThisMonth,
                goalWorkouts,
                workoutsThisMonth >= goalWorkouts ? "✅" : "💪",
                (workoutsThisMonth / (double) goalWorkouts * 100)
        );

        VBox streakCard = createProgressCard("🔥 Consistency", consistencyText, "#FF5722");
        progressChartsBox.getChildren().add(streakCard);
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

    private Map<String, Double> calculateMaxWeights(List<Workout> workouts) {
        Map<String, Double> maxWeights = new HashMap<>();

        for (Workout workout : workouts) {
            for (WorkoutSet set : workout.getSets()) {
                String exerciseName = set.getExercise().getName();
                double weight = set.getWeight();
                maxWeights.put(exerciseName,
                        Math.max(maxWeights.getOrDefault(exerciseName, 0.0), weight));
            }
        }

        // Return top 4 exercises
        return maxWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Double> calculatePreviousMaxWeights(List<Workout> workouts) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        return workouts.stream()
                .filter(w -> w.getStartTime().isBefore(monthAgo))
                .flatMap(w -> w.getSets().stream())
                .collect(Collectors.groupingBy(
                        set -> set.getExercise().getName(),
                        Collectors.mapping(
                                WorkoutSet::getWeight,
                                Collectors.maxBy(Double::compare)
                        )
                ))
                .entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));
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
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    workoutDates.get(i - 1), workoutDates.get(i));

            if (daysBetween == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    private VBox createProgressCard(String title, String content, String color) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + color + ", " +
                        adjustBrightness(color, 0.7) + "); " +
                        "-fx-padding: 20px; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 5);"
        );
        card.setPrefWidth(320);
        card.setMinHeight(150);
        VBox.setMargin(card, new Insets(10));

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 2);"
        );

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 15px; " +
                        "-fx-line-spacing: 5px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 2, 0, 0, 1);"
        );

        card.getChildren().addAll(titleLabel, contentLabel);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.03);
            card.setScaleY(1.03);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }

    private String adjustBrightness(String color, double factor) {
        return switch (color) {
            case "#4CAF50" -> "#2E7D32";
            case "#2196F3" -> "#1565C0";
            case "#FF5722" -> "#D84315";
            case "#9C27B0" -> "#7B1FA2";
            default -> color;
        };
    }
}
