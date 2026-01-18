package com.ossimulator.manager.memory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.Process;
import com.ossimulator.model.main.ProcessControlBlock;

public class MemoryManager {
    //==========================================Variable==========================================
    private final long totalMemory;
    private long availableMemory;
    private final Map<Integer, Long> allocatedMemory;
    private final SwapSpace swapSpace;
    private final ReentrantLock lock;

    //========================================Constructor=========================================
    public MemoryManager(long totalMemory) {
        this.totalMemory = totalMemory;
        this.availableMemory = totalMemory;
        this.allocatedMemory = new ConcurrentHashMap<>();
        this.swapSpace = new SwapSpace();
        this.lock = new ReentrantLock();
    }

    //===========================================Method===========================================
    /**
     * Check if memory is available for process
     */
    public boolean hasAvailableMemory(ProcessControlBlock pcb) {
        lock.lock();
        try {
            long requiredMemory = getRequiredMemory(pcb);
            return availableMemory >= requiredMemory;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Allocate memory for process
     * CREATED → READY_MEMORY (memory available)
     */
    public boolean allocateMemory(ProcessControlBlock pcb) {
        lock.lock();
        try {
            int pid = pcb.getIdentifier().getPid();
            long requiredMemory = getRequiredMemory(pcb);

            if (availableMemory >= requiredMemory) {
                availableMemory -= requiredMemory;
                allocatedMemory.put(pid, requiredMemory);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Free memory allocated to process
     */
    public void freeMemory(ProcessControlBlock pcb) {
        lock.lock();
        try {
            int pid = pcb.getIdentifier().getPid();
            Long memory = allocatedMemory.remove(pid);
            if (memory != null) {
                availableMemory += memory;
            }
            swapSpace.remove(pid);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Swap out process from memory to swap space
     * READY_MEMORY → READY_SWAPPED
     * SLEEP → SLEEP_SWAPPED
     */
    public void swapOut(ProcessControlBlock pcb) {
        lock.lock();
        try {
            int pid = pcb.getIdentifier().getPid();
            Long memory = allocatedMemory.remove(pid);
            if (memory != null) {
                availableMemory += memory;
            }
            swapSpace.add(pid);

            ProcessState currentState = pcb.getProcessState();
            if (currentState == ProcessState.READY_MEMORY) {
                pcb.setState(ProcessState.READY_SWAPPED);
            } else if (currentState == ProcessState.SLEEP) {
                pcb.setState(ProcessState.SLEEP_SWAPPED);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Swap in process from swap space to memory
     * READY_SWAPPED → READY_MEMORY
     */
    public boolean swapIn(ProcessControlBlock pcb) {
        lock.lock();
        try {
            int pid = pcb.getIdentifier().getPid();
            long requiredMemory = getRequiredMemory(pcb);

            if (availableMemory >= requiredMemory && swapSpace.contains(pid)) {
                availableMemory -= requiredMemory;
                allocatedMemory.put(pid, requiredMemory);
                swapSpace.remove(pid);

                if (pcb.getProcessState() == ProcessState.READY_SWAPPED) {
                    pcb.setState(ProcessState.READY_MEMORY);
                }
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if process is in swap space
     */
    public boolean isSwapped(ProcessControlBlock pcb) {
        return swapSpace.contains(pcb.getIdentifier().getPid());
    }

    /**
     * Get required memory for process (placeholder - can be customized)
     */
    private long getRequiredMemory(ProcessControlBlock pcb) {
        return 1024; // Default 1KB per process
    }

    //==========================================Get Set===========================================
    public long getTotalMemory() { return totalMemory; }
    public long getAvailableMemory() { return availableMemory; }
    public long getUsedMemory() { return totalMemory - availableMemory; }
    public SwapSpace getSwapSpace() { return swapSpace; }

    /**
     * Get number of processes currently in memory
     */
    public int getMemoryUsage() {
        return allocatedMemory.size();
    }

    /**
     * Get maximum memory slots (each process takes 1KB = 1 slot)
     */
    public long getMaxMemorySlots() {
        return totalMemory / 1024;
    }

    /**
     * Get number of processes in swap space
     */
    public int getSwapUsage() {
        return swapSpace.size();
    }
}
