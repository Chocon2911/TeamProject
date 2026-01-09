package com.ossimulator.scheduler;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoundRobin Scheduler Tests")
class RoundRobinSchedulerTest {

    private RoundRobinScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RoundRobinScheduler(2); // time quantum = 2
    }

    @Test
    @DisplayName("Scheduler khởi tạo với queue rỗng")
    void testInitialEmpty() {
        assertTrue(scheduler.isEmpty());
        assertEquals(0, scheduler.size());
    }

    @Test
    @DisplayName("Time quantum được set đúng")
    void testTimeQuantum() {
        assertEquals(2, scheduler.getTimeQuantum());
    }

    @Test
    @DisplayName("Tên scheduler đúng format")
    void testName() {
        assertEquals("Round Robin (quantum=2ms)", scheduler.getName());
    }

    @Test
    @DisplayName("Add process và queue không rỗng")
    void testAddProcess() {
        Process p = new Process(1, "Test", 10, 1);
        scheduler.addProcess(p);

        assertFalse(scheduler.isEmpty());
        assertEquals(1, scheduler.size());
    }

    @Test
    @DisplayName("Add process tự động admit (NEW -> READY)")
    void testAddProcessAutoAdmit() {
        Process p = new Process(1, "Test", 10, 1);
        assertEquals(ProcessState.NEW, p.getState());

        scheduler.addProcess(p);
        assertEquals(ProcessState.READY, p.getState());
    }

    @Test
    @DisplayName("SelectNext trả về process theo FIFO")
    void testSelectNextFIFO() {
        Process p1 = new Process(1, "First", 10, 1);
        Process p2 = new Process(2, "Second", 10, 1);
        Process p3 = new Process(3, "Third", 10, 1);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);
        scheduler.addProcess(p3);

        assertEquals(p1, scheduler.selectNext().orElse(null));
        assertEquals(p2, scheduler.selectNext().orElse(null));
        assertEquals(p3, scheduler.selectNext().orElse(null));
    }

    @Test
    @DisplayName("SelectNext trả về empty khi queue rỗng")
    void testSelectNextEmpty() {
        Optional<Process> result = scheduler.selectNext();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Requeue đưa process về cuối queue")
    void testRequeue() {
        Process p1 = new Process(1, "First", 10, 1);
        Process p2 = new Process(2, "Second", 10, 1);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);

        // Lấy p1 ra
        Process selected = scheduler.selectNext().orElse(null);
        assertEquals(p1, selected);

        // Giả lập preempt và requeue
        p1.preempt();
        scheduler.requeue(p1);

        // Bây giờ p2 trước, p1 sau
        assertEquals(p2, scheduler.selectNext().orElse(null));
        assertEquals(p1, scheduler.selectNext().orElse(null));
    }

    @Test
    @DisplayName("Requeue không thêm process đã completed")
    void testRequeueCompletedProcess() {
        Process p = new Process(1, "Test", 5, 1);
        scheduler.addProcess(p);

        // Lấy process ra khỏi queue
        scheduler.selectNext();

        // Execute hết
        p.execute(5);
        assertTrue(p.isCompleted());

        // Requeue không thêm vào vì đã completed
        scheduler.requeue(p);

        // Queue vẫn rỗng
        assertTrue(scheduler.isEmpty());
    }

    @Test
    @DisplayName("Round Robin: nhiều vòng lặp")
    void testMultipleRounds() {
        Process p1 = new Process(1, "P1", 6, 1);
        Process p2 = new Process(2, "P2", 4, 1);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);

        // Round 1: p1
        Process current = scheduler.selectNext().orElse(null);
        assertEquals(p1, current);
        current.execute(2);
        current.preempt();
        scheduler.requeue(current);

        // Round 1: p2
        current = scheduler.selectNext().orElse(null);
        assertEquals(p2, current);
        current.execute(2);
        current.preempt();
        scheduler.requeue(current);

        // Round 2: p1
        current = scheduler.selectNext().orElse(null);
        assertEquals(p1, current);

        // Round 2: p2
        current = scheduler.selectNext().orElse(null);
        assertEquals(p2, current);
    }
}
