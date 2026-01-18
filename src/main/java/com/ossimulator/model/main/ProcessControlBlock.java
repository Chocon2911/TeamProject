package com.ossimulator.model.main;

import com.ossimulator.model.component.*;

public class ProcessControlBlock {
    //==========================================Variable==========================================
    private final Identifier identifier;
    private final AccountingInformation accountingInformation;
    private final StatusInformationIO statusInformationIO;
    private final MemoryPointer memoryPointer;
    private final ContextData contextData;
    private ProcessState processState;
    private Priority priority;
    private Process process;

    //========================================Constructor=========================================
    public ProcessControlBlock(Identifier identifier, ProcessState processState, ContextData contextData,
        Priority priority, AccountingInformation accountingInformation, StatusInformationIO statusInformationIO, 
        MemoryPointer memoryPointer, Process process
    ) {
        this.identifier = identifier;
        this.processState = processState;
        this.contextData = contextData;
        this.priority = priority;
        this.accountingInformation = accountingInformation;
        this.statusInformationIO = statusInformationIO;
        this.memoryPointer = memoryPointer;
        this.process = process;
    }

    //==========================================Get Set===========================================
    public Identifier getIdentifier() { return identifier; }
    public ProcessState getProcessState() { return processState; }
    public ContextData getContextData() { return contextData; }
    public Priority getPriority() { return priority; }
    public AccountingInformation getAccountingInformation() { return accountingInformation; }
    public StatusInformationIO getStatusInformationIO() { return statusInformationIO; }
    public MemoryPointer getMemoryPointer() { return memoryPointer; }
    public Process getProcess() { return process; }

    public void setState(ProcessState state) { this.processState = state; }

    //===========================================Method===========================================
    public void saveContext(int pc, int[] regs, int sp) {
        contextData.setFlagsRegister(pc);
        contextData.setRegisters(regs.clone());
        contextData.setStackPointer(sp);
    }

    public int[] restoreContext() {
        return new int[] { contextData.getFlagsRegister(), contextData.getStackPointer() };
    }

    public void addCpuTime(long time) {
        accountingInformation.setCpuTimeUsed(accountingInformation.getCpuTimeUsed() + time);
        accountingInformation.setLastScheduledTime(System.currentTimeMillis());
    }
}
