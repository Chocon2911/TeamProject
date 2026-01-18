package com.ossimulator.model.component;

public class Priority {
    //==========================================Variable==========================================
    private int value;

    //========================================Constructor=========================================
    public Priority(int value) {
        this.value = value;
    }

    //==========================================Get Set===========================================
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
