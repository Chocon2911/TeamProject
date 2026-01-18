package com.ossimulator.model.component;

import com.ossimulator.model.main.ProcessControlBlock;

public class ThreadControlBlock {
    private ProcessControlBlock processControlBlock;
    private ProgramData programData;
    private Program program;

    public ThreadControlBlock(
        ProcessControlBlock processControlBlock,
        ProgramData programData,
        Program program
    ) {
        this.processControlBlock = processControlBlock;
        this.programData = programData;
        this.program = program;
    }
    
}