package com.musclemmap.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intelligent Diet Generator (IDG)
 * Calculates calories/macros and produces a simple meal plan scaffold.
 * UI can render or extend these structures without modifying this class.
 */
/**
 * Intelligent Diet Generator (IDG)
 * Calculates calories/macros and produces a simple meal plan scaffold.
 * Extensible, robust, and UI-agnostic.
 */
public class IntelligentDietGenerator {

    public static class DietRequest {
        public String gender; // "male" | "female"
        public int age;
        public double heightCm;
        public double weightKg;
        public String activityLevel; // "sedentary", "light", "moderate", "high", "athlete"
        public String goal; // "muscle_gain", "fat_loss", "maintenance"
        public List<String> dietaryRestrictions = new ArrayList<>();
        public List<String> preferences = new ArrayList<>();
        public double budgetPerDay; // optional
        public int mealsPerDay = 3;
        public double workoutMinutesPerDay = 0; // can be fed from exercise logs externally

        public DietRequest() {}
        public DietRequest(String gender, int age, double heightCm, double weightKg, String activityLevel, String goal, List<String> restrictions, List<String> preferences, double budget, int meals, double workoutMinutes) {
            this.gender = gender;
            this.age = age;
            this.heightCm = heightCm;
            this.weightKg = weightKg;
            this.activityLevel = activityLevel;
            this.goal = goal;
            if (restrictions != null) this.dietaryRestrictions.addAll(restrictions);
            if (preferences != null) this.preferences.addAll(preferences);
            this.budgetPerDay = budget;
            this.mealsPerDay = Math.max(2, Math.min(6, meals));
            this.workoutMinutesPerDay = Math.max(0, workoutMinutes);
        }
    }

    public static class MacroPlan {
        public double calories;
        public double proteinG;
        public double carbsG;
        public double fatsG;
    }

    public static class MealItem {
        public String name;
        public double grams;
        public String notes;
    }

    public static class Meal {
        public String name;
        public List<MealItem> items = new ArrayList<>();
    }

    public static class MealPlan {
        public MacroPlan targets = new MacroPlan();
        public List<Meal> meals = new ArrayList<>();
        public List<String> shoppingList = new ArrayList<>();
    }

    /**
     * Calculate macro targets for a given diet request. Validates input and supports flexible macro splits.
     */
    public MacroPlan calculateTargets(DietRequest req) {
        if (req == null || req.weightKg <= 0 || req.heightCm <= 0 || req.age <= 0) return new MacroPlan();
        double bmr = mifflinStJeor(req.gender, req.weightKg, req.heightCm, req.age);
        double tdee = bmr * activityMultiplier(req.activityLevel);
        tdee += Math.min(300, req.workoutMinutesPerDay * 5);

        double calories;
        switch (req.goal) {
            case "muscle_gain":
                calories = tdee * 1.1; break;
            case "fat_loss":
                calories = tdee * 0.85; break;
            default:
                calories = tdee;
        }

        MacroPlan mp = new MacroPlan();
        mp.calories = round(calories, 0);

        // protein: allow override by preference
        double proteinPerKg = req.goal.equals("fat_loss") ? 2.0 : (req.goal.equals("muscle_gain") ? 1.8 : 1.6);
        if (req.preferences.contains("high_protein")) proteinPerKg += 0.2;
        mp.proteinG = round(req.weightKg * proteinPerKg, 0);

        // fats: allow override by preference
        double fatPct = req.preferences.contains("low_fat") ? 0.18 : 0.25;
        mp.fatsG = round((mp.calories * fatPct) / 9.0, 0);

        // carbs: remaining calories
        double remaining = mp.calories - (mp.proteinG * 4 + mp.fatsG * 9);
        mp.carbsG = round(Math.max(0, remaining) / 4.0, 0);
        return mp;
    }

    public MealPlan generateMealPlan(DietRequest req) {
        MacroPlan targets = calculateTargets(req);
        MealPlan plan = new MealPlan();
        plan.targets = targets;

        int meals = Math.max(2, Math.min(6, req.mealsPerDay));
        double proteinPerMeal = targets.proteinG / meals;
        double carbsPerMeal = targets.carbsG / meals;
        double fatsPerMeal = targets.fatsG / meals;

        for (int i = 1; i <= meals; i++) {
            Meal m = new Meal();
            m.name = "Meal " + i;
            m.items.addAll(suggestMealItems(req, proteinPerMeal, carbsPerMeal, fatsPerMeal));
            plan.meals.add(m);
        }

        plan.shoppingList.addAll(buildShoppingList(plan));
        return plan;
    }

    private List<MealItem> suggestMealItems(DietRequest req, double protein, double carbs, double fats) {
        List<MealItem> items = new ArrayList<>();
        boolean vegetarian = req.dietaryRestrictions.contains("vegetarian") || req.dietaryRestrictions.contains("vegan");
        boolean glutenFree = req.dietaryRestrictions.contains("gluten_free");
        boolean dairyFree = req.dietaryRestrictions.contains("dairy_free");

        if (!vegetarian) {
            MealItem chicken = new MealItem();
            chicken.name = "Chicken breast";
            chicken.grams = round(protein / 0.31 * 100, 0);
            chicken.notes = "Lean protein";
            items.add(chicken);
        } else {
            MealItem tofu = new MealItem();
            tofu.name = "Tofu";
            tofu.grams = round(protein / 0.08 * 100, 0);
            tofu.notes = "Plant protein";
            items.add(tofu);
        }

        if (!glutenFree) {
            MealItem rice = new MealItem();
            rice.name = "Cooked rice";
            rice.grams = round(carbs / 0.28 * 100, 0);
            rice.notes = "Complex carbs";
            items.add(rice);
        } else {
            MealItem potato = new MealItem();
            potato.name = "Potato";
            potato.grams = round(carbs / 0.17 * 100, 0);
            potato.notes = "Gluten-free carb";
            items.add(potato);
        }

        if (!dairyFree) {
            MealItem yogurt = new MealItem();
            yogurt.name = "Greek yogurt";
            yogurt.grams = round(fats / 0.04 * 100, 0);
            yogurt.notes = "Healthy fats & protein";
            items.add(yogurt);
        } else {
            MealItem olive = new MealItem();
            olive.name = "Olive oil";
            olive.grams = round(fats / 1.0 * 11, 0);
            olive.notes = "Healthy fat";
            items.add(olive);
        }
        return items;
    }

    private List<String> buildShoppingList(MealPlan plan) {
        Map<String, Double> totals = new HashMap<>();
        for (Meal m : plan.meals) {
            for (MealItem it : m.items) {
                totals.merge(it.name, it.grams, Double::sum);
            }
        }
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            list.add(e.getKey() + ": " + Math.round(e.getValue()) + " g");
        }
        return list;
    }

    private double mifflinStJeor(String gender, double weightKg, double heightCm, int age) {
        // weight in kg, height in cm
        if (gender != null && gender.equalsIgnoreCase("female")) {
            return 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
        }
        return 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
    }

    private double activityMultiplier(String activity) {
        if (activity == null) return 1.2; // sedentary default
        String a = activity.toLowerCase();
        if (a.contains("athlete")) return 1.9;
        if (a.contains("high") || a.contains("very")) return 1.725;
        if (a.contains("moderate")) return 1.55;
        if (a.contains("light")) return 1.375;
        return 1.2;
    }

    private double round(double v, double step) {
        // If step <= 0 treat as "round to nearest integer" which is the common expected behaviour
        if (step <= 0) return Math.round(v);
        return Math.round(v / step) * step;
    }
}


