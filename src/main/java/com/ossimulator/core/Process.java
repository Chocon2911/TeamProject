package com.ossimulator.core;

public class Process {

    private final int pid;
    private final String name;
    private ProcessState state;
    private int priority;

    private final int burstTime;
    private int remainingTime;

    private final long arrivalTime;
    private long startTime;
    private long completionTime;

    private int memoryRequired;
    private boolean inMainMemory;

    public Process(int pid, String name, int burstTime, int priority) {
        this.pid = pid;
        this.name = name;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.state = ProcessState.NEW;
        this.arrivalTime = System.currentTimeMillis();
        this.memoryRequired = 100;
        this.inMainMemory = false;
    }

    public void admit() {
        if (state == ProcessState.NEW) {
            state = ProcessState.READY;
        }
    }

    public void dispatch() {
        if (state == ProcessState.READY) {
            state = ProcessState.RUNNING;
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
        }
    }

    public void preempt() {
        if (state == ProcessState.RUNNING) {
            state = ProcessState.READY;
        }
    }

    public void block() {
        if (state == ProcessState.RUNNING) {
            state = ProcessState.BLOCKED;
        }
    }

    public void unblock() {
        if (state == ProcessState.BLOCKED) {
            state = ProcessState.READY;
        }
    }

    public void terminate() {
        state = ProcessState.TERMINATED;
        completionTime = System.currentTimeMillis();
    }

    public void swapOut() {
        if (state == ProcessState.READY) {
            state = ProcessState.SUSPENDED_READY;
        } else if (state == ProcessState.BLOCKED) {
            state = ProcessState.SUSPENDED_BLOCKED;
        }
        inMainMemory = false;
    }

    public void swapIn() {
        if (state == ProcessState.SUSPENDED_READY) {
            state = ProcessState.READY;
        } else if (state == ProcessState.SUSPENDED_BLOCKED) {
            state = ProcessState.BLOCKED;
        }
        inMainMemory = true;
    }

    public int execute(int timeSlice) {
        int actualTime = Math.min(timeSlice, remainingTime);
        remainingTime -= actualTime;
        return actualTime;
    }

    public boolean isCompleted() {
        return remainingTime <= 0;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public ProcessState getState() {
        return state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public int getMemoryRequired() {
        return memoryRequired;
    }

    public boolean isInMainMemory() {
        return inMainMemory;
    }

    public void setInMainMemory(boolean inMainMemory) {
        this.inMainMemory = inMainMemory;
    }

    public long getTurnaroundTime() {
        if (completionTime == 0) {
            return 0;
        }
        return completionTime - arrivalTime;
    }

    public long getWaitingTime() {
        return getTurnaroundTime() - burstTime;
    }

    @Override
    public String toString() {
        return String.format("Process[pid=%d, name=%s, state=%s, priority=%d, remaining=%d]",
                pid, name, state, priority, remainingTime);
    }
}
