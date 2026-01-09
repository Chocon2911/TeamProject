package com.ossimulator.kernel;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;
import com.ossimulator.dispatcher.Dispatcher;
import com.ossimulator.memory.MemoryManager;
import com.ossimulator.scheduler.PriorityScheduler;
import com.ossimulator.scheduler.Scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Kernel {
    private final Map<Integer, Process> processTable;
    private final Scheduler scheduler;
    private final Dispatcher dispatcher;
    private final MemoryManager memoryManager;

    private final AtomicInteger nextPid;
    private final AtomicBoolean running;
    private int simulationTime;

    private final int timeQuantum;
    private final int maxMemory;

    public Kernel(int timeQuantum, int maxMemory) {
        this.timeQuantum = timeQuantum;
        this.maxMemory = maxMemory;
        this.processTable = new HashMap<>();
        this.scheduler = new PriorityScheduler(timeQuantum);
        this.dispatcher = new Dispatcher();
        this.memoryManager = new MemoryManager(maxMemory, processTable);
        this.nextPid = new AtomicInteger(1);
        this.running = new AtomicBoolean(false);
        this.simulationTime = 0;
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

    public synchronized void runCycle() {
        Optional<Process> nextOpt = scheduler.selectNext();

        if (nextOpt.isEmpty()) {
            System.out.println("[Kernel] No process ready to run");
            return;
        }

        Process next = nextOpt.get();

        if (!memoryManager.isInMemory(next.getPid())) {
            memoryManager.swapIn(next);
        }

        dispatcher.dispatch(next);

        int executedTime = next.execute(timeQuantum);
        simulationTime += executedTime;
        memoryManager.updateLRU(next.getPid());

        System.out.printf("[Kernel] P%d executed for %dms (remaining: %dms)%n",
                next.getPid(), executedTime, next.getRemainingTime());

        if (next.isCompleted()) {
            next.terminate();
            memoryManager.releaseMemory(next);
            System.out.printf("[Kernel] P%d TERMINATED%n", next.getPid());
        } else {
            next.preempt();
            scheduler.requeue(next);
        }
    }

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

    public int getSimulationTime() {
        return simulationTime;
    }

    public void printStatistics() {
        System.out.println("\n========== SIMULATION COMPLETE ==========");
        System.out.printf("Total simulation time: %dms%n", simulationTime);
        System.out.printf("Context switches: %d%n", dispatcher.getContextSwitchCount());
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

    public Scheduler getScheduler() { return scheduler; }
    public Dispatcher getDispatcher() { return dispatcher; }
    public MemoryManager getMemoryManager() { return memoryManager; }
    public Map<Integer, Process> getProcessTable() { return processTable; }
}
