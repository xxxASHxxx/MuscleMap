package com.musclemmap.models;

public class Exercise {
    private int id;
    private String name;
    private String muscleGroup;
    private String difficulty;
    private String description;
    private String equipment;
    private String instructions;
    private String gifUrl;
    private boolean isFavorite;

    public Exercise() {
        // Default constructor
    }

    public Exercise(int id, String name, String muscleGroup, String difficulty, String description, String equipment, String instructions) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
        this.description = description;
        this.equipment = equipment;
        this.instructions = instructions;
        this.gifUrl = null;
        this.isFavorite = false;
    }

    // Constructor with gifUrl parameter
    public Exercise(int id, String name, String muscleGroup, String difficulty, String description, String equipment, String instructions, String gifUrl) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
        this.description = description;
        this.equipment = equipment;
        this.instructions = instructions;
        this.gifUrl = gifUrl;
        this.isFavorite = false;
    }

    // Constructor for creating exercises without ID (for hardcoded exercises)
    public Exercise(String name, String muscleGroup, String difficulty, String description, String equipment) {
        this.id = 0; // Will be set by database
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
        this.description = description;
        this.equipment = equipment;
        this.instructions = description; // Use description as instructions if not provided
        this.isFavorite = false;
    }

    // ✅ PROFESSIONAL: No GIF URLs - using placeholders only
    private static String getExerciseImageUrl(String exerciseName) {
        // Return null to use professional placeholders instead of broken GIFs
        return null;
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getExerciseImageUrl() {
        return getExerciseImageUrl(this.name);
    }

    public String getGifUrl() {
        return gifUrl;
    }

    public void setGifUrl(String gifUrl) {
        this.gifUrl = gifUrl;
    }

    // Helper method for getting equipment emoji
    public String getEquipmentEmoji() {
        if (equipment == null) return "🏋️";
        return switch (equipment.toLowerCase()) {
            case "barbell" -> "🏋️";
            case "dumbbell" -> "🏋️‍♂️";
            case "kettlebell" -> "🏋️‍♀️";
            case "bodyweight" -> "🤸";
            case "cable" -> "🔗";
            case "machine" -> "⚙️";
            case "resistance band" -> "🎗️";
            default -> "🏋️";
        };
    }

    // Helper method for getting difficulty emoji
    public String getDifficultyEmoji() {
        if (difficulty == null) return "⭐";
        return switch (difficulty.toLowerCase()) {
            case "beginner" -> "⭐";
            case "intermediate" -> "⭐⭐";
            case "advanced" -> "⭐⭐⭐";
            default -> "⭐";
        };
    }

    @Override
    public String toString() {
        return name + " (" + difficulty + ")";
    }
}