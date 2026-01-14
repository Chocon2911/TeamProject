package com.ossimulator.ui;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public final class UIConfig {

    private UIConfig() {
    } // Utility class

    public static final int WINDOW_WIDTH = 1600; // Increased width
    public static final int WINDOW_HEIGHT = 900; // Increased height
    public static final String WINDOW_TITLE = "OS Simulator - Kernel Visualization";

    public static final Color BG_DARK = Color.web("#0B0C10");
    public static final Color BG_MEDIUM = Color.web("#1F2833");
    public static final Color BG_LIGHT = Color.web("#2C3531");

    public static final Color ACCENT_PRIMARY = Color.web("#66FCF1");
    public static final Color ACCENT_SECONDARY = Color.web("#45A29E");
    public static final Color ACCENT_WARN = Color.YELLOW;

    public static final Color PULSE_INNER = Color.YELLOW;
    public static final Color PULSE_OUTER = Color.rgb(255, 255, 100, 0.4);

    public static final Color GRID_COLOR = Color.TRANSPARENT;
    public static final Color GRID_LINE = Color.TRANSPARENT;

    public static final Color TEXT_PRIMARY = Color.web("#FFFFFF");
    public static final Color TEXT_SECONDARY = Color.web("#C5C6C7");
    public static final Color TEXT_MUTED = Color.web("#808080");

    public static Color getPriorityColor(int priority) {
        return switch (priority) {
            case 4 -> Color.web("#FF0055");
            case 3 -> Color.web("#FF6600");
            case 2 -> Color.web("#FFCC00");
            case 1 -> Color.web("#00FF99");
            default -> Color.web("#00CCFF");
        };
    }

    public static Color getPriorityGlow(int priority) {
        return getPriorityColor(priority).deriveColor(0, 1, 1, 0.3);
    }

    public static final Color STATE_READY = Color.web("#00D1FF");
    public static final Color STATE_RUNNING = Color.web("#00FF88");
    public static final Color STATE_WAITING = Color.web("#FF5555");
    public static final Color STATE_COMPLETE = Color.web("#FFD700");
    public static final Color STATE_SWAPPED_READY = Color.web("#0088AA");
    public static final Color STATE_SWAPPED_WAITING = Color.web("#AA3333");
    public static final Color STATE_NEW = Color.web("#FFFFFF");

    // Ready Queue Zone (Moved Back Left to fit 10 slots)
    public static final double READY_QUEUE_X = 100;
    public static final double READY_QUEUE_Y = 150;
    public static final double READY_QUEUE_SLOT_WIDTH = 135; // Matches PROCESS_WIDTH + 15
    public static final double READY_QUEUE_SLOT_GAP = 15;

    // CPU Zone (Shifted slightly Left)
    public static final double CPU_CENTER_X = 750;
    public static final double CPU_CENTER_Y = 550;
    public static final double CPU_RADIUS = 100;

    // Spawn Zone
    public static final double SPAWN_X = 50; // Far left
    public static final double SPAWN_Y = 350;

    // Terminated Zone
    public static final double TERMINATED_X = 1350;
    public static final double TERMINATED_Y = 350;

    // Memory Bar (bottom)
    public static final double MEMORY_BAR_X = 150;
    public static final double MEMORY_BAR_Y = 800;
    public static final double MEMORY_BAR_WIDTH = 1300;
    public static final double MEMORY_BAR_HEIGHT = 50;
    public static final double MEMORY_SLOT_GAP = 15;

    public static final double PROCESS_WIDTH = 120;
    public static final double PROCESS_HEIGHT = 100;
    public static final double PROCESS_ARC = 12;
    public static final double PROCESS_BORDER_WIDTH = 2.0;
    public static final double PROCESS_GLOW_RADIUS = 8;

    public static final double ANIM_MOVE_DURATION = 0.5;
    public static final double ANIM_FADE_DURATION = 0.3;
    public static final double ANIM_PULSE_DURATION = 0.8;
    public static final double ANIM_PARTICLE_DURATION = 0.5;

    // Delay between simulation cycles (seconds)
    public static final double CYCLE_DELAY = 1.2;

    public static final double HUD_PADDING = 20;
    public static final double HUD_HEIGHT = 60;
    public static final String FONT_FAMILY = "Helvetica Neue";
    public static final int FONT_SIZE_LARGE = 28;
    public static final int FONT_SIZE_MEDIUM = 18;
    public static final int FONT_SIZE_SMALL = 14;

    public static LinearGradient getBackgroundGradient() {
        return new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, BG_DARK),
                new Stop(1, BG_MEDIUM));
    }
}
