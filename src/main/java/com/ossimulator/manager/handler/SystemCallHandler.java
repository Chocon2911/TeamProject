package com.ossimulator.manager.handler;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

import java.util.concurrent.locks.ReentrantLock;

public class SystemCallHandler {
    //==========================================Variable==========================================
    private final ReentrantLock lock;

    //========================================Constructor=========================================
    public SystemCallHandler() {
        this.lock = new ReentrantLock();
    }

    //===========================================Method===========================================
    /**
     * Handle system call
     * USER_RUNNING → KERNEL_RUNNING
     */
    public void handleSystemCall(ProcessControlBlock pcb, int syscallNumber) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.USER_RUNNING) {
                pcb.setState(ProcessState.KERNEL_RUNNING);

                // Process the system call based on syscall number
                switch (syscallNumber) {
                    case SyscallNumbers.SYS_READ:
                        handleRead(pcb);
                        break;
                    case SyscallNumbers.SYS_WRITE:
                        handleWrite(pcb);
                        break;
                    case SyscallNumbers.SYS_OPEN:
                        handleOpen(pcb);
                        break;
                    case SyscallNumbers.SYS_CLOSE:
                        handleClose(pcb);
                        break;
                    case SyscallNumbers.SYS_FORK:
                        handleFork(pcb);
                        break;
                    case SyscallNumbers.SYS_EXIT:
                        handleExit(pcb);
                        break;
                    case SyscallNumbers.SYS_WAIT:
                        handleWait(pcb);
                        break;
                    default:
                        // Unknown syscall
                        break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return from system call
     * KERNEL_RUNNING → USER_RUNNING
     */
    public void returnFromSyscall(ProcessControlBlock pcb) {
        lock.lock();
        try {
            if (pcb.getProcessState() == ProcessState.KERNEL_RUNNING) {
                pcb.setState(ProcessState.USER_RUNNING);
            }
        } finally {
            lock.unlock();
        }
    }

    // System call handlers
    private void handleRead(ProcessControlBlock pcb) {
        // Handle read syscall - may block for I/O
    }

    private void handleWrite(ProcessControlBlock pcb) {
        // Handle write syscall - may block for I/O
    }

    private void handleOpen(ProcessControlBlock pcb) {
        // Handle open syscall
    }

    private void handleClose(ProcessControlBlock pcb) {
        // Handle close syscall
    }

    private void handleFork(ProcessControlBlock pcb) {
        // Handle fork syscall - creates new process
    }

    private void handleExit(ProcessControlBlock pcb) {
        // Handle exit syscall - transitions to ZOMBIE
        pcb.setState(ProcessState.ZOMBIE);
    }

    private void handleWait(ProcessControlBlock pcb) {
        // Handle wait syscall - waits for child process
    }

    //==========================================Constants==========================================
    public static class SyscallNumbers {
        public static final int SYS_READ = 0;
        public static final int SYS_WRITE = 1;
        public static final int SYS_OPEN = 2;
        public static final int SYS_CLOSE = 3;
        public static final int SYS_FORK = 4;
        public static final int SYS_EXIT = 5;
        public static final int SYS_WAIT = 6;
    }
}
