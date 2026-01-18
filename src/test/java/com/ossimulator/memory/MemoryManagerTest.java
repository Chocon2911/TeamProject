package com.ossimulator.memory;

import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Memory Manager Tests")
class MemoryManagerTest {

    private MemoryManager memoryManager;
    private Map<Integer, Process> processTable;

    @BeforeEach
    void setUp() {
        processTable = new HashMap<>();
        memoryManager = new MemoryManager(3, processTable); // Max 3 processes in RAM
    }

    private Process createAndRegister(int pid, String name) {
        Process p = new Process(pid, name, 10, 1);
        processTable.put(pid, p);
        return p;
    }

    @Test
    @DisplayName("Load process vào memory thành công")
    void testLoadToMemory() {
        Process p = createAndRegister(1, "Test");

        boolean loaded = memoryManager.loadToMemory(p);

        assertTrue(loaded);
        assertTrue(memoryManager.isInMemory(1));
        assertEquals(1, memoryManager.getMemoryUsage());
    }

    @Test
    @DisplayName("Memory usage tăng đúng")
    void testMemoryUsageIncreases() {
        Process p1 = createAndRegister(1, "P1");
        Process p2 = createAndRegister(2, "P2");

        memoryManager.loadToMemory(p1);
        assertEquals(1, memoryManager.getMemoryUsage());

        memoryManager.loadToMemory(p2);
        assertEquals(2, memoryManager.getMemoryUsage());
    }

    @Test
    @DisplayName("Swap out khi memory đầy (LRU)")
    void testSwapOutWhenFull() {
        Process p1 = createAndRegister(1, "P1");
        Process p2 = createAndRegister(2, "P2");
        Process p3 = createAndRegister(3, "P3");
        Process p4 = createAndRegister(4, "P4");

        // Load 3 processes (full)
        p1.admit();
        p2.admit();
        p3.admit();
        p4.admit();

        memoryManager.loadToMemory(p1);
        memoryManager.loadToMemory(p2);
        memoryManager.loadToMemory(p3);

        assertEquals(3, memoryManager.getMemoryUsage());
        assertEquals(0, memoryManager.getSwapUsage());

        // Load p4 -> p1 should be swapped out (LRU)
        memoryManager.loadToMemory(p4);

        assertEquals(3, memoryManager.getMemoryUsage());
        assertEquals(1, memoryManager.getSwapUsage());
        assertTrue(memoryManager.isInSwap(1)); // p1 swapped
        assertTrue(memoryManager.isInMemory(4)); // p4 loaded
    }

    @Test
    @DisplayName("SwapOut thay đổi trạng thái process")
    void testSwapOutChangesState() {
        Process p = createAndRegister(1, "Test");
        p.admit(); // NEW -> READY
        memoryManager.loadToMemory(p);

        memoryManager.swapOut(p);

        assertEquals(ProcessState.SWAPPED_READY, p.getState());
        assertFalse(p.isInMainMemory());
        assertTrue(memoryManager.isInSwap(1));
    }

    @Test
    @DisplayName("SwapIn khôi phục process vào memory")
    void testSwapIn() {
        Process p = createAndRegister(1, "Test");
        p.admit();
        memoryManager.loadToMemory(p);
        memoryManager.swapOut(p);

        assertTrue(memoryManager.isInSwap(1));

        memoryManager.swapIn(p);

        assertTrue(memoryManager.isInMemory(1));
        assertFalse(memoryManager.isInSwap(1));
        assertEquals(ProcessState.READY, p.getState());
    }

    @Test
    @DisplayName("LRU: Process được access gần nhất không bị swap")
    void testLRUOrder() {
        Process p1 = createAndRegister(1, "P1");
        Process p2 = createAndRegister(2, "P2");
        Process p3 = createAndRegister(3, "P3");
        Process p4 = createAndRegister(4, "P4");

        p1.admit();
        p2.admit();
        p3.admit();
        p4.admit();

        memoryManager.loadToMemory(p1);
        memoryManager.loadToMemory(p2);
        memoryManager.loadToMemory(p3);

        // Access p1 again -> p1 moves to end of LRU queue
        memoryManager.updateLRU(1);

        // Load p4 -> p2 should be swapped (oldest in LRU)
        memoryManager.loadToMemory(p4);

        assertTrue(memoryManager.isInSwap(2)); // p2 swapped (oldest)
        assertTrue(memoryManager.isInMemory(1)); // p1 still in memory (recently accessed)
    }

    @Test
    @DisplayName("ReleaseMemory giải phóng memory")
    void testReleaseMemory() {
        Process p = createAndRegister(1, "Test");
        memoryManager.loadToMemory(p);

        assertEquals(1, memoryManager.getMemoryUsage());

        memoryManager.releaseMemory(p);

        assertEquals(0, memoryManager.getMemoryUsage());
        assertFalse(memoryManager.isInMemory(1));
    }

    @Test
    @DisplayName("Load process đã trong memory không duplicate")
    void testLoadAlreadyInMemory() {
        Process p = createAndRegister(1, "Test");

        memoryManager.loadToMemory(p);
        memoryManager.loadToMemory(p);
        memoryManager.loadToMemory(p);

        assertEquals(1, memoryManager.getMemoryUsage());
    }

    @Test
    @DisplayName("SwapOut process không trong memory fails")
    void testSwapOutNotInMemory() {
        Process p = createAndRegister(1, "Test");

        boolean result = memoryManager.swapOut(p);

        assertFalse(result);
    }

    @Test
    @DisplayName("Max memory slots được set đúng")
    void testMaxMemorySlots() {
        assertEquals(3, memoryManager.getMaxMemorySlots());
    }
}
