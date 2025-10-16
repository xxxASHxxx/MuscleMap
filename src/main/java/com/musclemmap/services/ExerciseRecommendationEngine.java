package com.musclemmap.services;

import com.musclemmap.models.Exercise;
import com.musclemmap.models.Workout;
import com.musclemmap.models.WorkoutSet;
import com.musclemmap.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exercise Recommendation Engine (ERE)
 * Suggests exercises based on goal, equipment, and observed preferences.
 */
/**
 * Exercise Recommendation Engine (ERE)
 * Suggests exercises based on goal, equipment, and observed preferences.
 * Robust, extensible, and UI-agnostic.
 */
public class ExerciseRecommendationEngine {

    public static class RecommendationRequest {
        public int userId;
        public String goal; // e.g., "hypertrophy", "strength", "endurance", "rehab", "mobility"
        public Set<String> targetMuscleGroups = new HashSet<>();
        public Set<String> availableEquipment = new HashSet<>();
        public int limit = 8;

        public RecommendationRequest() {}
        public RecommendationRequest(int userId, String goal, Set<String> muscleGroups, Set<String> equipment, int limit) {
            this.userId = userId;
            this.goal = goal;
            if (muscleGroups != null) this.targetMuscleGroups.addAll(muscleGroups);
            if (equipment != null) this.availableEquipment.addAll(equipment);
            this.limit = Math.max(1, limit);
        }
    }

    public static class Recommendation {
        public Exercise exercise;
        public double score;
        public String rationale;

        public Recommendation() {}
        public Recommendation(Exercise exercise, double score, String rationale) {
            this.exercise = exercise;
            this.score = score;
            this.rationale = rationale;
        }
    }

    private final DatabaseHelper db;

    public ExerciseRecommendationEngine() {
        this.db = DatabaseHelper.getInstance();
    }

    /**
     * Recommend exercises for a user based on their goal, muscle groups, and equipment.
     * Returns a ranked list of recommendations with rationale.
     */
    public List<Recommendation> recommend(RecommendationRequest req) {
        if (req == null || req.userId <= 0) return List.of();
        List<Exercise> all = db.getAllExercises();
        if (all == null || all.isEmpty()) return List.of();
        List<Workout> history = db.getWorkoutsByUserId(req.userId);

        Map<String, Integer> usageCount = countExerciseUsage(history);
        Map<String, Double> improvementProxy = estimateImprovement(history);

        List<Recommendation> scored = new ArrayList<>();
        for (Exercise e : all) {
            if (e == null) continue;
            if (!req.targetMuscleGroups.isEmpty() && (e.getMuscleGroup() == null || !req.targetMuscleGroups.contains(e.getMuscleGroup()))) {
                continue;
            }
            if (!req.availableEquipment.isEmpty() && (e.getEquipment() == null || !req.availableEquipment.contains(e.getEquipment()))) {
                continue;
            }
            double score = 0.0;
            // Preference signals
            score += usageCount.getOrDefault(e.getName(), 0) * 0.25;
            // Improvement proxy
            score += Math.max(-2.0, Math.min(2.0, improvementProxy.getOrDefault(e.getName(), 0.0) / 5.0)) * 0.6;
            // Goal alignment
            score += goalAlignmentBonus(req.goal, e.getDifficulty()) * 1.0;
            // Novelty
            score += noveltyBonus(usageCount.getOrDefault(e.getName(), 0)) * 0.5;
            // Penalize overuse
            if (usageCount.getOrDefault(e.getName(), 0) > 15) score -= 0.2;
            // Small random tie-breaker
            score += (e.getName().hashCode() % 10) * 0.01;

            String rationale = buildRationale(e, req, usageCount, improvementProxy);
            scored.add(new Recommendation(e, score, rationale));
        }

        if (scored.isEmpty()) return List.of();
        return scored.stream()
                .sorted(Comparator.comparingDouble((Recommendation r) -> r.score).reversed())
                .limit(Math.max(1, req.limit))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> countExerciseUsage(List<Workout> history) {
        Map<String, Integer> count = new HashMap<>();
        for (Workout w : history) {
            if (w.getSets() == null) continue;
            for (WorkoutSet s : w.getSets()) {
                if (s.getExercise() == null) continue;
                count.merge(s.getExercise().getName(), 1, Integer::sum);
            }
        }
        return count;
    }

    private Map<String, Double> estimateImprovement(List<Workout> history) {
        // Approximate improvement: trend of top set weight across time
        Map<String, List<Double>> byExercise = new HashMap<>();
        for (Workout w : history) {
            if (w.getSets() == null) continue;
            Map<String, Double> maxByName = new HashMap<>();
            for (WorkoutSet s : w.getSets()) {
                if (s.getExercise() == null) continue;
                String name = s.getExercise().getName();
                maxByName.merge(name, s.getWeight(), Math::max);
            }
            for (Map.Entry<String, Double> e : maxByName.entrySet()) {
                byExercise.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
            }
        }
        Map<String, Double> trend = new HashMap<>();
        for (Map.Entry<String, List<Double>> e : byExercise.entrySet()) {
            List<Double> seq = e.getValue();
            if (seq.size() < 2) { trend.put(e.getKey(), 0.0); continue; }
            double first = seq.get(0);
            double last = seq.get(seq.size() - 1);
            trend.put(e.getKey(), last - first);
        }
        return trend;
    }

    private double goalAlignmentBonus(String goal, String difficulty) {
        if (goal == null) return 0.0;
        String g = goal.toLowerCase();
        double base = 0.0;
        if (g.contains("strength")) base = 0.3;
        if (g.contains("hypertrophy")) base = 0.25;
        if (g.contains("endurance") || g.contains("fat loss")) base = 0.2;
        if (g.contains("rehab")) base = 0.15;
        if (g.contains("mobility")) base = 0.15;
        if (difficulty != null && difficulty.toLowerCase().contains("advanced")) base += 0.05;
        return base;
    }

    private double noveltyBonus(int usage) {
        // Encourage variation if something is overused
        if (usage == 0) return 0.2;
        if (usage < 3) return 0.1;
        if (usage > 10) return -0.1;
        return 0.0;
    }

    private String buildRationale(Exercise e,
                                  RecommendationRequest req,
                                  Map<String, Integer> usageCount,
                                  Map<String, Double> improvementProxy) {
        List<String> reasons = new ArrayList<>();
        if (req.targetMuscleGroups != null && req.targetMuscleGroups.contains(e.getMuscleGroup())) reasons.add("targets desired muscle group");
        if (req.availableEquipment != null && req.availableEquipment.contains(e.getEquipment())) reasons.add("fits available equipment");
        int usage = usageCount.getOrDefault(e.getName(), 0);
        if (usage > 0) reasons.add("matches your past preferences");
        if (usage > 10) reasons.add("consider rotating for variety");
        double improv = improvementProxy.getOrDefault(e.getName(), 0.0);
        if (improv > 0.1) reasons.add("progressing on this movement");
        else if (improv < -0.1) reasons.add("may need assistance — progress stalled");
        if (reasons.isEmpty()) reasons.add("good diversity candidate");
        return String.join(", ", reasons);
    }
}


