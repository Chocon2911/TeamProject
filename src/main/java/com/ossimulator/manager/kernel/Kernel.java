package com.ossimulator.manager.kernel;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.component.Priority;
import com.ossimulator.model.main.Process;
import com.ossimulator.model.main.ProcessControlBlock;
import com.ossimulator.manager.process.ProcessManager;
import com.ossimulator.manager.memory.MemoryManager;
import com.ossimulator.manager.scheduler.Scheduler;
import com.ossimulator.manager.scheduler.PriorityScheduler;
import com.ossimulator.manager.scheduler.RoundRobinScheduler;
import com.ossimulator.manager.dispatcher.Dispatcher;
import com.ossimulator.manager.handler.SystemCallHandler;
import com.ossimulator.manager.handler.InterruptHandler;
import com.ossimulator.manager.io.IOSubsystem;

import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import com.ossimulator.util.Logger;
import com.ossimulator.util.StateHistoryLogger;

public class Kernel {
    //==========================================Variable==========================================
    private final ProcessManager processManager;
    private final MemoryManager memoryManager;
    private final Scheduler priorityScheduler;
    private final Scheduler roundRobinScheduler;
    private final Dispatcher dispatcher;
    private final SystemCallHandler systemCallHandler;
    private final InterruptHandler interruptHandler;
    private final IOSubsystem ioSubsystem;
    private final ReentrantLock lock;
    private final Logger logger;

    private Scheduler activeScheduler;
    private ProcessControlBlock runningProcess;

    // Simulation tracking
    private long simulationStartTime;
    private int cycleCount;
    private int tickCount;
    private List<Process> allProcesses;
    private List<Process> completedProcesses;

    // State history logging
    private StateHistoryLogger historyLogger;
    private Consumer<String> stateChangeCallback;

    //========================================Constructor=========================================
    public Kernel(int timeQuantum, long maxMemorySlots) {
        this.memoryManager = new MemoryManager(maxMemorySlots * 1024); // Convert slots to bytes
        this.processManager = new ProcessManager(memoryManager);
        this.priorityScheduler = new PriorityScheduler(timeQuantum);
        this.roundRobinScheduler = new RoundRobinScheduler(timeQuantum);
        this.activeScheduler = roundRobinScheduler; // Default scheduler
        this.dispatcher = new Dispatcher(activeScheduler);
        this.systemCallHandler = new SystemCallHandler();
        this.interruptHandler = new InterruptHandler(dispatcher);
        this.ioSubsystem = new IOSubsystem(memoryManager);
        this.lock = new ReentrantLock();
        this.runningProcess = null;
        this.logger = Logger.getInstance();

        // Initialize simulation tracking
        this.simulationStartTime = System.currentTimeMillis();
        this.cycleCount = 0;
        this.tickCount = 0;
        this.allProcesses = new ArrayList<>();
        this.completedProcesses = new ArrayList<>();
        this.historyLogger = null;
        this.stateChangeCallback = null;
    }

    /**
     * Enable state history logging to file
     */
    public void enableHistoryLogging(String filePath) {
        this.historyLogger = new StateHistoryLogger(filePath);
    }

    /**
     * Set callback for state changes (for GUI updates)
     */
    public void setStateChangeCallback(Consumer<String> callback) {
        this.stateChangeCallback = callback;
    }

    /**
     * Notify state change to callback and history logger
     */
    private void notifyStateChange(Process process, ProcessState fromState, ProcessState toState, String reason) {
        tickCount++;
        String message = String.format("[Tick %d] %s: %s → %s (%s)",
                tickCount, process.getName(), fromState, toState, reason);

        if (historyLogger != null) {
            historyLogger.logStateTransition(tickCount, process.getName(), fromState, toState, reason);
        }

        if (stateChangeCallback != null) {
            stateChangeCallback.accept(message);
        }
    }

    /**
     * Log snapshot of all process states
     */
    private void logSnapshot(String event) {
        if (historyLogger != null) {
            historyLogger.logTickSnapshot(tickCount, allProcesses, event);
        }
    }


    //===========================================Method===========================================
    /**
     * Fork: Create new process
     * — → CREATED (Process Management)
     */
    public ProcessControlBlock fork(Process process, int parentPid, Priority priority) {
        lock.lock();
        try {
            return processManager.fork(process, parentPid, priority);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Admit: Transition from CREATED based on memory
     * CREATED → READY_MEMORY (memory available) - Memory Management
     * CREATED → READY_SWAPPED (memory unavailable) - Memory Management
     */
    public void admit(ProcessControlBlock pcb) {
        lock.lock();
        try {
            processManager.admit(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Schedule: Select next process to run
     * READY_MEMORY → USER_RUNNING (Priority/Round Robin Scheduler → Dispatcher)
     */
    public void schedule() {
        lock.lock();
        try {
            if (runningProcess != null) {
                return; // Already running a process
            }

            var nextProcess = activeScheduler.selectNext();
            if (nextProcess.isPresent()) {
                // Dispatch the process
                // runningProcess = dispatch(nextProcess.get());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * System call handler
     * USER_RUNNING → KERNEL_RUNNING (System Call Handler)
     */
    public void handleSystemCall(ProcessControlBlock pcb, int syscallNumber) {
        lock.lock();
        try {
            systemCallHandler.handleSystemCall(pcb, syscallNumber);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Interrupt handler
     * USER_RUNNING → KERNEL_RUNNING (Interrupt Handler)
     */
    public void handleInterrupt(ProcessControlBlock pcb, int interruptType) {
        lock.lock();
        try {
            interruptHandler.handleInterrupt(pcb, interruptType);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Time quantum expired - preempt
     * USER_RUNNING → PREEMPTED (Round Robin Scheduler)
     */
    public void timeQuantumExpired(ProcessControlBlock pcb) {
        lock.lock();
        try {
            dispatcher.preempt(pcb);
            dispatcher.enqueueReady(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Higher priority process ready - preempt
     * USER_RUNNING → PREEMPTED (Priority Scheduler)
     */
    public void preemptForHigherPriority(ProcessControlBlock currentPcb, ProcessControlBlock higherPriorityPcb) {
        lock.lock();
        try {
            interruptHandler.preemptForHigherPriority(currentPcb, higherPriorityPcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return to user mode
     * KERNEL_RUNNING → USER_RUNNING (Dispatcher)
     */
    public void returnToUser(ProcessControlBlock pcb) {
        lock.lock();
        try {
            dispatcher.returnToUser(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Blocking I/O call
     * KERNEL_RUNNING → SLEEP (I/O Subsystem)
     */
    public void blockingIO(ProcessControlBlock pcb, int deviceId, int operation) {
        lock.lock();
        try {
            ioSubsystem.blockForIO(pcb, deviceId, operation);
            runningProcess = null;
            schedule(); // Schedule next process
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reschedule - kernel decides to reschedule
     * KERNEL_RUNNING → READY_MEMORY (Priority + Round Robin Scheduler)
     */
    public void reschedule(ProcessControlBlock pcb) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.KERNEL_RUNNING) {
                pcb.setState(ProcessState.READY_MEMORY);
                activeScheduler.addProcess(pcb.getProcess());
                runningProcess = null;
                schedule();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Process exit
     * USER_RUNNING → KERNEL_RUNNING → ZOMBIE (Process Termination)
     */
    public void exit(ProcessControlBlock pcb) {
        lock.lock();
        try {
            pcb.setState(ProcessState.KERNEL_RUNNING);
            processManager.exit(pcb);
            runningProcess = null;
            schedule();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enqueue preempted process to ready queue
     * PREEMPTED → READY_MEMORY (Round Robin Scheduler)
     */
    public void enqueueReady(ProcessControlBlock pcb) {
        lock.lock();
        try {
            dispatcher.enqueueReady(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Dispatch preempted process
     * PREEMPTED → USER_RUNNING (Dispatcher)
     */
    public void dispatchPreempted(ProcessControlBlock pcb) {
        lock.lock();
        try {
            dispatcher.dispatchToUser(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wakeup sleeping process
     * SLEEP → READY_MEMORY (I/O Subsystem)
     */
    public void wakeup(ProcessControlBlock pcb) {
        lock.lock();
        try {
            ioSubsystem.wakeup(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Swap out process
     * READY_MEMORY → READY_SWAPPED (Memory Management)
     * SLEEP → SLEEP_SWAPPED (Memory Management)
     */
    public void swapOut(ProcessControlBlock pcb) {
        lock.lock();
        try {
            memoryManager.swapOut(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Swap in process
     * READY_SWAPPED → READY_MEMORY (Memory Management)
     */
    public void swapIn(ProcessControlBlock pcb) {
        lock.lock();
        try {
            memoryManager.swapIn(pcb);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Parent wait() for child process
     * ZOMBIE → PCB destroyed (Process Management)
     */
    public void waitForChild(int parentPid, int childPid) {
        lock.lock();
        try {
            processManager.wait(parentPid, childPid);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set active scheduler
     */
    public void setScheduler(SchedulerType type) {
        lock.lock();
        try {
            if (type == SchedulerType.PRIORITY) {
                activeScheduler = priorityScheduler;
            } else {
                activeScheduler = roundRobinScheduler;
            }
        } finally {
            lock.unlock();
        }
    }

    //==========================================Get Set===========================================
    public ProcessManager getProcessManager() { return processManager; }
    public MemoryManager getMemoryManager() { return memoryManager; }
    public Scheduler getPriorityScheduler() { return priorityScheduler; }
    public Scheduler getRoundRobinScheduler() { return roundRobinScheduler; }
    public Scheduler getActiveScheduler() { return activeScheduler; }
    public Dispatcher getDispatcher() { return dispatcher; }
    public SystemCallHandler getSystemCallHandler() { return systemCallHandler; }
    public InterruptHandler getInterruptHandler() { return interruptHandler; }
    public IOSubsystem getIOSubsystem() { return ioSubsystem; }
    public ProcessControlBlock getRunningProcess() { return runningProcess; }
    public Scheduler getScheduler() { return activeScheduler; }
    public long getSimulationTime() { return System.currentTimeMillis() - simulationStartTime; }
    public List<Process> getAllProcesses() { return allProcesses; }
    public List<Process> getCompletedProcesses() { return completedProcesses; }
    public int getCycleCount() { return cycleCount; }

    //==========================================Simulation Methods==========================================
    /**
     * Create a new process and add to scheduler
     */
    public Process createProcess(String name, int burstTime, int priority) {
        lock.lock();
        try {
            int pid = allProcesses.size() + 1;
            Process process = new Process(pid, name, burstTime, priority);

            Priority priorityObj = new Priority(priority);
            ProcessControlBlock pcb = fork(process, 0, priorityObj);

            admit(pcb);
            activeScheduler.addProcess(process);
            allProcesses.add(process);

            logger.kernel("Created process: %s (PID=%d, burst=%dms, priority=%d) → %s",
                    name, pid, burstTime, priority, pcb.getProcessState());

            return process;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Run one scheduling cycle - dispatches, executes, and handles preemption
     * Following UNIX Process State Transition Diagram:
     * READY_MEMORY → (dispatch) → KERNEL_RUNNING → (return) → USER_RUNNING
     * USER_RUNNING → (syscall/interrupt) → KERNEL_RUNNING → (preempt) → PREEMPTED
     * PREEMPTED → (return to user) → USER_RUNNING
     * KERNEL_RUNNING → (exit) → ZOMBIE
     */
    public void runCycle() {
        runCycleWithDelay(0);
    }

    /**
     * Run one scheduling cycle with delay between state transitions
     * @param delayMs delay in milliseconds between each state transition (0 for no delay)
     */
    public void runCycleWithDelay(int delayMs) {
        lock.lock();
        try {
            cycleCount++;
            int timeQuantum = activeScheduler.getTimeQuantum();

            var nextProcessOpt = activeScheduler.selectNext();
            if (nextProcessOpt.isEmpty()) {
                return;
            }

            Process process = nextProcessOpt.get();
            ProcessState prevState = process.getState();

            logger.scheduler("[Cycle %d] Selected: %s (priority=%d, remaining=%dms)",
                    cycleCount, process.getName(), process.getPriority(), process.getRemainingTime());
            logSnapshot("Cycle " + cycleCount + " - Process selected: " + process.getName());

            // Dispatch: READY_MEMORY → KERNEL_RUNNING (context switch in kernel mode)
            process.setState(ProcessState.KERNEL_RUNNING);
            dispatcher.incrementContextSwitch();
            notifyStateChange(process, prevState, ProcessState.KERNEL_RUNNING, "dispatch - context switch");
            logger.dispatcher("Context switch: %s → KERNEL_RUNNING", process.getName());
            sleepIfNeeded(delayMs);

            // Return to user: KERNEL_RUNNING → USER_RUNNING
            prevState = process.getState();
            process.setState(ProcessState.USER_RUNNING);
            notifyStateChange(process, prevState, ProcessState.USER_RUNNING, "return to user mode");
            logger.dispatcher("Return to user: %s → USER_RUNNING", process.getName());
            sleepIfNeeded(delayMs);

            // Execute for time quantum
            int executedTime = process.execute(timeQuantum);
            logger.process("%s executed for %dms (remaining=%dms)",
                    process.getName(), executedTime, process.getRemainingTime());
            if (historyLogger != null) {
                historyLogger.logExecution(tickCount, process.getName(), executedTime, process.getRemainingTime());
            }
            sleepIfNeeded(delayMs);

            // Check if process completed - needs to go through KERNEL_RUNNING first
            if (process.isCompleted()) {
                // USER_RUNNING → KERNEL_RUNNING (exit system call)
                prevState = process.getState();
                process.setState(ProcessState.KERNEL_RUNNING);
                notifyStateChange(process, prevState, ProcessState.KERNEL_RUNNING, "exit() system call");
                logger.kernel("%s: exit() system call → KERNEL_RUNNING", process.getName());
                sleepIfNeeded(delayMs);

                // KERNEL_RUNNING → ZOMBIE (exit)
                prevState = process.getState();
                process.setState(ProcessState.ZOMBIE);
                process.setCompletionTime(System.currentTimeMillis());
                completedProcesses.add(process);
                notifyStateChange(process, prevState, ProcessState.ZOMBIE, "process terminated");
                logger.kernel("%s: exit complete → ZOMBIE (turnaround=%dms)",
                        process.getName(), process.getTurnaroundTime());
            } else {
                // Time quantum expired: USER_RUNNING → KERNEL_RUNNING (timer interrupt)
                prevState = process.getState();
                process.setState(ProcessState.KERNEL_RUNNING);
                notifyStateChange(process, prevState, ProcessState.KERNEL_RUNNING, "timer interrupt");
                logger.dispatcher("%s: timer interrupt → KERNEL_RUNNING", process.getName());
                sleepIfNeeded(delayMs);

                // KERNEL_RUNNING → PREEMPTED
                prevState = process.getState();
                process.setState(ProcessState.PREEMPTED);
                notifyStateChange(process, prevState, ProcessState.PREEMPTED, "preempted by scheduler");
                logger.dispatcher("%s: preempt → PREEMPTED", process.getName());
                sleepIfNeeded(delayMs);

                // Reschedule: PREEMPTED → READY_MEMORY (back to queue)
                prevState = process.getState();
                process.setState(ProcessState.READY_MEMORY);
                activeScheduler.requeue(process);
                notifyStateChange(process, prevState, ProcessState.READY_MEMORY, "requeued to ready queue");
                logger.scheduler("%s: reschedule → READY_MEMORY (requeued)", process.getName());
            }

            logSnapshot("Cycle " + cycleCount + " completed");
        } finally {
            lock.unlock();
        }
    }

    private void sleepIfNeeded(int delayMs) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Print final simulation statistics
     */
    public void printStatistics() {
        logger.separator();
        System.out.println("\n+===========================================================+");
        System.out.println("|                 SIMULATION STATISTICS                     |");
        System.out.println("+===========================================================+");
        System.out.printf("| Total Simulation Time: %-33dms |%n", getSimulationTime());
        System.out.printf("| Total Cycles: %-42d |%n", cycleCount);
        System.out.printf("| Scheduler: %-45s |%n", activeScheduler.getName());
        System.out.printf("| Context Switches: %-38d |%n", dispatcher.getContextSwitchCount());
        System.out.println("+-----------------------------------------------------------+");
        System.out.println("| PROCESS STATISTICS                                        |");
        System.out.println("+-----------------------------------------------------------+");
        System.out.printf("| %-12s | %-8s | %-10s | %-12s | %-6s |%n",
                "Process", "Priority", "Burst(ms)", "Turnaround", "State");
        System.out.println("+-----------------------------------------------------------+");

        long totalTurnaround = 0;
        for (Process p : allProcesses) {
            long turnaround = p.getTurnaroundTime();
            totalTurnaround += turnaround;
            System.out.printf("| %-12s | %-8d | %-10d | %-12d | %-6s |%n",
                    p.getName(), p.getPriority(), p.getBurstTime(), turnaround, p.getState());
        }

        System.out.println("+-----------------------------------------------------------+");
        if (!allProcesses.isEmpty()) {
            System.out.printf("| Average Turnaround Time: %-31.2fms |%n",
                    (double) totalTurnaround / allProcesses.size());
        }
        System.out.println("+===========================================================+");
    }

    /**
     * Enable file logging
     */
    public void enableFileLogging(String filePath) {
        logger.enableFileLogging(filePath);
    }

    /**
     * Close logger
     */
    public void closeLogger() {
        // Write summary to history log
        if (historyLogger != null) {
            historyLogger.writeSummary(allProcesses, cycleCount, getSimulationTime(), dispatcher.getContextSwitchCount());
            historyLogger.close();
        }
        logger.close();
    }

    /**
     * Get tick count
     */
    public int getTickCount() {
        return tickCount;
    }

    /**
     * Get history logger
     */
    public StateHistoryLogger getHistoryLogger() {
        return historyLogger;
    }

    //==========================================Constants==========================================
    public enum SchedulerType {
        PRIORITY,
        ROUND_ROBIN
    }
}
