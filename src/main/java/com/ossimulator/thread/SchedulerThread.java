package com.ossimulator.thread;

import com.ossimulator.manager.kernel.Kernel;

public class SchedulerThread extends Thread {
    //==========================================Variable==========================================
    private final Kernel kernel;
    private volatile boolean running;

    //========================================Constructor=========================================
    public SchedulerThread(Kernel kernel) {
        super("SchedulerThread");
        this.kernel = kernel;
        this.running = true;
    }

    //===========================================Method===========================================
    @Override
    public void run() {
        System.out.println("[SchedulerThread] Started - Thread ID: " + Thread.currentThread().getId());

        while (running && !kernel.getScheduler().isEmpty()) {
            kernel.runCycle();

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[SchedulerThread] Finished - Thread ID: " + Thread.currentThread().getId());
    }

    public void stopRunning() {
        running = false;
    }
}
