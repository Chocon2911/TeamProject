package com.ossimulator.kernel;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kernel Tests")
class KernelTest {

    private Kernel kernel;

    @BeforeEach
    void setUp() {
        kernel = new Kernel(2, 3); // time quantum = 2, max memory = 3
    }

    @Test
    @DisplayName("Kernel khởi tạo đúng")
    void testInitialization() {
        assertNotNull(kernel.getScheduler());
        assertNotNull(kernel.getDispatcher());
        assertNotNull(kernel.getMemoryManager());
        assertTrue(kernel.getProcessTable().isEmpty());
    }

    @Test
    @DisplayName("CreateProcess tạo và đăng ký process")
    void testCreateProcess() {
        Process p = kernel.createProcess("Test", 10, 1);

        assertNotNull(p);
        assertEquals(1, p.getPid());
        assertEquals("Test", p.getName());
        assertEquals(ProcessState.READY, p.getState());
        assertEquals(1, kernel.getProcessTable().size());
    }

    @Test
    @DisplayName("CreateProcess với PIDs tăng dần")
    void testIncrementingPids() {
        Process p1 = kernel.createProcess("P1", 10, 1);
        Process p2 = kernel.createProcess("P2", 10, 1);
        Process p3 = kernel.createProcess("P3", 10, 1);

        assertEquals(1, p1.getPid());
        assertEquals(2, p2.getPid());
        assertEquals(3, p3.getPid());
    }

    @Test
    @DisplayName("RunCycle thực thi một process")
    void testRunCycle() {
        kernel.createProcess("Test", 10, 1);

        kernel.runCycle();

        assertEquals(1, kernel.getDispatcher().getContextSwitchCount());
    }

    @Test
    @DisplayName("RunCycle với empty queue không gây lỗi")
    void testRunCycleEmptyQueue() {
        assertDoesNotThrow(() -> kernel.runCycle());
    }

    @Test
    @DisplayName("Process hoàn thành sau đủ cycles")
    void testProcessCompletion() {
        Process p = kernel.createProcess("Test", 4, 1);

        // Burst time = 4, quantum = 2 -> 2 cycles
        kernel.runCycle();
        assertEquals(2, p.getRemainingTime());

        kernel.runCycle();
        assertEquals(0, p.getRemainingTime());
        assertEquals(ProcessState.TERMINATED, p.getState());
    }

    @Test
    @DisplayName("Priority scheduling: high priority first")
    void testPriorityOrder() {
        // Higher number = Higher priority
        kernel.createProcess("Low", 10, 1);   // Priority 1 (lowest)
        Process pHigh = kernel.createProcess("High", 10, 5); // Priority 5 (highest)

        kernel.runCycle();

        // High priority should run first
        assertEquals(ProcessState.READY, pHigh.getState()); // Was running, now preempted
        assertEquals(8, pHigh.getRemainingTime()); // Executed 2ms
    }

    @Test
    @DisplayName("Memory swap khi quá giới hạn")
    void testMemorySwap() {
        kernel.createProcess("P1", 10, 1);
        kernel.createProcess("P2", 10, 2);
        kernel.createProcess("P3", 10, 3);
        kernel.createProcess("P4", 10, 4); // 4th process, should trigger swap

        // Max memory = 3, so one should be swapped
        assertEquals(1, kernel.getMemoryManager().getSwapUsage());
    }

    @Test
    @DisplayName("GetSimulationTime tăng sau mỗi cycle")
    void testSimulationTime() {
        kernel.createProcess("Test", 10, 1);

        assertEquals(0, kernel.getSimulationTime());

        kernel.runCycle();
        assertEquals(2, kernel.getSimulationTime());

        kernel.runCycle();
        assertEquals(4, kernel.getSimulationTime());
    }

    @Test
    @DisplayName("Multiple processes với priority khác nhau")
    void testMultipleProcessesPriority() {
        // Higher number = Higher priority
        Process p1 = kernel.createProcess("Priority3", 6, 3); // Highest priority (runs first)
        kernel.createProcess("Priority2", 4, 2);
        kernel.createProcess("Priority1", 2, 1); // Lowest priority

        // Run until p1 completes (priority 3 = highest, runs first)
        while (p1.getState() != ProcessState.TERMINATED) {
            kernel.runCycle();
        }

        // p1 should complete first (highest priority)
        assertTrue(p1.isCompleted());
        // Other processes should still have remaining time (might have run some cycles)
    }

    @Test
    @DisplayName("Kernel stop/isRunning")
    void testStopRunning() {
        assertFalse(kernel.isRunning());

        kernel.stop();
        assertFalse(kernel.isRunning());
    }
}
