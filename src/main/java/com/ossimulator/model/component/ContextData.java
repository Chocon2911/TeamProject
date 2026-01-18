package com.ossimulator.model.component;

public class ContextData {
    //==========================================Variable==========================================
    private int[] registers;             // General-purpose registers
    private int stackPointer;            // Stack Pointer
    private int flagsRegister;           // Status / Flags register

    //========================================Constructor=========================================
    public ContextData(int[] registers, int stackPointer, int flagsRegister) {
        this.registers = registers;
        this.stackPointer = stackPointer;
        this.flagsRegister = flagsRegister;
    }

    //==========================================Get Set===========================================
    public int[] getRegisters() { return registers; }
    public int getStackPointer() { return stackPointer; }
    public int getFlagsRegister() { return flagsRegister; }

    public void setRegisters(int[] registers) { this.registers = registers; }
    public void setStackPointer(int stackPointer) { this.stackPointer = stackPointer; }
    public void setFlagsRegister(int flagsRegister) { this.flagsRegister = flagsRegister; }
}
