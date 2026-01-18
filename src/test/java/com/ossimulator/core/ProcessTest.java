package com.ossimulator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Process Tests")
class ProcessTest {

    private Process process;

    @BeforeEach
    void setUp() {
        process = new Process(1, "TestProcess", 10, 2);
    }

    @Test
    @DisplayName("Process khởi tạo với trạng thái NEW")
    void testInitialState() {
        assertEquals(ProcessState.NEW, process.getState());
        assertEquals(1, process.getPid());
        assertEquals("TestProcess", process.getName());
        assertEquals(10, process.getBurstTime());
        assertEquals(10, process.getRemainingTime());
        assertEquals(2, process.getPriority());
    }

    @Test
    @DisplayName("Admit: NEW -> READY")
    void testAdmit() {
        process.admit();
        assertEquals(ProcessState.READY, process.getState());
    }

    @Test
    @DisplayName("Dispatch: READY -> RUNNING")
    void testDispatch() {
        process.admit();
        process.dispatch();
        assertEquals(ProcessState.RUNNING, process.getState());
    }

    @Test
    @DisplayName("Preempt: RUNNING -> READY")
    void testPreempt() {
        process.admit();
        process.dispatch();
        process.preempt();
        assertEquals(ProcessState.READY, process.getState());
    }

    @Test
    @DisplayName("Block: RUNNING -> BLOCKED")
    void testBlock() {
        process.admit();
        process.dispatch();
        process.block();
        assertEquals(ProcessState.WAITING, process.getState());
    }

    @Test
    @DisplayName("Unblock: BLOCKED -> READY")
    void testUnblock() {
        process.admit();
        process.dispatch();
        process.block();
        process.unblock();
        assertEquals(ProcessState.READY, process.getState());
    }

    @Test
    @DisplayName("Terminate: -> TERMINATED")
    void testTerminate() {
        process.admit();
        process.dispatch();
        process.terminate();
        assertEquals(ProcessState.TERMINATED, process.getState());
    }

    @Test
    @DisplayName("SwapOut: READY -> SUSPENDED_READY")
    void testSwapOutFromReady() {
        process.admit();
        process.swapOut();
        assertEquals(ProcessState.SWAPPED_READY, process.getState());
        assertFalse(process.isInMainMemory());
    }

    @Test
    @DisplayName("SwapOut: BLOCKED -> SUSPENDED_BLOCKED")
    void testSwapOutFromBlocked() {
        process.admit();
        process.dispatch();
        process.block();
        process.swapOut();
        assertEquals(ProcessState.SWAPPED_WAITING, process.getState());
    }

    @Test
    @DisplayName("SwapIn: SWAPPED_READY -> READY")
    void testSwapIn() {
        process.admit();
        process.swapOut();
        process.swapIn();
        assertEquals(ProcessState.READY, process.getState());
        assertTrue(process.isInMainMemory());
    }

    @Test
    @DisplayName("Execute giảm remainingTime đúng")
    void testExecute() {
        int timeSlice = 3;
        int executed = process.execute(timeSlice);

        assertEquals(3, executed);
        assertEquals(7, process.getRemainingTime());
        assertFalse(process.isCompleted());
    }

    @Test
    @DisplayName("Execute không vượt quá remainingTime")
    void testExecuteNotExceedRemaining() {
        int timeSlice = 15; // Lớn hơn burstTime
        int executed = process.execute(timeSlice);

        assertEquals(10, executed); // Chỉ execute được 10
        assertEquals(0, process.getRemainingTime());
        assertTrue(process.isCompleted());
    }

    @Test
    @DisplayName("Process hoàn thành khi remainingTime = 0")
    void testIsCompleted() {
        process.execute(10);
        assertTrue(process.isCompleted());
    }

    @Test
    @DisplayName("Thay đổi priority")
    void testSetPriority() {
        process.setPriority(5);
        assertEquals(5, process.getPriority());
    }
}
