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

        // Realistic meal names and timing
        String[] mealNames = {"Breakfast", "Mid-Morning Snack", "Lunch", "Afternoon Snack", "Dinner", "Evening Snack"};
        String[] mealTimes = {"7:00 AM", "10:00 AM", "1:00 PM", "4:00 PM", "7:00 PM", "9:00 PM"};
        
        for (int i = 0; i < meals; i++) {
            Meal m = new Meal();
            m.name = mealNames[i] + " (" + mealTimes[i] + ")";
            
            // Adjust macros based on meal timing
            double proteinMultiplier = (i == 0 || i == meals - 1) ? 1.2 : 1.0; // More protein for breakfast and dinner
            double carbMultiplier = (i == 0 || i == 2) ? 1.3 : 0.8; // More carbs for breakfast and lunch
            double fatMultiplier = (i == 2 || i == 4) ? 1.2 : 0.9; // More fats for lunch and dinner
            
            m.items.addAll(suggestMealItems(req, 
                proteinPerMeal * proteinMultiplier, 
                carbsPerMeal * carbMultiplier, 
                fatsPerMeal * fatMultiplier));
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
        boolean keto = req.preferences.contains("keto");
        boolean paleo = req.preferences.contains("paleo");

        // Protein sources - more diverse and realistic
        if (!vegetarian) {
            String[] proteins = {"Chicken breast", "Salmon fillet", "Lean beef", "Turkey breast", "Eggs", "Greek yogurt"};
            String proteinChoice = proteins[(int)(Math.random() * proteins.length)];
            
            MealItem proteinItem = new MealItem();
            proteinItem.name = proteinChoice;
            double proteinDensity = getProteinDensity(proteinChoice);
            proteinItem.grams = round(protein / proteinDensity * 100, 0);
            proteinItem.notes = "High-quality protein";
            items.add(proteinItem);
        } else {
            String[] vegProteins = {"Tofu", "Tempeh", "Lentils", "Chickpeas", "Quinoa", "Black beans"};
            String proteinChoice = vegProteins[(int)(Math.random() * vegProteins.length)];
            
            MealItem proteinItem = new MealItem();
            proteinItem.name = proteinChoice;
            double proteinDensity = getProteinDensity(proteinChoice);
            proteinItem.grams = round(protein / proteinDensity * 100, 0);
            proteinItem.notes = "Plant-based protein";
            items.add(proteinItem);
        }

        // Carbohydrate sources - more variety
        if (!keto) {
            String[] carbSources = {"Brown rice", "Sweet potato", "Oats", "Quinoa", "Whole wheat bread", "Banana"};
            String carbChoice = carbSources[(int)(Math.random() * carbSources.length)];
            
            if (!glutenFree || !carbChoice.contains("wheat")) {
                MealItem carbItem = new MealItem();
                carbItem.name = carbChoice;
                double carbDensity = getCarbDensity(carbChoice);
                carbItem.grams = round(carbs / carbDensity * 100, 0);
                carbItem.notes = "Complex carbohydrates";
                items.add(carbItem);
            }
        }

        // Fat sources - more realistic options
        String[] fatSources = {"Avocado", "Olive oil", "Almonds", "Walnuts", "Coconut oil", "Chia seeds"};
        String fatChoice = fatSources[(int)(Math.random() * fatSources.length)];
        
        MealItem fatItem = new MealItem();
        fatItem.name = fatChoice;
        double fatDensity = getFatDensity(fatChoice);
        fatItem.grams = round(fats / fatDensity * 100, 0);
        fatItem.notes = "Healthy fats";
        items.add(fatItem);

        // Add vegetables for micronutrients
        String[] vegetables = {"Broccoli", "Spinach", "Bell peppers", "Carrots", "Asparagus", "Brussels sprouts"};
        String vegChoice = vegetables[(int)(Math.random() * vegetables.length)];
        
        MealItem vegItem = new MealItem();
        vegItem.name = vegChoice;
        vegItem.grams = 150; // Standard serving
        vegItem.notes = "Rich in vitamins and minerals";
        items.add(vegItem);

        return items;
    }

    private double getProteinDensity(String food) {
        // Protein per 100g
        return switch (food.toLowerCase()) {
            case "chicken breast" -> 31.0;
            case "salmon fillet" -> 25.0;
            case "lean beef" -> 26.0;
            case "turkey breast" -> 29.0;
            case "eggs" -> 13.0;
            case "greek yogurt" -> 10.0;
            case "tofu" -> 8.0;
            case "tempeh" -> 19.0;
            case "lentils" -> 9.0;
            case "chickpeas" -> 8.9;
            case "quinoa" -> 4.4;
            case "black beans" -> 8.9;
            default -> 20.0;
        };
    }

    private double getCarbDensity(String food) {
        // Carbs per 100g
        return switch (food.toLowerCase()) {
            case "brown rice" -> 23.0;
            case "sweet potato" -> 20.0;
            case "oats" -> 66.0;
            case "quinoa" -> 22.0;
            case "whole wheat bread" -> 41.0;
            case "banana" -> 23.0;
            default -> 25.0;
        };
    }

    private double getFatDensity(String food) {
        // Fat per 100g
        return switch (food.toLowerCase()) {
            case "avocado" -> 15.0;
            case "olive oil" -> 100.0;
            case "almonds" -> 49.0;
            case "walnuts" -> 65.0;
            case "coconut oil" -> 100.0;
            case "chia seeds" -> 31.0;
            default -> 20.0;
        };
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


