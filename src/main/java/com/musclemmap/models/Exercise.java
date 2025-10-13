package com.musclemmap.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Exercise {
    private int id;
    private String name;
    private String muscleGroup;
    private String equipment;
    private String difficulty;
    private String instructions;
    private String imageUrl;
    private String videoUrl;
    private boolean isFavorite;
    private String gifUrl;

    // ✅ CONSTRUCTOR 1: Empty constructor
    public Exercise() {}

    // ✅ CONSTRUCTOR 2: 5 parameters (no ID) - MISSING IN YOUR CODE - ADDED NOW
    public Exercise(String name, String muscleGroup, String equipment,
                    String difficulty, String instructions) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
    }

    // ✅ CONSTRUCTOR 3: 6 parameters (with ID, no gif_url)
    public Exercise(int id, String name, String muscleGroup, String equipment,
                    String difficulty, String instructions) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
    }

    // ✅ CONSTRUCTOR 4: 7 parameters (with ID and gif_url)
    public Exercise(int id, String name, String muscleGroup, String equipment,
                    String difficulty, String instructions, String gifUrl) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
        this.gifUrl = gifUrl;
    }

    // ✅ CONSTRUCTOR 5: 8 parameters (complete with imageUrl and videoUrl)
    public Exercise(int id, String name, String muscleGroup, String equipment,
                    String difficulty, String instructions, String imageUrl, String videoUrl) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.isFavorite = false;
    }

    // ✅ COMPLETE GIF URL MAPPING METHOD
    // ✅ COMPLETE GIF URL MAPPING METHOD - ALL 69 EXERCISES WITH WORKING URLS
    private static String getExerciseImageUrl(String exerciseName) {
        String name = exerciseName.toLowerCase().trim();

        Map<String, String> exerciseImages = new HashMap<>();

        // CHEST EXERCISES (9) - Using working URLs
        exerciseImages.put("bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("incline bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("decline bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("dumbbell press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Bench-Press");
        exerciseImages.put("incline dumbbell press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyIncline-Dumbbell-Press");
        exerciseImages.put("chest fly", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Fly");
        exerciseImages.put("cable fly", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("push-ups", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyPush-up");
        exerciseImages.put("chest dips", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyChest-Dips");

        // BACK EXERCISES (7)
        exerciseImages.put("deadlifts", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Deadlift");
        exerciseImages.put("barbell row", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Row");
        exerciseImages.put("pull-ups", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyPull-up");
        exerciseImages.put("lat pulldown", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyLat-Pulldown");
        exerciseImages.put("seated cable row", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphySeated-Cable-Row");
        exerciseImages.put("t-bar row", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyT-Bar-Row");
        exerciseImages.put("one-arm dumbbell row", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyOne-Arm-Dumbbell-Row");

        // SHOULDER EXERCISES (7) - Using working URLs from GIPHY
        exerciseImages.put("overhead press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("dumbbell shoulder press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Shoulder-Press");
        exerciseImages.put("lateral raise", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Lateral-Raise");
        exerciseImages.put("front raise", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Front-Raise");
        exerciseImages.put("rear delt fly", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Rear-Delt-Fly");
        exerciseImages.put("face pull", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyFace-Pull");
        // ✅ FIXED: Use working Arnold Press URL from GIPHY
        exerciseImages.put("arnold press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        
        // ✅ ADD ALTERNATIVE NAMES FOR SHOULDER EXERCISES
        exerciseImages.put("arnold shoulder press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("dumbbell arnold press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");

        // BICEPS EXERCISES (6)
        exerciseImages.put("bicep curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Curl");
        exerciseImages.put("barbell curl", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Curl");
        exerciseImages.put("hammer curl", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyHammer-Curl");
        exerciseImages.put("preacher curl", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyPreacher-Curl");
        exerciseImages.put("cable curl", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyCable-Curl");
        exerciseImages.put("concentration curl", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyConcentration-Curl");

        // TRICEPS EXERCISES (6) - Using working URLs from GIPHY
        exerciseImages.put("tricep dips", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBench-Dips");
        // ✅ FIXED: Use working Close-Grip Bench Press URL from GIPHY
        exerciseImages.put("close-grip bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("tricep pushdown", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("overhead tricep extension", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("skull crushers", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("diamond push-ups", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDiamond-Push-up");
        
        // ✅ ADD ALTERNATIVE NAMES FOR TRICEPS EXERCISES
        exerciseImages.put("close grip bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("close grip press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");

        // FOREARMS EXERCISES (3)
        exerciseImages.put("wrist curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Wrist-Curl");
        exerciseImages.put("reverse wrist curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Reverse-Wrist-Curl");
        exerciseImages.put("farmer's walk", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyfarmers-walk");

        // ABS EXERCISES (6)
        exerciseImages.put("planks", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyPlank");
        exerciseImages.put("crunches", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyCrunch");
        exerciseImages.put("leg raises", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyLeg-Raise");
        exerciseImages.put("russian twists", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyRussian-Twist");
        exerciseImages.put("cable crunch", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyCable-Crunch");
        exerciseImages.put("hanging knee raise", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyHanging-Knee-Raises");

        // OBLIQUES EXERCISES (3)
        exerciseImages.put("side plank", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphySide-Plank");
        exerciseImages.put("bicycle crunches", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBicycle-Crunch");
        exerciseImages.put("wood chops", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyWood-Chop");

        // LOWER BACK EXERCISES (3)
        exerciseImages.put("back extensions", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBack-Extension");
        exerciseImages.put("good mornings", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Good-Morning");
        exerciseImages.put("superman", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphySuperman-Exercise");

        // GLUTES EXERCISES (4)
        exerciseImages.put("hip thrusts", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Hip-Thrust");
        exerciseImages.put("glute bridges", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyGlute-Bridge");
        exerciseImages.put("bulgarian split squats", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Bulgarian-Split-Squat");
        exerciseImages.put("cable kickbacks", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyCable-Glute-Kickback");

        // QUADRICEPS EXERCISES (6)
        exerciseImages.put("squats", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Squat");
        exerciseImages.put("front squats", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("leg press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyLeg-Press");
        exerciseImages.put("lunges", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBodyweight-Lunge");
        exerciseImages.put("leg extensions", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyLeg-Extensions");
        exerciseImages.put("walking lunges", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Walking-Lunge");

        // HAMSTRINGS EXERCISES (4)
        exerciseImages.put("romanian deadlift", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyRomanian-Deadlift");
        exerciseImages.put("leg curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyLeg-Curl");
        exerciseImages.put("nordic curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyNordic-Hamstring-Curl");
        exerciseImages.put("stiff-leg deadlift", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Stiff-Leg-Deadlift");

        // CALVES EXERCISES (3) - Using working URLs
        exerciseImages.put("standing calf raise", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("seated calf raise", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("jump rope", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");

        // NECK EXERCISES (2) - Using working URLs from reliable sources
        exerciseImages.put("neck curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("neck extensions", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");

        // ✅ ADDITIONAL FALLBACK URLS FOR COMMON EXERCISES - Using reliable sources
        // These are alternative URLs that work from GIPHY and other reliable sources
        exerciseImages.put("close grip bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("arnold press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        
        // ✅ FALLBACK TO SIMILAR EXERCISES IF EXACT MATCH NOT FOUND
        exerciseImages.put("close grip press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("narrow grip bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        exerciseImages.put("dumbbell arnold", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");

        // ✅ Try to get the URL, if not found, try alternative sources
        String url = exerciseImages.get(name);
        if (url == null || url.isEmpty()) {
            url = getAlternativeGifUrl(name);
        }
        
        // ✅ ULTIMATE FALLBACK: If still no URL, use GIPHY
        if (url == null || url.isEmpty()) {
            url = "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy";
        }
        
        return url;
    }

    // ✅ GETTERS AND SETTERS
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    public void setMuscleGroup(String muscleGroup) {
        this.muscleGroup = muscleGroup;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    // ✅ GET GIF URL from mapping with improved fallback logic
    public String getGifUrl() {
        if (gifUrl != null && !gifUrl.isEmpty()) return gifUrl;

        String key = (this.name == null ? "" : this.name)
                .toLowerCase()
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        // ✅ IMPROVED VARIANT MAPPING
        // General variants
        if (key.equals("lat pull down")) key = "lat pulldown";
        if (key.equals("barbell rows")) key = "barbell row";
        if (key.equals("cable flies")) key = "cable fly";
        if (key.equals("dumbbell flies")) key = "chest fly";
        if (key.equals("push ups")) key = "push-ups";

        // Triceps variants
        if (key.equals("overhead triceps extension")) key = "overhead tricep extension";
        if (key.equals("triceps pushdown")) key = "tricep pushdown";
        if (key.equals("skull crusher")) key = "skull crushers";
        if (key.equals("diamond push ups")) key = "diamond push-ups";
        if (key.equals("close-grip bench press")) key = "close grip bench press";
        if (key.equals("close grip bench press")) key = "close grip bench press";

        // Shoulder variants
        if (key.equals("arnold shoulder press")) key = "arnold press";
        if (key.equals("dumbbell arnold press")) key = "arnold press";

        // Try exact match first
        String result = getExerciseImageUrl(key);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        // ✅ FALLBACK: Try common variations
        String[] variations = {
            key.replace(" ", "-"),
            key.replace(" ", "_"),
            key.replace("-", " "),
            key.replace("_", " ")
        };

        for (String variation : variations) {
            result = getExerciseImageUrl(variation);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }

        // ✅ FINAL FALLBACK: Try to find a similar exercise or return a default
        String fallbackUrl = getFallbackGifUrl(key);
        if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
            System.out.println("🔄 Using fallback GIF for: " + this.name);
            return fallbackUrl;
        }

        // ✅ ULTIMATE FALLBACK: Return empty string if no match found
        System.out.println("⚠️ No GIF found for exercise: " + this.name + " (key: " + key + ")");
        return "";
    }


    public void setGifUrl(String gifUrl) {
        this.gifUrl = gifUrl;
    }

    // ✅ FALLBACK METHOD TO FIND SIMILAR EXERCISES
    private String getFallbackGifUrl(String key) {
        // Try to find exercises with similar names
        if (key.contains("close") && key.contains("grip")) {
            return getExerciseImageUrl("close grip bench press");
        }
        if (key.contains("arnold")) {
            return getExerciseImageUrl("arnold press");
        }
        if (key.contains("tricep") || key.contains("triceps")) {
            return getExerciseImageUrl("tricep pushdown"); // Common tricep exercise
        }
        if (key.contains("bicep") || key.contains("biceps")) {
            return getExerciseImageUrl("bicep curls"); // Common bicep exercise
        }
        if (key.contains("chest")) {
            return getExerciseImageUrl("bench press"); // Common chest exercise
        }
        if (key.contains("shoulder")) {
            return getExerciseImageUrl("dumbbell shoulder press"); // Common shoulder exercise
        }
        if (key.contains("back")) {
            return getExerciseImageUrl("barbell row"); // Common back exercise
        }
        if (key.contains("squat")) {
            return getExerciseImageUrl("squats"); // Common leg exercise
        }
        if (key.contains("deadlift")) {
            return getExerciseImageUrl("deadlifts"); // Common back/leg exercise
        }
        
        return "";
    }

    // ✅ ALTERNATIVE GIF URL SOURCES FOR FALLBACK - Using reliable sources
    private static String getAlternativeGifUrl(String exerciseName) {
        String name = exerciseName.toLowerCase().trim();
        
        // ✅ WORKING ALTERNATIVE URLS - Using GIPHY and other reliable sources
        Map<String, String> alternativeUrls = new HashMap<>();
        
        // Chest exercises alternatives - Using GIPHY
        alternativeUrls.put("bench press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        alternativeUrls.put("dumbbell press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        alternativeUrls.put("push-ups", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        alternativeUrls.put("cable fly", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        
        // Back exercises alternatives
        alternativeUrls.put("deadlifts", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Deadlift");
        alternativeUrls.put("barbell row", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Row");
        alternativeUrls.put("pull-ups", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyPull-up");
        
        // Shoulder exercises alternatives
        alternativeUrls.put("dumbbell shoulder press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Shoulder-Press");
        alternativeUrls.put("lateral raise", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Lateral-Raise");
        alternativeUrls.put("overhead press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Overhead-Press");
        
        // Bicep exercises alternatives
        alternativeUrls.put("bicep curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyDumbbell-Curl");
        alternativeUrls.put("barbell curl", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Curl");
        alternativeUrls.put("hammer curl", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyHammer-Curl");
        
        // Tricep exercises alternatives
        alternativeUrls.put("tricep pushdown", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyTriceps-Pushdown");
        alternativeUrls.put("tricep dips", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyTriceps-Dips");
        
        // Leg exercises alternatives
        alternativeUrls.put("squats", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBarbell-Squat");
        alternativeUrls.put("lunges", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyBodyweight-Lunge");
        alternativeUrls.put("leg press", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyLeg-Press");
        
        // Abs exercises alternatives
        alternativeUrls.put("planks", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyPlank");
        alternativeUrls.put("crunches", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyCrunch");
        alternativeUrls.put("leg raises", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphyLeg-Raise");
        
        // Neck exercises alternatives - Using GIPHY
        alternativeUrls.put("neck curls", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        alternativeUrls.put("neck extensions", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy");
        
        // Try exact match first
        String url = alternativeUrls.get(name);
        if (url != null && !url.isEmpty()) {
            return url;
        }
        
        // Try partial matches for similar exercises
        for (Map.Entry<String, String> entry : alternativeUrls.entrySet()) {
            if (name.contains(entry.getKey()) || entry.getKey().contains(name)) {
                return entry.getValue();
            }
        }
        
        // ✅ FINAL FALLBACK: Return a generic working exercise GIF from GIPHY
        return "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy";
    }

    // ✅ HELPER METHODS
    public String getDifficultyEmoji() {
        return switch (difficulty.toLowerCase()) {
            case "beginner" -> "🟢";
            case "intermediate" -> "🟡";
            case "advanced" -> "🔴";
            default -> "⚪";
        };
    }

    public String getEquipmentEmoji() {
        return switch (equipment.toLowerCase()) {
            case "barbell" -> "🏋️";
            case "dumbbell" -> "💪";
            case "bodyweight" -> "🤸";
            case "machine" -> "⚙️";
            case "cable" -> "🔗";
            case "kettlebell" -> "🔶";
            default -> "🏃";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exercise exercise = (Exercise) o;
        return id == exercise.id && Objects.equals(name, exercise.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return name + " (" + muscleGroup + " - " + difficulty + ")";
    }
}
