package com.ossimulator.transition;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

public class RescheduleProcess implements Transition {
    //==========================================Variable==========================================
    private boolean executed;

    //========================================Constructor=========================================
    public RescheduleProcess() {
        this.executed = false;
    }

    //=========================================Instructor=========================================
    @Override
    public void switchState(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.KERNEL_RUNNING);
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
