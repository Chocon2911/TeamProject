package com.ossimulator.transition;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

public class SwapIn implements Transition {
    //==========================================Variable==========================================
    private boolean executed;

    //========================================Constructor=========================================
    public SwapIn() {
        this.executed = false;
    }

    //=========================================Instructor=========================================
    @Override
    public void switchState(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.READY_MEMORY);
    }

    @Override
    public void execute(ProcessControlBlock pcb) {
        executed = true;
    }

    @Override
    public boolean isSatisfied() {
        return executed;
    }
}
