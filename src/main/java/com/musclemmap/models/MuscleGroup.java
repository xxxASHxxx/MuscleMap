package com.musclemmap.models;

import java.util.List;
import java.util.ArrayList;

public class MuscleGroup {
    private int id;
    private String name;
    private String displayName;
    private String description;
    private List<Exercise> exercises;
    private String anatomicalRegion;
    private String color; // For UI display

    public MuscleGroup() {
        this.exercises = new ArrayList<>();
    }

    public MuscleGroup(String name, String description, String anatomicalRegion) {
        this.name = name;
        this.displayName = name;
        this.description = description;
        this.anatomicalRegion = anatomicalRegion;
        this.exercises = new ArrayList<>();
        this.color = "#1E88E5"; // Default blue
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Exercise> getExercises() { return exercises; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }

    public String getAnatomicalRegion() { return anatomicalRegion; }
    public void setAnatomicalRegion(String anatomicalRegion) { this.anatomicalRegion = anatomicalRegion; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public void addExercise(Exercise exercise) {
        if (!exercises.contains(exercise)) {
            exercises.add(exercise);
        }
    }

    @Override
    public String toString() {
        return displayName + " (" + exercises.size() + " exercises)";
    }
}