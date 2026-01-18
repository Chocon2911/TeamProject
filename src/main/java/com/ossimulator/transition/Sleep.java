package com.ossimulator.transition;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.ProcessControlBlock;

public class Sleep implements Transition {
    //==========================================Variable==========================================
    private int sleepTime;

    //========================================Constructor=========================================
    public Sleep(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    //=========================================Instructor=========================================
    @Override
    public void switchState(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.SLEEP);
    }

    @Override
    public void execute(ProcessControlBlock pcb) {
        if (sleepTime > 0) {
            sleepTime--;
        }
    }

    @Override
    public boolean isSatisfied() {
        return sleepTime <= 0;
    }

    public int getSleepTime() {
        return sleepTime;
    }
}
