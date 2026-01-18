package com.ossimulator.manager.process;

import com.ossimulator.model.component.*;
import com.ossimulator.model.main.Process;
import com.ossimulator.model.main.ProcessControlBlock;
import com.ossimulator.manager.memory.MemoryManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessManager {
    //==========================================Variable==========================================
    private final Map<Integer, ProcessControlBlock> processTable;
    private final AtomicInteger pidCounter;
    private final MemoryManager memoryManager;

    //========================================Constructor=========================================
    public ProcessManager(MemoryManager memoryManager) {
        this.processTable = new ConcurrentHashMap<>();
        this.pidCounter = new AtomicInteger(1);
        this.memoryManager = memoryManager;
    }

    //===========================================Method===========================================
    /**
     * Fork: Creates a new process → CREATED state
     */
    public ProcessControlBlock fork(Process process, int parentPid, Priority priority) {
        int pid = pidCounter.getAndIncrement();

        Identifier identifier = new Identifier(pid, parentPid, process);
        AccountingInformation accountingInfo = new AccountingInformation();
        StatusInformationIO statusIO = new StatusInformationIO();
        MemoryPointer memoryPointer = new MemoryPointer(0, 0);
        ContextData contextData = new ContextData(new int[16], 0, 0);

        ProcessControlBlock pcb = new ProcessControlBlock(
            identifier,
            ProcessState.CREATED,
            contextData,
            priority,
            accountingInfo,
            statusIO,
            memoryPointer,
            process
        );

        processTable.put(pid, pcb);
        return pcb;
    }

    /**
     * Admit: Transition from CREATED based on memory availability
     * Returns true if admitted to READY_MEMORY, false if admitted to READY_SWAPPED
     */
    public boolean admit(ProcessControlBlock pcb) {
        if (pcb.getProcessState() != ProcessState.CREATED) {
            return false;
        }

        if (memoryManager.hasAvailableMemory(pcb)) {
            memoryManager.allocateMemory(pcb);
            pcb.setState(ProcessState.READY_MEMORY);
            return true;
        } else {
            memoryManager.swapOut(pcb);
            pcb.setState(ProcessState.READY_SWAPPED);
            return false;
        }
    }

    /**
     * Exit: Transition to ZOMBIE state
     */
    public void exit(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.ZOMBIE);
    }

    /**
     * Wait: Parent waits for child process, destroys PCB
     */
    public void wait(int parentPid, int childPid) {
        ProcessControlBlock childPcb = processTable.get(childPid);
        if (childPcb != null && childPcb.getProcessState() == ProcessState.ZOMBIE) {
            destroyProcess(childPid);
        }
    }

    /**
     * Destroy process and clean up resources
     */
    public void destroyProcess(int pid) {
        ProcessControlBlock pcb = processTable.remove(pid);
        if (pcb != null) {
            memoryManager.freeMemory(pcb);
            // PCB is removed from table, no state change needed
        }
    }

    /**
     * Get process by PID
     */
    public Optional<ProcessControlBlock> getProcess(int pid) {
        return Optional.ofNullable(processTable.get(pid));
    }

    /**
     * Get all processes
     */
    public Map<Integer, ProcessControlBlock> getAllProcesses() {
        return new ConcurrentHashMap<>(processTable);
    }

    /**
     * Get process count
     */
    public int getProcessCount() {
        return processTable.size();
    }
}
