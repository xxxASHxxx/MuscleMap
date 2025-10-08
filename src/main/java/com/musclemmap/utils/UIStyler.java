package com.musclemmap.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Intelligent UI Styler for Muscle Map Application
 * ONLY TEXT COLORS CHANGED - Everything else identical
 * Maximum visibility text colors for all scenarios
 */
public class UIStyler {

    // ===== INTELLIGENT COLOR SYSTEM =====

    // Accent Colors (Always vibrant and beautiful)
    public static final String ACCENT_CYAN = "#00d4ff";         // Electric cyan
    public static final String ACCENT_CORAL = "#ff6b6b";        // Warm coral
    public static final String ACCENT_EMERALD = "#10b981";      // Rich emerald
    public static final String ACCENT_AMBER = "#f59e0b";        // Golden amber
    public static final String ACCENT_VIOLET = "#8b5cf6";       // Deep violet
    public static final String ACCENT_GOLD = "#fbbf24";         // Warm gold

    // Background System (Dark Theme - Primary)
    public static final String BG_DEEPEST = "#0c0c0c";         // Deepest space
    public static final String BG_DEEPER = "#151515";          // Deep charcoal
    public static final String BG_DEEP = "#1f1f1f";            // Rich charcoal
    public static final String BG_MEDIUM = "#2a2a2a";          // Medium gray
    public static final String BG_ELEVATED = "#353535";        // Elevated surface
    public static final String BG_LIGHTEST = "#404040";        // Lightest dark

    // Light Background System (Alternative)
    public static final String BG_LIGHT_BASE = "#ffffff";      // Pure white
    public static final String BG_LIGHT_SOFT = "#f8fafc";      // Soft white
    public static final String BG_LIGHT_COOL = "#f1f5f9";      // Cool light
    public static final String BG_LIGHT_WARM = "#fefcf8";      // Warm light
    public static final String BG_LIGHT_ELEVATED = "#e2e8f0";  // Elevated light

    // MAXIMUM VISIBILITY TEXT COLORS - ONLY CHANGE
    public static final String TEXT_ON_DARK = "#FFFFFF";       // PURE WHITE - Maximum contrast
    public static final String TEXT_ON_DARK_SOFT = "#FFFFFF";  // PURE WHITE - Maximum visibility
    public static final String TEXT_ON_DARK_MUTED = "#E0E0E0"; // VERY LIGHT GRAY - Still visible

    public static final String TEXT_ON_LIGHT = "#000000";      // PURE BLACK - Maximum contrast
    public static final String TEXT_ON_LIGHT_SOFT = "#000000"; // PURE BLACK - Maximum visibility
    public static final String TEXT_ON_LIGHT_MUTED = "#333333"; // DARK GRAY - Still visible

    // ===== INTELLIGENT CONTRAST SYSTEM =====

    /**
     * Get optimal text color based on background - UPDATED WITH MAX VISIBILITY
     */
    private static String getOptimalTextColor(String backgroundColor, String intensity) {
        // Determine if background is light or dark
        boolean isDarkBackground = isBackgroundDark(backgroundColor);

        return switch (intensity.toLowerCase()) {
            case "primary" -> isDarkBackground ? TEXT_ON_DARK : TEXT_ON_LIGHT;
            case "soft" -> isDarkBackground ? TEXT_ON_DARK_SOFT : TEXT_ON_LIGHT_SOFT;
            case "muted" -> isDarkBackground ? TEXT_ON_DARK_MUTED : TEXT_ON_LIGHT_MUTED;
            default -> isDarkBackground ? TEXT_ON_DARK : TEXT_ON_LIGHT;
        };
    }

    /**
     * Determine if a background color is dark
     */
    private static boolean isBackgroundDark(String backgroundColor) {
        // Simple heuristic - if it contains typical dark values, it's dark
        return backgroundColor.toLowerCase().contains("#0") ||
                backgroundColor.toLowerCase().contains("#1") ||
                backgroundColor.toLowerCase().contains("#2") ||
                backgroundColor.equals(BG_DEEPEST) ||
                backgroundColor.equals(BG_DEEPER) ||
                backgroundColor.equals(BG_DEEP) ||
                backgroundColor.equals(BG_MEDIUM);
    }

    /**
     * Get optimal shadow for text based on background - ENHANCED FOR VISIBILITY
     */
    private static String getOptimalTextShadow(String backgroundColor) {
        boolean isDark = isBackgroundDark(backgroundColor);
        return isDark ?
                "dropshadow(gaussian, rgba(0,0,0,1), 3, 0, 0, 1)" :  // STRONGER BLACK SHADOW
                "dropshadow(gaussian, rgba(255,255,255,1), 3, 0, 0, 1)"; // STRONGER WHITE SHADOW
    }

    // ===== BEAUTIFUL BUTTON STYLES =====

    /**
     * Primary button with intelligent contrast
     */
    public static void styleButton(Button button) {
        button.setStyle(
                "-fx-background-color: linear-gradient(135deg, " + ACCENT_CYAN + " 0%, #0ea5e9 100%); " +
                        "-fx-text-fill: " + TEXT_ON_DARK + "; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12px 24px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,212,255,0.3), 8, 0, 0, 4), " +
                        "dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"
        );

        addSmartButtonHover(button, ACCENT_CYAN);
    }

    /**
     * Secondary button with adaptive styling
     */
    public static void styleSecondaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: " + BG_ELEVATED + "; " +
                        "-fx-text-fill: " + getOptimalTextColor(BG_ELEVATED, "primary") + "; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12px 24px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: rgba(255,255,255,0.1); " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2), " +
                        "innershadow(gaussian, rgba(255,255,255,0.1), 1, 0, 0, 1);"
        );

        addSmartButtonHover(button, "rgba(255,255,255,0.1)");
    }

    /**
     * Success button with emerald theme
     */
    public static void styleSuccessButton(Button button) {
        button.setStyle(
                "-fx-background-color: linear-gradient(135deg, " + ACCENT_EMERALD + " 0%, #059669 100%); " +
                        "-fx-text-fill: " + TEXT_ON_DARK + "; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12px 24px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(16,185,129,0.3), 8, 0, 0, 4), " +
                        "dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"
        );

        addSmartButtonHover(button, ACCENT_EMERALD);
    }

    /**
     * Warning button with coral theme
     */
    public static void styleWarningButton(Button button) {
        button.setStyle(
                "-fx-background-color: linear-gradient(135deg, " + ACCENT_CORAL + " 0%, #ef4444 100%); " +
                        "-fx-text-fill: " + TEXT_ON_DARK + "; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12px 24px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(255,107,107,0.3), 8, 0, 0, 4), " +
                        "dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"
        );

        addSmartButtonHover(button, ACCENT_CORAL);
    }

    // ===== INTELLIGENT INPUT FIELDS =====

    /**
     * Smart text field with adaptive contrast
     */
    public static void styleTextField(TextField textField) {
        String bgColor = BG_ELEVATED;
        String textColor = getOptimalTextColor(bgColor, "primary");
        String mutedColor = getOptimalTextColor(bgColor, "muted");

        textField.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-prompt-text-fill: " + mutedColor + "; " +
                        "-fx-border-color: rgba(255,255,255,0.1); " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-padding: 12px 16px; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: 500; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1), " +
                        "innershadow(gaussian, rgba(0,0,0,0.05), 1, 0, 0, 1);"
        );

        addSmartFieldFocus(textField, bgColor);
    }

    /**
     * Intelligent text area with beautiful styling
     */
    public static void styleTextArea(TextArea textArea) {
        String bgColor = BG_ELEVATED;
        String textColor = getOptimalTextColor(bgColor, "primary");

        textArea.setStyle(
                "-fx-control-inner-background: " + bgColor + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-border-color: rgba(255,255,255,0.1); " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: 500; " +
                        "-fx-font-family: 'SF Pro Display', 'Segoe UI Variable', 'Inter', system-ui; " +
                        "-fx-padding: 16px; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2), " +
                        "innershadow(gaussian, rgba(0,0,0,0.05), 2, 0, 0, 1);"
        );

        addSmartFieldFocus(textArea, bgColor);
    }

    // ===== SMART SELECTION CONTROLS =====

    /**
     * Beautiful combo box with intelligent styling
     */
    /**
     * Beautiful combo box with maximum visibility styling
     */
    /**
     * Beautiful combo box with maximum visibility styling
     */
    public static void styleComboBox(ComboBox<?> comboBox) {
        // BUTTON AREA STYLING - DARK BACKGROUND WITH WHITE TEXT
        comboBox.setStyle(
                "-fx-background-color: #1a1a1a; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12px 16px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: #00f5ff; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.3), 8, 0, 0, 4);"
        );

        // DROPDOWN POPUP LIST STYLING - MAXIMUM VISIBILITY
        comboBox.setCellFactory(param -> new ListCell() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());

                    // DARK BACKGROUND WITH WHITE TEXT FOR EACH ITEM
                    setStyle(
                            "-fx-background-color: #1a1a1a; " +
                                    "-fx-text-fill: #FFFFFF; " +
                                    "-fx-font-size: 15px; " +
                                    "-fx-font-weight: 600; " +
                                    "-fx-padding: 12px 16px; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);"
                    );

                    // HOVER STATE - CYAN BACKGROUND
                    setOnMouseEntered(e -> {
                        if (!isEmpty()) {
                            setStyle(
                                    "-fx-background-color: #00f5ff; " +
                                            "-fx-text-fill: #000000; " +
                                            "-fx-font-size: 15px; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-padding: 12px 16px; " +
                                            "-fx-effect: dropshadow(gaussian, rgba(0,245,255,0.6), 8, 0, 0, 4);"
                            );
                        }
                    });

                    setOnMouseExited(e -> {
                        if (!isEmpty()) {
                            setStyle(
                                    "-fx-background-color: #1a1a1a; " +
                                            "-fx-text-fill: #FFFFFF; " +
                                            "-fx-font-size: 15px; " +
                                            "-fx-font-weight: 600; " +
                                            "-fx-padding: 12px 16px; " +
                                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);"
                            );
                        }
                    });
                }
            }
        });

        // BUTTON CELL (SELECTED VALUE DISPLAY) - WHITE TEXT
        comboBox.setButtonCell(new ListCell() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setStyle(
                            "-fx-text-fill: #FFFFFF; " +
                                    "-fx-font-size: 16px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 1);"
                    );
                }
            }
        });
    }





    /**
     * Elegant list view with smooth styling
     */
    public static void styleListView(ListView<?> listView) {
        listView.setStyle(
                "-fx-background-color: #1f1f1f; " +
                        "-fx-control-inner-background: #2b2b2b; " +
                        "-fx-border-color: #00d4ff; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-width: 2px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,212,255,0.3), 10, 0, 0, 5);"
        );
    }

    // ===== MAXIMUM VISIBILITY TYPOGRAPHY =====

    /**
     * Smart label styling with perfect contrast - ENHANCED TEXT VISIBILITY
     */
    public static void styleLabel(Label label, String style) {
        styleLabel(label, style, BG_DEEPEST); // Default dark background
    }

    /**
     * Smart label styling with custom background awareness - MAXIMUM VISIBILITY
     */
    public static void styleLabel(Label label, String style, String backgroundColor) {
        String textColor = getOptimalTextColor(backgroundColor, "primary");
        String softColor = getOptimalTextColor(backgroundColor, "soft");
        String mutedColor = getOptimalTextColor(backgroundColor, "muted");
        String shadow = getOptimalTextShadow(backgroundColor);

        switch (style.toLowerCase()) {
            case "title" -> label.setStyle(
                    "-fx-text-fill: " + textColor + "; " +
                            "-fx-font-size: 32px; " +
                            "-fx-font-weight: 700; " +
                            "-fx-letter-spacing: -0.5px; " +
                            "-fx-effect: " + shadow + ";"
            );

            case "subtitle" -> label.setStyle(
                    "-fx-text-fill: " + textColor + "; " +
                            "-fx-font-size: 24px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-letter-spacing: -0.3px; " +
                            "-fx-effect: " + shadow + ";"
            );

            case "header" -> label.setStyle(
                    "-fx-text-fill: " + ACCENT_CYAN + "; " +
                            "-fx-font-size: 20px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-effect: " +
                            "dropshadow(gaussian, rgba(0,212,255,0.3), 4, 0, 0, 0), " +
                            shadow + ";"
            );

            case "body" -> label.setStyle(
                    "-fx-text-fill: " + softColor + "; " +
                            "-fx-font-size: 16px; " +
                            "-fx-font-weight: 500; " +
                            "-fx-line-spacing: 2px; " +
                            "-fx-effect: " + shadow + ";"
            );

            case "caption" -> label.setStyle(
                    "-fx-text-fill: " + mutedColor + "; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 500; " +
                            "-fx-effect: " + shadow + ";"
            );

            case "accent" -> label.setStyle(
                    "-fx-text-fill: " + ACCENT_GOLD + "; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-effect: " +
                            "dropshadow(gaussian, rgba(251,191,36,0.3), 4, 0, 0, 0), " +
                            shadow + ";"
            );
        }
    }

    // ===== BEAUTIFUL CONTAINERS (UNCHANGED) =====

    /**
     * Elegant card with intelligent styling
     */
    public static void styleCard(Region container) {
        container.setStyle(
                "-fx-background-color: " + BG_MEDIUM + "; " +
                        "-fx-background-radius: 16px; " +
                        "-fx-border-color: rgba(255,255,255,0.08); " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 16px; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 6), " +
                        "innershadow(gaussian, rgba(255,255,255,0.03), 2, 0, 0, 1);"
        );
    }

    /**
     * Stunning neon container with intelligent contrast
     */
    public static void styleNeonContainer(Region container, String accentColor) {
        container.setStyle(
                "-fx-background-color: " + BG_MEDIUM + "; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-border-color: " + accentColor + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 20px; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, " + accentColor + ", 16, 0.4, 0, 8), " +
                        "dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 4), " +
                        "innershadow(gaussian, rgba(255,255,255,0.05), 3, 0, 0, 1);"
        );
    }

    // ===== SMART ANIMATION SYSTEM (UNCHANGED) =====

    /**
     * Intelligent button hover with beautiful transitions
     */
    private static void addSmartButtonHover(Button button, String accentColor) {
        button.setOnMouseEntered(e -> {
            Timeline hover = new Timeline(
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(button.scaleXProperty(), 1.05),
                            new KeyValue(button.scaleYProperty(), 1.05),
                            new KeyValue(button.opacityProperty(), 0.92)
                    )
            );
            hover.play();
        });

        button.setOnMouseExited(e -> {
            Timeline exit = new Timeline(
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(button.scaleXProperty(), 1.0),
                            new KeyValue(button.scaleYProperty(), 1.0),
                            new KeyValue(button.opacityProperty(), 1.0)
                    )
            );
            exit.play();
        });
    }

    /**
     * Smart field focus with beautiful glow
     */
    private static void addSmartFieldFocus(Control field, String backgroundColor) {
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                String focusStyle = field.getStyle().replace(
                        "border-color: rgba(255,255,255,0.1)",
                        "border-color: " + ACCENT_CYAN
                ) + "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,212,255,0.3), 8, 0, 0, 4), ";
                field.setStyle(focusStyle);
            } else {
                // Reset based on field type
                if (field instanceof TextField) {
                    styleTextField((TextField) field);
                } else if (field instanceof TextArea) {
                    styleTextArea((TextArea) field);
                }
            }
        });
    }

    /**
     * Beautiful glow effect with intelligent contrast
     */
    public static void addGlowEffect(Node node, String color) {
        node.setStyle(node.getStyle() +
                "-fx-effect: dropshadow(gaussian, " + color + ", 12, 0.6, 0, 0);"
        );
    }

    /**
     * Elegant pulse animation
     */
    public static void addPulseAnimation(Node node) {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(node.scaleXProperty(), 1.0),
                        new KeyValue(node.scaleYProperty(), 1.0),
                        new KeyValue(node.opacityProperty(), 1.0)
                ),
                new KeyFrame(Duration.millis(800),
                        new KeyValue(node.scaleXProperty(), 1.08),
                        new KeyValue(node.scaleYProperty(), 1.08),
                        new KeyValue(node.opacityProperty(), 0.8)
                ),
                new KeyFrame(Duration.millis(1600),
                        new KeyValue(node.scaleXProperty(), 1.0),
                        new KeyValue(node.scaleYProperty(), 1.0),
                        new KeyValue(node.opacityProperty(), 1.0)
                )
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ===== THEME SYSTEMS (UNCHANGED) =====

    /**
     * Apply intelligent dark theme
     */
    public static void applyDarkTheme(Region pane) {
        pane.setStyle(
                "-fx-background-color: " + BG_DEEPEST + ";"
        );
    }

    /**
     * Apply elegant light theme
     */
    public static void applyLightTheme(Region pane) {
        pane.setStyle(
                "-fx-background-color: " + BG_LIGHT_BASE + ";"
        );
    }

    /**
     * Beautiful glass morphism with perfect contrast
     */
    public static void applyGlassMorphism(Region container) {
        container.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.06); " +
                        "-fx-background-radius: 20px; " +
                        "-fx-border-color: rgba(255, 255, 255, 0.12); " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 20px; " +
                        "-fx-effect: " +
                        "dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 10), " +
                        "innershadow(gaussian, rgba(255,255,255,0.08), 4, 0, 0, 2);"
        );
    }
}
