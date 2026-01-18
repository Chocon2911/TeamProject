package com.ossimulator.model.component;

import com.ossimulator.transition.Transition;

import java.util.List;

public class Program {

    //==========================================Variable==========================================
    private final List<Transition> transitions;
    private int currentInstructionIndex;

    //========================================Constructor=========================================
    public Program() {
        this.transitions = new java.util.ArrayList<>();
        this.currentInstructionIndex = 0;
    }

    public Program(List<Transition> transitions) {
        this.transitions = transitions;
        this.currentInstructionIndex = 0;
    }

    //==========================================Get Set===========================================
    public List<Transition> getInstructions() {
        return transitions;
    }

    public int getCurrentInstructionIndex() {
        return currentInstructionIndex;
    }

    public void setCurrentInstructionIndex(int currentInstructionIndex) {
        this.currentInstructionIndex = currentInstructionIndex;
    }

    //===========================================Method===========================================
    public void nextInstruction() {
        if (currentInstructionIndex < transitions.size() - 1) {
            currentInstructionIndex++;
        }
    }

    public boolean isFinished() {
        return currentInstructionIndex >= transitions.size();
    }
}
