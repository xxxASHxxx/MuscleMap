package com.musclemmap.utils;

import com.musclemmap.models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private static DatabaseHelper instance;
    private static final String DB_URL = "jdbc:sqlite:musclemap.db";
    private Connection connection;

    private DatabaseHelper() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            initializeDatabase();
            backfillGifUrlsIfMissing();
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

    // ==================== USER AUTHENTICATION ====================

    public User authenticateUser(String username, String password) {
        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // ✅ FIXED: Uses underscores
                String storedPasswordHash = rs.getString("password_hash");

                if (PasswordUtil.verifyPassword(password, storedPasswordHash)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setWorkoutStreak(rs.getInt("workout_streak"));
                    user.setTotalVolumeLifted(rs.getDouble("total_volume_lifted"));

                    updateLastLogin(user.getId());

                    System.out.println("✅ User authenticated: " + username);
                    rs.close();
                    pstmt.close();
                    return user;
                } else {
                    System.out.println("❌ Invalid password for user: " + username);
                }
            } else {
                System.out.println("❌ User not found: " + username);
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Authentication error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean registerUser(String username, String email, String password) {
        try {
            if (isUsernameTaken(username)) {
                System.out.println("Username already taken: " + username);
                return false;
            }

            if (isEmailTaken(email)) {
                System.out.println("Email already registered: " + email);
                return false;
            }

            String passwordHash = PasswordUtil.hashPassword(password);

            // ✅ FIXED: Uses underscores to match CREATE TABLE
            String sql = "INSERT INTO users (username, email, password_hash, workout_streak, total_volume_lifted) VALUES (?, ?, ?, 0, 0.0)";

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, passwordHash);
            pstmt.executeUpdate();
            pstmt.close();

            System.out.println("✅ User registered successfully: " + username);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Registration error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
            // ✅ FIXED: Uses last_login with underscore
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }
    public void updateUserStreak(int userId, int newStreak) {
        try {
            // ✅ Uses workout_streak with underscore
            String sql = "UPDATE users SET workout_streak = ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, newStreak);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            pstmt.close();
            System.out.println("✅ Updated workout streak for user " + userId + " to " + newStreak);
        } catch (SQLException e) {
            System.err.println("Error updating user streak: " + e.getMessage());
        }
    }
    public void updateTotalVolume(int userId, double additionalVolume) {
        try {
            // ✅ Uses total_volume_lifted with underscores
            String sql = "UPDATE users SET total_volume_lifted = total_volume_lifted + ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setDouble(1, additionalVolume);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            pstmt.close();
            System.out.println("✅ Updated total volume for user " + userId);
        } catch (SQLException e) {
            System.err.println("Error updating total volume: " + e.getMessage());
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
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setWorkoutStreak(rs.getInt("workout_streak"));  // ✅ underscore
                user.setTotalVolumeLifted(rs.getDouble("total_volume_lifted"));  // ✅ underscores

                rs.close();
                pstmt.close();
                return user;
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("Error getting user: " + e.getMessage());
        }
        return null;
    }

    // ==================== DATABASE INITIALIZATION ====================

    private void initializeDatabase() {
        try {
            Statement stmt = connection.createStatement();

            // USERS TABLE
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT UNIQUE NOT NULL, "
                    + "email TEXT UNIQUE NOT NULL, "
                    + "password_hash TEXT NOT NULL, "
                    + "createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "last_login TIMESTAMP, "
                    + "workout_streak INTEGER DEFAULT 0, "
                    + "total_volume_lifted REAL DEFAULT 0.0"
                    + ")";
            stmt.execute(createUsersTable);

            // ✅ EXERCISES TABLE - ADDED gifurl COLUMN
            String createExercisesTable = "CREATE TABLE IF NOT EXISTS exercises ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT UNIQUE NOT NULL, "
                    + "musclegroup TEXT NOT NULL, "
                    + "equipment TEXT NOT NULL, "
                    + "difficulty TEXT NOT NULL, "
                    + "instructions TEXT, "
                    + "gifurl TEXT"  // ✅ ADDED THIS!
                    + ")";

            stmt.execute(createExercisesTable);

            // WORKOUTS TABLE
            String createWorkoutsTable = "CREATE TABLE IF NOT EXISTS workouts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "userid INTEGER NOT NULL, "
                    + "name TEXT NOT NULL, "
                    + "starttime TIMESTAMP NOT NULL, "
                    + "endtime TIMESTAMP, "
                    + "totalvolume REAL DEFAULT 0.0, "
                    + "notes TEXT, "
                    + "FOREIGN KEY (userid) REFERENCES users(id) ON DELETE CASCADE"
                    + ")";
            stmt.execute(createWorkoutsTable);

            // SETS TABLE
            // Option 1: Add to CREATE TABLE
            String createSetsTable = "CREATE TABLE IF NOT EXISTS workoutsets ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "workoutid INTEGER NOT NULL, "
                    + "exercisename TEXT NOT NULL, "
                    + "musclegroup TEXT, "  // ✅ ADD THIS
                    + "reps INTEGER NOT NULL, "
                    + "weight REAL NOT NULL, "
                    + "settype TEXT DEFAULT 'NORMAL', "
                    + "completed BOOLEAN DEFAULT 0, "
                    + "FOREIGN KEY (workoutid) REFERENCES workouts(id) ON DELETE CASCADE"
                    + ")";

            stmt.execute(createSetsTable);

            // PERSONAL RECORDS TABLE
            String createPersonalRecordsTable = """
                    CREATE TABLE IF NOT EXISTS personal_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        exercise_id INTEGER NOT NULL,
                        max_weight REAL NOT NULL,
                        max_reps INTEGER NOT NULL,
                        date_achieved TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE,
                        UNIQUE(user_id, exercise_id)
                    )""";
            stmt.execute(createPersonalRecordsTable);

            // WORKOUT ROUTINES TABLE
            String createRoutinesTable = """
                    CREATE TABLE IF NOT EXISTS workout_routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""";
            stmt.execute(createRoutinesTable);

            // ROUTINE EXERCISES TABLE (Junction table)
            String createRoutineExercisesTable = """
                    CREATE TABLE IF NOT EXISTS routine_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        routine_id INTEGER NOT NULL,
                        exercise_id INTEGER NOT NULL,
                        sets INTEGER DEFAULT 3,
                        target_reps INTEGER DEFAULT 10,
                        order_index INTEGER DEFAULT 0,
                        FOREIGN KEY (routine_id) REFERENCES workout_routines(id) ON DELETE CASCADE,
                        FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
                    )""";
            stmt.execute(createRoutineExercisesTable);

            // BODY METRICS TABLE
            String createBodyMetricsTable = """
                    CREATE TABLE IF NOT EXISTS body_metrics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        weight REAL,
                        body_fat_percentage REAL,
                        muscle_mass REAL,
                        notes TEXT,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""";
            stmt.execute(createBodyMetricsTable);

            stmt.close();
            System.out.println("✅ Database tables initialized successfully with gifurl column!");

            // Insert sample exercises
            insertSampleExercises();

        } catch (SQLException e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertSampleExercises() {
        try {
            // ✅ Check if exercises already exist
            String checkSql = "SELECT COUNT(*) FROM exercises";
            Statement checkStmt = connection.createStatement();
            ResultSet rs = checkStmt.executeQuery(checkSql);

            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("✅ Exercises already exist in database (" + rs.getInt(1) + " exercises)");
                rs.close();
                checkStmt.close();
                return;
            }
            rs.close();
            checkStmt.close();

            // ✅ FIXED: Now includes ALL 6 columns (with empty gifurl)
            String insertSql = "INSERT INTO exercises (name, musclegroup, equipment, difficulty, instructions, gifurl) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(insertSql);

            // ✅ COMPLETE 69 EXERCISES - ALL 14 MUSCLE GROUPS
            Object[][] exercises = {
                    // CHEST (9)
                    {"Bench Press", "Chest", "Barbell", "Intermediate", "Lie on bench, lower bar to chest, press up", ""},
                    {"Incline Bench Press", "Chest", "Barbell", "Intermediate", "Set bench to 30-45 degrees, press barbell upward", ""},
                    {"Decline Bench Press", "Chest", "Barbell", "Intermediate", "Set bench to decline, press barbell from lower chest", ""},
                    {"Dumbbell Press", "Chest", "Dumbbell", "Beginner", "Press dumbbells from chest level to full extension", ""},
                    {"Incline Dumbbell Press", "Chest", "Dumbbell", "Intermediate", "Press dumbbells on incline bench", ""},
                    {"Chest Fly", "Chest", "Dumbbell", "Intermediate", "Open arms wide, bring dumbbells together above chest", ""},
                    {"Cable Fly", "Chest", "Cable", "Intermediate", "Pull cables from sides to center with slight bend in elbows", ""},
                    {"Push-ups", "Chest", "Bodyweight", "Beginner", "Lower body, push back up", ""},
                    {"Chest Dips", "Chest", "Bodyweight", "Intermediate", "Lower body between bars, focusing on chest stretch", ""},

                    // BACK (7)
                    {"Deadlifts", "Back", "Barbell", "Advanced", "Lift bar from ground to hip level", ""},
                    {"Barbell Row", "Back", "Barbell", "Intermediate", "Bend forward, pull barbell to lower chest", ""},
                    {"Pull-ups", "Back", "Bodyweight", "Intermediate", "Hang from bar, pull body up", ""},
                    {"Lat Pulldown", "Back", "Machine", "Beginner", "Pull bar down to upper chest with wide grip", ""},
                    {"Seated Cable Row", "Back", "Cable", "Beginner", "Pull handle to torso, squeeze shoulder blades", ""},
                    {"T-Bar Row", "Back", "Barbell", "Intermediate", "Pull loaded end of barbell to chest", ""},
                    {"One-Arm Dumbbell Row", "Back", "Dumbbell", "Intermediate", "Support on bench, row dumbbell to hip", ""},

                    // SHOULDERS (7)
                    {"Overhead Press", "Shoulders", "Barbell", "Intermediate", "Press bar overhead", ""},
                    {"Dumbbell Shoulder Press", "Shoulders", "Dumbbell", "Intermediate", "Press dumbbells overhead from shoulder level", ""},
                    {"Lateral Raise", "Shoulders", "Dumbbell", "Beginner", "Raise dumbbells to sides until arms parallel to ground", ""},
                    {"Front Raise", "Shoulders", "Dumbbell", "Beginner", "Raise dumbbells forward to shoulder height", ""},
                    {"Rear Delt Fly", "Shoulders", "Dumbbell", "Intermediate", "Bend forward, raise dumbbells to sides", ""},
                    {"Face Pull", "Shoulders", "Cable", "Intermediate", "Pull rope to face level, flare elbows out", ""},
                    {"Arnold Press", "Shoulders", "Dumbbell", "Advanced", "Rotate palms while pressing dumbbells overhead", ""},

                    // BICEPS (6)
                    {"Bicep Curls", "Biceps", "Dumbbell", "Beginner", "Curl weights to shoulders", ""},
                    {"Barbell Curl", "Biceps", "Barbell", "Beginner", "Curl barbell to shoulders with underhand grip", ""},
                    {"Hammer Curl", "Biceps", "Dumbbell", "Beginner", "Curl dumbbells with neutral grip", ""},
                    {"Preacher Curl", "Biceps", "Barbell", "Intermediate", "Curl barbell on preacher bench for isolation", ""},
                    {"Cable Curl", "Biceps", "Cable", "Beginner", "Curl cable handle to shoulders", ""},
                    {"Concentration Curl", "Biceps", "Dumbbell", "Intermediate", "Sit, rest elbow on thigh, curl dumbbell up", ""},

                    // TRICEPS (6)
                    {"Tricep Dips", "Triceps", "Bodyweight", "Intermediate", "Lower body between bars, push up", ""},
                    {"Close-Grip Bench Press", "Triceps", "Barbell", "Intermediate", "Bench press with hands shoulder-width apart", ""},
                    {"Tricep Pushdown", "Triceps", "Cable", "Beginner", "Push cable bar down until arms fully extended", ""},
                    {"Overhead Tricep Extension", "Triceps", "Dumbbell", "Intermediate", "Extend dumbbell overhead, lower behind head", ""},
                    {"Skull Crushers", "Triceps", "Barbell", "Intermediate", "Lower barbell to forehead, extend back up", ""},
                    {"Diamond Push-ups", "Triceps", "Bodyweight", "Intermediate", "Push-ups with hands forming diamond shape", ""},

                    // FOREARMS (3)
                    {"Wrist Curls", "Forearms", "Dumbbell", "Beginner", "Curl wrists upward with forearms on thighs", ""},
                    {"Reverse Wrist Curls", "Forearms", "Dumbbell", "Beginner", "Curl wrists upward with overhand grip", ""},
                    {"Farmer's Walk", "Forearms", "Dumbbell", "Intermediate", "Walk while holding heavy dumbbells at sides", ""},

                    // ABS (6)
                    {"Planks", "Abs", "Bodyweight", "Beginner", "Hold plank position", ""},
                    {"Crunches", "Abs", "Bodyweight", "Beginner", "Lie on back, lift shoulders off ground", ""},
                    {"Leg Raises", "Abs", "Bodyweight", "Intermediate", "Lie on back, raise legs to vertical", ""},
                    {"Russian Twists", "Abs", "Bodyweight", "Intermediate", "Sit with feet elevated, twist torso side to side", ""},
                    {"Cable Crunch", "Abs", "Cable", "Intermediate", "Kneel, crunch torso down against cable resistance", ""},
                    {"Hanging Knee Raise", "Abs", "Bodyweight", "Advanced", "Hang from bar, raise knees to chest", ""},

                    // OBLIQUES (3)
                    {"Side Plank", "Obliques", "Bodyweight", "Intermediate", "Hold body sideways on one forearm", ""},
                    {"Bicycle Crunches", "Obliques", "Bodyweight", "Beginner", "Alternate bringing opposite elbow to knee", ""},
                    {"Wood Chops", "Obliques", "Cable", "Intermediate", "Pull cable diagonally across body", ""},

                    // LOWER BACK (3)
                    {"Back Extensions", "Lower Back", "Bodyweight", "Beginner", "Lie face down, lift upper body off ground", ""},
                    {"Good Mornings", "Lower Back", "Barbell", "Intermediate", "Bend forward at hips with barbell on shoulders", ""},
                    {"Superman", "Lower Back", "Bodyweight", "Beginner", "Lie face down, lift arms and legs simultaneously", ""},

                    // GLUTES (4)
                    {"Hip Thrusts", "Glutes", "Barbell", "Intermediate", "Thrust hips upward with barbell on hips", ""},
                    {"Glute Bridges", "Glutes", "Bodyweight", "Beginner", "Lie on back, lift hips until body forms straight line", ""},
                    {"Bulgarian Split Squats", "Glutes", "Dumbbell", "Advanced", "Single leg squat with rear foot elevated", ""},
                    {"Cable Kickbacks", "Glutes", "Cable", "Beginner", "Kick leg back against cable resistance", ""},

                    // QUADRICEPS (6)
                    {"Squats", "Quadriceps", "Barbell", "Intermediate", "Lower body until thighs parallel", ""},
                    {"Front Squats", "Quadriceps", "Barbell", "Advanced", "Squat with barbell held at front of shoulders", ""},
                    {"Leg Press", "Quadriceps", "Machine", "Beginner", "Push platform away with feet", ""},
                    {"Lunges", "Quadriceps", "Bodyweight", "Beginner", "Step forward, lower back knee", ""},
                    {"Leg Extensions", "Quadriceps", "Machine", "Beginner", "Extend legs against resistance", ""},
                    {"Walking Lunges", "Quadriceps", "Dumbbell", "Intermediate", "Lunge forward continuously while walking", ""},

                    // HAMSTRINGS (4)
                    {"Romanian Deadlift", "Hamstrings", "Barbell", "Intermediate", "Lower barbell by bending at hips, legs nearly straight", ""},
                    {"Leg Curls", "Hamstrings", "Machine", "Beginner", "Curl legs upward against resistance", ""},
                    {"Nordic Curls", "Hamstrings", "Bodyweight", "Advanced", "Lower body forward with knees fixed", ""},
                    {"Stiff-Leg Deadlift", "Hamstrings", "Barbell", "Intermediate", "Deadlift with minimal knee bend", ""},

                    // CALVES (3)
                    {"Standing Calf Raise", "Calves", "Machine", "Beginner", "Raise heels as high as possible, lower slowly", ""},
                    {"Seated Calf Raise", "Calves", "Machine", "Beginner", "Raise heels with knees bent", ""},
                    {"Jump Rope", "Calves", "Bodyweight", "Beginner", "Jump rope continuously for calf work", ""},

                    // NECK (2)
                    {"Neck Curls", "Neck", "Bodyweight", "Beginner", "Lie on back, lift head toward chest", ""},
                    {"Neck Extensions", "Neck", "Bodyweight", "Beginner", "Lie face down, lift head upward",""}
            };

            int insertCount = 0;
            for (Object[] ex : exercises) {
                pstmt.setString(1, (String) ex[0]);  // name
                pstmt.setString(2, (String) ex[1]);  // muscle_group
                pstmt.setString(3, (String) ex[2]);  // equipment
                pstmt.setString(4, (String) ex[3]);  // difficulty
                pstmt.setString(5, (String) ex[4]);  // instructions
                pstmt.setString(6, ex.length >= 6 ? (String) ex[5] : "");
                pstmt.executeUpdate();
                insertCount++;
            }
            pstmt.close();

            System.out.println("💪 Successfully inserted " + insertCount + " exercises!");

        } catch (SQLException e) {
            System.err.println("⚠️ Error inserting exercises: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== WORKOUT OPERATIONS ====================

    public int saveWorkout(Workout workout, int userId) {
        try {
            // ✅ FIXED: Column names match CREATE TABLE exactly
            String workoutSql = "INSERT INTO workouts (userid, name, starttime, endtime, totalvolume) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement workoutStmt = connection.prepareStatement(workoutSql, Statement.RETURN_GENERATED_KEYS);

            workoutStmt.setInt(1, userId);
            workoutStmt.setString(2, workout.getName());
            workoutStmt.setTimestamp(3, Timestamp.valueOf(workout.getStartTime()));
            workoutStmt.setTimestamp(4, workout.getEndTime() != null ? Timestamp.valueOf(workout.getEndTime()) : Timestamp.valueOf(LocalDateTime.now()));
            workoutStmt.setDouble(5, workout.getTotalVolume());

            int affectedRows = workoutStmt.executeUpdate();

            if (affectedRows == 0) {
                System.err.println("❌ Failed to insert workout");
                return -1;
            }

            // Get generated workout ID
            ResultSet generatedKeys = workoutStmt.getGeneratedKeys();
            int workoutId = -1;
            if (generatedKeys.next()) {
                workoutId = generatedKeys.getInt(1);
                System.out.println("✅ Workout saved with ID: " + workoutId);
            }

            generatedKeys.close();
            workoutStmt.close();

            // Save all sets
            if (workout.getSets() != null && !workout.getSets().isEmpty()) {
                saveSets(workoutId, workout.getSets());
            }

            return workoutId;

        } catch (SQLException e) {
            System.err.println("❌ Error saving workout: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }


    private void saveSets(int workoutId, List<WorkoutSet> sets) {
        try {
            // ✅ FIXED: Matches workoutsets table columns exactly
            String sql = "INSERT INTO workoutsets (workoutid, exercisename, musclegroup, reps, weight, settype, completed) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            for (WorkoutSet set : sets) {
                pstmt.setInt(1, workoutId);
                pstmt.setString(2, set.getExercise().getName());
                pstmt.setString(3, set.getExercise().getMuscleGroup());
                pstmt.setInt(4, set.getReps());
                pstmt.setDouble(5, set.getWeight());
                pstmt.setString(6, set.getType() != null ? set.getType().toString() : "NORMAL");
                pstmt.setBoolean(7, set.isCompleted());
                pstmt.executeUpdate();
            }

            pstmt.close();
            System.out.println("✅ Saved " + sets.size() + " sets for workout " + workoutId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to save sets: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Method 1: Compatibility alias
    public List<Workout> getWorkoutsByUser(int userId) {
        return getWorkoutsByUserId(userId);
    }

    // Method 2: Main implementation
    public List<Workout> getWorkoutsByUserId(int userId) {
        List<Workout> workouts = new ArrayList<>();
        try {
            // ✅ FIXED: Use correct column name 'userid' instead of 'user_id'
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
            System.out.println("✅ Loaded " + workouts.size() + " workouts for user " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Failed to load user workouts: " + e.getMessage());
            e.printStackTrace();
        }

        return workouts;
    }

    public Map<String, Double> getPersonalRecords(int userId) {
        Map<String, Double> prs = new HashMap<>();
        try {
            // ✅ FIXED: Use correct table name 'workoutsets' and column names
            String sql = """
            SELECT ws.exercisename, MAX(ws.weight) as max_weight
            FROM workoutsets ws
            JOIN workouts w ON ws.workoutid = w.id
            WHERE w.userid = ?
            GROUP BY ws.exercisename
            ORDER BY max_weight DESC
            """;

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                prs.put(rs.getString("exercisename"), rs.getDouble("max_weight"));
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("Failed to load PRs: " + e.getMessage());
        }
        return prs;
    }

    private Workout buildWorkoutFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        // ✅ FIXED: Use correct column names from database schema
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

        // CRITICAL: Load the sets for this workout
        List<WorkoutSet> sets = getSetsForWorkout(id);
        workout.setSets(sets);

        return workout;
    }

    private List<WorkoutSet> getSetsForWorkout(int workoutId) {
        List<WorkoutSet> sets = new ArrayList<>();

        try {
            // ✅ FIXED: Column names match workoutsets table
            String sql = "SELECT * FROM workoutsets WHERE workoutid = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, workoutId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String exerciseName = rs.getString("exercisename");
                String muscleGroup = rs.getString("musclegroup");

                Exercise exercise = getExerciseByName(exerciseName);
                if (exercise == null) {
                    exercise = new Exercise(exerciseName, muscleGroup != null ? muscleGroup : "Unknown", "Unknown", "Intermediate", "");
                }

                WorkoutSet set = new WorkoutSet(
                        exercise,
                        rs.getInt("reps"),
                        rs.getDouble("weight")
                );

                set.setId(rs.getInt("id"));
                set.setCompleted(rs.getBoolean("completed"));
                sets.add(set);
            }

            rs.close();
            pstmt.close();
            System.out.println("✅ Loaded " + sets.size() + " sets for workout " + workoutId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to load sets: " + e.getMessage());
            e.printStackTrace();
        }

        return sets;
    }

    public List<Workout> getAllWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        try {
            // ✅ FIXED: Use correct column name 'starttime'
            String sql = "SELECT * FROM workouts ORDER BY starttime DESC";
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

    public List<Workout> getWorkoutsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Workout> workouts = new ArrayList<>();
        try {
            // ✅ FIXED: Use correct column name 'starttime'
            String sql = "SELECT * FROM workouts WHERE starttime BETWEEN ? AND ? ORDER BY starttime DESC";
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

    // ==================== EXERCISE OPERATIONS ====================

    public void addExercise(Exercise exercise) {
        try {
            // ✅ FIXED: Use correct column names from database schema
            String sql = "INSERT INTO exercises (name, musclegroup, equipment, difficulty, instructions, gifurl) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);

            pstmt.setString(1, exercise.getName());
            pstmt.setString(2, exercise.getMuscleGroup());
            pstmt.setString(3, exercise.getEquipment());
            pstmt.setString(4, exercise.getDifficulty());
            pstmt.setString(5, exercise.getInstructions());
            pstmt.setString(6, exercise.getGifUrl());

            pstmt.executeUpdate();
            pstmt.close();
            System.out.println("✅ Exercise added: " + exercise.getName());

        } catch (SQLException e) {
            System.err.println("❌ Failed to add exercise: " + e.getMessage());
        }
    }

    public List<Exercise> getAllExercises() {
        List<Exercise> exercises = new ArrayList<>();
        try {
            // ✅ FIXED: Use correct column name 'musclegroup'
            String sql = "SELECT * FROM exercises ORDER BY musclegroup, name";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                // ✅ FIXED: Use 7-parameter constructor with correct column names
                Exercise exercise = new Exercise(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("musclegroup"),
                        rs.getString("equipment"),
                        rs.getString("difficulty"),
                        rs.getString("instructions"),
                        rs.getString("gifurl")
                );
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
                // ✅ FIXED: Use 7-parameter constructor with correct column names
                exercise = new Exercise(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("musclegroup"),
                        rs.getString("equipment"),
                        rs.getString("difficulty"),
                        rs.getString("instructions"),
                        rs.getString("gifurl")
                );
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

        if (muscleGroup == null || muscleGroup.isBlank()) {
            System.err.println("⚠️ Null or empty muscle group requested");
            return exercises;
        }

        try {
            // ✅ CASE-INSENSITIVE query
            String sql = "SELECT * FROM exercises WHERE LOWER(musclegroup) = LOWER(?) ORDER BY name";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, muscleGroup.trim());

            System.out.println("🔍 Querying database for muscle group: '" + muscleGroup + "'");

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // ✅ Use 7-parameter constructor with gifurl
                Exercise exercise = new Exercise(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("musclegroup"),
                        rs.getString("equipment"),
                        rs.getString("difficulty"),
                        rs.getString("instructions"),
                        rs.getString("gifurl")  // ✅ Correct column name
                );
                exercises.add(exercise);
            }

            rs.close();
            pstmt.close();

            System.out.println("✅ Found " + exercises.size() + " exercises for: " + muscleGroup);

            // ✅ DEBUG: If no exercises found, show what's actually in the database
            if (exercises.isEmpty()) {
                System.err.println("❌ NO EXERCISES FOUND FOR: " + muscleGroup);
                showAvailableMuscleGroups();
            }

        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
            e.printStackTrace();
        }

        return exercises;
    }

    // ✅ ADD THIS DEBUG METHOD
    private void showAvailableMuscleGroups() {
        try {
            String sql = "SELECT DISTINCT musclegroup, COUNT(*) as count FROM exercises GROUP BY musclegroup ORDER BY musclegroup";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n📊 MUSCLE GROUPS IN DATABASE:");
            System.out.println("=" + "=".repeat(50));
            while (rs.next()) {
                System.out.println("  ✓ " + rs.getString("musclegroup") + " → " + rs.getInt("count") + " exercises");
            }
            System.out.println("=" + "=".repeat(50) + "\n");

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Failed to query muscle groups: " + e.getMessage());
        }
    }

    public Map<String, List<WorkoutSet>> getExerciseHistory(String exerciseName) {
        Map<String, List<WorkoutSet>> history = new HashMap<>();
        try {
            // ✅ FIXED: Use correct table name 'workoutsets' and column names
            String sql = """
                SELECT ws.*, w.starttime, w.name as workout_name
                FROM workoutsets ws
                JOIN workouts w ON ws.workoutid = w.id
                WHERE ws.exercisename = ?
                ORDER BY w.starttime DESC
                """;

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, exerciseName);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String workoutDate = rs.getTimestamp("starttime").toString();
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

    public Map<String, Object> getWorkoutStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // ✅ FIXED: Use correct column name 'totalvolume'
            String sql = """
                SELECT 
                    COUNT(*) as total_workouts,
                    SUM(totalvolume) as total_volume,
                    AVG(totalvolume) as avg_volume,
                    MAX(totalvolume) as max_volume
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
    private void backfillGifUrlsIfMissing() {
        final String select = "SELECT id, name, gifurl FROM exercises";
        final String update = "UPDATE exercises SET gifurl = ? WHERE id = ?";
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery(select);
             PreparedStatement up = connection.prepareStatement(update)) {
            int patched = 0;
            while (rs.next()) {
                String current = rs.getString("gifurl");
                if (current != null && !current.isBlank()) continue;

                String name = rs.getString("name");
                String key = name == null ? "" : name.toLowerCase()
                        .replace('-', ' ')
                        .replace('_', ' ')
                        .replaceAll("\\s+", " ")
                        .trim();
                if (key.equals("lat pull down")) key = "lat pulldown";
                if (key.equals("barbell rows")) key = "barbell row";
                if (key.equals("cable flies")) key = "cable fly";
                if (key.equals("dumbbell flies")) key = "chest fly";
                if (key.equals("push ups")) key = "push-ups";
                if (key.equals("overhead triceps extension")) key = "overhead tricep extension";
                if (key.equals("triceps pushdown")) key = "tricep pushdown";
                if (key.equals("skull crusher")) key = "skull crushers";
                if (key.equals("diamond push ups")) key = "diamond push-ups";

                String mapped = null;
                try {
                    var m = com.musclemmap.models.Exercise.class
                            .getDeclaredMethod("getExerciseImageUrl", String.class);
                    m.setAccessible(true);
                    mapped = (String) m.invoke(null, key);
                } catch (Exception ignore) {}

                if (mapped != null && !mapped.isBlank()) {
                    up.setString(1, mapped);
                    up.setInt(2, rs.getInt("id"));
                    up.addBatch();
                    patched++;
                }
            }
            up.executeBatch();
            if (patched > 0) System.out.println("🔁 Backfilled gifurl for " + patched + " exercises.");
        } catch (Exception e) {
            System.err.println("gifurl backfill failed: " + e.getMessage());
        }
    }

    // ==================== WORKOUT LOGGING METHODS ====================
    
    /**
     * Start a new workout for a user
     */
    public int startWorkout(int userId, String workoutName) {
        try {
            String sql = "INSERT INTO workouts (userid, name, starttime, totalvolume) VALUES (?, ?, ?, 0.0)";
            PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, workoutName);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int workoutId = generatedKeys.getInt(1);
                    System.out.println("✅ Started workout: " + workoutName + " (ID: " + workoutId + ")");
                    generatedKeys.close();
                    pstmt.close();
                    return workoutId;
                }
            }
            
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Failed to start workout: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
    
    /**
     * End a workout and calculate total volume
     */
    public boolean endWorkout(int workoutId) {
        try {
            // First, calculate total volume from all sets
            double totalVolume = calculateWorkoutVolume(workoutId);
            
            String sql = "UPDATE workouts SET endtime = ?, totalvolume = ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setDouble(2, totalVolume);
            pstmt.setInt(3, workoutId);
            
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Ended workout ID: " + workoutId + " (Total Volume: " + totalVolume + ")");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to end workout: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Add a set to an existing workout
     */
    public boolean addSetToWorkout(int workoutId, String exerciseName, int reps, double weight, String setType) {
        try {
            // Get exercise details
            Exercise exercise = getExerciseByName(exerciseName);
            if (exercise == null) {
                System.err.println("❌ Exercise not found: " + exerciseName);
                return false;
            }
            
            String sql = "INSERT INTO workoutsets (workoutid, exercisename, musclegroup, reps, weight, settype, completed) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            
            pstmt.setInt(1, workoutId);
            pstmt.setString(2, exerciseName);
            pstmt.setString(3, exercise.getMuscleGroup());
            pstmt.setInt(4, reps);
            pstmt.setDouble(5, weight);
            pstmt.setString(6, setType != null ? setType : "NORMAL");
            pstmt.setBoolean(7, true); // Mark as completed when added
            
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Added set: " + exerciseName + " - " + reps + " reps @ " + weight + " lbs");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to add set: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Calculate total volume for a workout
     */
    private double calculateWorkoutVolume(int workoutId) {
        double totalVolume = 0.0;
        try {
            String sql = "SELECT SUM(reps * weight) as total_volume FROM workoutsets WHERE workoutid = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, workoutId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalVolume = rs.getDouble("total_volume");
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to calculate workout volume: " + e.getMessage());
        }
        return totalVolume;
    }
    
    /**
     * Get workout summary for a user
     */
    public Map<String, Object> getWorkoutSummary(int userId) {
        Map<String, Object> summary = new HashMap<>();
        try {
            String sql = """
                SELECT 
                    COUNT(*) as total_workouts,
                    SUM(totalvolume) as total_volume,
                    AVG(totalvolume) as avg_volume,
                    MAX(totalvolume) as max_volume,
                    COUNT(DISTINCT DATE(starttime)) as workout_days
                FROM workouts 
                WHERE userid = ?
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                summary.put("total_workouts", rs.getInt("total_workouts"));
                summary.put("total_volume", rs.getDouble("total_volume"));
                summary.put("avg_volume", rs.getDouble("avg_volume"));
                summary.put("max_volume", rs.getDouble("max_volume"));
                summary.put("workout_days", rs.getInt("workout_days"));
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to get workout summary: " + e.getMessage());
        }
        return summary;
    }
    
    /**
     * Get recent workouts for a user (last 10)
     */
    public List<Workout> getRecentWorkouts(int userId, int limit) {
        List<Workout> workouts = new ArrayList<>();
        try {
            String sql = "SELECT * FROM workouts WHERE userid = ? ORDER BY starttime DESC LIMIT ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Workout workout = buildWorkoutFromResultSet(rs);
                workouts.add(workout);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("❌ Failed to get recent workouts: " + e.getMessage());
        }
        return workouts;
    }

    // ==================== UPDATE/DELETE OPERATIONS ====================

    public boolean updateWorkout(int workoutId, Workout updatedWorkout) {
        try {
            // ✅ FIXED: Use correct column names from database schema
            String sql = "UPDATE workouts SET name = ?, endtime = ?, totalvolume = ?, notes = ? WHERE id = ?";
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
