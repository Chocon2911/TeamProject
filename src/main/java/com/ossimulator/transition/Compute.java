package com.ossimulator.transition;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

public class Compute implements Transition {
    //==========================================Variable==========================================
    private int cycles;

    //========================================Constructor=========================================
    public Compute(int cycles) {
        this.cycles = cycles;
    }

    //=========================================Instructor=========================================
    @Override
    public void switchState(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.USER_RUNNING);
    }

    @Override
    public void execute(ProcessControlBlock pcb) {
        if (cycles > 0) {
            cycles--;
        }
    }

    @Override
    public boolean isSatisfied() {
        return cycles <= 0;
    }

    public int getCycles() {
        return cycles;
    }
}
