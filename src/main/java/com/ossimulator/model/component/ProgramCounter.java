package com.ossimulator.model.component;

public class ProgramCounter {
    //==========================================Variable==========================================
    private int value;

    //========================================Constructor=========================================
    public ProgramCounter (int value) {
        this.value = value;
    }

    //==========================================Get Set===========================================
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
