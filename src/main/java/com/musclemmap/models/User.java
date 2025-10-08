package com.musclemmap.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class User {
    private Integer id;
    private String username;
    private String name;
    private String email;
    private String passwordHash;
    private LocalDate joinDate;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private List<Workout> workouts;
    private UserStats stats;
    private String profileImageUrl;
    private double bodyWeight;
    private String fitnessGoal;
    private int experienceLevel;

    public User() {
        this.workouts = new ArrayList<>();
        this.stats = new UserStats();
        this.joinDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.experienceLevel = 1;
    }

    public User(String name, String email) {
        this.name = name;
        this.username = name;
        this.email = email;
        this.joinDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.workouts = new ArrayList<>();
        this.stats = new UserStats();
        this.experienceLevel = 1;
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.name = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.joinDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.workouts = new ArrayList<>();
        this.stats = new UserStats();
        this.experienceLevel = 1;
    }

    public User(Integer id, String username, String email, String passwordHash,
                LocalDateTime createdAt, LocalDateTime lastLogin, int workoutStreak, double totalVolumeLifted) {
        this.id = id;
        this.username = username;
        this.name = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.joinDate = createdAt != null ? createdAt.toLocalDate() : LocalDate.now();
        this.workouts = new ArrayList<>();
        this.stats = new UserStats();
        this.stats.setCurrentStreak(workoutStreak);
        this.stats.setTotalVolume(totalVolumeLifted);
        this.experienceLevel = 1;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name != null ? name : username;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        if (this.joinDate == null && createdAt != null) {
            this.joinDate = createdAt.toLocalDate();
        }
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public List<Workout> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = workouts;
        updateStats();
    }

    public UserStats getStats() {
        return stats;
    }

    public void setStats(UserStats stats) {
        this.stats = stats;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public double getBodyWeight() {
        return bodyWeight;
    }

    public void setBodyWeight(double bodyWeight) {
        this.bodyWeight = bodyWeight;
    }

    public String getFitnessGoal() {
        return fitnessGoal;
    }

    public void setFitnessGoal(String fitnessGoal) {
        this.fitnessGoal = fitnessGoal;
    }

    public int getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(int experienceLevel) {
        this.experienceLevel = Math.max(1, Math.min(5, experienceLevel));
    }

    public void addWorkout(Workout workout) {
        workouts.add(workout);
        updateStats();
    }

    public void updateStats() {
        if (workouts != null && !workouts.isEmpty()) {
            stats.setTotalWorkouts(workouts.size());
            stats.setTotalVolume(workouts.stream()
                    .mapToDouble(Workout::getTotalVolume)
                    .sum());
        }
    }

    public int getWorkoutStreak() {
        return stats != null ? stats.getCurrentStreak() : 0;
    }

    public void setWorkoutStreak(int streak) {
        if (stats != null) {
            stats.setCurrentStreak(streak);
        }
    }

    public double getTotalVolumeLifted() {
        return stats != null ? stats.getTotalVolume() : 0.0;
    }

    public void setTotalVolumeLifted(double volume) {
        if (stats != null) {
            stats.setTotalVolume(volume);
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", workoutStreak=" + getWorkoutStreak() +
                ", totalVolume=" + getTotalVolumeLifted() +
                '}';
    }

    public static class UserStats {
        private int totalWorkouts;
        private double totalVolume;
        private int currentStreak;
        private int longestStreak;

        public UserStats() {
            this.totalWorkouts = 0;
            this.totalVolume = 0.0;
            this.currentStreak = 0;
            this.longestStreak = 0;
        }

        public int getTotalWorkouts() {
            return totalWorkouts;
        }

        public void setTotalWorkouts(int totalWorkouts) {
            this.totalWorkouts = totalWorkouts;
        }

        public double getTotalVolume() {
            return totalVolume;
        }

        public void setTotalVolume(double totalVolume) {
            this.totalVolume = totalVolume;
        }

        public int getCurrentStreak() {
            return currentStreak;
        }

        public void setCurrentStreak(int currentStreak) {
            this.currentStreak = currentStreak;
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak;
            }
        }

        public int getLongestStreak() {
            return longestStreak;
        }

        public void setLongestStreak(int longestStreak) {
            this.longestStreak = longestStreak;
        }
    }
}
