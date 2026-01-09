package com.ossimulator.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SwapSpace Tests")
class SwapSpaceTest {

    private SwapSpace swapSpace;

    @BeforeEach
    void setUp() {
        swapSpace = new SwapSpace();
    }

    @Test
    @DisplayName("SwapSpace khởi tạo rỗng")
    void testInitialEmpty() {
        assertEquals(0, swapSpace.size());
        assertTrue(swapSpace.getPids().isEmpty());
    }

    @Test
    @DisplayName("Add PID vào swap space")
    void testAdd() {
        swapSpace.add(1);

        assertEquals(1, swapSpace.size());
        assertTrue(swapSpace.contains(1));
    }

    @Test
    @DisplayName("Add nhiều PIDs")
    void testAddMultiple() {
        swapSpace.add(1);
        swapSpace.add(2);
        swapSpace.add(3);

        assertEquals(3, swapSpace.size());
        assertTrue(swapSpace.contains(1));
        assertTrue(swapSpace.contains(2));
        assertTrue(swapSpace.contains(3));
    }

    @Test
    @DisplayName("Remove PID khỏi swap space")
    void testRemove() {
        swapSpace.add(1);
        swapSpace.add(2);

        swapSpace.remove(1);

        assertEquals(1, swapSpace.size());
        assertFalse(swapSpace.contains(1));
        assertTrue(swapSpace.contains(2));
    }

    @Test
    @DisplayName("Contains trả về false cho PID không tồn tại")
    void testContainsNonExistent() {
        assertFalse(swapSpace.contains(999));
    }

    @Test
    @DisplayName("GetPids trả về copy của set")
    void testGetPidsReturnsCopy() {
        swapSpace.add(1);
        swapSpace.add(2);

        Set<Integer> pids = swapSpace.getPids();
        pids.add(3); // Modify returned set

        // Original should not be affected
        assertEquals(2, swapSpace.size());
        assertFalse(swapSpace.contains(3));
    }

    @Test
    @DisplayName("Add duplicate PID không tăng size")
    void testAddDuplicate() {
        swapSpace.add(1);
        swapSpace.add(1);
        swapSpace.add(1);

        assertEquals(1, swapSpace.size());
    }

    @Test
    @DisplayName("Remove PID không tồn tại không gây lỗi")
    void testRemoveNonExistent() {
        assertDoesNotThrow(() -> swapSpace.remove(999));
    }
}
