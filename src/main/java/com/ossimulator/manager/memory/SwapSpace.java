package com.ossimulator.manager.memory;

import java.util.HashSet;
import java.util.Set;

public class SwapSpace {
    private final Set<Integer> swappedProcesses;

    public SwapSpace() {
        this.swappedProcesses = new HashSet<>();
    }

    public void add(int pid) {
        swappedProcesses.add(pid);
    }

    public void remove(int pid) {
        swappedProcesses.remove(pid);
    }

    public boolean contains(int pid) {
        return swappedProcesses.contains(pid);
    }

    public int size() {
        return swappedProcesses.size();
    }

    public Set<Integer> getPids() {
        return new HashSet<>(swappedProcesses);
    }
}
