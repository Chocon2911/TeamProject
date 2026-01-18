package com.ossimulator.thread;

import com.ossimulator.manager.kernel.Kernel;

public class MonitorThread extends Thread {
    //==========================================Variable==========================================
    private final Kernel kernel;
    private volatile boolean running;
    private final int intervalMs;

    //========================================Constructor=========================================
    public MonitorThread(Kernel kernel, int intervalMs) {
        super("MonitorThread");
        this.kernel = kernel;
        this.intervalMs = intervalMs;
        this.running = true;
    }

    //===========================================Method===========================================
    @Override
    public void run() {
        System.out.println("[MonitorThread] Started - Thread ID: " + Thread.currentThread().getId());

        while (running) {
            printSystemStatus();

            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[MonitorThread] Finished - Thread ID: " + Thread.currentThread().getId());
    }

    private void printSystemStatus() {
        System.out.println();
        System.out.println("+=========================================+");
        System.out.println("|           SYSTEM MONITOR                |");
        System.out.println("|   Thread ID: " + String.format("%-26d", Thread.currentThread().getId()) + "|");
        System.out.println("+-----------------------------------------+");
        System.out.printf("|  Simulation Time: %-20dms |%n", kernel.getSimulationTime());
        System.out.printf("|  Ready Queue Size: %-20d |%n", kernel.getScheduler().size());
        System.out.printf("|  Memory Usage: %d/%-22d |%n",
                kernel.getMemoryManager().getMemoryUsage(),
                kernel.getMemoryManager().getMaxMemorySlots());
        System.out.printf("|  Swap Usage: %-26d |%n",
                kernel.getMemoryManager().getSwapUsage());
        System.out.printf("|  Context Switches: %-20d |%n",
                kernel.getDispatcher().getContextSwitchCount());
        System.out.println("+=========================================+");
    }

    public void stopRunning() {
        running = false;
    }
}
