package com.ossimulator.dispatcher;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessControlBlock;
import com.ossimulator.core.ProcessState;

import java.util.HashMap;
import java.util.Map;

public class Dispatcher {
    private Process currentProcess;
    private final Map<Integer, ProcessControlBlock> pcbTable;

    private int programCounter;
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

    public Process dispatch(Process nextProcess) {
        long startTime = System.nanoTime();

        Process previousProcess = currentProcess;

        if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
            saveContext(currentProcess);
            currentProcess.preempt();
        }

        contextSwitch(nextProcess);

        restoreContext(nextProcess);
        nextProcess.dispatch();

        currentProcess = nextProcess;
        contextSwitchCount++;

        long endTime = System.nanoTime();
        totalDispatchTime += (endTime - startTime);

        printDispatchInfo(previousProcess, nextProcess);

        return previousProcess;
    }

    private void saveContext(Process process) {
        ProcessControlBlock pcb = pcbTable.get(process.getPid());
        if (pcb != null) {
            pcb.saveContext(programCounter, registers, stackPointer);
            System.out.printf("  [Dispatcher] Saved context for P%d (PC=%d)%n",
                    process.getPid(), programCounter);
        }
    }

    private void contextSwitch(Process nextProcess) {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void restoreContext(Process process) {
        ProcessControlBlock pcb = pcbTable.get(process.getPid());
        if (pcb != null) {
            int[] state = pcb.restoreContext();
            programCounter = state[0];
            stackPointer = state[1];
            System.out.printf("  [Dispatcher] Restored context for P%d (PC=%d)%n",
                    process.getPid(), programCounter);
        }
    }

    private void printDispatchInfo(Process prev, Process next) {
        System.out.println("------------------------------------------");
        System.out.printf("  DISPATCH #%d%n", contextSwitchCount);
        if (prev != null) {
            System.out.printf("  Previous: P%d (%s) -> %s%n",
                    prev.getPid(), prev.getName(), prev.getState());
        }
        System.out.printf("  Current:  P%d (%s) -> RUNNING%n",
                next.getPid(), next.getName());
        System.out.println("------------------------------------------");
    }

    public Process getCurrentProcess() {
        return currentProcess;
    }

    public int getContextSwitchCount() {
        return contextSwitchCount;
    }

    public double getAverageDispatchTime() {
        if (contextSwitchCount == 0) return 0;
        return (double) totalDispatchTime / contextSwitchCount / 1_000_000;
    }
}
