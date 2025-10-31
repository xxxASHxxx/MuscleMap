package com.musclemmap.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility class to generate exercise step images programmatically
 */
public class ImageGenerator {
    
    private static final int IMG_WIDTH = 400;
    private static final int IMG_HEIGHT = 300;
    
    /**
     * Generate a step image for an exercise
     */
    public static void generateStepImage(String muscleGroup, String exerciseName, int stepNumber, String stepDescription) {
        try {
            // Create buffered image
            BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // Enable anti-aliasing for better quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Background gradient
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(26, 26, 26),
                IMG_WIDTH, IMG_HEIGHT, new Color(45, 45, 45)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
            
            // Border
            g2d.setColor(new Color(0, 212, 255));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(5, 5, IMG_WIDTH - 10, IMG_HEIGHT - 10, 20, 20);
            
            // Step number circle
            g2d.setColor(new Color(0, 212, 255));
            g2d.fillOval(IMG_WIDTH / 2 - 40, 30, 80, 80);
            
            // Step number text
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String stepNum = String.valueOf(stepNumber);
            int x = (IMG_WIDTH - fm.stringWidth(stepNum)) / 2;
            g2d.drawString(stepNum, x, 85);
            
            // Exercise name
            g2d.setColor(new Color(0, 212, 255));
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            String exerciseTitle = exerciseName.replace("-", " ").toUpperCase();
            drawCenteredString(g2d, exerciseTitle, IMG_WIDTH, 140, g2d.getFont());
            
            // Step description
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            drawWrappedString(g2d, stepDescription, 30, 180, IMG_WIDTH - 60);
            
            // Muscle group label
            g2d.setColor(new Color(0, 212, 255));
            g2d.setFont(new Font("Arial", Font.ITALIC, 14));
            String muscleLabel = "Target: " + muscleGroup.toUpperCase();
            fm = g2d.getFontMetrics();
            g2d.drawString(muscleLabel, (IMG_WIDTH - fm.stringWidth(muscleLabel)) / 2, IMG_HEIGHT - 20);
            
            g2d.dispose();
            
            // Save image
            File outputDir = new File("src/main/resources/images/exercises/" + muscleGroup + "/" + exerciseName);
            outputDir.mkdirs();
            File outputFile = new File(outputDir, "step-" + stepNumber + ".jpg");
            ImageIO.write(image, "jpg", outputFile);
            
            System.out.println("[+] Generated: " + outputFile.getPath());
            
        } catch (IOException e) {
            System.err.println("[-] Failed to generate image: " + e.getMessage());
        }
    }
    
    /**
     * Draw centered string
     */
    private static void drawCenteredString(Graphics2D g2d, String text, int width, int y, Font font) {
        FontMetrics fm = g2d.getFontMetrics(font);
        int x = (width - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }
    
    /**
     * Draw wrapped text
     */
    private static void drawWrappedString(Graphics2D g2d, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineHeight = fm.getHeight();
        
        for (String word : words) {
            String testLine = line + word + " ";
            if (fm.stringWidth(testLine) > maxWidth && line.length() > 0) {
                g2d.drawString(line.toString(), x, y);
                y += lineHeight;
                line = new StringBuilder(word + " ");
            } else {
                line.append(word).append(" ");
            }
        }
        g2d.drawString(line.toString(), x, y);
    }
    
    /**
     * Generate all step images for an exercise
     */
    public static void generateAllStepsForExercise(String muscleGroup, String exerciseName, String[] stepDescriptions) {
        for (int i = 0; i < stepDescriptions.length && i < 6; i++) {
            generateStepImage(muscleGroup, exerciseName, i + 1, stepDescriptions[i]);
        }
    }
}

