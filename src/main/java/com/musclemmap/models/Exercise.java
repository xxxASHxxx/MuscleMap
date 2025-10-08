// ========== Exercise.java (src/main/java/com/musclemap/models/Exercise.java) ==========

package com.musclemmap.models;

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

    public Exercise() {}

    public Exercise(String name, String muscleGroup, String equipment, String difficulty, String instructions) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
        this.isFavorite = false;
    }

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

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

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
