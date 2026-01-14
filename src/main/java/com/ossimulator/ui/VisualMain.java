package com.ossimulator.ui;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.ossimulator.core.Process;
import com.ossimulator.kernel.Kernel;
import com.ossimulator.kernel.Kernel.CoreStepResult;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.ossimulator.ui.UIConfig.*;

public class VisualMain extends GameApplication {

    private static final int TIME_QUANTUM = 2;
    private static final int NUM_CORES = 2;
    private static final int MAX_MEMORY = 3; // Match Main.java

    // Animation timing
    private static final double MOVE_DURATION = 0.7;
    private static final double CYCLE_INTERVAL = 2.0;
    private static final double SPAWN_STAGGER = 0.5;

    private Kernel kernel;

    // Animation system
    private static class ActiveAnimation {
        final Entity entity;
        final Point2D start, end;
        final double duration;
        double elapsed = 0;
        final Runnable onComplete;

        ActiveAnimation(Entity entity, Point2D start, Point2D end, double duration, Runnable onComplete) {
            this.entity = entity;
            this.start = start;
            this.end = end;
            this.duration = duration;
            this.onComplete = onComplete;
        }
    }

    private final List<ActiveAnimation> activeAnimations = new ArrayList<>();

    private enum SimState {
        INIT, SPAWNING, IDLE, PROCESSING, COMPLETE
    }

    private SimState currentState = SimState.INIT;
    private double stateTimer = 0;

    private final Map<Integer, Entity> processEntities = new HashMap<>();
    private final Map<Integer, Point2D> targetPositions = new HashMap<>();

    private static final int MAX_QUEUE_SLOTS = 10;
    private final Integer[] queueSlots = new Integer[MAX_QUEUE_SLOTS];

    private final Entity[] cpuEntities = new Entity[NUM_CORES];
    private final ScaleTransition[] cpuPulses = new ScaleTransition[NUM_CORES];

    private int spawnIndex = 0;
    private List<Process> processesToSpawn;

    private HUDManager hudManager;
    private Text statusText;
    private Text core0Label, core1Label;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(WINDOW_WIDTH);
        settings.setHeight(WINDOW_HEIGHT);
        settings.setTitle("OS Kernel Visualizer - Fully Integrated");
        settings.setVersion("7.0");
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new ProcessEntityFactory());
        Arrays.fill(queueSlots, null);

        drawBackground();
        drawZones();
        spawnCPUs();

        // ═══════════════════════════════════════════════════════════════
        // CREATE KERNEL - All scheduling logic is in the Kernel
        // ═══════════════════════════════════════════════════════════════
        kernel = new Kernel(TIME_QUANTUM, MAX_MEMORY, NUM_CORES);

        // Load unified scenario matching console mode
        com.ossimulator.core.ScenarioLoader.loadStandardScenario(kernel);

        // Get processes from kernel for spawning visuals
        processesToSpawn = new ArrayList<>(kernel.getProcessTable().values());

        // Create visual entities for each process
        spawnProcessEntities();
        currentState = SimState.INIT;
    }

    private void spawnProcessEntities() {
        int i = 0;
        for (Process p : processesToSpawn) {
            double x = SPAWN_X;
            double y = SPAWN_Y + (i * 30) - 60;
            Entity entity = spawn("process", new SpawnData(x, y).put("process", p));
            processEntities.put(p.getPid(), entity);
            i++;
        }
    }

    private void spawnCPUs() {
        cpuEntities[0] = spawn("cpu", new SpawnData(
                CPU_CENTER_X - 200 - CPU_RADIUS, CPU_CENTER_Y - CPU_RADIUS));
        cpuEntities[1] = spawn("cpu", new SpawnData(
                CPU_CENTER_X + 200 - CPU_RADIUS, CPU_CENTER_Y - CPU_RADIUS));
    }

    @Override
    protected void initUI() {
        hudManager = new HUDManager();
        addUINode(hudManager.getRoot(), 0, 0);
        hudManager.bind(WINDOW_WIDTH);

        core0Label = new Text("CORE 1: Idle");
        core0Label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));
        core0Label.setFill(TEXT_SECONDARY);
        addUINode(core0Label, CPU_CENTER_X - 250, CPU_CENTER_Y - CPU_RADIUS - 20);

        core1Label = new Text("CORE 2: Idle");
        core1Label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));
        core1Label.setFill(TEXT_SECONDARY);
        addUINode(core1Label, CPU_CENTER_X + 150, CPU_CENTER_Y - CPU_RADIUS - 20);

        statusText = new Text("Status: INIT");
        statusText.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 20));
        statusText.setFill(TEXT_PRIMARY);
        addUINode(statusText, WINDOW_WIDTH / 2 - 100, WINDOW_HEIGHT - 100);

        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(10));
        legend.setStyle(
                "-fx-background-color: rgba(30, 30, 40, 0.8); -fx-background-radius: 20; -fx-border-color: #4C566A; -fx-border-radius: 20;");
        legend.getChildren().addAll(
                legendItem("★★★★ (Highest)", getPriorityColor(4)),
                legendItem("★★★ (High)", getPriorityColor(3)),
                legendItem("★★ (Medium)", getPriorityColor(2)),
                legendItem("★ (Low)", getPriorityColor(1)));
        addUINode(legend, WINDOW_WIDTH / 2 - 250, WINDOW_HEIGHT - 50);
    }

    private HBox legendItem(String label, Color color) {
        Rectangle r = new Rectangle(10, 10);
        r.setFill(color);
        r.setArcWidth(3);
        r.setArcHeight(3);

        Text t = new Text(label);
        t.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, 11));
        t.setFill(TEXT_SECONDARY);

        HBox box = new HBox(6, r, t);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    @Override
    protected void onUpdate(double tpf) {
        updateAnimations(tpf);
        updateHUD();
        stateTimer += tpf;

        if (activeAnimations.isEmpty()) {
            runStateMachine();
        }
    }

    private void updateAnimations(double tpf) {
        List<ActiveAnimation> done = new ArrayList<>();

        for (ActiveAnimation a : activeAnimations) {
            a.elapsed += tpf;
            double t = Math.min(1.0, a.elapsed / a.duration);
            double ease = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;

            double x = a.start.getX() + (a.end.getX() - a.start.getX()) * ease;
            double y = a.start.getY() + (a.end.getY() - a.start.getY()) * ease;
            a.entity.setPosition(x, y);

            if (t >= 1.0)
                done.add(a);
        }

        for (ActiveAnimation a : done) {
            activeAnimations.remove(a);
            if (a.onComplete != null)
                a.onComplete.run();
        }
    }

    private void animate(Entity e, double tx, double ty, Runnable onDone) {
        Point2D start = new Point2D(e.getX(), e.getY());
        Point2D end = new Point2D(tx, ty);
        activeAnimations.add(new ActiveAnimation(e, start, end, MOVE_DURATION, onDone));
    }

    /**
     * State machine for visual simulation.
     * Drives the visual flow, but ALL scheduling logic is in Kernel.
     */
    private void runStateMachine() {
        switch (currentState) {
            case INIT -> {
                if (stateTimer > 0.5) {
                    setStatus("SPAWNING", Color.ORANGE);
                    currentState = SimState.SPAWNING;
                    stateTimer = 0;
                    spawnIndex = 0;
                    startNextSpawn();
                }
            }
            case SPAWNING -> {
                /* handled by callbacks */ }
            case IDLE -> {
                if (stateTimer > CYCLE_INTERVAL) {
                    // Delegate scheduling to Kernel
                    if (kernel.isComplete()) {
                        currentState = SimState.COMPLETE;
                        showComplete();
                        return;
                    }

                    stateTimer = 0;

                    // Execute cores in parallel
                    currentState = SimState.PROCESSING;

                    for (int core = 0; core < NUM_CORES; core++) {
                        processCore(core);
                    }
                }
            }
            case PROCESSING -> {
                currentState = SimState.IDLE;
                stateTimer = 0;
            }
            case COMPLETE -> {
            }
        }
    }

    private void startNextSpawn() {
        if (spawnIndex >= processesToSpawn.size()) {
            // Reposition visuals to match initial scheduler state
            repositionQueueVisuals();
            setStatus("RUNNING", getPriorityColor(1));
            currentState = SimState.IDLE;
            stateTimer = 0;
            return;
        }

        Process p = processesToSpawn.get(spawnIndex);
        Entity e = processEntities.get(p.getPid());

        // 1. Initial State: NEW (Keep at spawned position)
        // e.setPosition(SPAWN_X, SPAWN_Y); // Removed to prevent jumping
        updateProcessStatus(p, "NEW", STATE_NEW);

        // 2. Wait, then animate to READY
        runOnce(() -> {
            int slot = spawnIndex % MAX_QUEUE_SLOTS;
            queueSlots[slot] = p.getPid();
            double tx = getSlotX(slot);
            double ty = READY_QUEUE_Y;
            targetPositions.put(p.getPid(), new Point2D(tx, ty));

            updateProcessStatus(p, "READY", STATE_READY);

            animate(e, tx, ty, () -> {
                spawnIndex++;
                runOnce(this::startNextSpawn, Duration.seconds(SPAWN_STAGGER));
            });
        }, Duration.seconds(0.7)); // 0.7s delay to visualize NEW state
    }

    /**
     * Advances the simulation for a specific core by delegating logic to the
     * Kernel.
     */
    private void processCore(int core) {
        // Execute one step in the Kernel
        CoreStepResult result = kernel.stepCore(core);

        // Update visualization based on the step result
        switch (result.action()) {
            case DISPATCHED -> animateDispatch(result.process(), core);
            case EXECUTED -> animateExecution(result.process(), core);
            case PREEMPTED -> animatePreempt(result.process(), core);
            case TERMINATED -> animateTerminate(result.process(), core);
            case NONE -> {
                // Nothing to do, core idle and queue empty
                setStatus("IDLE", TEXT_SECONDARY);
            }
        }
    }

    private void animateDispatch(Process p, int core) {
        freeSlot(p.getPid());

        Entity e = processEntities.get(p.getPid());
        double tx = getCoreX(core);
        double ty = getCoreY();
        targetPositions.put(p.getPid(), new Point2D(tx, ty));

        setStatus("DISPATCHING", ACCENT_PRIMARY);

        animate(e, tx, ty, () -> {
            updateProcessStatus(p, "RUNNING", STATE_RUNNING);
            startCorePulse(core);
            updateCoreLabel(core);
            repositionQueueVisuals();
        });
    }

    private void animateExecution(Process p, int core) {
        setStatus("EXECUTION", ACCENT_WARN);
        updateCoreLabel(core);
    }

    private void animatePreempt(Process p, int core) {
        stopCorePulse(core);
        updateCoreLabel(core);

        int slot = findEmptySlot();
        if (slot == -1)
            slot = 0;
        queueSlots[slot] = p.getPid();

        Entity e = processEntities.get(p.getPid());
        double tx = getSlotX(slot);
        double ty = READY_QUEUE_Y;
        targetPositions.put(p.getPid(), new Point2D(tx, ty));

        setStatus("PREEMPTING", ACCENT_WARN);
        flashEntity(e, ACCENT_WARN);

        animate(e, tx, ty, () -> {
            updateProcessStatus(p, "READY", STATE_READY);
            this.repositionQueueVisuals();
        });
    }

    private void animateTerminate(Process p, int core) {
        stopCorePulse(core);
        updateCoreLabel(core);

        int exitSlot = kernel.getTerminatedCount() - 1;
        double tx = TERMINATED_X;
        double ty = TERMINATED_Y + (exitSlot * 35) - 50;

        Entity e = processEntities.get(p.getPid());

        setStatus("TERMINATED", getPriorityColor(1));

        animate(e, tx, ty, () -> {
            updateProcessStatus(p, "COMPLETE", STATE_COMPLETE);
            spawnParticles(e.getX() + PROCESS_WIDTH / 2, e.getY() + PROCESS_HEIGHT / 2);

            var node = e.getViewComponent().getChildren().get(0);
            FadeTransition fade = new FadeTransition(Duration.seconds(0.5), node);
            fade.setToValue(0.4);
            fade.play();
        });
    }

    // ... repos and helpers ...

    private void repositionQueueVisuals() {
        Arrays.fill(queueSlots, null);

        // Get processes in queue from Kernel's scheduler (peek without removing)
        // We need to temporarily extract and re-add to see the order
        List<Process> queueOrder = new ArrayList<>();
        while (!kernel.getScheduler().isEmpty()) {
            kernel.getScheduler().selectNext().ifPresent(queueOrder::add);
        }

        // Put them back and update visuals
        for (int i = 0; i < queueOrder.size(); i++) {
            Process p = queueOrder.get(i);
            p.preempt(); // Ensure READY state
            kernel.getScheduler().requeue(p);

            if (i < MAX_QUEUE_SLOTS) {
                queueSlots[i] = p.getPid();
                Entity e = processEntities.get(p.getPid());
                if (e != null) {
                    double tx = getSlotX(i);
                    double ty = READY_QUEUE_Y;

                    Point2D current = targetPositions.getOrDefault(p.getPid(),
                            new Point2D(e.getX(), e.getY()));
                    if (Math.abs(current.getX() - tx) > 5 || Math.abs(current.getY() - ty) > 5) {
                        targetPositions.put(p.getPid(), new Point2D(tx, ty));
                        animate(e, tx, ty, null);
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VISUAL HELPERS (no scheduling logic here!)
    // ═══════════════════════════════════════════════════════════════

    private void flashEntity(Entity e, Color color) {
        var node = e.getViewComponent().getChildren().get(0);
        DropShadow flash = new DropShadow(30, color);
        node.setEffect(flash);

        runOnce(() -> {
            node.setEffect(new DropShadow(15, ACCENT_PRIMARY));
        }, Duration.seconds(0.3));
    }

    // ... slot helpers ...

    private int findEmptySlot() {
        for (int i = 0; i < MAX_QUEUE_SLOTS; i++) {
            if (queueSlots[i] == null)
                return i;
        }
        return -1;
    }

    private void freeSlot(int pid) {
        for (int i = 0; i < MAX_QUEUE_SLOTS; i++) {
            if (queueSlots[i] != null && queueSlots[i] == pid) {
                queueSlots[i] = null;
                return;
            }
        }
    }

    private double getSlotX(int slot) {
        return READY_QUEUE_X + slot * (PROCESS_WIDTH + 15);
    }

    private double getCoreX(int core) {
        return (core == 0 ? CPU_CENTER_X - 200 : CPU_CENTER_X + 200) - PROCESS_WIDTH / 2;
    }

    private double getCoreY() {
        return CPU_CENTER_Y + CPU_RADIUS + 25;
    }

    private void startCorePulse(int core) {
        if (cpuEntities[core] == null)
            return;
        var view = cpuEntities[core].getViewComponent().getChildren().get(0);

        cpuPulses[core] = new ScaleTransition(Duration.seconds(0.5), view);
        cpuPulses[core].setFromX(1.0);
        cpuPulses[core].setFromY(1.0);
        cpuPulses[core].setToX(1.1);
        cpuPulses[core].setToY(1.1);
        cpuPulses[core].setCycleCount(-1);
        cpuPulses[core].setAutoReverse(true);
        cpuPulses[core].play();

        if (view instanceof javafx.scene.layout.StackPane stack) {
            for (var c : stack.getChildren()) {
                if (c instanceof Rectangle r && r.getStroke() != null && r.getStrokeWidth() > 2) {
                    // Main body (width > 2 checks for cpuBody vs outerRing)
                    r.setStroke(PULSE_INNER);
                    r.setEffect(new DropShadow(30, PULSE_INNER)); // Strong yellow glow
                } else if (c instanceof Rectangle r && r.getStroke() != null) {
                    // Outer ring
                    r.setStroke(PULSE_OUTER);
                }
            }
        }
    }

    private void updateProcessStatus(Process p, String status, Color color) {
        Entity e = processEntities.get(p.getPid());
        if (e != null) {
            e.getViewComponent().getChildren().forEach(node -> {
                if (node instanceof javafx.scene.layout.StackPane card) {
                    // Find text box
                    if (card.getChildren().size() > 1 && card.getChildren().get(1) instanceof VBox box) {
                        box.getChildren().stream()
                                .filter(n -> "stateText".equals(n.getUserData()) && n instanceof Text)
                                .map(n -> (Text) n)
                                .findFirst()
                                .ifPresent(t -> {
                                    t.setText(status);
                                    t.setFill(color);
                                });
                    }
                }
            });
        }
    }

    private void stopCorePulse(int core) {
        if (cpuPulses[core] != null)
            cpuPulses[core].stop();
        if (cpuEntities[core] == null)
            return;

        var view = cpuEntities[core].getViewComponent().getChildren().get(0);
        view.setScaleX(1.0);
        view.setScaleY(1.0);

        if (view instanceof javafx.scene.layout.StackPane stack) {
            for (var c : stack.getChildren()) {
                if (c instanceof Rectangle r && r.getStroke() != null && r.getStrokeWidth() > 2) {
                    // Reset body
                    r.setStroke(ACCENT_PRIMARY);
                    r.setEffect(new DropShadow(30, ACCENT_PRIMARY));
                } else if (c instanceof Rectangle r && r.getStroke() != null) {
                    // Reset outer ring
                    r.setStroke(ACCENT_PRIMARY.deriveColor(0, 1, 1, 0.3));
                }
            }
        }
    }

    private void updateCoreLabel(int core) {
        Text label = core == 0 ? core0Label : core1Label;
        Process p = kernel.getCoreProcess(core);

        if (p != null) {
            String stars = "★".repeat(Math.min(p.getPriority(), 5));
            int remaining = kernel.getCoreTimeRemaining(core);
            label.setText(String.format("CORE %d: P%d %s %s [%dms]",
                    core + 1, p.getPid(), p.getName(), stars, remaining));
            label.setFill(getPriorityColor(p.getPriority()));
        } else {
            label.setText(String.format("CORE %d: Idle", core + 1));
            label.setFill(TEXT_SECONDARY);
        }
    }

    private void spawnParticles(double cx, double cy) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12;
            double ex = cx + Math.cos(angle) * 70;
            double ey = cy + Math.sin(angle) * 70;

            Rectangle r = new Rectangle(8, 8);
            r.setFill(i % 2 == 0 ? ACCENT_PRIMARY : ACCENT_SECONDARY);
            r.setArcWidth(4);
            r.setArcHeight(4);
            r.setEffect(new DropShadow(5, (Color) r.getFill()));

            Entity pe = entityBuilder().at(cx, cy).view(r).zIndex(100).buildAndAttach();
            animate(pe, ex, ey, () -> pe.removeFromWorld());

            FadeTransition fade = new FadeTransition(Duration.seconds(MOVE_DURATION), r);
            fade.setToValue(0);
            fade.play();
        }
    }

    private void setStatus(String s, Color c) {
        statusText.setText("Status: " + s);
        statusText.setFill(c);
    }

    private void updateHUD() {
        hudManager.update(kernel);
    }

    // ... drawBackground ...

    private void drawBackground() {
        Rectangle bg = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        bg.setFill(getBackgroundGradient());
        entityBuilder().at(0, 0).view(bg).zIndex(-100).buildAndAttach();
    }

    private void drawZones() {
        double queueWidth = (PROCESS_WIDTH + 10) * MAX_QUEUE_SLOTS + 20;

        drawZone("READY QUEUE (Sorted by Priority)", READY_QUEUE_X - 20, READY_QUEUE_Y - 40,
                queueWidth, PROCESS_HEIGHT + 60);

        drawZone("CORE 1", CPU_CENTER_X - 270, CPU_CENTER_Y - CPU_RADIUS - 40,
                160, CPU_RADIUS * 2 + 80);
        drawZone("CORE 2", CPU_CENTER_X + 130, CPU_CENTER_Y - CPU_RADIUS - 40,
                160, CPU_RADIUS * 2 + 80);

        drawZone("NEW", SPAWN_X - 15, SPAWN_Y - 80, PROCESS_WIDTH + 30, 200);
        drawZone("EXIT", TERMINATED_X - 15, TERMINATED_Y - 80, PROCESS_WIDTH + 30, 230);
    }

    private void drawZone(String label, double x, double y, double w, double h) {
        Rectangle r = new Rectangle(w, h);
        r.setFill(Color.web("#1F2833", 0.3));
        r.setStroke(Color.web("#45A29E", 0.5));
        r.setStrokeWidth(1);
        r.setArcWidth(8);
        r.setArcHeight(8);
        entityBuilder().at(x, y).view(r).zIndex(-50).buildAndAttach();

        Text t = new Text(label);
        t.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 12));
        t.setFill(TEXT_MUTED);
        entityBuilder().at(x + 10, y + 15).view(t).zIndex(-40).buildAndAttach();
    }

    private void showComplete() {
        setStatus("COMPLETE", ACCENT_PRIMARY);

        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: rgba(31, 40, 51, 0.95); -fx-background-radius: 12; " +
                "-fx-border-color: #66FCF1; -fx-border-radius: 12; -fx-border-width: 2;");

        Text title = new Text("SIMULATION COMPLETE");
        title.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 30));
        title.setFill(ACCENT_PRIMARY);
        title.setEffect(new DropShadow(15, ACCENT_PRIMARY));

        // Statistics from Kernel
        Text stats = new Text(String.format(
                "Total Time: %dms\nDispatches: %d\nPreemptions: %d\nCompleted: %d/%d",
                kernel.getSimulationTime(),
                kernel.getDispatchCount(),
                kernel.getPreemptCount(),
                kernel.getTerminatedCount(),
                kernel.getTotalProcessCount()));
        stats.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, 18));
        stats.setFill(TEXT_PRIMARY);

        box.getChildren().addAll(title, stats);
        double boxWidth = 300;
        double boxHeight = 200;
        addUINode(box, (WINDOW_WIDTH - boxWidth) / 2, (WINDOW_HEIGHT - boxHeight) / 2);
    }
}
