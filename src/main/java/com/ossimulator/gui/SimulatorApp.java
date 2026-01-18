package com.ossimulator.gui;

import com.ossimulator.manager.kernel.Kernel;
import com.ossimulator.model.main.Process;
import com.ossimulator.model.component.ProcessState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * OS Kernel Simulator - GUI Application
 * Allows interactive configuration, execution, and monitoring of the simulation
 */
public class SimulatorApp extends JFrame {
    //==========================================Variable==========================================
    private Kernel kernel;
    private List<ProcessConfig> processConfigs;
    private volatile boolean simulationRunning;
    private Thread simulationThread;
    private Thread monitorThread;

    // Configuration components
    private JSpinner timeQuantumSpinner;
    private JSpinner memorySlotSpinner;
    private JSpinner transitionDelaySpinner;
    private JComboBox<String> schedulerComboBox;

    // Process table
    private DefaultTableModel processTableModel;
    private JTable processTable;

    // Log area
    private JTextArea logArea;
    private List<String> logHistory;

    // Statistics
    private JLabel statusLabel;
    private JLabel tickLabel;
    private JLabel cycleLabel;
    private JLabel memoryLabel;
    private JLabel swapLabel;
    private JLabel contextSwitchLabel;
    private JLabel timeLabel;

    // Buttons
    private JButton startButton;
    private JButton stopButton;
    private JButton resetButton;
    private JButton addProcessButton;
    private JButton clearLogButton;
    private JButton exportLogButton;

    // Colors for all 9 process states
    private static final Color CREATED_COLOR = new Color(255, 255, 255);       // White
    private static final Color READY_MEMORY_COLOR = new Color(173, 216, 230);  // Light blue
    private static final Color READY_SWAPPED_COLOR = new Color(135, 206, 250); // Sky blue
    private static final Color SLEEP_COLOR = new Color(255, 218, 185);         // Peach
    private static final Color SLEEP_SWAPPED_COLOR = new Color(255, 182, 193); // Light pink
    private static final Color KERNEL_RUNNING_COLOR = new Color(255, 215, 0);  // Gold
    private static final Color USER_RUNNING_COLOR = new Color(144, 238, 144);  // Light green
    private static final Color PREEMPTED_COLOR = new Color(255, 165, 0);       // Orange
    private static final Color ZOMBIE_COLOR = new Color(211, 211, 211);        // Light gray

    //========================================Constructor=========================================
    public SimulatorApp() {
        super("OS Kernel Simulator - CS4448");
        this.processConfigs = new ArrayList<>();
        this.logHistory = new ArrayList<>();
        this.simulationRunning = false;

        initializeComponents();
        layoutComponents();
        setupEventListeners();
        addDefaultProcesses();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //==========================================Initialize==========================================
    private void initializeComponents() {
        // Configuration spinners
        timeQuantumSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        memorySlotSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        transitionDelaySpinner = new JSpinner(new SpinnerNumberModel(200, 50, 2000, 50));
        schedulerComboBox = new JComboBox<>(new String[]{"Round Robin", "Priority"});

        // Process table
        String[] columns = {"PID", "Name", "Burst(ms)", "Remaining", "Priority", "State", "Turnaround"};
        processTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setRowHeight(25);
        processTable.getColumnModel().getColumn(5).setCellRenderer(new StateCellRenderer());

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 0));

        // Status labels
        statusLabel = new JLabel("Status: Idle");
        tickLabel = new JLabel("Ticks: 0");
        cycleLabel = new JLabel("Cycles: 0");
        memoryLabel = new JLabel("Memory: 0/0");
        swapLabel = new JLabel("Swap: 0");
        contextSwitchLabel = new JLabel("Context Switches: 0");
        timeLabel = new JLabel("Time: 0ms");

        // Buttons
        startButton = new JButton("Start Simulation");
        stopButton = new JButton("Stop");
        resetButton = new JButton("Reset");
        addProcessButton = new JButton("Add Process");
        clearLogButton = new JButton("Clear Log");
        exportLogButton = new JButton("Export Log");

        stopButton.setEnabled(false);

        // Style buttons
        startButton.setBackground(new Color(76, 175, 80));
        startButton.setForeground(Color.WHITE);
        stopButton.setBackground(new Color(244, 67, 54));
        stopButton.setForeground(Color.WHITE);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // Top panel - Configuration
        JPanel topPanel = createConfigPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center panel - Split between process table and log
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setTopComponent(createProcessPanel());
        centerSplit.setBottomComponent(createLogPanel());
        centerSplit.setDividerLocation(300);
        add(centerSplit, BorderLayout.CENTER);

        // Right panel - Statistics
        JPanel rightPanel = createStatsPanel();
        add(rightPanel, BorderLayout.EAST);

        // Add padding
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        JPanel configGrid = new JPanel(new GridLayout(2, 4, 10, 5));

        configGrid.add(new JLabel("Time Quantum (ms):"));
        configGrid.add(timeQuantumSpinner);
        configGrid.add(new JLabel("Memory Slots:"));
        configGrid.add(memorySlotSpinner);
        configGrid.add(new JLabel("Scheduler:"));
        configGrid.add(schedulerComboBox);
        configGrid.add(new JLabel("Transition Delay (ms):"));
        configGrid.add(transitionDelaySpinner);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(addProcessButton);

        panel.add(configGrid, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProcessPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Processes"));

        JScrollPane scrollPane = new JScrollPane(processTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Legend - All 9 process states
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        legendPanel.add(createLegendItem("CREATED", CREATED_COLOR));
        legendPanel.add(createLegendItem("READY_MEMORY", READY_MEMORY_COLOR));
        legendPanel.add(createLegendItem("READY_SWAPPED", READY_SWAPPED_COLOR));
        legendPanel.add(createLegendItem("KERNEL_RUNNING", KERNEL_RUNNING_COLOR));
        legendPanel.add(createLegendItem("USER_RUNNING", USER_RUNNING_COLOR));
        legendPanel.add(createLegendItem("PREEMPTED", PREEMPTED_COLOR));
        legendPanel.add(createLegendItem("SLEEP", SLEEP_COLOR));
        legendPanel.add(createLegendItem("SLEEP_SWAPPED", SLEEP_SWAPPED_COLOR));
        legendPanel.add(createLegendItem("ZOMBIE", ZOMBIE_COLOR));
        panel.add(legendPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createLegendItem(String text, Color color) {
        JLabel label = new JLabel(" " + text + " ");
        label.setOpaque(true);
        label.setBackground(color);
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return label;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Execution Log"));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(800, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel logButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logButtons.add(clearLogButton);
        logButtons.add(exportLogButton);
        panel.add(logButtons, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        panel.setPreferredSize(new Dimension(200, 0));

        JPanel statsGrid = new JPanel(new GridLayout(7, 1, 5, 10));
        statsGrid.add(statusLabel);
        statsGrid.add(timeLabel);
        statsGrid.add(tickLabel);
        statsGrid.add(cycleLabel);
        statsGrid.add(memoryLabel);
        statsGrid.add(swapLabel);
        statsGrid.add(contextSwitchLabel);

        panel.add(statsGrid);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    //==========================================Events==========================================
    private void setupEventListeners() {
        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopSimulation());
        resetButton.addActionListener(e -> resetSimulation());
        addProcessButton.addActionListener(e -> showAddProcessDialog());
        clearLogButton.addActionListener(e -> clearLog());
        exportLogButton.addActionListener(e -> exportLog());
    }

    private void startSimulation() {
        if (processConfigs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one process!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int timeQuantum = (Integer) timeQuantumSpinner.getValue();
        int memorySlots = (Integer) memorySlotSpinner.getValue();
        int transitionDelay = (Integer) transitionDelaySpinner.getValue();
        String schedulerType = (String) schedulerComboBox.getSelectedItem();

        // Create kernel
        kernel = new Kernel(timeQuantum, memorySlots);

        // Enable history logging to file
        String historyFile = "logs/state_history_" + System.currentTimeMillis() + ".log";
        new java.io.File("logs").mkdirs();
        kernel.enableHistoryLogging(historyFile);

        // Set callback for state changes - update GUI on each transition
        kernel.setStateChangeCallback(message -> {
            SwingUtilities.invokeLater(() -> {
                log(message);
                updateProcessTable();
            });
        });

        // Set scheduler type
        if ("Priority".equals(schedulerType)) {
            kernel.setScheduler(Kernel.SchedulerType.PRIORITY);
        } else {
            kernel.setScheduler(Kernel.SchedulerType.ROUND_ROBIN);
        }

        // Create processes
        for (ProcessConfig config : processConfigs) {
            kernel.createProcess(config.name, config.burstTime, config.priority);
        }

        simulationRunning = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        addProcessButton.setEnabled(false);
        statusLabel.setText("Status: Running");
        statusLabel.setForeground(new Color(76, 175, 80));

        log("=".repeat(60));
        log("SIMULATION STARTED");
        log("Scheduler: " + schedulerType + " (quantum=" + timeQuantum + "ms)");
        log("Memory: " + memorySlots + " slots");
        log("Transition Delay: " + transitionDelay + "ms");
        log("Processes: " + processConfigs.size());
        log("History Log: " + historyFile);
        log("=".repeat(60));

        // Start simulation thread
        simulationThread = new Thread(this::runSimulation);
        simulationThread.start();

        // Start monitor thread
        monitorThread = new Thread(this::runMonitor);
        monitorThread.start();
    }

    private void runSimulation() {
        int transitionDelay = (Integer) transitionDelaySpinner.getValue();

        while (simulationRunning && !kernel.getScheduler().isEmpty()) {
            // Run cycle with delay between state transitions
            kernel.runCycleWithDelay(transitionDelay);

            // Small delay between cycles
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

        SwingUtilities.invokeLater(() -> {
            simulationRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            addProcessButton.setEnabled(true);
            statusLabel.setText("Status: Completed");
            statusLabel.setForeground(Color.BLUE);

            log("=".repeat(60));
            log("SIMULATION COMPLETED");
            log("Total Ticks: " + kernel.getTickCount());
            logStatistics();
            log("=".repeat(60));
            log("History log saved. Open the log file to view detailed state transitions.");

            // Close loggers
            kernel.closeLogger();

            updateProcessTable();
        });
    }

    private void runMonitor() {
        while (simulationRunning) {
            SwingUtilities.invokeLater(this::updateStats);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void stopSimulation() {
        simulationRunning = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
        if (monitorThread != null) {
            monitorThread.interrupt();
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        addProcessButton.setEnabled(true);
        statusLabel.setText("Status: Stopped");
        statusLabel.setForeground(Color.RED);

        log(">>> SIMULATION STOPPED BY USER <<<");
    }

    private void resetSimulation() {
        stopSimulation();
        kernel = null;
        processConfigs.clear();
        processTableModel.setRowCount(0);

        addDefaultProcesses();

        tickLabel.setText("Ticks: 0");
        cycleLabel.setText("Cycles: 0");
        memoryLabel.setText("Memory: 0/0");
        swapLabel.setText("Swap: 0");
        contextSwitchLabel.setText("Context Switches: 0");
        timeLabel.setText("Time: 0ms");
        statusLabel.setText("Status: Idle");
        statusLabel.setForeground(Color.BLACK);

        log(">>> SIMULATION RESET <<<");
    }

    private void showAddProcessDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));

        JTextField nameField = new JTextField("Process" + (processConfigs.size() + 1));
        JSpinner burstSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        JSpinner prioritySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));

        panel.add(new JLabel("Process Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Burst Time (ms):"));
        panel.add(burstSpinner);
        panel.add(new JLabel("Priority (1=highest):"));
        panel.add(prioritySpinner);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Process",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            int burst = (Integer) burstSpinner.getValue();
            int priority = (Integer) prioritySpinner.getValue();

            if (!name.isEmpty()) {
                ProcessConfig config = new ProcessConfig(processConfigs.size() + 1, name, burst, priority);
                processConfigs.add(config);
                processTableModel.addRow(new Object[]{
                        config.pid, config.name, config.burstTime, config.burstTime,
                        config.priority, "CREATED", "-"
                });
                log("Added process: " + name + " (burst=" + burst + "ms, priority=" + priority + ")");
            }
        }
    }

    private void addDefaultProcesses() {
        addProcess("Chrome", 10, 2);
        addProcess("VSCode", 8, 1);
        addProcess("Spotify", 6, 3);
        addProcess("Terminal", 4, 2);
        addProcess("Calculator", 2, 4);
    }

    private void addProcess(String name, int burst, int priority) {
        ProcessConfig config = new ProcessConfig(processConfigs.size() + 1, name, burst, priority);
        processConfigs.add(config);
        processTableModel.addRow(new Object[]{
                config.pid, config.name, config.burstTime, config.burstTime,
                config.priority, "CREATED", "-"
        });
    }

    //==========================================Update==========================================
    private void updateProcessTable() {
        if (kernel == null) return;

        List<Process> processes = kernel.getAllProcesses();
        for (int i = 0; i < processes.size() && i < processTableModel.getRowCount(); i++) {
            Process p = processes.get(i);
            processTableModel.setValueAt(p.getRemainingTime(), i, 3);
            processTableModel.setValueAt(p.getState().toString(), i, 5);
            if (p.getState() == ProcessState.ZOMBIE) {
                processTableModel.setValueAt(p.getTurnaroundTime() + "ms", i, 6);
            }
        }
        processTable.repaint();
    }

    private void updateStats() {
        if (kernel == null) return;

        timeLabel.setText("Time: " + kernel.getSimulationTime() + "ms");
        tickLabel.setText("Ticks: " + kernel.getTickCount());
        cycleLabel.setText("Cycles: " + kernel.getCycleCount());
        memoryLabel.setText("Memory: " + kernel.getMemoryManager().getMemoryUsage() +
                "/" + kernel.getMemoryManager().getMaxMemorySlots());
        swapLabel.setText("Swap: " + kernel.getMemoryManager().getSwapUsage());
        contextSwitchLabel.setText("Context Switches: " + kernel.getDispatcher().getContextSwitchCount());
    }

    private void logStatistics() {
        if (kernel == null) return;

        log("");
        log("=== FINAL STATISTICS ===");
        log("Total Time: " + kernel.getSimulationTime() + "ms");
        log("Total Cycles: " + kernel.getCycleCount());
        log("Context Switches: " + kernel.getDispatcher().getContextSwitchCount());
        log("");
        log("Process Results:");
        log(String.format("%-12s | %-8s | %-10s | %-12s | %-8s",
                "Name", "Priority", "Burst(ms)", "Turnaround", "State"));
        log("-".repeat(60));

        long totalTurnaround = 0;
        for (Process p : kernel.getAllProcesses()) {
            long turnaround = p.getTurnaroundTime();
            totalTurnaround += turnaround;
            log(String.format("%-12s | %-8d | %-10d | %-12d | %-8s",
                    p.getName(), p.getPriority(), p.getBurstTime(), turnaround, p.getState()));
        }

        if (!kernel.getAllProcesses().isEmpty()) {
            log("-".repeat(60));
            log(String.format("Average Turnaround: %.2fms",
                    (double) totalTurnaround / kernel.getAllProcesses().size()));
        }
    }

    //==========================================Log==========================================
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String logLine = "[" + timestamp + "] " + message;
        logHistory.add(logLine);

        SwingUtilities.invokeLater(() -> {
            logArea.append(logLine + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void clearLog() {
        logArea.setText("");
        logHistory.clear();
        log("Log cleared");
    }

    private void exportLog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("simulation_log.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fileChooser.getSelectedFile())) {
                for (String line : logHistory) {
                    writer.println(line);
                }
                JOptionPane.showMessageDialog(this, "Log exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to export log: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //==========================================Inner Classes==========================================
    private static class ProcessConfig {
        int pid;
        String name;
        int burstTime;
        int priority;

        ProcessConfig(int pid, String name, int burstTime, int priority) {
            this.pid = pid;
            this.name = name;
            this.burstTime = burstTime;
            this.priority = priority;
        }
    }

    private static class StateCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null) {
                String state = value.toString();
                switch (state) {
                    case "CREATED":
                        c.setBackground(CREATED_COLOR);
                        break;
                    case "READY_MEMORY":
                        c.setBackground(READY_MEMORY_COLOR);
                        break;
                    case "READY_SWAPPED":
                        c.setBackground(READY_SWAPPED_COLOR);
                        break;
                    case "SLEEP":
                        c.setBackground(SLEEP_COLOR);
                        break;
                    case "SLEEP_SWAPPED":
                        c.setBackground(SLEEP_SWAPPED_COLOR);
                        break;
                    case "KERNEL_RUNNING":
                        c.setBackground(KERNEL_RUNNING_COLOR);
                        break;
                    case "USER_RUNNING":
                        c.setBackground(USER_RUNNING_COLOR);
                        break;
                    case "PREEMPTED":
                        c.setBackground(PREEMPTED_COLOR);
                        break;
                    case "ZOMBIE":
                        c.setBackground(ZOMBIE_COLOR);
                        break;
                    default:
                        c.setBackground(Color.WHITE);
                        break;
                }
            }

            if (isSelected) {
                c.setBackground(c.getBackground().darker());
            }

            return c;
        }
    }

    //==========================================Main==========================================
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Run on EDT
        SwingUtilities.invokeLater(SimulatorApp::new);
    }
}
