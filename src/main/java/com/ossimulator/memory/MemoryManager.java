package com.ossimulator.memory;

import com.ossimulator.core.Process;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MemoryManager {
    private final int maxMemorySlots;
    private final Set<Integer> inMemory;
    private final SwapSpace swapSpace;
    private final Queue<Integer> lruQueue;
    private final ReentrantLock lock;
    private final Map<Integer, Process> processTable;

    public MemoryManager(int maxMemorySlots, Map<Integer, Process> processTable) {
        this.maxMemorySlots = maxMemorySlots;
        this.inMemory = new HashSet<>();
        this.swapSpace = new SwapSpace();
        this.lruQueue = new LinkedList<>();
        this.processTable = processTable;
        this.lock = new ReentrantLock();
    }

    public boolean loadToMemory(Process process) {
        lock.lock();
        try {
            int pid = process.getPid();

            if (inMemory.contains(pid)) {
                updateLRU(pid);
                return true;
            }

            if (inMemory.size() >= maxMemorySlots) {
                if (!swapOutVictim()) {
                    return false;
                }
            }

            inMemory.add(pid);
            lruQueue.offer(pid);
            process.setInMainMemory(true);

            if (swapSpace.contains(pid)) {
                swapSpace.remove(pid);
                process.swapIn();
            }

            System.out.printf("  [Memory] Loaded P%d to RAM. Memory: %d/%d%n",
                    pid, inMemory.size(), maxMemorySlots);

            return true;
        } finally {
            lock.unlock();
        }
    }

    private boolean swapOutVictim() {
        Integer victimPid = lruQueue.poll();
        if (victimPid == null) {
            return false;
        }

        Process victim = processTable.get(victimPid);
        if (victim != null) {
            return swapOut(victim);
        }
        return false;
    }

    public boolean swapOut(Process process) {
        lock.lock();
        try {
            int pid = process.getPid();

            if (!inMemory.contains(pid)) {
                return false;
            }

            inMemory.remove(pid);
            lruQueue.remove(pid);
            swapSpace.add(pid);
            process.swapOut();
            process.setInMainMemory(false);

            System.out.printf("  [Memory] Swapped out P%d. Memory: %d/%d, Swap: %d%n",
                    pid, inMemory.size(), maxMemorySlots, swapSpace.size());

            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean swapIn(Process process) {
        return loadToMemory(process);
    }

    public void updateLRU(int pid) {
        lock.lock();
        try {
            lruQueue.remove(pid);
            lruQueue.offer(pid);
        } finally {
            lock.unlock();
        }
    }

    public void releaseMemory(Process process) {
        lock.lock();
        try {
            int pid = process.getPid();
            inMemory.remove(pid);
            lruQueue.remove(pid);
            swapSpace.remove(pid);
            System.out.printf("  [Memory] Released memory for P%d%n", pid);
        } finally {
            lock.unlock();
        }
    }

    public boolean isInMemory(int pid) {
        return inMemory.contains(pid);
    }

    public boolean isInSwap(int pid) {
        return swapSpace.contains(pid);
    }

    public int getMemoryUsage() {
        return inMemory.size();
    }

    public int getSwapUsage() {
        return swapSpace.size();
    }

    public int getMaxMemorySlots() {
        return maxMemorySlots;
    }

    public void printStatus() {
        System.out.println("+-----------------------------------------+");
        System.out.println("|           MEMORY STATUS                 |");
        System.out.printf("|  RAM:  %d/%d processes                   |%n", inMemory.size(), maxMemorySlots);
        System.out.printf("|  Swap: %d processes                      |%n", swapSpace.size());
        System.out.printf("|  In RAM: %-30s|%n", inMemory);
        System.out.printf("|  In Swap: %-29s|%n", swapSpace.getPids());
        System.out.println("+-----------------------------------------+");
    }
}
