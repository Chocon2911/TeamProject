package com.ossimulator.transition;

import com.ossimulator.model.main.ProcessControlBlock;

public interface Transition {
    public void switchState(ProcessControlBlock pcb);
    public void execute(ProcessControlBlock pcb);
    public boolean isSatisfied();
}