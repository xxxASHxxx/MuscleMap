package com.musclemmap.services;

import com.musclemmap.models.Exercise;
import com.musclemmap.models.Workout;
import com.musclemmap.models.WorkoutSet;
import com.musclemmap.utils.DatabaseHelper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intelligent Workout Generator (IWG)
 *
 * Generates an adaptive workout plan using historical performance, muscle balance, and schedule constraints.
 * This class is designed to be UI-agnostic and only depends on existing models and DatabaseHelper.
 */
/**
 * Intelligent Workout Generator (IWG)
 * Generates adaptive workout plans using history, muscle balance, and schedule constraints.
 * Robust, extensible, and UI-agnostic.
 */
public class IntelligentWorkoutGenerator {

    public static class SessionPlan {
        public String name;
        public LocalDate date;
        public int targetDurationMinutes;
        public List<PlannedExercise> exercises = new ArrayList<>();
    }

    public static class PlannedExercise {
        public Exercise exercise;
        public int sets;
        public int targetReps;
        public double suggestedWeight;
        public String notes;
    }

    public static class WorkoutPlan {
        public int userId;
        public List<SessionPlan> sessions = new ArrayList<>();
    }

    private final DatabaseHelper db;

    public IntelligentWorkoutGenerator() {
        this.db = DatabaseHelper.getInstance();
    }

    /**
     * Generate a weekly plan starting from the next Monday (or today if daysPerWeek == 1).
     *
     * @param userId current user id
     * @param daysPerWeek number of sessions per week (1-6)
     * @param minutesPerSession target duration per session
     * @return WorkoutPlan with session breakdown
     */
    /**
     * Generate a weekly plan for a user. Validates input and supports custom splits.
     */
    public WorkoutPlan generatePlanForUser(int userId, int daysPerWeek, int minutesPerSession) {
        if (userId <= 0 || daysPerWeek < 1 || minutesPerSession < 10) return new WorkoutPlan();
        int clampedDays = Math.max(1, Math.min(6, daysPerWeek));

        List<Workout> history = db.getWorkoutsByUserId(userId);
        List<Exercise> allExercises = db.getAllExercises();
        if (allExercises == null || allExercises.isEmpty()) return new WorkoutPlan();

        Map<String, Double> avgVolumeByMuscle = estimateAverageVolumeByMuscle(history);
        Map<String, Double> topWeightByExercise = db.getPersonalRecords(userId);

        List<String> split = buildSplit(clampedDays);

        WorkoutPlan plan = new WorkoutPlan();
        plan.userId = userId;

        LocalDate start = nextMonday(LocalDate.now());
        for (int i = 0; i < clampedDays; i++) {
            String dayFocus = split.get(i);
            SessionPlan session = new SessionPlan();
            session.name = dayFocus + " Session";
            session.date = start.plusDays(i);
            session.targetDurationMinutes = minutesPerSession;

            // Build exercise list per focus
            List<Exercise> candidate = selectExercisesForFocus(allExercises, dayFocus);
            if (candidate.isEmpty()) continue;

            // Choose 5-7 exercises balanced across focus groups
            int targetExercises = Math.max(4, Math.min(7, (int) Math.round(minutesPerSession / 12.0)));
            List<Exercise> chosen = pickDiverse(candidate, targetExercises);

            for (Exercise e : chosen) {
                PlannedExercise pe = new PlannedExercise();
                pe.exercise = e;
                pe.sets = suggestSets(avgVolumeByMuscle.getOrDefault(e.getMuscleGroup(), 0.0));
                pe.targetReps = suggestReps(e.getDifficulty());
                pe.suggestedWeight = suggestWeight(topWeightByExercise, e.getName(), pe.targetReps);
                pe.notes = "Progressive overload: aim +1 rep or +2.5% weight vs last meso";
                session.exercises.add(pe);
            }

            plan.sessions.add(session);
        }

        return plan;
    }

    /**
     * Convert a SessionPlan into a concrete Workout model instance you can persist.
     */
    public Workout materializeWorkout(SessionPlan session) {
        Workout w = new Workout(session.name);
        w.setStartTime(LocalDateTime.now());
        List<WorkoutSet> sets = new ArrayList<>();
        for (PlannedExercise pe : session.exercises) {
            for (int i = 0; i < pe.sets; i++) {
                WorkoutSet s = new WorkoutSet(pe.exercise, pe.targetReps, pe.suggestedWeight);
                sets.add(s);
            }
        }
        w.setSets(sets);
        return w;
    }

    private Map<String, Double> estimateAverageVolumeByMuscle(List<Workout> history) {
        Map<String, Double> sums = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (Workout w : history) {
            if (w.getSets() == null) continue;
            Map<String, Double> volByMuscle = new HashMap<>();
            for (WorkoutSet s : w.getSets()) {
                if (s.getExercise() == null) continue;
                String mg = s.getExercise().getMuscleGroup();
                double volume = s.getWeight() * s.getReps();
                volByMuscle.merge(mg, volume, Double::sum);
            }
            for (Map.Entry<String, Double> e : volByMuscle.entrySet()) {
                sums.merge(e.getKey(), e.getValue(), Double::sum);
                counts.merge(e.getKey(), 1, Integer::sum);
            }
        }
        Map<String, Double> avg = new HashMap<>();
        for (String k : sums.keySet()) {
            avg.put(k, sums.get(k) / Math.max(1, counts.getOrDefault(k, 1)));
        }
        return avg;
    }

    private List<String> buildSplit(int days) {
        // Common sensible splits
        switch (days) {
            case 1:
                return List.of("Full Body");
            case 2:
                return List.of("Upper", "Lower");
            case 3:
                return List.of("Push", "Pull", "Legs");
            case 4:
                return List.of("Upper", "Lower", "Push", "Pull");
            case 5:
                return List.of("Upper", "Lower", "Push", "Pull", "Arms/Core");
            default:
                return List.of("Push", "Pull", "Legs", "Upper", "Lower", "Arms/Core");
        }
    }

    private LocalDate nextMonday(LocalDate from) {
        if (from.getDayOfWeek() == DayOfWeek.MONDAY) return from;
        int add = (DayOfWeek.MONDAY.getValue() - from.getDayOfWeek().getValue() + 7) % 7;
        return from.plusDays(add);
    }

    private List<Exercise> selectExercisesForFocus(List<Exercise> all, String focus) {
        Set<String> groups = new HashSet<>();
        switch (focus) {
            case "Full Body":
                groups.addAll(List.of("Chest", "Back", "Shoulders", "Quadriceps", "Hamstrings", "Glutes", "Core"));
                break;
            case "Upper":
                groups.addAll(List.of("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Core"));
                break;
            case "Lower":
                groups.addAll(List.of("Quadriceps", "Hamstrings", "Glutes", "Calves", "Core"));
                break;
            case "Push":
                groups.addAll(List.of("Chest", "Shoulders", "Triceps"));
                break;
            case "Pull":
                groups.addAll(List.of("Back", "Biceps", "Rear Delts"));
                break;
            case "Legs":
                groups.addAll(List.of("Quadriceps", "Hamstrings", "Glutes", "Calves"));
                break;
            case "Arms/Core":
                groups.addAll(List.of("Biceps", "Triceps", "Forearms", "Core"));
                break;
            default:
                groups.add("Chest");
        }
        return all.stream()
                .filter(e -> e.getMuscleGroup() != null && groups.contains(e.getMuscleGroup()))
                .collect(Collectors.toList());
    }

    private List<Exercise> pickDiverse(List<Exercise> candidates, int n) {
        Map<String, List<Exercise>> byGroup = candidates.stream().collect(Collectors.groupingBy(Exercise::getMuscleGroup));
        List<Exercise> result = new ArrayList<>();
        Random rnd = new Random();
        // Round-robin across groups for diversity
        while (result.size() < n && !byGroup.isEmpty()) {
            for (String g : new ArrayList<>(byGroup.keySet())) {
                List<Exercise> list = byGroup.get(g);
                if (list == null || list.isEmpty()) {
                    byGroup.remove(g);
                    continue;
                }
                Exercise e = list.remove(rnd.nextInt(list.size()));
                result.add(e);
                if (result.size() >= n) break;
                if (list.isEmpty()) byGroup.remove(g);
            }
        }
        // If still short, fill with remaining
        if (result.size() < n) {
            result.addAll(candidates.stream()
                    .filter(e -> !result.contains(e))
                    .limit(Math.max(0, n - result.size()))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    private int suggestSets(double avgVolumeForMuscle) {
        if (avgVolumeForMuscle <= 0) return 3;
        if (avgVolumeForMuscle < 2000) return 3;
        if (avgVolumeForMuscle < 4000) return 4;
        if (avgVolumeForMuscle < 6000) return 5;
        return 6;
    }

    private int suggestReps(String difficulty) {
        if (difficulty == null) return 8;
        String d = difficulty.toLowerCase();
        if (d.contains("beginner")) return 10;
        if (d.contains("intermediate")) return 8;
        if (d.contains("advanced")) return 6;
        return 8;
    }

    private double suggestWeight(Map<String, Double> topWeightByExercise, String exerciseName, int reps) {
        double pr = topWeightByExercise.getOrDefault(exerciseName, 0.0);
        if (pr <= 0) {
            // Start conservative if no history
            return reps >= 10 ? 0.4 : 0.5; // This will likely be overridden by user; keep non-zero to avoid divide issues
        }
        // Simple progression target: 80-85% of best single for sets of 6-10
        double intensity;
        if (reps >= 10) intensity = 0.7;
        else if (reps >= 8) intensity = 0.75;
        else intensity = 0.8;
        return roundToNearest(pr * intensity, 2.5);
    }

    private double roundToNearest(double value, double step) {
        if (value <= 0) return 0.0;
        double q = Math.round(value / step);
        return q * step;
    }
}


