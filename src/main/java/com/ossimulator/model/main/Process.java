package com.ossimulator.model.main;

import com.ossimulator.model.component.Identifier;
import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.component.Program;
import com.ossimulator.model.component.ProgramData;

public class Process {

    //==========================================Variable==========================================
    private Identifier identifier;
    private Program program;
    private ProgramData programData;

    // Process scheduling attributes
    private int pid;
    private String name;
    private int priority;
    private ProcessState state;

    // Timing attributes
    private long arrivalTime;
    private long startTime;
    private long completionTime;
    private int burstTime;
    private int remainingTime;

    // Memory attributes
    private boolean inMainMemory;

    //========================================Constructor=========================================
    public Process(Identifier identifier, Program program, ProgramData programData) {
        this.identifier = identifier;
        this.program = program;
        this.programData = programData;
        this.state = ProcessState.CREATED;
        this.arrivalTime = System.currentTimeMillis();
        this.inMainMemory = false;
    }

    /**
     * Constructor with scheduling parameters
     */
    public Process(int pid, String name, int burstTime, int priority) {
        this.pid = pid;
        this.name = name;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.state = ProcessState.CREATED;
        this.arrivalTime = System.currentTimeMillis();
        this.inMainMemory = false;

        // Initialize default components
        this.identifier = new Identifier(pid, 0, this);
        this.program = new Program();
        this.programData = new ProgramData();
    }

    //==========================================Get Set===========================================
    public Identifier getIdentifier() { return identifier; }
    public Program getProgram() { return program; }
    public ProgramData getProgramData() { return programData; }
    public int getPid() { return pid; }
    public String getName() { return name; }
    public int getPriority() { return priority; }
    public ProcessState getState() { return state; }
    public long getArrivalTime() { return arrivalTime; }
    public long getStartTime() { return startTime; }
    public long getCompletionTime() { return completionTime; }
    public int getBurstTime() { return burstTime; }
    public int getRemainingTime() { return remainingTime; }
    public boolean isInMainMemory() { return inMainMemory; }

    public void setIdentifier(Identifier identifier) { this.identifier = identifier; }
    public void setProgram(Program program) { this.program = program; }
    public void setProgramData(ProgramData programData) { this.programData = programData; }
    public void setPid(int pid) { this.pid = pid; }
    public void setName(String name) { this.name = name; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setState(ProcessState state) { this.state = state; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }
    public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }
    public void setInMainMemory(boolean inMainMemory) { this.inMainMemory = inMainMemory; }

    //===========================================Method===========================================
    /**
     * Execute process for given time slice
     * @return actual time executed
     */
    public int execute(int timeSlice) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        int actualTime = Math.min(timeSlice, remainingTime);
        remainingTime -= actualTime;
        return actualTime;
    }

    /**
     * Check if process has completed execution
     */
    public boolean isCompleted() {
        return remainingTime <= 0;
    }

    /**
     * Get turnaround time (completion - arrival)
     */
    public long getTurnaroundTime() {
        if (completionTime == 0) {
            return System.currentTimeMillis() - arrivalTime;
        }
        return completionTime - arrivalTime;
    }

    /**
     * Get waiting time (turnaround - burst)
     */
    public long getWaitingTime() {
        return getTurnaroundTime() - burstTime;
    }

    @Override
    public String toString() {
        return String.format("Process[pid=%d, name=%s, state=%s, priority=%d, remaining=%d]",
                pid, name, state, priority, remainingTime);
    }
}
