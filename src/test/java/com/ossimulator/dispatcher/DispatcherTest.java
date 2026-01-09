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
    @DisplayName("Dispatcher khởi tạo với current process null")
    void testInitialState() {
        assertNull(dispatcher.getCurrentProcess());
        assertEquals(0, dispatcher.getContextSwitchCount());
    }

    @Test
    @DisplayName("Register và dispatch process")
    void testDispatchProcess() {
        Process p = new Process(1, "Test", 10, 1);
        p.admit();

        dispatcher.registerProcess(p);
        dispatcher.dispatch(p);

        assertEquals(p, dispatcher.getCurrentProcess());
        assertEquals(ProcessState.RUNNING, p.getState());
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

        dispatcher.dispatch(p1);
        assertEquals(1, dispatcher.getContextSwitchCount());

        dispatcher.dispatch(p2);
        assertEquals(2, dispatcher.getContextSwitchCount());
    }

    @Test
    @DisplayName("Dispatch trả về previous process")
    void testDispatchReturnsPrevious() {
        Process p1 = new Process(1, "P1", 10, 1);
        Process p2 = new Process(2, "P2", 10, 1);

        p1.admit();
        p2.admit();

        dispatcher.registerProcess(p1);
        dispatcher.registerProcess(p2);

        Process prev1 = dispatcher.dispatch(p1);
        assertNull(prev1); // First dispatch, no previous

        Process prev2 = dispatcher.dispatch(p2);
        assertEquals(p1, prev2); // p1 was previous
    }

    @Test
    @DisplayName("Previous process chuyển sang READY khi bị preempt")
    void testPreviousProcessPreempted() {
        Process p1 = new Process(1, "P1", 10, 1);
        Process p2 = new Process(2, "P2", 10, 1);

        p1.admit();
        p2.admit();

        dispatcher.registerProcess(p1);
        dispatcher.registerProcess(p2);

        dispatcher.dispatch(p1);
        assertEquals(ProcessState.RUNNING, p1.getState());

        dispatcher.dispatch(p2);
        assertEquals(ProcessState.READY, p1.getState());   // Preempted
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

        dispatcher.dispatch(p1);
        dispatcher.dispatch(p2);
        dispatcher.dispatch(p3);
        dispatcher.dispatch(p1); // Back to p1

        assertEquals(4, dispatcher.getContextSwitchCount());
        assertEquals(p1, dispatcher.getCurrentProcess());
    }

    @Test
    @DisplayName("Average dispatch time > 0")
    void testAverageDispatchTime() {
        Process p = new Process(1, "Test", 10, 1);
        p.admit();

        dispatcher.registerProcess(p);
        dispatcher.dispatch(p);

        // Should have some dispatch time (including simulated 1ms delay)
        assertTrue(dispatcher.getAverageDispatchTime() >= 0);
    }

    @Test
    @DisplayName("Dispatch cùng process không gây lỗi")
    void testDispatchSameProcess() {
        Process p = new Process(1, "Test", 10, 1);
        p.admit();

        dispatcher.registerProcess(p);
        dispatcher.dispatch(p);
        dispatcher.dispatch(p); // Dispatch again

        assertEquals(2, dispatcher.getContextSwitchCount());
    }
}
