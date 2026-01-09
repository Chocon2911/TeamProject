package com.ossimulator.core;

public class ProcessControlBlock {
    private final int pid;
    private final Process process;

    private int programCounter;
    private int[] registers;
    private int stackPointer;

    private int priority;
    private long cpuTimeUsed;
    private long lastScheduledTime;

    public ProcessControlBlock(Process process) {
        this.pid = process.getPid();
        this.process = process;
        this.priority = process.getPriority();
        this.registers = new int[16];
        this.programCounter = 0;
        this.stackPointer = 0;
        this.cpuTimeUsed = 0;
    }

    public void saveContext(int pc, int[] regs, int sp) {
        this.programCounter = pc;
        this.registers = regs.clone();
        this.stackPointer = sp;
    }

    public int[] restoreContext() {
        return new int[] { programCounter, stackPointer };
    }

    public void addCpuTime(long time) {
        cpuTimeUsed += time;
        lastScheduledTime = System.currentTimeMillis();
    }

    public int getPid() { return pid; }
    public Process getProcess() { return process; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public long getCpuTimeUsed() { return cpuTimeUsed; }
    public int getProgramCounter() { return programCounter; }
    public int[] getRegisters() { return registers; }
    public int getStackPointer() { return stackPointer; }
}
