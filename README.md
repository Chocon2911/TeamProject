# OS Kernel Simulator

A simulation of basic Operating System kernel operations including:
- Process scheduling (Round Robin + Priority)
- Dispatching and context switching
- Memory management with Swap In/Out
- Multi-threading demonstration

## Project Structure

```
os-simulator/
├── pom.xml                              # Maven configuration
├── README.md                            # This file
├── docs/
│   ├── 1_plan.md                        # Project plan
│   └── 2_implementation.md              # Implementation details
└── src/main/java/com/ossimulator/
    ├── Main.java                        # Entry point
    ├── core/                            # Core domain models
    │   ├── Process.java
    │   ├── ProcessState.java
    │   └── ProcessControlBlock.java
    ├── memory/                          # Memory management
    │   ├── MemoryManager.java
    │   └── SwapSpace.java
    ├── scheduler/                       # Scheduling algorithms
    │   ├── Scheduler.java               # Interface
    │   ├── RoundRobinScheduler.java
    │   └── PriorityScheduler.java
    ├── dispatcher/                      # CPU dispatching
    │   └── Dispatcher.java
    ├── kernel/                          # Kernel coordinator
    │   └── Kernel.java
    └── thread/                          # Multi-threading demo
        ├── SchedulerThread.java
        └── MonitorThread.java
```

## Requirements

- Java 17 or higher
- Maven 3.8+ (optional, for building)

## How to Build and Run

### Option 1: Using Maven

```bash
# Build
mvn clean compile

# Run
mvn exec:java

# Or build JAR and run
mvn clean package
java -jar target/os-simulator-1.0.0.jar
```

### Option 2: Using javac directly

```bash
# Compile
cd src/main/java
javac -d ../../../target/classes com/ossimulator/**/*.java

# Run
cd ../../../target/classes
java com.ossimulator.Main
```

### Option 3: Run Visual Simulator

```bash
# Run the FXGL visualizer
mvn exec:java -Pvisual
```

### Option 4: Using IDE

1. Open project in IntelliJ IDEA / Eclipse / VS Code
2. Import as Maven project
3. Run `Main.java` for console or `VisualMain.java` for visualizer

## Features Demonstrated

1. **Process States**: NEW → READY → RUNNING → BLOCKED → TERMINATED
2. **Priority Scheduling**: Higher priority number = runs first (4★ > 3★ > 2★ > 1★)
3. **Round Robin**: Equal time quantum for same priority level
4. **Context Switching**: Save/restore CPU state when switching processes
5. **Memory Management**: Swap out LRU processes when RAM is full
6. **Multi-threading**: Two threads running concurrently:
   - SchedulerThread: Runs scheduling cycles
   - MonitorThread: Displays system status
7. **Visual Simulator**: FXGL-based visualizer showing:
   - Process spawning and queue animations
   - Dual-core CPU with dispatch animations
   - Pulsating CPU when executing
   - Particle effects on termination

## Sample Output

```
+===========================================================+
|              OS KERNEL SIMULATOR                          |
|    Round Robin + Priority Scheduling with Swap            |
+===========================================================+

=== Creating Processes ===

  [Memory] Loaded P1 to RAM. Memory: 1/3
[Kernel] Created process: Process[pid=1, name=Chrome, state=READY, priority=2, remaining=10]
...

=== Starting Multi-threaded Demo ===

[SchedulerThread] Started - Thread ID: 14
[MonitorThread] Started - Thread ID: 15

------------------------------------------
  DISPATCH #1
  Current:  P2 (VSCode) -> RUNNING
------------------------------------------
[Kernel] P2 executed for 2ms (remaining: 6ms)

+=========================================+
|           SYSTEM MONITOR                |
|   Thread ID: 15                         |
+-----------------------------------------+
|  Ready Queue Size: 4                    |
|  Memory Usage: 3/3                      |
|  Swap Usage: 2                          |
+=========================================+
...
```

## Course Information

- Course: CS4448 - Operating Systems
- Project: Implementing a Simulation of basic operations of Operating System
