# Kế Hoạch Dự Án: Mô Phỏng Hệ Điều Hành

## 1. Tổng Quan Dự Án

### 1.1 Mục Tiêu
Xây dựng chương trình mô phỏng các hoạt động cơ bản của **kernel** (nhân) hệ điều hành, bao gồm:
- **Dispatching**: Phân phối CPU cho tiến trình
- **Scheduling**: Lập lịch tiến trình (Round Robin + Priority)
- **Process State Transition**: Chuyển đổi trạng thái tiến trình
- **Multi-threading**: Chạy đồng thời nhiều luồng

### 1.2 Giải Thích Thuật Ngữ Cơ Bản

| Thuật Ngữ | Giải Thích Đơn Giản |
|-----------|---------------------|
| **Process (Tiến trình)** | Một chương trình đang chạy. Ví dụ: Chrome, Word đều là các tiến trình |
| **Thread (Luồng)** | Đơn vị thực thi nhỏ hơn trong tiến trình. Một tiến trình có thể có nhiều luồng |
| **CPU** | Bộ xử lý trung tâm - "bộ não" của máy tính, thực hiện các phép tính |
| **Kernel** | Phần lõi của hệ điều hành, quản lý tài nguyên máy tính |
| **Scheduler** | Bộ lập lịch - quyết định tiến trình nào được chạy tiếp theo |
| **Dispatcher** | Bộ phân phối - thực hiện việc chuyển CPU từ tiến trình này sang tiến trình khác |
| **Round Robin** | Thuật toán lập lịch: mỗi tiến trình được chạy một khoảng thời gian bằng nhau |
| **Priority** | Lập lịch theo độ ưu tiên: tiến trình quan trọng hơn chạy trước |

---

## 2. Kiến Trúc Tổng Quan (Top-Down)

### 2.1 Sơ Đồ Mức Cao Nhất

```mermaid
graph TB
    subgraph "OS Simulator"
        A[Main Application] --> B[Process Manager]
        A --> C[Scheduler]
        A --> D[Dispatcher]
        A --> E[Thread Demo]
    end

    B --> F[Process Pool]
    C --> G[Ready Queue]
    D --> H[CPU Execution]
    E --> I[Thread 1: Scheduler]
    E --> J[Thread 2: Monitor]
```

### 2.2 Luồng Hoạt Động Chính

```mermaid
flowchart LR
    A[Tạo Process] --> B[Thêm vào Ready Queue]
    B --> C[Scheduler chọn Process]
    C --> D[Dispatcher gán CPU]
    D --> E{Hết Time Quantum?}
    E -->|Có| F[Quay lại Queue]
    E -->|Chưa| G{Hoàn thành?}
    G -->|Có| H[Terminated]
    G -->|Chưa| D
    F --> C
```

---

## 3. Trạng Thái Tiến Trình (Process States)

### 3.1 Sơ Đồ Chuyển Trạng Thái

```mermaid
stateDiagram-v2
    [*] --> NEW : Tạo tiến trình
    NEW --> READY : Admit
    READY --> RUNNING : Dispatch
    RUNNING --> READY : Timeout/Preempt
    RUNNING --> WAITING : I/O Request
    WAITING --> READY : I/O Complete
    RUNNING --> TERMINATED : Exit
    TERMINATED --> [*]
```

### 3.2 Bảng Mô Tả Trạng Thái

| Trạng Thái | Mô Tả | Ví Dụ Thực Tế |
|------------|-------|---------------|
| **NEW** | Tiến trình vừa được tạo | Bạn vừa click mở Chrome |
| **READY** | Sẵn sàng chạy, đang chờ CPU | Chrome đã load xong, chờ đến lượt |
| **RUNNING** | Đang được CPU thực thi | Chrome đang xử lý trang web |
| **WAITING** | Đang chờ I/O (đọc file, mạng) | Chrome đang tải hình ảnh từ mạng |
| **TERMINATED** | Đã kết thúc | Bạn đóng Chrome |

---

## 4. Thuật Toán Lập Lịch

### 4.1 Round Robin với Priority

```mermaid
flowchart TB
    subgraph "Priority Queues"
        Q1[Queue Priority 1 - Cao nhất]
        Q2[Queue Priority 2]
        Q3[Queue Priority 3 - Thấp nhất]
    end

    subgraph "Round Robin trong mỗi Queue"
        RR1[P1 → P2 → P3 → P1...]
    end

    Q1 --> RR1
    Q2 --> RR1
    Q3 --> RR1

    RR1 --> CPU[CPU Execution]
```

### 4.2 Ví Dụ Hoạt Động

| Thời điểm | Process | Priority | Trạng thái | Time Quantum |
|-----------|---------|----------|------------|--------------|
| T=0 | P1 | 1 | RUNNING | 2ms |
| T=2 | P2 | 1 | RUNNING | 2ms |
| T=4 | P1 | 1 | RUNNING | 2ms |
| T=6 | P3 | 2 | RUNNING | 2ms |

---

## 5. Cấu Trúc Dự Án

### 5.1 Cấu Trúc Thư Mục

```
os-simulator/
├── pom.xml                          # Maven build file
├── README.md
├── docs/
│   ├── 1_plan.md                    # File này
│   ├── 2_class_diagram.md
│   └── 3_solaris_description.md
└── src/
    └── main/
        └── java/
            └── com/
                └── ossimulator/
                    ├── Main.java
                    ├── core/
                    │   ├── Process.java
                    │   ├── ProcessState.java
                    │   └── ProcessControlBlock.java
                    ├── scheduler/
                    │   ├── Scheduler.java
                    │   ├── RoundRobinScheduler.java
                    │   └── PriorityScheduler.java
                    ├── dispatcher/
                    │   └── Dispatcher.java
                    ├── queue/
                    │   └── ReadyQueue.java
                    └── thread/
                        ├── SchedulerThread.java
                        └── MonitorThread.java
```

### 5.2 Giải Thích Cấu Trúc

| Thư mục/File | Mục đích |
|--------------|----------|
| `pom.xml` | Cấu hình Maven - quản lý build và dependencies |
| `core/` | Các class cốt lõi: Process, trạng thái |
| `scheduler/` | Các thuật toán lập lịch |
| `dispatcher/` | Chuyển CPU giữa các process |
| `queue/` | Hàng đợi Ready |
| `thread/` | Demo chạy đa luồng |

---

## 6. Thiết Kế Class (Class Diagram)

```mermaid
classDiagram
    class Process {
        -int pid
        -String name
        -ProcessState state
        -int priority
        -int burstTime
        -int remainingTime
        +run()
        +setState()
    }

    class Scheduler {
        <<interface>>
        +selectNext() Process
        +addProcess(Process)
    }

    class RoundRobinScheduler {
        -int timeQuantum
        -Queue~Process~ readyQueue
        +selectNext() Process
    }

    class PriorityScheduler {
        -Map~Integer, Queue~ priorityQueues
        +selectNext() Process
    }

    class Dispatcher {
        -Process currentProcess
        +dispatch(Process)
        +contextSwitch()
    }

    Scheduler <|.. RoundRobinScheduler
    Scheduler <|.. PriorityScheduler
    Dispatcher --> Process
    RoundRobinScheduler --> Process
    PriorityScheduler --> Process
```

---

## 7. Về Việc Chia Module/Repository

### 7.1 Khuyến Nghị: **KHÔNG CẦN** chia thành nhiều repository

| Lựa chọn | Ưu điểm | Nhược điểm | Khuyến nghị |
|----------|---------|------------|-------------|
| **1 Repository duy nhất** | Đơn giản, dễ quản lý, phù hợp dự án học tập | Ít modular | ✅ **Chọn cái này** |
| Nhiều Repository | Modular hơn | Phức tạp, overkill cho dự án này | ❌ |

### 7.2 Về Maven Modules

```
os-simulator/                    # Single module project
├── pom.xml                      # Parent POM
└── src/...
```

**Lý do:**
- Dự án có scope vừa phải
- Các thành phần liên kết chặt chẽ
- Dễ build và test
- Phù hợp yêu cầu môn học

---

## 8. Kế Hoạch Thực Hiện

### 8.1 Các Bước Triển Khai

```mermaid
flowchart TB
    subgraph "Phase 1: Nền tảng"
        A1[Tạo Maven Project]
        A2[Định nghĩa Process, State]
        A3[Tạo ProcessControlBlock]
    end

    subgraph "Phase 2: Lập lịch"
        B1[Implement ReadyQueue]
        B2[Round Robin Scheduler]
        B3[Priority Scheduler]
    end

    subgraph "Phase 3: Dispatcher"
        C1[Implement Dispatcher]
        C2[Context Switch Logic]
    end

    subgraph "Phase 4: Multi-thread"
        D1[SchedulerThread]
        D2[MonitorThread]
        D3[Demo 2 threads]
    end

    subgraph "Phase 5: Hoàn thiện"
        E1[Test & Debug]
        E2[Documentation]
        E3[Class Diagram]
    end

    A1 --> A2 --> A3 --> B1 --> B2 --> B3 --> C1 --> C2 --> D1 --> D2 --> D3 --> E1 --> E2 --> E3
```

### 8.2 Bảng Chi Tiết Công Việc

| Phase | Task | Mô tả | Output |
|-------|------|-------|--------|
| **1** | Setup Maven | Tạo project structure | `pom.xml`, folders |
| **1** | Core classes | Process, ProcessState enum | Java files |
| **2** | Ready Queue | Hàng đợi FIFO các process | `ReadyQueue.java` |
| **2** | Round Robin | Thuật toán RR với time quantum | `RoundRobinScheduler.java` |
| **2** | Priority | Lập lịch theo priority | `PriorityScheduler.java` |
| **3** | Dispatcher | Logic dispatch và context switch | `Dispatcher.java` |
| **4** | Threads | 2 threads chạy song song | Thread demo |
| **5** | Docs | Class diagram, mô tả Solaris | Markdown files |

---

## 9. Yêu Cầu Kỹ Thuật

### 9.1 Công Nghệ Sử Dụng

| Thành phần | Công nghệ | Phiên bản |
|------------|-----------|-----------|
| Ngôn ngữ | Java | 11+ |
| Build tool | Maven | 3.8+ |
| IDE | IntelliJ / Eclipse / VS Code | Any |

### 9.2 Dependencies (pom.xml)

```xml
<!-- Không cần dependencies bên ngoài cho dự án này -->
<!-- Java core là đủ -->
```

---

## 10. Deliverables (Sản Phẩm Bàn Giao)

| # | Sản phẩm | Mô tả |
|---|----------|-------|
| 1 | Source Code | Java code hoàn chỉnh |
| 2 | Class Diagram | Sơ đồ UML các class |
| 3 | Solaris Description | Mô tả chi tiết về Solaris |
| 4 | Demo | Video/Screenshot chạy 2 threads |
| 5 | README | Hướng dẫn build và chạy |

---

## 11. Tóm Tắt

```mermaid
mindmap
  root((OS Simulator))
    Core
      Process
      ProcessState
      PCB
    Scheduling
      Round Robin
      Priority
      Ready Queue
    Dispatching
      Context Switch
      CPU Assignment
    Threading
      SchedulerThread
      MonitorThread
    Documentation
      Class Diagram
      Solaris Desc
```

**Kết luận:**
- Sử dụng **1 Maven project duy nhất** (không cần chia repo)
- Cấu trúc package rõ ràng theo chức năng
- Java thuần, không cần dependencies ngoài
- Focus vào simulation logic và multi-threading demo
