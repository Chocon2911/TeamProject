package com.ossimulator;

import com.ossimulator.kernel.Kernel;
import com.ossimulator.thread.MonitorThread;
import com.ossimulator.thread.SchedulerThread;

public class Main {

    public static void main(String[] args) {
        System.out.println("+===========================================================+");
        System.out.println("|              OS KERNEL SIMULATOR                          |");
        System.out.println("|    Round Robin + Priority Scheduling with Swap            |");
        System.out.println("|                                                           |");
        System.out.println("|    Course: CS4448 - Operating Systems                     |");
        System.out.println("+===========================================================+");
        System.out.println();

        // Configuration
        int timeQuantum = 2;  // 2ms time quantum
        int maxMemory = 3;    // Max 3 processes in RAM (to demo swap)

        // Create kernel
        Kernel kernel = new Kernel(timeQuantum, maxMemory);

        // Create some processes with different priorities
        // Higher number = Higher priority (runs first)
        System.out.println("=== Creating Processes ===\n");
        kernel.createProcess("VSCode",      8, 7);  // Highest priority (4★)
        kernel.createProcess("Chrome",     10, 5);  // High priority (3★)
        kernel.createProcess("Terminal",    4, 4);  // High priority (3★)
        kernel.createProcess("Spotify",     6, 2);  // Medium priority (2★)
        kernel.createProcess("Calculator",  2, 1);  // Lowest priority (1★)

        System.out.println("\n=== Demo Options ===");
        System.out.println("1. Single-threaded simulation");
        System.out.println("2. Multi-threaded demo (2 threads running concurrently)");
        System.out.println();

        // Run multi-threaded demo (as required by project)
        System.out.println(">>> Running Multi-threaded Demo <<<\n");
        runMultiThreadDemo(kernel);
    }

    private static void runMultiThreadDemo(Kernel kernel) {
        System.out.println("=== Starting Multi-threaded Demo ===");
        System.out.println("Thread 1: SchedulerThread - runs scheduling cycles");
        System.out.println("Thread 2: MonitorThread - monitors system status");
        System.out.println();

        // Thread 1: Scheduler - runs the scheduling loop
        SchedulerThread schedulerThread = new SchedulerThread(kernel);

        // Thread 2: Monitor - prints system status periodically
        MonitorThread monitorThread = new MonitorThread(kernel, 800);

        // Start both threads concurrently
        System.out.println("[Main] Starting threads...");
        System.out.println("[Main] Main thread ID: " + Thread.currentThread().getId());

        schedulerThread.start();
        monitorThread.start();

        System.out.printf("[Main] Started: %s (ID: %d), %s (ID: %d)%n",
                schedulerThread.getName(), schedulerThread.getId(),
                monitorThread.getName(), monitorThread.getId());

        // Wait for scheduler to finish (all processes completed)
        try {
            schedulerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop monitor thread
        monitorThread.stopRunning();
        try {
            monitorThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Print final statistics
        System.out.println("\n[Main] Both threads have finished.");
        kernel.printStatistics();

        System.out.println("\n+===========================================================+");
        System.out.println("|                    DEMO COMPLETE                          |");
        System.out.println("|                                                           |");
        System.out.println("| This demo showed:                                         |");
        System.out.println("| 1. Process scheduling with Priority + Round Robin         |");
        System.out.println("| 2. Context switching via Dispatcher                       |");
        System.out.println("| 3. Memory management with Swap In/Out                     |");
        System.out.println("| 4. Two threads running concurrently:                      |");
        System.out.println("|    - SchedulerThread: executes scheduling cycles          |");
        System.out.println("|    - MonitorThread: monitors and displays system status   |");
        System.out.println("+===========================================================+");
    }
}
