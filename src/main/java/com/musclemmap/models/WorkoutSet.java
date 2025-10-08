package com.musclemmap.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class WorkoutSet {
    private int id;
    private Exercise exercise;
    private int reps;
    private double weight;
    private SetType type;
    private String notes;
    private boolean completed;
    private LocalDateTime timestamp;
    private boolean isPersonalRecord;
    private double rpe; // Rate of Perceived Exertion (1-10)

    public enum SetType {
        NORMAL("Normal", "💪"),
        WARMUP("Warm-up", "🔥"),
        DROP_SET("Drop Set", "📉"),
        FAILURE("To Failure", "💥"),
        SUPERSET("Superset", "⚡");

        private final String displayName;
        private final String emoji;

        SetType(String displayName, String emoji) {
            this.displayName = displayName;
            this.emoji = emoji;
        }

        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }

        @Override
        public String toString() {
            return emoji + " " + displayName;
        }
    }

    public WorkoutSet() {
        this.type = SetType.NORMAL;
        this.completed = false;
        this.timestamp = LocalDateTime.now();
        this.rpe = 0.0;
    }

    public WorkoutSet(Exercise exercise, int reps, double weight) {
        this.exercise = exercise;
        this.reps = reps;
        this.weight = weight;
        this.type = SetType.NORMAL;
        this.completed = false;
        this.timestamp = LocalDateTime.now();
        this.rpe = 0.0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public SetType getType() { return type; }
    public void setType(SetType type) { this.type = type; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isPersonalRecord() { return isPersonalRecord; }
    public void setPersonalRecord(boolean personalRecord) { this.isPersonalRecord = personalRecord; }

    public double getRpe() { return rpe; }
    public void setRpe(double rpe) { this.rpe = Math.max(0, Math.min(10, rpe)); }

    public double getVolume() {
        return weight * reps;
    }

    @Override
    public String toString() {
        String pr = isPersonalRecord ? " 🏆 PR" : "";
        return String.format("%s: %d reps @ %.1f lbs%s",
                exercise.getName(), reps, weight, pr);
    }
}