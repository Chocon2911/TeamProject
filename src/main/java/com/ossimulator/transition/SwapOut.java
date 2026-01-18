package com.ossimulator.transition;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

public class SwapOut implements Transition {
    //==========================================Variable==========================================
    private boolean executed;

    //========================================Constructor=========================================
    public SwapOut() {
        this.executed = false;
    }

    //=========================================Instructor=========================================
    @Override
    public void switchState(ProcessControlBlock pcb) {
        ProcessState currentState = pcb.getProcessState();
        if (currentState == ProcessState.READY_MEMORY) {
            pcb.setState(ProcessState.READY_SWAPPED);
        } else if (currentState == ProcessState.SLEEP) {
            pcb.setState(ProcessState.SLEEP_SWAPPED);
        }
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
