package com.ossimulator.manager.dispatcher;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;
import com.ossimulator.manager.scheduler.Scheduler;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class Dispatcher {
    //==========================================Variable==========================================
    private final Scheduler scheduler;
    private ProcessControlBlock currentProcess;
    private final ReentrantLock lock;
    private int contextSwitchCount;

    //========================================Constructor=========================================
    public Dispatcher(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.currentProcess = null;
        this.lock = new ReentrantLock();
        this.contextSwitchCount = 0;
    }

    //===========================================Method===========================================
    /**
     * Dispatch: Select next process from ready queue and run it
     * READY_MEMORY → KERNEL_RUNNING (via scheduler)
     * PREEMPTED → USER_RUNNING
     */
    public Optional<ProcessControlBlock> dispatch() {
        lock.lock();
        try {
            Optional<com.ossimulator.model.main.Process> nextProcess = scheduler.selectNext();

            if (nextProcess.isPresent()) {
                // Context switch would happen here
                // For now, we just update state
                return Optional.empty(); // Placeholder - need PCB integration
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Dispatch a specific PCB to USER_RUNNING
     * PREEMPTED → USER_RUNNING
     * KERNEL_RUNNING → USER_RUNNING (return to user)
     */
    public void dispatchToUser(ProcessControlBlock pcb) {
        lock.lock();
        try {
            saveContext(currentProcess);
            restoreContext(pcb);
            pcb.setState(ProcessState.USER_RUNNING);
            currentProcess = pcb;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return to user mode
     * KERNEL_RUNNING → USER_RUNNING
     */
    public void returnToUser(ProcessControlBlock pcb) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.KERNEL_RUNNING) {
                pcb.setState(ProcessState.USER_RUNNING);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Context switch: Save current process context and restore new process context
     */
    public void contextSwitch(ProcessControlBlock oldProcess, ProcessControlBlock newProcess) {
        lock.lock();
        try {
            saveContext(oldProcess);
            restoreContext(newProcess);
            currentProcess = newProcess;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Save process context (registers, PC, SP)
     */
    private void saveContext(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getContextData() != null) {
            // Context is already stored in PCB's ContextData
            // In a real implementation, we would save CPU registers here
        }
    }

    /**
     * Restore process context (registers, PC, SP)
     */
    private void restoreContext(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getContextData() != null) {
            // In a real implementation, we would restore CPU registers here
        }
    }

    /**
     * Preempt current process
     * USER_RUNNING → PREEMPTED
     */
    public void preempt(ProcessControlBlock pcb) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.USER_RUNNING) {
                saveContext(pcb);
                pcb.setState(ProcessState.PREEMPTED);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enqueue preempted process to ready queue
     * PREEMPTED → READY_MEMORY
     */
    public void enqueueReady(ProcessControlBlock pcb) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.PREEMPTED) {
                pcb.setState(ProcessState.READY_MEMORY);
                // scheduler.addProcess(pcb.getProcess()); // Need integration
            }
        } finally {
            lock.unlock();
        }
    }

    //==========================================Get Set===========================================
    public ProcessControlBlock getCurrentProcess() { return currentProcess; }
    public Scheduler getScheduler() { return scheduler; }
    public int getContextSwitchCount() { return contextSwitchCount; }

    /**
     * Increment context switch counter
     */
    public void incrementContextSwitch() {
        lock.lock();
        try {
            contextSwitchCount++;
        } finally {
            lock.unlock();
        }
    }
}
