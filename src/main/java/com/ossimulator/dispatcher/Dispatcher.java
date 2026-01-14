package com.ossimulator.dispatcher;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessControlBlock;
import com.ossimulator.core.ProcessState;

import java.util.HashMap;
import java.util.Map;

public class Dispatcher {
    private final Map<Integer, ProcessControlBlock> pcbTable;

    private int[] registers;
    private int stackPointer;

    private int contextSwitchCount;
    private long totalDispatchTime;

    public Dispatcher() {
        this.pcbTable = new HashMap<>();
        this.registers = new int[16];
        this.contextSwitchCount = 0;
        this.totalDispatchTime = 0;
    }

    public void registerProcess(Process process) {
        ProcessControlBlock pcb = new ProcessControlBlock(process);
        pcbTable.put(process.getPid(), pcb);
    }

    public void dispatch(Process nextProcess, Process previousProcess, int coreId) {
        long startTime = System.nanoTime();

        if (previousProcess != null && previousProcess.getState() == ProcessState.RUNNING) {
            saveContext(previousProcess, coreId);
            previousProcess.preempt();
        }

        contextSwitch(nextProcess);

        restoreContext(nextProcess, coreId);
        nextProcess.dispatch();

        contextSwitchCount++;

        long endTime = System.nanoTime();
        totalDispatchTime += (endTime - startTime);

        printDispatchInfo(previousProcess, nextProcess, coreId);
    }

    public void saveContext(Process process, int coreId) {
        ProcessControlBlock pcb = pcbTable.get(process.getPid());
        if (pcb != null) {
            // Save current process state (Program Counter, registers, etc.)
            int currentPC = process.getProgramCounter();
            pcb.saveContext(currentPC, registers, stackPointer);
            System.out.printf("  [Dispatcher][Core %d] Saved context for P%d (PC=%d)%n",
                    coreId, process.getPid(), currentPC);
        }
    }

    private void contextSwitch(Process nextProcess) {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void restoreContext(Process process, int coreId) {
        ProcessControlBlock pcb = pcbTable.get(process.getPid());
        if (pcb != null) {
            int[] state = pcb.restoreContext();
            // Load process state to CPU
            int restoredPC = state[0];
            System.out.printf("  [Dispatcher][Core %d] Restored context for P%d (PC=%d)%n",
                    coreId, process.getPid(), restoredPC);
        }
    }

    private void printDispatchInfo(Process prev, Process next, int coreId) {
        System.out.println("------------------------------------------");
        System.out.printf("  DISPATCH #%d [Core %d]%n", contextSwitchCount, coreId);
        if (prev != null) {
            System.out.printf("  Previous: P%d (%s) -> %s%n",
                    prev.getPid(), prev.getName(), prev.getState());
        }
        System.out.printf("  Current:  P%d (%s) -> RUNNING%n",
                next.getPid(), next.getName());
        System.out.println("------------------------------------------");
    }

    public int getContextSwitchCount() {
        return contextSwitchCount;
    }

    public double getAverageDispatchTime() {
        if (contextSwitchCount == 0)
            return 0;
        return (double) totalDispatchTime / contextSwitchCount / 1_000_000;
    }
}
