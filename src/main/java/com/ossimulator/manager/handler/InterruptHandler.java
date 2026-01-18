package com.ossimulator.manager.handler;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;
import com.ossimulator.manager.dispatcher.Dispatcher;

import java.util.concurrent.locks.ReentrantLock;

public class InterruptHandler {
    //==========================================Variable==========================================
    private final Dispatcher dispatcher;
    private final ReentrantLock lock;

    //========================================Constructor=========================================
    public InterruptHandler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.lock = new ReentrantLock();
    }

    //===========================================Method===========================================
    /**
     * Handle interrupt
     * USER_RUNNING → KERNEL_RUNNING
     */
    public void handleInterrupt(ProcessControlBlock pcb, int interruptType) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.USER_RUNNING) {
                pcb.setState(ProcessState.KERNEL_RUNNING);

                switch (interruptType) {
                    case InterruptTypes.TIMER:
                        handleTimerInterrupt(pcb);
                        break;
                    case InterruptTypes.IO_COMPLETE:
                        handleIOInterrupt(pcb);
                        break;
                    case InterruptTypes.PAGE_FAULT:
                        handlePageFault(pcb);
                        break;
                    case InterruptTypes.HARDWARE:
                        handleHardwareInterrupt(pcb);
                        break;
                    default:
                        break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Handle timer interrupt - time quantum expired
     * USER_RUNNING → PREEMPTED (via Round Robin Scheduler)
     */
    private void handleTimerInterrupt(ProcessControlBlock pcb) {
        // Time quantum expired - preempt process
        dispatcher.preempt(pcb);
    }

    /**
     * Handle I/O completion interrupt
     */
    private void handleIOInterrupt(ProcessControlBlock pcb) {
        // I/O completed - may wake up blocked process
    }

    /**
     * Handle page fault interrupt
     */
    private void handlePageFault(ProcessControlBlock pcb) {
        // Page fault - may need to swap in page
    }

    /**
     * Handle hardware interrupt
     */
    private void handleHardwareInterrupt(ProcessControlBlock pcb) {
        // Generic hardware interrupt
    }

    /**
     * Return from interrupt
     * KERNEL_RUNNING → USER_RUNNING
     * ZOMBIE → KERNEL_RUNNING (interrupt return from zombie)
     */
    public void interruptReturn(ProcessControlBlock pcb) {
        lock.lock();
        try {
            ProcessState currentState = pcb.getProcessState();
            if (currentState == ProcessState.KERNEL_RUNNING) {
                pcb.setState(ProcessState.USER_RUNNING);
            } else if (currentState == ProcessState.ZOMBIE) {
                pcb.setState(ProcessState.KERNEL_RUNNING);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Preempt due to higher priority process
     * USER_RUNNING → PREEMPTED (via Priority Scheduler)
     */
    public void preemptForHigherPriority(ProcessControlBlock currentPcb, ProcessControlBlock higherPriorityPcb) {
        lock.lock();
        try {
            if (currentPcb.getProcessState() == ProcessState.USER_RUNNING) {
                dispatcher.preempt(currentPcb);
                dispatcher.enqueueReady(currentPcb);
            }
        } finally {
            lock.unlock();
        }
    }

    //==========================================Constants==========================================
    public static class InterruptTypes {
        public static final int TIMER = 0;
        public static final int IO_COMPLETE = 1;
        public static final int PAGE_FAULT = 2;
        public static final int HARDWARE = 3;
    }
}
