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
// ✅ COMPLETE GIF URL MAPPING METHOD - ALL 69 EXERCISES
    private static String getExerciseImageUrl(String exerciseName) {
        String name = exerciseName.toLowerCase().trim();

        Map<String, String> exerciseImages = new HashMap<>();

        // CHEST EXERCISES (9)
        exerciseImages.put("bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Bench-Press.gif");
        exerciseImages.put("incline bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Incline-Bench-Press.gif");
        exerciseImages.put("decline bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Decline-Barbell-Bench-Press.gif");
        exerciseImages.put("dumbbell press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Bench-Press.gif");
        exerciseImages.put("incline dumbbell press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Incline-Dumbbell-Press.gif");
        exerciseImages.put("chest fly", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Fly.gif");
        exerciseImages.put("cable fly", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Cross-over-Variation.gif");
        exerciseImages.put("push-ups", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Push-up.gif");
        exerciseImages.put("chest dips", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Chest-Dips.gif");

        // BACK EXERCISES (7)
        exerciseImages.put("deadlifts", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Deadlift.gif");
        exerciseImages.put("barbell row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Row.gif");
        exerciseImages.put("pull-ups", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Pull-up.gif");
        exerciseImages.put("lat pulldown", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Lat-Pulldown.gif");
        exerciseImages.put("seated cable row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Seated-Cable-Row.gif");
        exerciseImages.put("t-bar row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/T-Bar-Row.gif");
        exerciseImages.put("one-arm dumbbell row", "https://fitnessprogramer.com/wp-content/uploads/2021/02/One-Arm-Dumbbell-Row.gif");

        // SHOULDER EXERCISES (7)
        exerciseImages.put("overhead press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Overhead-Press.gif");
        exerciseImages.put("dumbbell shoulder press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Shoulder-Press.gif");
        exerciseImages.put("lateral raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Lateral-Raise.gif");
        exerciseImages.put("front raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Front-Raise.gif");
        exerciseImages.put("rear delt fly", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Rear-Delt-Fly.gif");
        exerciseImages.put("face pull", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Face-Pull.gif");
        exerciseImages.put("arnold press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Arnold-Press.gif");

        // BICEPS EXERCISES (6)
        exerciseImages.put("bicep curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Curl.gif");
        exerciseImages.put("barbell curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Curl.gif");
        exerciseImages.put("hammer curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hammer-Curl.gif");
        exerciseImages.put("preacher curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Preacher-Curl.gif");
        exerciseImages.put("cable curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Curl.gif");
        exerciseImages.put("concentration curl", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Concentration-Curl.gif");

        // TRICEPS EXERCISES (6)
        exerciseImages.put("tricep dips", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Triceps-Dips.gif");
        exerciseImages.put("close-grip bench press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Close-Grip-Barbell-Bench-Press.gif");
        exerciseImages.put("tricep pushdown", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Triceps-Pushdown.gif");
        exerciseImages.put("overhead tricep extension", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Overhead-Triceps-Extension.gif");
        exerciseImages.put("skull crushers", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Skull-Crusher.gif");
        exerciseImages.put("diamond push-ups", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Diamond-Push-Up.gif");

        // FOREARMS EXERCISES (3)
        exerciseImages.put("wrist curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Wrist-Curl.gif");
        exerciseImages.put("reverse wrist curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Reverse-Wrist-Curl.gif");
        exerciseImages.put("farmer's walk", "https://fitnessprogramer.com/wp-content/uploads/2021/02/farmers-walk.gif");

        // ABS EXERCISES (6)
        exerciseImages.put("planks", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Plank.gif");
        exerciseImages.put("crunches", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Crunch.gif");
        exerciseImages.put("leg raises", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Raise.gif");
        exerciseImages.put("russian twists", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Russian-Twist.gif");
        exerciseImages.put("cable crunch", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Crunch.gif");
        exerciseImages.put("hanging knee raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hanging-Knee-Raises.gif");

        // OBLIQUES EXERCISES (3)
        exerciseImages.put("side plank", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Side-Plank.gif");
        exerciseImages.put("bicycle crunches", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Bicycle-Crunch.gif");
        exerciseImages.put("wood chops", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Wood-Chop.gif");

        // LOWER BACK EXERCISES (3)
        exerciseImages.put("back extensions", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Back-Extension.gif");
        exerciseImages.put("good mornings", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Good-Morning.gif");
        exerciseImages.put("superman", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Superman-Exercise.gif");

        // GLUTES EXERCISES (4)
        exerciseImages.put("hip thrusts", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Hip-Thrust.gif");
        exerciseImages.put("glute bridges", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Glute-Bridge.gif");
        exerciseImages.put("bulgarian split squats", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Bulgarian-Split-Squat.gif");
        exerciseImages.put("cable kickbacks", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Cable-Glute-Kickback.gif");

        // QUADRICEPS EXERCISES (6)
        exerciseImages.put("squats", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Squat.gif");
        exerciseImages.put("front squats", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Front-Squat.gif");
        exerciseImages.put("leg press", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Press.gif");
        exerciseImages.put("lunges", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Bodyweight-Lunge.gif");
        exerciseImages.put("leg extensions", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Extensions.gif");
        exerciseImages.put("walking lunges", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Walking-Lunge.gif");

        // HAMSTRINGS EXERCISES (4)
        exerciseImages.put("romanian deadlift", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Romanian-Deadlift.gif");
        exerciseImages.put("leg curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Curl.gif");
        exerciseImages.put("nordic curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Nordic-Hamstring-Curl.gif");
        exerciseImages.put("stiff-leg deadlift", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Stiff-Leg-Deadlift.gif");

        // CALVES EXERCISES (3)
        exerciseImages.put("standing calf raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Calf-Raises.gif");
        exerciseImages.put("seated calf raise", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Machine-Seated-Calf-Raise.gif");
        exerciseImages.put("jump rope", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Jump-Rope.gif");

        // NECK EXERCISES (2)
        exerciseImages.put("neck curls", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Neck-Flexion.gif");
        exerciseImages.put("neck extensions", "https://fitnessprogramer.com/wp-content/uploads/2021/02/Neck-Extension.gif");

        return exerciseImages.getOrDefault(name, "");
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

    // ✅ GET GIF URL from mapping
    public String getGifUrl() {
        if (gifUrl != null && !gifUrl.isEmpty()) {
            return gifUrl;
        }
        return getExerciseImageUrl(this.name);
    }

    public void setGifUrl(String gifUrl) {
        this.gifUrl = gifUrl;
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
