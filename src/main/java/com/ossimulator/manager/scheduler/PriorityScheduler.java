package com.ossimulator.manager.scheduler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import com.ossimulator.model.component.ProcessState;
import com.ossimulator.model.main.Process;

public class PriorityScheduler implements Scheduler {
    //==========================================Variable==========================================
    private static final int MAX_PRIORITY_LEVELS = 10;

    private final Map<Integer, Queue<Process>> priorityQueues;
    private final int timeQuantum;
    private final ReentrantLock lock;

    public PriorityScheduler(int timeQuantum) {
        this.priorityQueues = new HashMap<>();
        this.timeQuantum = timeQuantum;
        this.lock = new ReentrantLock();

        for (int i = 1; i <= MAX_PRIORITY_LEVELS; i++) {
            priorityQueues.put(i, new LinkedList<>());
        }
    }

    @Override
    public void addProcess(Process process) {
        lock.lock();
        try {
            if (process.getState() == ProcessState.CREATED) {
                process.setState(ProcessState.READY_MEMORY);
            }

            int priority = Math.min(Math.max(process.getPriority(), 1), MAX_PRIORITY_LEVELS);
            priorityQueues.get(priority).offer(process);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Process> selectNext() {
        lock.lock();
        try {
            for (int priority = 1; priority <= MAX_PRIORITY_LEVELS; priority++) {
                Queue<Process> queue = priorityQueues.get(priority);
                if (!queue.isEmpty()) {
                    return Optional.of(queue.poll());
                }
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void requeue(Process process) {
        lock.lock();
        try {
            if (!process.isCompleted() && process.getState() == ProcessState.READY_MEMORY) {
                int priority = Math.min(Math.max(process.getPriority(), 1), MAX_PRIORITY_LEVELS);
                priorityQueues.get(priority).offer(process);
            }
        } finally {
            lock.unlock();
        }
    }

    public void changePriority(Process process, int newPriority) {
        lock.lock();
        try {
            int oldPriority = process.getPriority();
            Queue<Process> oldQueue = priorityQueues.get(oldPriority);

            if (oldQueue.remove(process)) {
                process.setPriority(newPriority);
                priorityQueues.get(newPriority).offer(process);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return priorityQueues.values().stream()
                    .allMatch(Queue::isEmpty);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return priorityQueues.values().stream()
                    .mapToInt(Queue::size)
                    .sum();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getTimeQuantum() {
        return timeQuantum;
    }

    @Override
    public String getName() {
        return "Priority Scheduler (quantum=" + timeQuantum + "ms)";
    }

    public void printQueues() {
        lock.lock();
        try {
            System.out.println("=== Priority Queues ===");
            for (int i = 1; i <= MAX_PRIORITY_LEVELS; i++) {
                Queue<Process> q = priorityQueues.get(i);
                if (!q.isEmpty()) {
                    System.out.printf("Priority %d: %s%n", i, q);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
