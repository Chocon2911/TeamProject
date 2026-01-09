package com.ossimulator.scheduler;

import com.ossimulator.core.Process;
import java.util.Optional;

public interface Scheduler {
    void addProcess(Process process);
    Optional<Process> selectNext();
    void requeue(Process process);
    boolean isEmpty();
    int size();
    int getTimeQuantum();
    String getName();
}
