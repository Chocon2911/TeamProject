package com.ossimulator.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessState Enum Tests")
class ProcessStateTest {

    @Test
    @DisplayName("Enum có đủ 7 trạng thái")
    void testAllStatesExist() {
        ProcessState[] states = ProcessState.values();
        assertEquals(7, states.length);
    }

    @Test
    @DisplayName("Các trạng thái cơ bản tồn tại")
    void testBasicStates() {
        assertNotNull(ProcessState.NEW);
        assertNotNull(ProcessState.READY);
        assertNotNull(ProcessState.RUNNING);
        assertNotNull(ProcessState.WAITING);
        assertNotNull(ProcessState.TERMINATED);
    }

    @Test
    @DisplayName("Các trạng thái suspended tồn tại")
    void testSuspendedStates() {
        assertNotNull(ProcessState.SWAPPED_READY);
        assertNotNull(ProcessState.SWAPPED_WAITING);
    }

    @Test
    @DisplayName("valueOf hoạt động đúng")
    void testValueOf() {
        assertEquals(ProcessState.NEW, ProcessState.valueOf("NEW"));
        assertEquals(ProcessState.RUNNING, ProcessState.valueOf("RUNNING"));
        assertEquals(ProcessState.SWAPPED_READY, ProcessState.valueOf("SWAPPED_READY"));
    }
}
