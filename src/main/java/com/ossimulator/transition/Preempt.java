package com.ossimulator.transition;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

public class Preempt implements Transition {
    //==========================================Variable==========================================
    private boolean executed;

    //========================================Constructor=========================================
    public Preempt() {
        this.executed = false;
    }

    //=========================================Instructor=========================================
    @Override
    public void switchState(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.PREEMPTED);
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
