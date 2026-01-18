package com.ossimulator.model.component;

import com.ossimulator.model.main.Process;

public class Identifier {
    //==========================================Variable==========================================
    private final int pid;          // Process ID
    private final int parentPid;    // Parent Process ID
    private final Process process;  // Process logic

    //========================================Constructor=========================================
    public Identifier(int pid, int parentPid, Process process) {
        this.pid = pid;
        this.parentPid = parentPid;
        this.process = process;
    }

    //==========================================Get Set===========================================
    public int getPid() { return pid; }
    public int getParentPid() { return parentPid; }
    public Process getProcess() { return process; }
}
