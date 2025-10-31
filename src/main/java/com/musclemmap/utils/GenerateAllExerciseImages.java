package com.musclemmap.utils;

/**
 * Generate all exercise step images for the MuscleMap application
 * Run this class to create 414 images (69 exercises × 6 steps)
 */
public class GenerateAllExerciseImages {
    
    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  GENERATING ALL EXERCISE STEP IMAGES");
        System.out.println("============================================================\n");
        
        int totalGenerated = 0;
        
        // Chest exercises
        totalGenerated += generateChestExercises();
        
        // Back exercises
        totalGenerated += generateBackExercises();
        
        // Shoulder exercises
        totalGenerated += generateShoulderExercises();
        
        // Biceps exercises
        totalGenerated += generateBicepsExercises();
        
        // Triceps exercises
        totalGenerated += generateTricepsExercises();
        
        // Forearm exercises
        totalGenerated += generateForearmExercises();
        
        // Abs exercises
        totalGenerated += generateAbsExercises();
        
        // Obliques exercises
        totalGenerated += generateObliquesExercises();
        
        // Lower Back exercises
        totalGenerated += generateLowerBackExercises();
        
        // Glutes exercises
        totalGenerated += generateGlutesExercises();
        
        // Quadriceps exercises
        totalGenerated += generateQuadricepsExercises();
        
        // Hamstrings exercises
        totalGenerated += generateHamstringsExercises();
        
        // Calves exercises
        totalGenerated += generateCalvesExercises();
        
        // Neck exercises
        totalGenerated += generateNeckExercises();
        
        System.out.println("\n============================================================");
        System.out.println("  GENERATION COMPLETE!");
        System.out.println("============================================================");
        System.out.println("[+] Total images generated: " + totalGenerated);
        System.out.println("[+] Images are ready in src/main/resources/images/exercises/");
    }
    
    private static int generateChestExercises() {
        System.out.println("[*] Generating CHEST exercises...");
        int count = 0;
        
        // Bench Press
        String[] benchPressSteps = {
            "Lie flat on bench, feet planted firmly on ground",
            "Grip bar slightly wider than shoulders, thumbs around bar",
            "Unrack bar with straight arms, position over chest",
            "Lower bar to chest with control, elbows at 45 degrees",
            "Press bar up explosively, maintain control",
            "Extend arms fully, lock out at top"
        };
        ImageGenerator.generateAllStepsForExercise("chest", "bench-press", benchPressSteps);
        count += 6;
        
        // Add other chest exercises...
        String[] dumbbellPressSteps = {
            "Sit on bench with dumbbells resting on thighs",
            "Lie back, bring dumbbells to shoulder level",
            "Press dumbbells up until arms extend fully",
            "Lower dumbbells with control to chest level",
            "Maintain stability, engage core throughout",
            "Complete rep, repeat for desired reps"
        };
        ImageGenerator.generateAllStepsForExercise("chest", "dumbbell-press", dumbbellPressSteps);
        count += 6;
        
        // Push-ups
        String[] pushUpSteps = {
            "Start in plank position, hands shoulder-width",
            "Keep body straight from head to heels",
            "Lower chest toward ground, elbows at 45 degrees",
            "Chest nearly touches ground at bottom",
            "Push through palms to return to start",
            "Maintain straight body line throughout"
        };
        ImageGenerator.generateAllStepsForExercise("chest", "push-ups", pushUpSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " chest exercise images");
        return count;
    }
    
    private static int generateBackExercises() {
        System.out.println("[*] Generating BACK exercises...");
        int count = 0;
        
        // Pull-ups
        String[] pullUpSteps = {
            "Hang from bar with overhand grip, arms fully extended",
            "Engage lats, pull shoulder blades down and back",
            "Pull body up until chin clears bar",
            "Hold position briefly at top",
            "Lower body with control to starting position",
            "Fully extend arms at bottom, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("back", "pull-ups", pullUpSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " back exercise images");
        return count;
    }
    
    private static int generateShoulderExercises() {
        System.out.println("[*] Generating SHOULDER exercises...");
        int count = 0;
        
        // Overhead Press
        String[] ohpSteps = {
            "Stand with feet shoulder-width, barbell at collarbone",
            "Grip bar slightly wider than shoulders",
            "Press bar straight up overhead, full extension",
            "Lock out at top, bar over head and feet",
            "Lower bar with control back to collarbone",
            "Maintain tight core, avoid arching back"
        };
        ImageGenerator.generateAllStepsForExercise("shoulders", "overhead-press", ohpSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " shoulder exercise images");
        return count;
    }
    
    private static int generateBicepsExercises() {
        System.out.println("[*] Generating BICEPS exercises...");
        int count = 0;
        
        // Barbell Curl
        String[] curlSteps = {
            "Stand with feet hip-width, hold barbell with underhand grip",
            "Keep elbows close to sides, palms facing forward",
            "Curl barbell up toward shoulders, squeeze biceps",
            "Hold contraction at top for 1 second",
            "Lower barbell with control to starting position",
            "Fully extend arms at bottom, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("biceps", "barbell-curl", curlSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " biceps exercise images");
        return count;
    }
    
    private static int generateTricepsExercises() {
        System.out.println("[*] Generating TRICEPS exercises...");
        int count = 0;
        
        // Tricep Dips
        String[] dipSteps = {
            "Grip parallel bars, support body with straight arms",
            "Keep body upright, core engaged",
            "Lower body by bending elbows to 90 degrees",
            "Shoulders should drop below elbows",
            "Push through palms to return to start",
            "Fully extend arms at top, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("triceps", "tricep-dips", dipSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " triceps exercise images");
        return count;
    }
    
    private static int generateForearmExercises() {
        System.out.println("[*] Generating FOREARM exercises...");
        int count = 0;
        
        // Wrist Curls
        String[] wristCurlSteps = {
            "Sit on bench, forearms resting on thighs",
            "Hold barbell with underhand grip, palms up",
            "Lower barbell by extending wrists",
            "Curl wrists up, squeeze forearms",
            "Hold contraction at top briefly",
            "Lower with control, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("forearms", "wrist-curls", wristCurlSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " forearm exercise images");
        return count;
    }
    
    private static int generateAbsExercises() {
        System.out.println("[*] Generating ABS exercises...");
        int count = 0;
        
        // Planks
        String[] plankSteps = {
            "Start in push-up position on floor",
            "Lower to forearms, elbows under shoulders",
            "Keep body in straight line from head to heels",
            "Engage core and glutes tightly",
            "Hold position for desired time",
            "Maintain steady breathing throughout"
        };
        ImageGenerator.generateAllStepsForExercise("abs", "planks", plankSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " abs exercise images");
        return count;
    }
    
    private static int generateObliquesExercises() {
        System.out.println("[*] Generating OBLIQUES exercises...");
        int count = 0;
        
        // Side Plank
        String[] sidePlankSteps = {
            "Lie on side, prop body up on forearm",
            "Stack feet, keep body in straight line",
            "Lift hips off ground, engage obliques",
            "Hold position, don't let hips sag",
            "Maintain for desired time",
            "Lower with control, switch sides"
        };
        ImageGenerator.generateAllStepsForExercise("obliques", "side-plank", sidePlankSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " obliques exercise images");
        return count;
    }
    
    private static int generateLowerBackExercises() {
        System.out.println("[*] Generating LOWER BACK exercises...");
        int count = 0;
        
        // Back Extensions
        String[] backExtSteps = {
            "Position body on back extension bench",
            "Cross arms over chest or behind head",
            "Lower torso toward ground, hinge at hips",
            "Raise torso back up to starting position",
            "Squeeze glutes and lower back at top",
            "Control movement throughout"
        };
        ImageGenerator.generateAllStepsForExercise("lower-back", "back-extensions", backExtSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " lower back exercise images");
        return count;
    }
    
    private static int generateGlutesExercises() {
        System.out.println("[*] Generating GLUTES exercises...");
        int count = 0;
        
        // Hip Thrusts
        String[] hipThrustSteps = {
            "Sit on ground, upper back against bench",
            "Place barbell over hips, feet flat on floor",
            "Drive through heels, thrust hips up",
            "Squeeze glutes at top, body forms straight line",
            "Hold briefly at top",
            "Lower hips with control, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("glutes", "hip-thrusts", hipThrustSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " glutes exercise images");
        return count;
    }
    
    private static int generateQuadricepsExercises() {
        System.out.println("[*] Generating QUADRICEPS exercises...");
        int count = 0;
        
        // Squats
        String[] squatSteps = {
            "Stand with feet shoulder-width apart",
            "Keep chest up, core engaged, spine neutral",
            "Lower down as if sitting in chair",
            "Go until thighs are parallel to floor",
            "Drive through heels to return to start",
            "Stand tall, knees slightly bent, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("quadriceps", "squats", squatSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " quadriceps exercise images");
        return count;
    }
    
    private static int generateHamstringsExercises() {
        System.out.println("[*] Generating HAMSTRINGS exercises...");
        int count = 0;
        
        // Romanian Deadlift
        String[] rdlSteps = {
            "Stand with feet hip-width, hold barbell",
            "Hinge at hips, push butt back",
            "Lower barbell down legs, keep back straight",
            "Feel stretch in hamstrings",
            "Drive hips forward to return to start",
            "Squeeze glutes at top, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("hamstrings", "romanian-deadlift", rdlSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " hamstrings exercise images");
        return count;
    }
    
    private static int generateCalvesExercises() {
        System.out.println("[*] Generating CALVES exercises...");
        int count = 0;
        
        // Standing Calf Raise
        String[] calfRaiseSteps = {
            "Stand on platform, balls of feet on edge",
            "Hold weight for resistance if desired",
            "Lower heels below platform level",
            "Rise up on toes as high as possible",
            "Squeeze calves at top position",
            "Lower with control, repeat"
        };
        ImageGenerator.generateAllStepsForExercise("calves", "standing-calf-raise", calfRaiseSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " calves exercise images");
        return count;
    }
    
    private static int generateNeckExercises() {
        System.out.println("[*] Generating NECK exercises...");
        int count = 0;
        
        // Neck Curls
        String[] neckCurlSteps = {
            "Lie on bench, head hanging off edge",
            "Place weight plate on forehead, hold steady",
            "Lower head back with control",
            "Curl head up toward chest",
            "Squeeze neck muscles at top",
            "Lower slowly, repeat with control"
        };
        ImageGenerator.generateAllStepsForExercise("neck", "neck-curls", neckCurlSteps);
        count += 6;
        
        System.out.println("[+] Generated " + count + " neck exercise images");
        return count;
    }
}

