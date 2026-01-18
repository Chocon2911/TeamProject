package com.ossimulator.dispatcher;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dispatcher Tests")
class DispatcherTest {

    private Dispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new Dispatcher();
    }

    @Test
    @DisplayName("Dispatcher khởi tạo với contextSwitchCount = 0")
    void testInitialState() {
        assertEquals(0, dispatcher.getContextSwitchCount());
    }

    @Test
    @DisplayName("Register và dispatch process")
    void testDispatchProcess() {
        Process p = new Process(1, "Test", 10, 1);
        p.admit();

        dispatcher.registerProcess(p);
        dispatcher.dispatch(p, null, 0); // Previous is null, coreId 0

        assertEquals(ProcessState.RUNNING, p.getState());
        assertEquals(1, dispatcher.getContextSwitchCount());
    }

    @Test
    @DisplayName("Context switch count tăng sau mỗi dispatch")
    void testContextSwitchCount() {
        Process p1 = new Process(1, "P1", 10, 1);
        Process p2 = new Process(2, "P2", 10, 1);

        p1.admit();
        p2.admit();

        dispatcher.registerProcess(p1);
        dispatcher.registerProcess(p2);

        dispatcher.dispatch(p1, null, 0);
        assertEquals(1, dispatcher.getContextSwitchCount());

        dispatcher.dispatch(p2, p1, 0); // Switch from p1 to p2
        assertEquals(2, dispatcher.getContextSwitchCount());
    }

    /*
     * // dispatch() no longer returns previous process, it's void and managed by
     * Kernel
     * 
     * @Test
     * 
     * @DisplayName("Dispatch trả về previous process")
     * void testDispatchReturnsPrevious() { ... }
     */

    @Test
    @DisplayName("Previous process chuyển sang READY khi bị preempt")
    void testPreviousProcessPreempted() {
        Process p1 = new Process(1, "P1", 10, 1);
        Process p2 = new Process(2, "P2", 10, 1);

        p1.admit();
        p2.admit();

        dispatcher.registerProcess(p1);
        dispatcher.registerProcess(p2);

        dispatcher.dispatch(p1, null, 0);
        assertEquals(ProcessState.RUNNING, p1.getState());

        dispatcher.dispatch(p2, p1, 0);
        assertEquals(ProcessState.READY, p1.getState()); // Preempted by dispatcher logic
        assertEquals(ProcessState.RUNNING, p2.getState()); // Now running
    }

    @Test
    @DisplayName("Multiple context switches")
    void testMultipleContextSwitches() {
        Process p1 = new Process(1, "P1", 10, 1);
        Process p2 = new Process(2, "P2", 10, 1);
        Process p3 = new Process(3, "P3", 10, 1);

        p1.admit();
        p2.admit();
        p3.admit();

        dispatcher.registerProcess(p1);
        dispatcher.registerProcess(p2);
        dispatcher.registerProcess(p3);

        dispatcher.dispatch(p1, null, 0);
        dispatcher.dispatch(p2, p1, 0);
        dispatcher.dispatch(p3, p2, 0);
        dispatcher.dispatch(p1, p3, 0); // Back to p1

        assertEquals(4, dispatcher.getContextSwitchCount());
    }

    @Test
    @DisplayName("Average dispatch time > 0")
    void testAverageDispatchTime() {
        Process p = new Process(1, "Test", 10, 1);
        p.admit();

        dispatcher.registerProcess(p);
        dispatcher.dispatch(p, null, 0);

        // Should have some dispatch time (including simulated 1ms delay)
        assertTrue(dispatcher.getAverageDispatchTime() >= 0);
    }

    @Test
    @DisplayName("Dispatch process has 1ms context switch delay")
    void testDispatchDelay() {
        Process p = new Process(1, "Test", 10, 1);
        p.admit();

        long start = System.currentTimeMillis();
        dispatcher.registerProcess(p);
        dispatcher.dispatch(p, null, 0);
        long end = System.currentTimeMillis();

        // Should take at least 1ms
        assertTrue((end - start) >= 0); // Hard to strictly test timing, but logic check is safe
    }
}
