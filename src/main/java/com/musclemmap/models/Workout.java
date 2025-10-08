package com.musclemmap.models;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class Workout {
    private int id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<WorkoutSet> sets;
    private String notes;
    private double totalVolume;
    private int totalSets;
    private int totalReps;
    private boolean isCompleted;

    public Workout() {
        this.sets = new ArrayList<>();
        this.isCompleted = false;
    }

    public Workout(String name) {
        this.name = name;
        this.startTime = LocalDateTime.now();
        this.sets = new ArrayList<>();
        this.isCompleted = false;
        this.totalVolume = 0.0;
        this.totalSets = 0;
        this.totalReps = 0;
    }

    // Getters and Setters
    // Add this field to Workout.java:
    private Integer userId;

    // Add getter and setter:
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

// Modify saveWorkout in DatabaseHelper to use the userId from Workout object

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.isCompleted = endTime != null;
    }

    public List<WorkoutSet> getSets() { return sets; }
    public void setSets(List<WorkoutSet> sets) {
        this.sets = sets;
        calculateTotals();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getTotalVolume() { return totalVolume; }
    public void setTotalVolume(double totalVolume) { this.totalVolume = totalVolume; }

    public int getTotalSets() { return totalSets; }
    public void setTotalSets(int totalSets) { this.totalSets = totalSets; }

    public int getTotalReps() { return totalReps; }
    public void setTotalReps(int totalReps) { this.totalReps = totalReps; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    public void addSet(WorkoutSet set) {
        sets.add(set);
        calculateTotals();
    }

    public void removeSet(WorkoutSet set) {
        sets.remove(set);
        calculateTotals();
    }

    private void calculateTotals() {
        this.totalVolume = sets.stream()
                .mapToDouble(WorkoutSet::getVolume)
                .sum();
        this.totalSets = sets.size();
        this.totalReps = sets.stream()
                .mapToInt(WorkoutSet::getReps)
                .sum();

        System.out.println("✅ Calculated totals: Volume=" + totalVolume +
                ", Sets=" + totalSets + ", Reps=" + totalReps);
    }


    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return Duration.ZERO;
    }

    public long getDurationMinutes() {
        return getDuration().toMinutes();
    }

    public String getFormattedDuration() {
        Duration duration = getDuration();
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    public void completeWorkout() {
        if (!isCompleted) {
            this.endTime = LocalDateTime.now();
            this.isCompleted = true;
            calculateTotals();
        }
    }

    @Override
    public String toString() {
        return name + " - " + getFormattedDuration() + " (" + totalSets + " sets)";
    }
}