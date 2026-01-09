# Unit Testing Documentation

## 1. Tổng Quan

### 1.1 Unit Test là gì?

**Unit Test** (kiểm thử đơn vị) là phương pháp kiểm tra từng phần nhỏ nhất của code (thường là một method/function) để đảm bảo chúng hoạt động đúng một cách độc lập.

```
┌─────────────────────────────────────────────────────────┐
│                    TESTING PYRAMID                      │
│                                                         │
│                        /\                               │
│                       /  \       E2E Tests              │
│                      /    \      (ít tests)             │
│                     /______\                            │
│                    /        \    Integration Tests      │
│                   /          \   (một số tests)         │
│                  /____________\                         │
│                 /              \  Unit Tests            │
│                /________________\ (nhiều tests)         │
│                                                         │
│  Unit Tests: Nhanh, nhiều, kiểm tra từng component      │
└─────────────────────────────────────────────────────────┘
```

### 1.2 Tại Sao Cần Unit Test?

| Lý do | Giải thích |
|-------|------------|
| **Phát hiện bug sớm** | Tìm lỗi ngay khi viết code, không phải khi deploy |
| **Refactor an toàn** | Sửa code mà không lo phá vỡ chức năng cũ |
| **Documentation** | Tests cho thấy cách sử dụng code đúng |
| **Design tốt hơn** | Viết test buộc bạn thiết kế code dễ test hơn |

---

## 2. Công Nghệ Sử Dụng

### 2.1 JUnit 5

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

### 2.2 Cấu Trúc Test Folder

```
src/
├── main/java/com/ossimulator/    # Source code
│   ├── core/
│   ├── scheduler/
│   ├── memory/
│   ├── dispatcher/
│   └── kernel/
│
└── test/java/com/ossimulator/    # Test code (mirror structure)
    ├── core/
    │   ├── ProcessTest.java
    │   └── ProcessStateTest.java
    ├── scheduler/
    │   ├── RoundRobinSchedulerTest.java
    │   └── PrioritySchedulerTest.java
    ├── memory/
    │   ├── MemoryManagerTest.java
    │   └── SwapSpaceTest.java
    ├── dispatcher/
    │   └── DispatcherTest.java
    └── kernel/
        └── KernelTest.java
```

---

## 3. Giải Thích Từng Test Class

### 3.1 ProcessTest.java

Kiểm tra class `Process` - đơn vị cơ bản nhất của hệ thống.

| Test Method | Mục đích | Kỳ vọng |
|-------------|----------|---------|
| `testInitialState` | Kiểm tra khởi tạo | State = NEW, remaining = burst |
| `testAdmit` | NEW → READY | State chuyển đúng |
| `testDispatch` | READY → RUNNING | State chuyển đúng |
| `testPreempt` | RUNNING → READY | Bị preempt đúng |
| `testBlock` | RUNNING → WAITING | Block cho I/O đúng |
| `testUnblock` | WAITING → READY | Unblock đúng |
| `testTerminate` | → TERMINATED | Kết thúc đúng |
| `testSwapOut` | READY → SWAPPED_READY | Swap ra disk đúng |
| `testSwapIn` | SWAPPED → READY | Swap vào RAM đúng |
| `testExecute` | Thực thi | remainingTime giảm đúng |
| `testIsCompleted` | Hoàn thành | True khi remaining = 0 |

```java
@Test
@DisplayName("Execute giảm remainingTime đúng")
void testExecute() {
    Process p = new Process(1, "Test", 10, 1);
    int timeSlice = 3;

    int executed = p.execute(timeSlice);

    assertEquals(3, executed);           // Executed 3ms
    assertEquals(7, p.getRemainingTime()); // 10 - 3 = 7
    assertFalse(p.isCompleted());        // Chưa xong
}
```

### 3.2 ProcessStateTest.java

Kiểm tra enum `ProcessState` có đủ các trạng thái.

| Test | Mục đích |
|------|----------|
| `testAllStatesExist` | Đủ 7 trạng thái |
| `testBasicStates` | NEW, READY, RUNNING, WAITING, TERMINATED tồn tại |
| `testSwapStates` | SWAPPED_READY, SWAPPED_WAITING tồn tại |

---

### 3.3 RoundRobinSchedulerTest.java

Kiểm tra thuật toán Round Robin - mỗi process được chạy một time quantum.

| Test | Mục đích | Giải thích |
|------|----------|------------|
| `testInitialEmpty` | Queue rỗng ban đầu | Chưa add gì |
| `testTimeQuantum` | Time quantum đúng | Giá trị được set |
| `testAddProcess` | Thêm process | Queue không rỗng |
| `testSelectNextFIFO` | Lấy theo FIFO | First In First Out |
| `testRequeue` | Đưa về cuối | Sau khi hết quantum |
| `testMultipleRounds` | Nhiều vòng lặp | P1 → P2 → P1 → P2 |

```java
@Test
@DisplayName("Round Robin: nhiều vòng lặp")
void testMultipleRounds() {
    Process p1 = new Process(1, "P1", 6, 1);
    Process p2 = new Process(2, "P2", 4, 1);

    scheduler.addProcess(p1);
    scheduler.addProcess(p2);

    // Round 1: P1 chạy 2ms, quay lại cuối queue
    Process current = scheduler.selectNext().orElse(null);
    assertEquals(p1, current);
    current.execute(2);
    current.preempt();
    scheduler.requeue(current);

    // Round 1: P2 chạy 2ms
    current = scheduler.selectNext().orElse(null);
    assertEquals(p2, current);

    // Round 2: P1 lại (đã quay vòng)
    // ...
}
```

**Minh họa Round Robin:**

```
Time:   0    2    4    6    8
        ├────┼────┼────┼────┤
P1:     ████      ████      ████  (burst=6)
P2:          ████      ████       (burst=4)

Queue:  [P1,P2] → [P2,P1] → [P1,P2] → [P2,P1] → ...
```

---

### 3.4 PrioritySchedulerTest.java

Kiểm tra Priority Scheduler - process có priority cao chạy trước.

| Test | Mục đích |
|------|----------|
| `testSelectByPriority` | Priority 1 chạy trước priority 3 |
| `testRoundRobinWithinPriority` | Cùng priority thì FIFO |
| `testHighPriorityFirst` | High preempt low |
| `testChangePriority` | Đổi priority động |

```java
@Test
@DisplayName("SelectNext trả về process có priority cao nhất trước")
void testSelectByPriority() {
    Process pLow = new Process(1, "Low", 10, 3);    // Priority 3
    Process pHigh = new Process(2, "High", 10, 1);  // Priority 1
    Process pMedium = new Process(3, "Med", 10, 2); // Priority 2

    // Thêm theo thứ tự bất kỳ
    scheduler.addProcess(pLow);
    scheduler.addProcess(pHigh);
    scheduler.addProcess(pMedium);

    // Lấy ra theo priority (1 → 2 → 3)
    assertEquals(pHigh, scheduler.selectNext().orElse(null));
    assertEquals(pMedium, scheduler.selectNext().orElse(null));
    assertEquals(pLow, scheduler.selectNext().orElse(null));
}
```

**Minh họa Priority Queues:**

```
┌─────────────────────────────────────┐
│         PRIORITY QUEUES             │
├─────────────────────────────────────┤
│ Priority 1: [VSCode] ←── Cao nhất   │
│ Priority 2: [Chrome, Terminal]      │
│ Priority 3: [Spotify]               │
│ Priority 4: [Calculator] ←── Thấp   │
└─────────────────────────────────────┘

Thứ tự chạy: VSCode → Chrome → Terminal → Spotify → Calculator
```

---

### 3.5 MemoryManagerTest.java

Kiểm tra quản lý bộ nhớ với Swap In/Out.

| Test | Mục đích | Kỳ vọng |
|------|----------|---------|
| `testLoadToMemory` | Load vào RAM | isInMemory = true |
| `testSwapOutWhenFull` | RAM đầy → Swap | LRU victim bị swap |
| `testSwapOutChangesState` | State đổi | READY → SWAPPED_READY |
| `testSwapIn` | Đưa lại vào RAM | SWAPPED → READY |
| `testLRUOrder` | LRU đúng | Process cũ nhất bị swap |

```java
@Test
@DisplayName("Swap out khi memory đầy (LRU)")
void testSwapOutWhenFull() {
    // Max memory = 3
    Process p1 = createAndRegister(1, "P1");
    Process p2 = createAndRegister(2, "P2");
    Process p3 = createAndRegister(3, "P3");
    Process p4 = createAndRegister(4, "P4");

    // Load 3 processes (đầy)
    memoryManager.loadToMemory(p1);
    memoryManager.loadToMemory(p2);
    memoryManager.loadToMemory(p3);

    assertEquals(3, memoryManager.getMemoryUsage());
    assertEquals(0, memoryManager.getSwapUsage());

    // Load p4 → p1 bị swap (LRU - oldest)
    memoryManager.loadToMemory(p4);

    assertTrue(memoryManager.isInSwap(1));   // p1 → swap
    assertTrue(memoryManager.isInMemory(4)); // p4 → RAM
}
```

**Minh họa LRU Swap:**

```
Trước khi load P4:
┌─────────────────┐   ┌─────────────────┐
│      RAM        │   │      SWAP       │
│  P1, P2, P3     │   │     (empty)     │
│  (3/3 - FULL)   │   │                 │
└─────────────────┘   └─────────────────┘

Sau khi load P4 (P1 là LRU → swap out):
┌─────────────────┐   ┌─────────────────┐
│      RAM        │   │      SWAP       │
│  P2, P3, P4     │   │       P1        │
│  (3/3)          │   │                 │
└─────────────────┘   └─────────────────┘
```

---

### 3.6 SwapSpaceTest.java

Kiểm tra SwapSpace - lưu PIDs của processes bị swap.

| Test | Mục đích |
|------|----------|
| `testInitialEmpty` | Khởi tạo rỗng |
| `testAdd` | Thêm PID |
| `testRemove` | Xóa PID |
| `testGetPidsReturnsCopy` | Trả về bản copy |
| `testAddDuplicate` | Không duplicate |

---

### 3.7 DispatcherTest.java

Kiểm tra Dispatcher - chuyển đổi CPU giữa các processes.

| Test | Mục đích | Kỳ vọng |
|------|----------|---------|
| `testDispatchProcess` | Dispatch thành công | current = process, state = RUNNING |
| `testContextSwitchCount` | Đếm context switch | Tăng sau mỗi dispatch |
| `testDispatchReturnsPrevious` | Trả về previous | process trước đó |
| `testPreviousProcessPreempted` | Previous bị preempt | state = READY |

```java
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

    dispatcher.dispatch(p2);  // P1 bị preempt
    assertEquals(ProcessState.READY, p1.getState());   // Preempted
    assertEquals(ProcessState.RUNNING, p2.getState()); // Now running
}
```

**Minh họa Context Switch:**

```
┌─────────────────────────────────────────────────────────┐
│                  CONTEXT SWITCH                         │
│                                                         │
│  Before dispatch(P2):                                   │
│  ┌─────────┐                                            │
│  │   CPU   │ ← P1 (RUNNING)                             │
│  └─────────┘                                            │
│                                                         │
│  After dispatch(P2):                                    │
│  1. Save P1 context (PC, registers, stack)              │
│  2. P1.state = READY                                    │
│  3. Restore P2 context                                  │
│  4. P2.state = RUNNING                                  │
│  ┌─────────┐                                            │
│  │   CPU   │ ← P2 (RUNNING)                             │
│  └─────────┘                                            │
│                                                         │
│  P1 now in Ready Queue, waiting for next turn           │
└─────────────────────────────────────────────────────────┘
```

---

### 3.8 KernelTest.java

Kiểm tra Kernel - điều phối tất cả các components.

| Test | Mục đích |
|------|----------|
| `testInitialization` | Khởi tạo đúng |
| `testCreateProcess` | Tạo và đăng ký process |
| `testIncrementingPids` | PIDs tăng dần |
| `testRunCycle` | Thực thi một cycle |
| `testProcessCompletion` | Process hoàn thành |
| `testPriorityOrder` | Priority cao chạy trước |
| `testMemorySwap` | Swap khi RAM đầy |

```java
@Test
@DisplayName("Process hoàn thành sau đủ cycles")
void testProcessCompletion() {
    Process p = kernel.createProcess("Test", 4, 1);

    // Burst time = 4, quantum = 2 → cần 2 cycles
    kernel.runCycle();
    assertEquals(2, p.getRemainingTime());  // 4 - 2 = 2

    kernel.runCycle();
    assertEquals(0, p.getRemainingTime());  // 2 - 2 = 0
    assertEquals(ProcessState.TERMINATED, p.getState());
}
```

---

## 4. Cách Chạy Tests

### 4.1 Sử dụng Maven

```bash
# Chạy tất cả tests
mvn test

# Chạy test cụ thể
mvn test -Dtest=ProcessTest

# Chạy với verbose output
mvn test -X
```

### 4.2 Sử dụng IDE

- **IntelliJ**: Right-click test class → Run
- **Eclipse**: Right-click → Run As → JUnit Test
- **VS Code**: Click "Run Test" lens trên @Test method

---

## 5. Kết Quả Test

### 5.1 Tổng Kết

| Package | Test Class | Số Tests | Mô tả |
|---------|------------|----------|-------|
| `core` | ProcessTest | 14 | State transitions, execute |
| `core` | ProcessStateTest | 4 | Enum values |
| `scheduler` | RoundRobinSchedulerTest | 9 | FIFO, requeue |
| `scheduler` | PrioritySchedulerTest | 10 | Priority ordering |
| `memory` | MemoryManagerTest | 11 | RAM, Swap, LRU |
| `memory` | SwapSpaceTest | 8 | Basic operations |
| `dispatcher` | DispatcherTest | 8 | Context switch |
| `kernel` | KernelTest | 10 | Integration |
| **Total** | **8 classes** | **74 tests** | |

### 5.2 Coverage Mục Tiêu

```
┌─────────────────────────────────────────────────────────┐
│              TEST COVERAGE TARGET                       │
├─────────────────────────────────────────────────────────┤
│  Component          │  Target  │  Covered               │
├─────────────────────┼──────────┼────────────────────────┤
│  Process States     │  100%    │  All 7 states          │
│  State Transitions  │  100%    │  All transitions       │
│  Scheduler Logic    │  90%+    │  Core algorithms       │
│  Memory Management  │  90%+    │  Load, Swap, LRU       │
│  Dispatcher         │  85%+    │  Context switch        │
│  Kernel             │  80%+    │  Main flows            │
└─────────────────────────────────────────────────────────┘
```

---

## 6. Best Practices Đã Áp Dụng

### 6.1 Naming Convention

```java
// Pattern: test<Method>_<Scenario>_<Expected>
// Hoặc dùng @DisplayName cho readable name

@Test
@DisplayName("Process hoàn thành khi remainingTime = 0")
void testIsCompleted() { ... }
```

### 6.2 AAA Pattern

```java
@Test
void testExecute() {
    // Arrange - Setup
    Process p = new Process(1, "Test", 10, 1);
    int timeSlice = 3;

    // Act - Execute
    int executed = p.execute(timeSlice);

    // Assert - Verify
    assertEquals(3, executed);
    assertEquals(7, p.getRemainingTime());
}
```

### 6.3 One Assertion Per Test (khi có thể)

```java
@Test
void testAdmit() {
    process.admit();
    assertEquals(ProcessState.READY, process.getState());
}
```

### 6.4 Independence

- Mỗi test độc lập
- `@BeforeEach` setup fresh state
- Không phụ thuộc thứ tự chạy

---

## 7. Kết Luận

Unit tests trong project này:

1. **Cover các components chính**: Process, Scheduler, Memory, Dispatcher, Kernel
2. **Test các edge cases**: Empty queue, full memory, completed process
3. **Verify OS concepts**: State transitions, Round Robin, Priority, Swap
4. **Sử dụng JUnit 5**: Modern testing framework

Chạy tests giúp đảm bảo:
- Logic scheduling đúng
- State transitions hợp lệ
- Memory management hoạt động
- Không regression khi sửa code
