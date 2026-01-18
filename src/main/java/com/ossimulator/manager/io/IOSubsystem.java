package com.ossimulator.manager.io;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;
import com.ossimulator.manager.memory.MemoryManager;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class IOSubsystem {
    //==========================================Variable==========================================
    private final Queue<ProcessControlBlock> ioWaitQueue;
    private final Map<Integer, IORequest> pendingRequests;
    private final MemoryManager memoryManager;
    private final ReentrantLock lock;

    //========================================Constructor=========================================
    public IOSubsystem(MemoryManager memoryManager) {
        this.ioWaitQueue = new LinkedList<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.memoryManager = memoryManager;
        this.lock = new ReentrantLock();
    }

    //===========================================Method===========================================
    /**
     * Block process for I/O (blocking call)
     * KERNEL_RUNNING → SLEEP (Asleep in Memory)
     */
    public void blockForIO(ProcessControlBlock pcb, int deviceId, int operation) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.KERNEL_RUNNING) {
                pcb.setState(ProcessState.SLEEP);
                ioWaitQueue.offer(pcb);

                IORequest request = new IORequest(pcb.getIdentifier().getPid(), deviceId, operation);
                pendingRequests.put(pcb.getIdentifier().getPid(), request);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wake up process after I/O completion
     * SLEEP → READY_MEMORY (wakeup event)
     * SLEEP_SWAPPED → READY_SWAPPED (wakeup event)
     */
    public void wakeup(ProcessControlBlock pcb) {
        lock.lock();
        try {
            ProcessState currentState = pcb.getProcessState();

            if (currentState == ProcessState.SLEEP) {
                pcb.setState(ProcessState.READY_MEMORY);
                ioWaitQueue.remove(pcb);
                pendingRequests.remove(pcb.getIdentifier().getPid());
            } else if (currentState == ProcessState.SLEEP_SWAPPED) {
                pcb.setState(ProcessState.READY_SWAPPED);
                pendingRequests.remove(pcb.getIdentifier().getPid());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Handle I/O completion for a specific device
     */
    public void ioComplete(int pid) {
        lock.lock();
        try {
            IORequest request = pendingRequests.get(pid);
            if (request != null) {
                // Find the PCB and wake it up
                for (ProcessControlBlock pcb : ioWaitQueue) {
                    if (pcb.getIdentifier().getPid() == pid) {
                        wakeup(pcb);
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Swap out sleeping process
     * SLEEP → SLEEP_SWAPPED (via Memory Management)
     */
    public void swapOutSleeping(ProcessControlBlock pcb) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.SLEEP) {
                memoryManager.swapOut(pcb);
                // State change handled by MemoryManager
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get number of processes waiting for I/O
     */
    public int getWaitingCount() {
        lock.lock();
        try {
            return ioWaitQueue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if process is waiting for I/O
     */
    public boolean isWaitingForIO(int pid) {
        return pendingRequests.containsKey(pid);
    }

    //==========================================Get Set===========================================
    public Queue<ProcessControlBlock> getIoWaitQueue() {
        return new LinkedList<>(ioWaitQueue);
    }

    //========================================Inner Class=========================================
    public static class IORequest {
        private final int pid;
        private final int deviceId;
        private final int operation;
        private final long timestamp;

        public IORequest(int pid, int deviceId, int operation) {
            this.pid = pid;
            this.deviceId = deviceId;
            this.operation = operation;
            this.timestamp = System.currentTimeMillis();
        }

        public int getPid() { return pid; }
        public int getDeviceId() { return deviceId; }
        public int getOperation() { return operation; }
        public long getTimestamp() { return timestamp; }
    }

    public static class IOOperations {
        public static final int READ = 0;
        public static final int WRITE = 1;
    }
}
