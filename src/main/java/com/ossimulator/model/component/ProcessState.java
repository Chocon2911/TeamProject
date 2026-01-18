package com.ossimulator.model.component;

public enum ProcessState {
    CREATED,
    READY_MEMORY,
    READY_SWAPPED,
    SLEEP,
    SLEEP_SWAPPED,
    KERNEL_RUNNING,
    USER_RUNNING,
    PREEMPTED,
    ZOMBIE
}
