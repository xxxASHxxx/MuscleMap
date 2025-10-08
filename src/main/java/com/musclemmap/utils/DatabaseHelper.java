package com.musclemmap.utils;

import com.musclemmap.models.*;
import com.musclemmap.utils.PasswordUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private static DatabaseHelper instance;  // FIXED: Moved inside class
    private static final String DB_URL = "jdbc:sqlite:musclemap.db";
    private Connection connection;

    private DatabaseHelper() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            initializeDatabase();
            System.out.println("🗄️ Database connected successfully!");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    public static DatabaseHelper getInstance() {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper();
                }
            }
        }
        return instance;
    }
    public List<Workout> getWorkoutsByUserId(int userId) {
        List<Workout> workouts = new ArrayList<>();
        try {
            String sql = "SELECT * FROM workouts WHERE userid = ? ORDER BY starttime DESC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Workout workout = buildWorkoutFromResultSet(rs);
                workouts.add(workout);
            }

            rs.close();
            pstmt.close();
            System.out.println("Loaded " + workouts.size() + " workouts for user " + userId);
        } catch (SQLException e) {
            System.err.println("Failed to load user workouts: " + e.getMessage());
        }
        return workouts;
    }


    public User authenticateUser(String username, String password) {
        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPasswordHash = rs.getString("password_hash");

                if (PasswordUtil.verifyPassword(password, storedPasswordHash)) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            storedPasswordHash,
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            null,
                            rs.getInt("workout_streak"),
                            rs.getDouble("total_volume_lifted")
                    );

                    updateLastLogin(user.getId());
                    System.out.println("✅ Authentication successful for: " + username);
                    return user;
                } else {
                    System.out.println("❌ Invalid password for: " + username);
                }
            } else {
                System.out.println("❌ User not found: " + username);
            }
        } catch (SQLException e) {
            System.err.println("❌ Authentication error: " + e.getMessage());
        }
        return null;
    }

    public boolean registerUser(String username, String email, String password) {
        try {
            if (isUsernameTaken(username)) {
                System.out.println("⚠️ Username already taken: " + username);
                return false;
            }

            if (isEmailTaken(email)) {
                System.out.println("⚠️ Email already registered: " + email);
                return false;
            }

            String passwordHash = PasswordUtil.hashPassword(password);

            String sql = "INSERT INTO users (username, email, password_hash, workout_streak, total_volume_lifted) VALUES (?, ?, ?, 0, 0.0)";
            PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, passwordHash);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    System.out.println("✅ User registered successfully with ID: " + userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Registration error: " + e.getMessage());
        }
        return false;
    }

    public boolean isUsernameTaken(String username) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking username: " + e.getMessage());
        }
        return false;
    }

    public boolean isEmailTaken(String email) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, email);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking email: " + e.getMessage());
        }
        return false;
    }

    private void updateLastLogin(int userId) {
        try {
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error updating last login: " + e.getMessage());
        }
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        try {
            String sql = "SELECT password_hash FROM users WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if (!PasswordUtil.verifyPassword(oldPassword, storedHash)) {
                    System.out.println("❌ Old password is incorrect");
                    return false;
                }

                String newHash = PasswordUtil.hashPassword(newPassword);
                String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";
                PreparedStatement updatePstmt = connection.prepareStatement(updateSql);
                updatePstmt.setString(1, newHash);
                updatePstmt.setInt(2, userId);

                int rowsAffected = updatePstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✅ Password changed successfully");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error changing password: " + e.getMessage());
        }
        return false;
    }

    public User getUserById(int userId) {
        try {
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null,
                        rs.getInt("workout_streak"),
                        rs.getDouble("total_volume_lifted")
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving user: " + e.getMessage());
        }
        return null;
    }

    public List<Workout> getWorkoutsByUser(int userId) {
        List<Workout> workouts = new ArrayList<>();
        try {
            String sql = "SELECT * FROM workouts WHERE user_id = ? ORDER BY start_time DESC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Workout workout = buildWorkoutFromResultSet(rs);
                workouts.add(workout);
            }

            rs.close();
            pstmt.close();
            System.out.println("📊 Loaded " + workouts.size() + " workouts for user ID: " + userId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to load user workouts: " + e.getMessage());
        }
        return workouts;
    }

    private void initializeDatabase() {
        try {
            String createUsersTable = """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE NOT NULL,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_login TIMESTAMP,
            workout_streak INTEGER DEFAULT 0,
            total_volume_lifted REAL DEFAULT 0.0
        )""";

            String createExercisesTable = """
        CREATE TABLE IF NOT EXISTS exercises (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            muscle_group TEXT NOT NULL,
            equipment TEXT NOT NULL,
            difficulty TEXT NOT NULL,
            instructions TEXT
        )""";

            String createWorkoutsTable = """
        CREATE TABLE IF NOT EXISTS workouts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            name TEXT NOT NULL,
            start_time TIMESTAMP NOT NULL,
            end_time TIMESTAMP,
            total_volume REAL DEFAULT 0.0,
            notes TEXT,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )""";

            String createSetsTable = """
        CREATE TABLE IF NOT EXISTS workout_sets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            workout_id INTEGER NOT NULL,
            exercise_name TEXT NOT NULL,
            reps INTEGER NOT NULL,
            weight REAL NOT NULL,
            set_type TEXT DEFAULT 'NORMAL',
            completed BOOLEAN DEFAULT 0,
            FOREIGN KEY (workout_id) REFERENCES workouts(id) ON DELETE CASCADE
        )""";

            Statement stmt = connection.createStatement();
            stmt.execute(createUsersTable);
            stmt.execute(createExercisesTable);
            stmt.execute(createWorkoutsTable);
            stmt.execute(createSetsTable);

            insertSampleExercises();
            System.out.println("📋 Database tables initialized!");

        } catch (SQLException e) {
            System.err.println("❌ Database initialization failed: " + e.getMessage());
        }
    }

    private void insertSampleExercises() {
        try {
            String checkExercises = "SELECT COUNT(*) FROM exercises";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(checkExercises);

            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO exercises (name, muscle_group, equipment, difficulty, instructions) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(insertSql);

                Object[][] exercises = {
                        {"Bench Press", "Chest", "Barbell", "Intermediate", "Lie on bench, lower bar to chest, press up"},
                        {"Squats", "Quadriceps", "Barbell", "Intermediate", "Lower body until thighs parallel"},
                        {"Pull-ups", "Back", "Bodyweight", "Intermediate", "Hang from bar, pull body up"},
                        {"Push-ups", "Chest", "Bodyweight", "Beginner", "Lower body, push back up"},
                        {"Deadlifts", "Back", "Barbell", "Advanced", "Lift bar from ground to hip level"},
                        {"Overhead Press", "Shoulders", "Barbell", "Intermediate", "Press bar overhead"},
                        {"Bicep Curls", "Biceps", "Dumbbell", "Beginner", "Curl weights to shoulders"},
                        {"Planks", "Abs", "Bodyweight", "Beginner", "Hold plank position"},
                        {"Tricep Dips", "Triceps", "Bodyweight", "Intermediate", "Lower body between bars, push up"},
                        {"Lunges", "Quadriceps", "Bodyweight", "Beginner", "Step forward, lower back knee"}
                };

                for (Object[] ex : exercises) {
                    pstmt.setString(1, (String) ex[0]);
                    pstmt.setString(2, (String) ex[1]);
                    pstmt.setString(3, (String) ex[2]);
                    pstmt.setString(4, (String) ex[3]);
                    pstmt.setString(5, (String) ex[4]);
                    pstmt.executeUpdate();
                }
                pstmt.close();
                System.out.println("💪 Sample exercises inserted!");
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("⚠️ Error inserting sample exercises: " + e.getMessage());
        }
    }

    // ==================== CREATE OPERATIONS ====================

    // FIXED: Added userId parameter
    public int saveWorkout(Workout workout, int userId) {
        int workoutId = -1;
        try {
            String sql = "INSERT INTO workouts (user_id, name, start_time, end_time, total_volume, notes) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // FIXED: Corrected parameter order and removed duplicates
            pstmt.setInt(1, userId);
            pstmt.setString(2, workout.getName());
            pstmt.setTimestamp(3, Timestamp.valueOf(workout.getStartTime()));
            pstmt.setTimestamp(4, workout.getEndTime() != null ? Timestamp.valueOf(workout.getEndTime()) : null);
            pstmt.setDouble(5, workout.getTotalVolume());
            pstmt.setString(6, workout.getNotes());

            pstmt.executeUpdate();

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                workoutId = generatedKeys.getInt(1);
                saveSets(workoutId, workout.getSets());
            }

            generatedKeys.close();
            pstmt.close();
            System.out.println("✅ Workout saved to database! ID: " + workoutId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to save workout: " + e.getMessage());
        }
        return workoutId;
    }

    private void saveSets(int workoutId, List<WorkoutSet> sets) {
        try {
            String sql = "INSERT INTO workout_sets (workout_id, exercise_name, reps, weight, set_type, completed) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            for (WorkoutSet set : sets) {
                pstmt.setInt(1, workoutId);
                pstmt.setString(2, set.getExercise().getName());
                pstmt.setInt(3, set.getReps());
                pstmt.setDouble(4, set.getWeight());
                pstmt.setString(5, set.getType().toString());
                pstmt.setBoolean(6, set.isCompleted());
                pstmt.executeUpdate();
            }
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to save sets: " + e.getMessage());
        }
    }

    public void addExercise(Exercise exercise) {
        try {
            String sql = "INSERT INTO exercises (name, muscle_group, equipment, difficulty, instructions) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            pstmt.setString(1, exercise.getName());
            pstmt.setString(2, exercise.getMuscleGroup());
            pstmt.setString(3, exercise.getEquipment());
            pstmt.setString(4, exercise.getDifficulty());
            pstmt.setString(5, exercise.getInstructions());

            pstmt.executeUpdate();
            pstmt.close();
            System.out.println("✅ Exercise added: " + exercise.getName());

        } catch (SQLException e) {
            System.err.println("❌ Failed to add exercise: " + e.getMessage());
        }
    }

    // ==================== READ OPERATIONS ====================

    public List<Workout> getAllWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        try {
            String sql = "SELECT * FROM workouts ORDER BY start_time DESC";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Workout workout = buildWorkoutFromResultSet(rs);
                workouts.add(workout);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to load workouts: " + e.getMessage());
        }
        return workouts;
    }

    public Workout getWorkoutById(int workoutId) {
        Workout workout = null;
        try {
            String sql = "SELECT * FROM workouts WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, workoutId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                workout = buildWorkoutFromResultSet(rs);
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to load workout: " + e.getMessage());
        }
        return workout;
    }

    public List<Workout> getWorkoutsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Workout> workouts = new ArrayList<>();
        try {
            String sql = "SELECT * FROM workouts WHERE start_time BETWEEN ? AND ? ORDER BY start_time DESC";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            pstmt.setTimestamp(1, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(2, Timestamp.valueOf(endDate));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Workout workout = buildWorkoutFromResultSet(rs);
                workouts.add(workout);
            }

            rs.close();
            pstmt.close();
            System.out.println("📊 Found " + workouts.size() + " workouts in date range");

        } catch (SQLException e) {
            System.err.println("❌ Failed to load workouts by date: " + e.getMessage());
        }
        return workouts;
    }

    public Map<String, List<WorkoutSet>> getExerciseHistory(String exerciseName) {
        Map<String, List<WorkoutSet>> history = new HashMap<>();
        try {
            String sql = """
                SELECT ws.*, w.start_time, w.name as workout_name
                FROM workout_sets ws
                JOIN workouts w ON ws.workout_id = w.id
                WHERE ws.exercise_name = ?
                ORDER BY w.start_time DESC
                """;

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, exerciseName);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String workoutDate = rs.getTimestamp("start_time").toString();
                Exercise exercise = getExerciseByName(exerciseName);

                if (exercise != null) {
                    WorkoutSet set = new WorkoutSet(exercise, rs.getInt("reps"), rs.getDouble("weight"));
                    set.setCompleted(rs.getBoolean("completed"));

                    history.computeIfAbsent(workoutDate, k -> new ArrayList<>()).add(set);
                }
            }

            rs.close();
            pstmt.close();
            System.out.println("📜 Loaded history for " + exerciseName + ": " + history.size() + " sessions");

        } catch (SQLException e) {
            System.err.println("❌ Failed to load exercise history: " + e.getMessage());
        }
        return history;
    }

    public List<Exercise> getAllExercises() {
        List<Exercise> exercises = new ArrayList<>();
        try {
            String sql = "SELECT * FROM exercises ORDER BY muscle_group, name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Exercise exercise = new Exercise(
                        rs.getString("name"),
                        rs.getString("muscle_group"),
                        rs.getString("equipment"),
                        rs.getString("difficulty"),
                        rs.getString("instructions")
                );
                exercise.setId(rs.getInt("id"));
                exercises.add(exercise);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to load exercises: " + e.getMessage());
        }
        return exercises;
    }

    public Exercise getExerciseByName(String name) {
        Exercise exercise = null;
        try {
            String sql = "SELECT * FROM exercises WHERE name = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, name);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                exercise = new Exercise(
                        rs.getString("name"),
                        rs.getString("muscle_group"),
                        rs.getString("equipment"),
                        rs.getString("difficulty"),
                        rs.getString("instructions")
                );
                exercise.setId(rs.getInt("id"));
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to find exercise: " + e.getMessage());
        }
        return exercise;
    }

    public List<Exercise> getExercisesByMuscleGroup(String muscleGroup) {
        List<Exercise> exercises = new ArrayList<>();
        try {
            String sql = "SELECT * FROM exercises WHERE muscle_group = ? ORDER BY name";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, muscleGroup);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Exercise exercise = new Exercise(
                        rs.getString("name"),
                        rs.getString("muscle_group"),
                        rs.getString("equipment"),
                        rs.getString("difficulty"),
                        rs.getString("instructions")
                );
                exercise.setId(rs.getInt("id"));
                exercises.add(exercise);
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to load exercises by muscle group: " + e.getMessage());
        }
        return exercises;
    }

    private List<WorkoutSet> getSetsForWorkout(int workoutId) {
        List<WorkoutSet> sets = new ArrayList<>();
        try {
            String sql = "SELECT * FROM workoutsets WHERE workoutid = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, workoutId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String exerciseName = rs.getString("exercisename");
                Exercise exercise = getExerciseByName(exerciseName);

                if (exercise != null) {
                    WorkoutSet set = new WorkoutSet(
                            exercise,
                            rs.getInt("reps"),
                            rs.getDouble("weight")
                    );
                    set.setId(rs.getInt("id"));
                    set.setCompleted(rs.getBoolean("completed"));
                    sets.add(set);
                } else {
                    // Create placeholder exercise if not found
                    exercise = new Exercise(exerciseName, "Unknown", "Unknown", "Intermediate", "");
                    WorkoutSet set = new WorkoutSet(
                            exercise,
                            rs.getInt("reps"),
                            rs.getDouble("weight")
                    );
                    set.setId(rs.getInt("id"));
                    set.setCompleted(rs.getBoolean("completed"));
                    sets.add(set);
                }
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("Failed to load sets: " + e.getMessage());
        }
        return sets;
    }


    public Map<String, Object> getWorkoutStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            String sql = """
                SELECT 
                    COUNT(*) as total_workouts,
                    SUM(total_volume) as total_volume,
                    AVG(total_volume) as avg_volume,
                    MAX(total_volume) as max_volume
                FROM workouts
                """;

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                stats.put("total_workouts", rs.getInt("total_workouts"));
                stats.put("total_volume", rs.getDouble("total_volume"));
                stats.put("avg_volume", rs.getDouble("avg_volume"));
                stats.put("max_volume", rs.getDouble("max_volume"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to load workout stats: " + e.getMessage());
        }
        return stats;
    }

    // ==================== UPDATE OPERATIONS ====================

    public boolean updateWorkout(int workoutId, Workout updatedWorkout) {
        try {
            String sql = "UPDATE workouts SET name = ?, end_time = ?, total_volume = ?, notes = ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            pstmt.setString(1, updatedWorkout.getName());
            pstmt.setTimestamp(2, updatedWorkout.getEndTime() != null ? Timestamp.valueOf(updatedWorkout.getEndTime()) : null);
            pstmt.setDouble(3, updatedWorkout.getTotalVolume());
            pstmt.setString(4, updatedWorkout.getNotes());
            pstmt.setInt(5, workoutId);

            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();

            if (rowsAffected > 0) {
                System.out.println("✅ Workout updated successfully! ID: " + workoutId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to update workout: " + e.getMessage());
        }
        return false;
    }

    public boolean updateExercise(int exerciseId, Exercise updatedExercise) {
        try {
            String sql = "UPDATE exercises SET name = ?, muscle_group = ?, equipment = ?, difficulty = ?, instructions = ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            pstmt.setString(1, updatedExercise.getName());
            pstmt.setString(2, updatedExercise.getMuscleGroup());
            pstmt.setString(3, updatedExercise.getEquipment());
            pstmt.setString(4, updatedExercise.getDifficulty());
            pstmt.setString(5, updatedExercise.getInstructions());
            pstmt.setInt(6, exerciseId);

            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();

            if (rowsAffected > 0) {
                System.out.println("✅ Exercise updated successfully! ID: " + exerciseId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to update exercise: " + e.getMessage());
        }
        return false;
    }

    public boolean updateWorkoutSet(int setId, int reps, double weight, boolean completed) {
        try {
            String sql = "UPDATE workout_sets SET reps = ?, weight = ?, completed = ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            pstmt.setInt(1, reps);
            pstmt.setDouble(2, weight);
            pstmt.setBoolean(3, completed);
            pstmt.setInt(4, setId);

            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();

            if (rowsAffected > 0) {
                System.out.println("✅ Set updated successfully! ID: " + setId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to update set: " + e.getMessage());
        }
        return false;
    }

    // ==================== DELETE OPERATIONS ====================

    public boolean deleteWorkout(int workoutId) {
        try {
            String sql = "DELETE FROM workouts WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, workoutId);

            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();

            if (rowsAffected > 0) {
                System.out.println("🗑️ Workout deleted successfully! ID: " + workoutId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete workout: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteExercise(int exerciseId) {
        try {
            String sql = "DELETE FROM exercises WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, exerciseId);

            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();

            if (rowsAffected > 0) {
                System.out.println("🗑️ Exercise deleted successfully! ID: " + exerciseId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete exercise: " + e.getMessage());
        }
        return false;
    }

    public int deleteWorkoutsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        int deletedCount = 0;
        try {
            String sql = "DELETE FROM workouts WHERE start_time BETWEEN ? AND ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            pstmt.setTimestamp(1, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(2, Timestamp.valueOf(endDate));

            deletedCount = pstmt.executeUpdate();
            pstmt.close();

            System.out.println("🗑️ Deleted " + deletedCount + " workouts in date range");

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete workouts by date: " + e.getMessage());
        }
        return deletedCount;
    }

    public boolean deleteAllWorkouts() {
        try {
            String sql = "DELETE FROM workouts";
            Statement stmt = connection.createStatement();
            int rowsAffected = stmt.executeUpdate(sql);
            stmt.close();

            System.out.println("🗑️ All workouts deleted! Total: " + rowsAffected);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete all workouts: " + e.getMessage());
        }
        return false;
    }

    // ==================== HELPER METHODS ====================

    private Workout buildWorkoutFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        LocalDateTime startTime = rs.getTimestamp("starttime").toLocalDateTime();
        Timestamp endTimestamp = rs.getTimestamp("endtime");
        LocalDateTime endTime = endTimestamp != null ? endTimestamp.toLocalDateTime() : null;
        double totalVolume = rs.getDouble("totalvolume");
        String notes = rs.getString("notes");

        Workout workout = new Workout(name);
        workout.setId(id);
        workout.setStartTime(startTime);
        workout.setEndTime(endTime);
        workout.setTotalVolume(totalVolume);
        workout.setNotes(notes);

        // CRITICAL FIX: Load the sets for this workout
        List<WorkoutSet> sets = getSetsForWorkout(id);
        for (WorkoutSet set : sets) {
            workout.addSet(set);
        }

        // Recalculate totals based on loaded sets
        if (!sets.isEmpty()) {
            workout.setTotalVolume(sets.stream()
                    .mapToDouble(WorkoutSet::getVolume)
                    .sum());
        }

        return workout;
    }


    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🗄️ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to close database connection: " + e.getMessage());
        }
    }
}
