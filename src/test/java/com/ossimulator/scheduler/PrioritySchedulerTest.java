package com.ossimulator.scheduler;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Priority Scheduler Tests")
class PrioritySchedulerTest {

    private PriorityScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PriorityScheduler(2);
    }

    @Test
    @DisplayName("Scheduler khởi tạo với queue rỗng")
    void testInitialEmpty() {
        assertTrue(scheduler.isEmpty());
        assertEquals(0, scheduler.size());
    }

    @Test
    @DisplayName("Add process với các priority khác nhau")
    void testAddProcessWithPriority() {
        Process p1 = new Process(1, "Low", 10, 3);    // Priority 3
        Process p2 = new Process(2, "High", 10, 1);   // Priority 1
        Process p3 = new Process(3, "Medium", 10, 2); // Priority 2

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);
        scheduler.addProcess(p3);

        assertEquals(3, scheduler.size());
    }

    @Test
    @DisplayName("SelectNext trả về process có priority cao nhất trước")
    void testSelectByPriority() {
        Process pLow = new Process(1, "Low", 10, 3);
        Process pHigh = new Process(2, "High", 10, 1);
        Process pMedium = new Process(3, "Medium", 10, 2);

        // Thêm theo thứ tự ngẫu nhiên
        scheduler.addProcess(pLow);    // Priority 3
        scheduler.addProcess(pHigh);   // Priority 1 (cao nhất)
        scheduler.addProcess(pMedium); // Priority 2

        // Lấy ra theo priority
        assertEquals(pHigh, scheduler.selectNext().orElse(null));   // Priority 1
        assertEquals(pMedium, scheduler.selectNext().orElse(null)); // Priority 2
        assertEquals(pLow, scheduler.selectNext().orElse(null));    // Priority 3
    }

    @Test
    @DisplayName("Round Robin trong cùng priority level")
    void testRoundRobinWithinPriority() {
        Process p1 = new Process(1, "P1", 10, 2);
        Process p2 = new Process(2, "P2", 10, 2);
        Process p3 = new Process(3, "P3", 10, 2);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);
        scheduler.addProcess(p3);

        // FIFO trong cùng priority
        assertEquals(p1, scheduler.selectNext().orElse(null));
        assertEquals(p2, scheduler.selectNext().orElse(null));
        assertEquals(p3, scheduler.selectNext().orElse(null));
    }

    @Test
    @DisplayName("High priority preempt low priority")
    void testHighPriorityFirst() {
        Process pLow = new Process(1, "Low", 10, 5);
        Process pHigh = new Process(2, "High", 10, 1);

        scheduler.addProcess(pLow);
        scheduler.addProcess(pHigh);

        // High priority được chọn trước dù add sau
        assertEquals(pHigh, scheduler.selectNext().orElse(null));
        assertEquals(pLow, scheduler.selectNext().orElse(null));
    }

    @Test
    @DisplayName("Requeue giữ nguyên priority")
    void testRequeueMaintainsPriority() {
        Process pHigh = new Process(1, "High", 10, 1);
        Process pLow = new Process(2, "Low", 10, 3);

        scheduler.addProcess(pHigh);
        scheduler.addProcess(pLow);

        // Lấy high priority
        Process selected = scheduler.selectNext().orElse(null);
        assertEquals(pHigh, selected);

        // Preempt và requeue
        pHigh.preempt();
        scheduler.requeue(pHigh);

        // pHigh vẫn được ưu tiên hơn pLow
        assertEquals(pHigh, scheduler.selectNext().orElse(null));
    }

    @Test
    @DisplayName("ChangePriority di chuyển process sang queue mới")
    void testChangePriority() {
        Process p1 = new Process(1, "P1", 10, 3);
        Process p2 = new Process(2, "P2", 10, 3);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);

        // Thay đổi p2 lên priority cao hơn
        scheduler.changePriority(p2, 1);

        // p2 được chọn trước vì priority cao hơn
        assertEquals(p2, scheduler.selectNext().orElse(null));
        assertEquals(p1, scheduler.selectNext().orElse(null));
    }

    @Test
    @DisplayName("Priority bị giới hạn trong khoảng 1-10")
    void testPriorityBounds() {
        Process pZero = new Process(1, "Zero", 10, 0);  // Sẽ thành 1
        Process pHigh = new Process(2, "High", 10, 15); // Sẽ thành 10

        scheduler.addProcess(pZero);
        scheduler.addProcess(pHigh);

        // pZero (clamped to 1) có priority cao hơn pHigh (clamped to 10)
        assertEquals(pZero, scheduler.selectNext().orElse(null));
    }

    @Test
    @DisplayName("SelectNext trả về empty khi tất cả queues rỗng")
    void testSelectNextAllEmpty() {
        Optional<Process> result = scheduler.selectNext();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Mixed priority scheduling simulation")
    void testMixedPrioritySimulation() {
        Process p1 = new Process(1, "Chrome", 6, 2);
        Process p2 = new Process(2, "VSCode", 4, 1);   // Highest
        Process p3 = new Process(3, "Spotify", 8, 3);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);
        scheduler.addProcess(p3);

        // First round: VSCode (priority 1)
        Process current = scheduler.selectNext().orElse(null);
        assertEquals("VSCode", current.getName());
        current.execute(2);
        current.preempt();
        scheduler.requeue(current);

        // VSCode vẫn được chọn (priority 1)
        current = scheduler.selectNext().orElse(null);
        assertEquals("VSCode", current.getName());
        current.execute(2);
        // VSCode completed
        assertTrue(current.isCompleted());

        // Now Chrome (priority 2)
        current = scheduler.selectNext().orElse(null);
        assertEquals("Chrome", current.getName());
    }
}
