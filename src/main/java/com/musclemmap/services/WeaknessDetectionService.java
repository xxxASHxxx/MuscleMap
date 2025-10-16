package com.musclemmap.services;

import com.musclemmap.models.Workout;
import com.musclemmap.models.WorkoutSet;
import com.musclemmap.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weakness Detection System (WDS)
 * Identifies imbalances using simple strength ratio heuristics.
 */
/**
 * Weakness Detection System (WDS)
 * Identifies imbalances using strength ratio heuristics. Robust and extensible.
 */
public class WeaknessDetectionService {

    public static class WeakArea {
        public String muscleGroup;
        public double score; // negative means weaker vs paired group
        public String note;
    }

    private final DatabaseHelper db;

    public WeaknessDetectionService() {
        this.db = DatabaseHelper.getInstance();
    }

    /**
     * Analyze user workout history for muscle imbalances. Returns sorted list of weak areas.
     */
    public List<WeakArea> analyzeUser(int userId) {
        if (userId <= 0) return List.of();
        List<Workout> history = db.getWorkoutsByUserId(userId);
        if (history == null || history.isEmpty()) return List.of();
        Map<String, Double> avgTopSetByMuscle = estimateTopSetByMuscle(history);

        // Ideal ratios (expanded)
        double chestToBack = ratio(avgTopSetByMuscle, "Chest", "Back");
        double quadsToHams = ratio(avgTopSetByMuscle, "Quadriceps", "Hamstrings");
        double shouldersToBack = ratio(avgTopSetByMuscle, "Shoulders", "Back");
        double bicepsToTriceps = ratio(avgTopSetByMuscle, "Biceps", "Triceps");
        double calvesToHams = ratio(avgTopSetByMuscle, "Calves", "Hamstrings");

        List<WeakArea> result = new ArrayList<>();
        addIfWeak(result, "Chest", chestToBack, 0.9, "Strength lag vs Back");
        addIfWeak(result, "Quadriceps", quadsToHams, 0.9, "Strength lag vs Hamstrings");
        addIfWeak(result, "Shoulders", shouldersToBack, 0.85, "Delts lag vs Back");
        addIfWeak(result, "Biceps", bicepsToTriceps, 0.85, "Biceps lag vs Triceps");
        addIfWeak(result, "Calves", calvesToHams, 0.7, "Calves lag vs Hamstrings");

        // Add rationale for each weak area
        for (WeakArea wa : result) {
            wa.note += " (ratio: " + String.format("%.2f", wa.score + (wa.score < 0 ? wa.score : 0)) + ")";
        }

        // Sort by severity (more negative first)
        result.sort((a, b) -> Double.compare(a.score, b.score));
        return result;
    }

    private Map<String, Double> estimateTopSetByMuscle(List<Workout> history) {
        Map<String, Double> top = new HashMap<>();
        Map<String, Integer> cnt = new HashMap<>();
        for (Workout w : history) {
            if (w.getSets() == null) continue;
            Map<String, Double> sessionTop = new HashMap<>();
            for (WorkoutSet s : w.getSets()) {
                if (s.getExercise() == null) continue;
                String mg = s.getExercise().getMuscleGroup();
                sessionTop.merge(mg, s.getWeight(), Math::max);
            }
            for (Map.Entry<String, Double> e : sessionTop.entrySet()) {
                top.merge(e.getKey(), e.getValue(), Double::sum);
                cnt.merge(e.getKey(), 1, Integer::sum);
            }
        }
        Map<String, Double> avg = new HashMap<>();
        for (String k : top.keySet()) {
            avg.put(k, top.get(k) / Math.max(1, cnt.getOrDefault(k, 1)));
        }
        return avg;
    }

    private double ratio(Map<String, Double> map, String a, String b) {
        double va = map.getOrDefault(a, 0.0);
        double vb = map.getOrDefault(b, 0.0);
        if (vb == 0) return 1.0; // avoid divide by zero: assume fine
        return va / vb;
    }

    private void addIfWeak(List<WeakArea> list, String muscle, double ratio, double target, String note) {
        if (ratio < target) {
            WeakArea wa = new WeakArea();
            wa.muscleGroup = muscle;
            wa.score = ratio - target; // negative = weaker
            wa.note = note;
            list.add(wa);
        }
    }
}


