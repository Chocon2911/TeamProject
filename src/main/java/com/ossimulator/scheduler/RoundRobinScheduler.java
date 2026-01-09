package com.ossimulator.scheduler;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class RoundRobinScheduler implements Scheduler {
    private final Queue<Process> readyQueue;
    private final int timeQuantum;
    private final ReentrantLock lock;

    public RoundRobinScheduler(int timeQuantum) {
        this.readyQueue = new LinkedList<>();
        this.timeQuantum = timeQuantum;
        this.lock = new ReentrantLock();
    }

    @Override
    public void addProcess(Process process) {
        lock.lock();
        try {
            if (process.getState() == ProcessState.NEW) {
                process.admit();
            }
            readyQueue.offer(process);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Process> selectNext() {
        lock.lock();
        try {
            Process next = readyQueue.poll();
            return Optional.ofNullable(next);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void requeue(Process process) {
        lock.lock();
        try {
            if (!process.isCompleted() && process.getState() == ProcessState.READY) {
                readyQueue.offer(process);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return readyQueue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return readyQueue.size();
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
        return "Round Robin (quantum=" + timeQuantum + "ms)";
    }
}
