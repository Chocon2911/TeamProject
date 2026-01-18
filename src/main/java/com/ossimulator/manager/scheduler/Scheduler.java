package com.ossimulator.manager.scheduler;

import java.util.Optional;

import com.ossimulator.model.main.Process;

public interface Scheduler {
    void addProcess(Process process);
    Optional<Process> selectNext();
    void requeue(Process process);
    boolean isEmpty();
    int size();
    int getTimeQuantum();
    String getName();
}
