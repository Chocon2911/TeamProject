package com.ossimulator.util;

import com.ossimulator.model.main.Process;
import com.ossimulator.model.component.ProcessState;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Records detailed state history for all processes at each tick.
 * Creates a viewable log file showing process states over time.
 */
public class StateHistoryLogger {
    private final List<TickSnapshot> history;
    private final String logFilePath;
    private int currentTick;
    private PrintWriter writer;

    public StateHistoryLogger(String logFilePath) {
        this.history = new ArrayList<>();
        this.logFilePath = logFilePath;
        this.currentTick = 0;
        initWriter();
    }

    private void initWriter() {
        try {
            writer = new PrintWriter(new FileWriter(logFilePath));
            writer.println("=" .repeat(100));
            writer.println("OS KERNEL SIMULATOR - STATE HISTORY LOG");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("=".repeat(100));
            writer.println();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to create history log: " + e.getMessage());
        }
    }

    /**
     * Record a state transition for a single process
     */
    public void logStateTransition(int tick, String processName, ProcessState fromState, ProcessState toState, String reason) {
        String entry = String.format("[Tick %4d] %-12s: %-15s → %-15s  (%s)",
                tick, processName, fromState, toState, reason);

        if (writer != null) {
            writer.println(entry);
            writer.flush();
        }
    }

    /**
     * Record a snapshot of all process states at current tick
     */
    public void logTickSnapshot(int tick, List<Process> processes, String event) {
        currentTick = tick;
        TickSnapshot snapshot = new TickSnapshot(tick, event);

        if (writer != null) {
            writer.println();
            writer.println("-".repeat(100));
            writer.printf("TICK %d: %s%n", tick, event);
            writer.println("-".repeat(100));
            writer.printf("%-4s | %-12s | %-8s | %-10s | %-15s | %-10s%n",
                    "PID", "Name", "Priority", "Remaining", "State", "In Memory");
            writer.println("-".repeat(100));
        }

        for (Process p : processes) {
            ProcessSnapshot ps = new ProcessSnapshot(
                    p.getPid(), p.getName(), p.getPriority(),
                    p.getRemainingTime(), p.getState(), p.isInMainMemory()
            );
            snapshot.processStates.add(ps);

            if (writer != null) {
                writer.printf("%-4d | %-12s | %-8d | %-10d | %-15s | %-10s%n",
                        ps.pid, ps.name, ps.priority, ps.remainingTime, ps.state, ps.inMemory ? "Yes" : "No");
            }
        }

        if (writer != null) {
            writer.flush();
        }

        history.add(snapshot);
    }

    /**
     * Log execution details
     */
    public void logExecution(int tick, String processName, int executedTime, int remainingTime) {
        if (writer != null) {
            writer.printf("[Tick %4d] %-12s: Executed %dms, remaining %dms%n",
                    tick, processName, executedTime, remainingTime);
            writer.flush();
        }
    }

    /**
     * Log a general message
     */
    public void log(String message) {
        if (writer != null) {
            writer.println(message);
            writer.flush();
        }
    }

    /**
     * Write final summary
     */
    public void writeSummary(List<Process> processes, int totalCycles, long simulationTime, int contextSwitches) {
        if (writer != null) {
            writer.println();
            writer.println("=".repeat(100));
            writer.println("SIMULATION SUMMARY");
            writer.println("=".repeat(100));
            writer.printf("Total Ticks: %d%n", currentTick);
            writer.printf("Total Cycles: %d%n", totalCycles);
            writer.printf("Simulation Time: %dms%n", simulationTime);
            writer.printf("Context Switches: %d%n", contextSwitches);
            writer.println();
            writer.println("PROCESS FINAL STATES:");
            writer.printf("%-12s | %-8s | %-10s | %-15s | %-12s%n",
                    "Name", "Priority", "Burst(ms)", "State", "Turnaround");
            writer.println("-".repeat(70));

            long totalTurnaround = 0;
            for (Process p : processes) {
                long turnaround = p.getTurnaroundTime();
                totalTurnaround += turnaround;
                writer.printf("%-12s | %-8d | %-10d | %-15s | %-12dms%n",
                        p.getName(), p.getPriority(), p.getBurstTime(), p.getState(), turnaround);
            }

            if (!processes.isEmpty()) {
                writer.println("-".repeat(70));
                writer.printf("Average Turnaround Time: %.2fms%n", (double) totalTurnaround / processes.size());
            }
            writer.println("=".repeat(100));
            writer.flush();
        }
    }

    /**
     * Close the logger
     */
    public void close() {
        if (writer != null) {
            writer.close();
        }
    }

    /**
     * Get all history snapshots
     */
    public List<TickSnapshot> getHistory() {
        return history;
    }

    // Inner classes for snapshots
    public static class TickSnapshot {
        public final int tick;
        public final String event;
        public final List<ProcessSnapshot> processStates;

        public TickSnapshot(int tick, String event) {
            this.tick = tick;
            this.event = event;
            this.processStates = new ArrayList<>();
        }
    }

    public static class ProcessSnapshot {
        public final int pid;
        public final String name;
        public final int priority;
        public final int remainingTime;
        public final ProcessState state;
        public final boolean inMemory;

        public ProcessSnapshot(int pid, String name, int priority, int remainingTime, ProcessState state, boolean inMemory) {
            this.pid = pid;
            this.name = name;
            this.priority = priority;
            this.remainingTime = remainingTime;
            this.state = state;
            this.inMemory = inMemory;
        }
    }
}
