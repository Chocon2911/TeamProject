package com.ossimulator.transition;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

public class IORequest implements Transition {
    //==========================================Variable==========================================
    private int ioTime;
    private boolean completed;

    //========================================Constructor=========================================
    public IORequest(int ioTime) {
        this.ioTime = ioTime;
        this.completed = false;
    }

    //=========================================Instructor=========================================
    @Override
    public void switchState(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.SLEEP);
    }

    @Override
    public void execute(ProcessControlBlock pcb) {
        if (ioTime > 0) {
            ioTime--;
        }
        if (ioTime <= 0) {
            completed = true;
        }
    }

    @Override
    public boolean isSatisfied() {
        return completed;
    }

    public int getIoTime() {
        return ioTime;
    }
}
