package com.ossimulator.kernel;

import com.ossimulator.core.Process;

import com.ossimulator.dispatcher.Dispatcher;
import com.ossimulator.memory.MemoryManager;
import com.ossimulator.scheduler.PriorityScheduler;
import com.ossimulator.scheduler.Scheduler;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Kernel {

    // Core components
    private final Map<Integer, Process> processTable;
    private final Scheduler scheduler;
    private final Dispatcher dispatcher;
    private final MemoryManager memoryManager;

    // Configuration
    private final int numCores;
    private final int timeQuantum;
    private final int maxMemory;

    // Multi-core state
    private final Process[] coreProcess;
    private final int[] coreTimeRemaining;

    // Statistics
    private final AtomicInteger nextPid;
    private final AtomicBoolean running;
    private int simulationTime;
    private int preemptCount;
    private final List<Process> terminatedProcesses;

    /**
     * Creates a single-core kernel (for console mode).
     */
    public Kernel(int timeQuantum, int maxMemory) {
        this(timeQuantum, maxMemory, 1);
    }

    /**
     * Creates a multi-core kernel (for visualizer).
     */
    public Kernel(int timeQuantum, int maxMemory, int numCores) {
        this.timeQuantum = timeQuantum;
        this.maxMemory = maxMemory;
        this.numCores = numCores;

        this.processTable = new LinkedHashMap<>(); // Preserve insertion order
        this.scheduler = new PriorityScheduler(timeQuantum);
        this.dispatcher = new Dispatcher();
        this.memoryManager = new MemoryManager(maxMemory, processTable);

        this.coreProcess = new Process[numCores];
        this.coreTimeRemaining = new int[numCores];

        this.nextPid = new AtomicInteger(1);
        this.running = new AtomicBoolean(false);
        this.simulationTime = 0;
        this.preemptCount = 0;
        this.terminatedProcesses = new ArrayList<>();
    }

    public Process createProcess(String name, int burstTime, int priority) {
        int pid = nextPid.getAndIncrement();
        Process process = new Process(pid, name, burstTime, priority);

        processTable.put(pid, process);
        dispatcher.registerProcess(process);

        if (memoryManager.loadToMemory(process)) {
            scheduler.addProcess(process);
            System.out.printf("[Kernel] Created process: %s%n", process);
        } else {
            System.out.printf("[Kernel] Failed to load P%d to memory%n", pid);
        }

        return process;
    }

    /**
     * Runs one scheduling cycle (Unified Mode).
     * For each core, perform steps until time quantum expires or event occurs.
     * Use this method for console simulation.
     */
    public synchronized void runCycle() {
        // Step all cores by one quantum or until blocked
        boolean anyCoreActive = false;

        for (int core = 0; core < numCores; core++) {
            // Check if scheduler has work or core is busy
            if (scheduler.isEmpty() && isCoreIdle(core)) {
                continue;
            }
            anyCoreActive = true;

            // Run a full quantum or until switch for this core
            int steps = 0;
            while (steps < timeQuantum) {
                CoreStepResult result = stepCore(core);

                // Log significant events
                if (result.action() != CoreAction.NONE && result.action() != CoreAction.EXECUTED) {
                    System.out.printf("[Kernel] Core %d: %s P%d (%s)%n",
                            core, result.action(), result.process().getPid(), result.process().getName());
                }

                if (result.action() == CoreAction.EXECUTED) {
                    steps++;
                } else if (result.action() == CoreAction.DISPATCHED) {
                    // Dispatch event does not consume execution time
                } else {
                    // Preemption or termination stops the cycle for this core
                    break;
                }
            }

            Process p = coreProcess[core];
            if (p != null) {
                System.out.printf("[Kernel] Core %d: P%d executed for %dms (remaining: %dms)%n",
                        core, p.getPid(), steps, p.getRemainingTime());
            }
        }

        if (!anyCoreActive) {
            System.out.println("[Kernel] No process ready to run");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MULTI-CORE MODE (Visualizer)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Result of a core operation for the visualizer to animate.
     */
    public enum CoreAction {
        NONE,
        DISPATCHED,
        EXECUTED,
        PREEMPTED,
        TERMINATED
    }

    public record CoreStepResult(CoreAction action, Process process, int core) {
    }

    /**
     * Executes one step (1ms) on the specified core.
     * Returns what happened for the visualizer to animate.
     */
    public synchronized CoreStepResult stepCore(int core) {
        if (core < 0 || core >= numCores) {
            throw new IllegalArgumentException("Invalid core: " + core);
        }

        Process p = coreProcess[core];

        // If core is idle, try to dispatch
        if (p == null) {
            return tryDispatchToCore(core);
        }

        // Execute 1ms of work
        p.execute(1);
        coreTimeRemaining[core]--;
        simulationTime++;

        // Check if completed
        if (p.isCompleted()) {
            return terminateFromCore(core);
        }

        // Check if time quantum expired
        if (coreTimeRemaining[core] <= 0) {
            return preemptFromCore(core);
        }

        return new CoreStepResult(CoreAction.EXECUTED, p, core);
    }

    /**
     * Tries to dispatch next process from scheduler to the specified core.
     */
    private CoreStepResult tryDispatchToCore(int core) {
        if (scheduler.isEmpty()) {
            return new CoreStepResult(CoreAction.NONE, null, core);
        }

        Optional<Process> nextOpt = scheduler.selectNext();
        if (nextOpt.isEmpty()) {
            return new CoreStepResult(CoreAction.NONE, null, core);
        }

        Process next = nextOpt.get();

        // Ensure process is in memory
        if (!memoryManager.isInMemory(next.getPid())) {
            memoryManager.swapIn(next);
        }

        // Dispatch to this core
        // Previous is null because the core was idle (checked in stepCore)
        coreProcess[core] = next;
        coreTimeRemaining[core] = timeQuantum;

        dispatcher.dispatch(next, null, core);

        memoryManager.updateLRU(next.getPid());

        return new CoreStepResult(CoreAction.DISPATCHED, next, core);
    }

    /**
     * Preempts the process on the specified core (round-robin).
     */
    private CoreStepResult preemptFromCore(int core) {
        Process p = coreProcess[core];
        if (p == null) {
            return new CoreStepResult(CoreAction.NONE, null, core);
        }

        // Save context before preemption
        dispatcher.saveContext(p, core);

        p.preempt();
        scheduler.requeue(p);
        coreProcess[core] = null;
        preemptCount++;

        return new CoreStepResult(CoreAction.PREEMPTED, p, core);
    }

    /**
     * Terminates the process on the specified core.
     */
    private CoreStepResult terminateFromCore(int core) {
        Process p = coreProcess[core];
        if (p == null) {
            return new CoreStepResult(CoreAction.NONE, null, core);
        }

        p.terminate();
        memoryManager.releaseMemory(p);
        terminatedProcesses.add(p);
        coreProcess[core] = null;

        return new CoreStepResult(CoreAction.TERMINATED, p, core);
    }

    /**
     * Checks if a core is currently idle.
     */
    public boolean isCoreIdle(int core) {
        return coreProcess[core] == null;
    }

    /**
     * Gets the process currently running on a core.
     */
    public Process getCoreProcess(int core) {
        return coreProcess[core];
    }

    /**
     * Gets remaining time in current quantum for a core.
     */
    public int getCoreTimeRemaining(int core) {
        return coreTimeRemaining[core];
    }

    // ═══════════════════════════════════════════════════════════════
    // SIMULATION CONTROL
    // ═══════════════════════════════════════════════════════════════

    public void runSimulation() {
        running.set(true);
        System.out.println("\n========== STARTING SIMULATION ==========\n");

        int cycle = 0;
        while (running.get() && !scheduler.isEmpty()) {
            System.out.printf("%n--- Cycle %d (Time: %dms) ---%n", ++cycle, simulationTime);
            runCycle();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        running.set(false);
        printStatistics();
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Checks if all processes have terminated.
     */
    public boolean isComplete() {
        return terminatedProcesses.size() >= processTable.size();
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS & GETTERS
    // ═══════════════════════════════════════════════════════════════

    public int getSimulationTime() {
        return simulationTime;
    }

    public int getDispatchCount() {
        return dispatcher.getContextSwitchCount();
    }

    public int getPreemptCount() {
        return preemptCount;
    }

    public int getTerminatedCount() {
        return terminatedProcesses.size();
    }

    public int getTotalProcessCount() {
        return processTable.size();
    }

    public void printStatistics() {
        System.out.println("\n========== SIMULATION COMPLETE ==========");
        System.out.printf("Total simulation time: %dms%n", simulationTime);
        System.out.printf("Context switches: %d%n", dispatcher.getContextSwitchCount());
        System.out.printf("Preemptions: %d%n", preemptCount);
        System.out.printf("Avg dispatch time: %.3fms%n", dispatcher.getAverageDispatchTime());
        memoryManager.printStatus();

        System.out.println("\nProcess Statistics:");
        System.out.println("+-----+------------+------------+-------------+--------------+");
        System.out.println("| PID | Name       | State      | Turnaround  | Waiting Time |");
        System.out.println("+-----+------------+------------+-------------+--------------+");
        for (Process p : processTable.values()) {
            System.out.printf("| %3d | %-10s | %-10s | %8dms  | %9dms  |%n",
                    p.getPid(), p.getName(), p.getState(),
                    p.getTurnaroundTime(), p.getWaitingTime());
        }
        System.out.println("+-----+------------+------------+-------------+--------------+");
    }

    // Getters for components
    public Scheduler getScheduler() {
        return scheduler;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public Map<Integer, Process> getProcessTable() {
        return processTable;
    }

    public List<Process> getTerminatedProcesses() {
        return terminatedProcesses;
    }

    public int getTimeQuantum() {
        return timeQuantum;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public int getNumCores() {
        return numCores;
    }
}
