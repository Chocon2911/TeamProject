package com.ossimulator.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple Logger - ghi log ra console và file
 */
public class Logger {
    //==========================================Variable==========================================
    private static Logger instance;
    private PrintWriter fileWriter;
    private boolean fileLoggingEnabled;
    private final DateTimeFormatter timeFormatter;

    //========================================Constructor=========================================
    private Logger() {
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        this.fileLoggingEnabled = false;
    }

    //==========================================Get Set===========================================
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    //============================================File============================================
    /**
     * Bật logging ra file
     * @param filePath đường dẫn file log (e.g., "logs/simulation.log")
     */
    public void enableFileLogging(String filePath) {
        try {
            // Tạo thư mục nếu chưa có
            java.io.File file = new java.io.File(filePath);
            java.io.File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            this.fileWriter = new PrintWriter(new FileWriter(filePath, false)); // overwrite
            this.fileLoggingEnabled = true;
            log("LOGGER", "File logging enabled: " + filePath);
        } catch (IOException e) {
            System.err.println("[Logger] Failed to create log file: " + e.getMessage());
            this.fileLoggingEnabled = false;
        }
    }

    /**
     * Tắt logging ra file
     */
    public void disableFileLogging() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
        this.fileLoggingEnabled = false;
    }

    //============================================Log=============================================
    /**
     * Log message với component name
     */
    public void log(String component, String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String formatted = String.format("[%s] [%-12s] %s", timestamp, component, message);

        // Console
        System.out.println(formatted);

        // File
        if (fileLoggingEnabled && fileWriter != null) {
            fileWriter.println(formatted);
            fileWriter.flush();
        }
    }

    /**
     * Log với format
     */
    public void log(String component, String format, Object... args) {
        log(component, String.format(format, args));
    }

    /**
     * Log cho Kernel
     */
    public void kernel(String message) {
        log("Kernel", message);
    }

    public void kernel(String format, Object... args) {
        log("Kernel", format, args);
    }

    /**
     * Log cho Scheduler
     */
    public void scheduler(String message) {
        log("Scheduler", message);
    }

    public void scheduler(String format, Object... args) {
        log("Scheduler", format, args);
    }

    /**
     * Log cho Dispatcher
     */
    public void dispatcher(String message) {
        log("Dispatcher", message);
    }

    public void dispatcher(String format, Object... args) {
        log("Dispatcher", format, args);
    }

    /**
     * Log cho Memory
     */
    public void memory(String message) {
        log("Memory", message);
    }

    public void memory(String format, Object... args) {
        log("Memory", format, args);
    }

    /**
     * Log cho Process
     */
    public void process(String message) {
        log("Process", message);
    }

    public void process(String format, Object... args) {
        log("Process", format, args);
    }

    /**
     * Log debug info
     */
    public void debug(String component, String message) {
        log(component + ":DEBUG", message);
    }

    /**
     * Log error
     */
    public void error(String component, String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String formatted = String.format("[%s] [%-12s] ERROR: %s", timestamp, component, message);

        System.err.println(formatted);

        if (fileLoggingEnabled && fileWriter != null) {
            fileWriter.println(formatted);
            fileWriter.flush();
        }
    }

    /**
     * Log separator line
     */
    public void separator() {
        String line = "─".repeat(60);
        System.out.println(line);
        if (fileLoggingEnabled && fileWriter != null) {
            fileWriter.println(line);
            fileWriter.flush();
        }
    }

    /**
     * Đóng logger
     */
    public void close() {
        if (fileWriter != null) {
            log("LOGGER", "Closing log file");
            fileWriter.close();
            fileWriter = null;
        }
    }
}
