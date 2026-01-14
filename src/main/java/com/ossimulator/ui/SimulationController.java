package com.ossimulator.ui;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.GameWorld;
import com.almasb.fxgl.entity.SpawnData;
import com.ossimulator.core.Process;
import com.ossimulator.core.ProcessState;
import com.ossimulator.kernel.Kernel;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Point2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.ossimulator.ui.UIConfig.*;

public class SimulationController {

    private final Kernel kernel;
    @SuppressWarnings("unused")
    private final GameWorld gameWorld;

    private final Map<Integer, Entity> processEntities = new HashMap<>();
    private final Map<Integer, ProcessState> lastKnownStates = new HashMap<>();
    private final Map<Integer, Point2D> entityPositions = new HashMap<>();

    private final AtomicInteger nextReadySlot = new AtomicInteger(0);
    private final Map<Integer, Integer> readyQueueSlots = new HashMap<>();

    private Entity cpuEntity;
    private Integer currentRunningPid = null;

    private boolean animating = false;
    private Runnable onAnimationComplete;

    public SimulationController(Kernel kernel, GameWorld gameWorld) {
        this.kernel = kernel;
        this.gameWorld = gameWorld;
    }

    public void spawnAllProcessEntities() {
        spawnCPU();

        int index = 0;
        for (Process process : kernel.getProcessTable().values()) {
            spawnProcessEntity(process, index++);
        }
    }

    private void spawnCPU() {
        cpuEntity = spawn("cpu", new SpawnData(
                CPU_CENTER_X - CPU_RADIUS,
                CPU_CENTER_Y - CPU_RADIUS));
    }

    private void spawnProcessEntity(Process process, int index) {
        double startX = SPAWN_X;
        double startY = SPAWN_Y + (index * 20) - 50;

        SpawnData data = new SpawnData(startX, startY);
        data.put("process", process);

        Entity entity = spawn("process", data);

        processEntities.put(process.getPid(), entity);
        lastKnownStates.put(process.getPid(), ProcessState.NEW);
        entityPositions.put(process.getPid(), new Point2D(startX, startY));
    }

    public Map<Integer, ProcessState> captureStates() {
        Map<Integer, ProcessState> states = new HashMap<>();
        for (Process p : kernel.getProcessTable().values()) {
            states.put(p.getPid(), p.getState());
        }
        return states;
    }

    /**
     * Animates state changes between before and after states.
     * Calls onComplete when all animations finish.
     */
    public void animateStateChanges(Map<Integer, ProcessState> beforeStates, Runnable onComplete) {
        this.animating = true;
        this.onAnimationComplete = onComplete;

        Map<Integer, ProcessState> afterStates = captureStates();
        AtomicInteger pendingAnimations = new AtomicInteger(0);

        Runnable checkComplete = () -> {
            if (pendingAnimations.decrementAndGet() <= 0) {
                this.animating = false;
                if (onAnimationComplete != null) {
                    onAnimationComplete.run();
                }
            }
        };

        // Find changes and animate
        for (Integer pid : afterStates.keySet()) {
            ProcessState before = beforeStates.getOrDefault(pid, ProcessState.NEW);
            ProcessState after = afterStates.get(pid);

            if (before != after) {
                pendingAnimations.incrementAndGet();
                animateTransition(pid, before, after, checkComplete);
            }
        }

        // If no animations needed, complete immediately
        if (pendingAnimations.get() == 0) {
            this.animating = false;
            if (onComplete != null) {
                runOnce(() -> onComplete.run(), Duration.millis(100));
            }
        }
    }

    private void animateTransition(int pid, ProcessState from, ProcessState to, Runnable onComplete) {
        Entity entity = processEntities.get(pid);
        if (entity == null) {
            onComplete.run();
            return;
        }

        switch (to) {
            case READY -> animateToReadyQueue(entity, pid, onComplete);
            case RUNNING -> animateToCPU(entity, pid, onComplete);
            case TERMINATED -> animateTermination(entity, pid, onComplete);
            default -> onComplete.run();
        }

        lastKnownStates.put(pid, to);
    }

    private void animateToReadyQueue(Entity entity, int pid, Runnable onComplete) {
        // Assign a slot in the ready queue
        int slot = nextReadySlot.getAndIncrement() % 6; // Max 6 visible slots
        readyQueueSlots.put(pid, slot);

        double targetX = READY_QUEUE_X + (slot * (PROCESS_WIDTH + READY_QUEUE_SLOT_GAP));
        double targetY = READY_QUEUE_Y;

        // Remove from CPU tracking if was running
        if (currentRunningPid != null && currentRunningPid == pid) {
            currentRunningPid = null;
            stopCPUPulse();
        }

        animateMoveTo(entity, targetX, targetY, onComplete);
        entityPositions.put(pid, new Point2D(targetX, targetY));
    }

    private void animateToCPU(Entity entity, int pid, Runnable onComplete) {
        currentRunningPid = pid;

        // Free the ready queue slot
        readyQueueSlots.remove(pid);

        // Position at CPU center
        double targetX = CPU_CENTER_X - PROCESS_WIDTH / 2;
        double targetY = CPU_CENTER_Y + CPU_RADIUS + 20;

        animateMoveTo(entity, targetX, targetY, () -> {
            startCPUPulse();
            onComplete.run();
        });
        entityPositions.put(pid, new Point2D(targetX, targetY));
    }

    private void animateTermination(Entity entity, int pid, Runnable onComplete) {
        currentRunningPid = null;
        stopCPUPulse();

        // Particle burst effect + fade out
        playTerminationEffect(entity, () -> {
            entity.removeFromWorld();
            processEntities.remove(pid);
            lastKnownStates.remove(pid);
            entityPositions.remove(pid);
            readyQueueSlots.remove(pid);
            onComplete.run();
        });
    }

    /**
     * Smoothly animates an entity to a target position.
     */
    private void animateMoveTo(Entity entity, double targetX, double targetY, Runnable onComplete) {
        Point2D start = new Point2D(entity.getX(), entity.getY());
        Point2D end = new Point2D(targetX, targetY);

        animationBuilder()
                .duration(Duration.seconds(ANIM_MOVE_DURATION))
                .interpolator(javafx.animation.Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0))
                .onFinished(onComplete)
                .translate(entity)
                .from(start)
                .to(end)
                .buildAndPlay();
    }

    /**
     * Plays a termination effect (scale down + fade out).
     */
    private void playTerminationEffect(Entity entity, Runnable onComplete) {
        // Create particle burst
        spawnParticles(entity.getX() + PROCESS_WIDTH / 2, entity.getY() + PROCESS_HEIGHT / 2);

        // Scale and fade
        var node = entity.getViewComponent().getChildren().get(0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(ANIM_FADE_DURATION), node);
        scale.setToX(0.1);
        scale.setToY(0.1);

        FadeTransition fade = new FadeTransition(Duration.seconds(ANIM_FADE_DURATION), node);
        fade.setToValue(0);

        ParallelTransition parallel = new ParallelTransition(scale, fade);
        parallel.setOnFinished(e -> onComplete.run());
        parallel.play();
    }

    /**
     * Spawns particle effects at the given position.
     */
    private void spawnParticles(double x, double y) {
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8;
            double distance = 60;
            double endX = x + Math.cos(angle) * distance;
            double endY = y + Math.sin(angle) * distance;

            Rectangle particle = new Rectangle(8, 8);
            particle.setFill(ACCENT_PRIMARY);
            particle.setArcWidth(4);
            particle.setArcHeight(4);

            DropShadow glow = new DropShadow();
            glow.setColor(ACCENT_PRIMARY);
            glow.setRadius(10);
            particle.setEffect(glow);

            Entity particleEntity = entityBuilder()
                    .at(x, y)
                    .view(particle)
                    .buildAndAttach();

            animationBuilder()
                    .duration(Duration.seconds(ANIM_PARTICLE_DURATION))
                    .onFinished(() -> particleEntity.removeFromWorld())
                    .translate(particleEntity)
                    .from(new Point2D(x, y))
                    .to(new Point2D(endX, endY))
                    .buildAndPlay();

            // Fade out particle
            FadeTransition fade = new FadeTransition(Duration.seconds(ANIM_PARTICLE_DURATION), particle);
            fade.setToValue(0);
            fade.play();
        }
    }

    /**
     * Starts the CPU pulse animation when a process is running.
     */
    private void startCPUPulse() {
        if (cpuEntity == null)
            return;

        var cpuView = cpuEntity.getViewComponent().getChildren().get(0);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(ANIM_PULSE_DURATION), cpuView);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setCycleCount(-1);
        pulse.setAutoReverse(true);

        cpuView.setUserData(pulse);
        pulse.play();

        // Enhance glow
        if (cpuView instanceof javafx.scene.layout.StackPane stack) {
            for (var child : stack.getChildren()) {
                if (child instanceof Rectangle rect && rect.getStroke() != null) {
                    DropShadow glow = new DropShadow();
                    glow.setColor(ACCENT_WARN);
                    glow.setRadius(40);
                    glow.setSpread(0.4);
                    rect.setEffect(glow);
                }
            }
        }
    }

    /**
     * Stops the CPU pulse animation.
     */
    private void stopCPUPulse() {
        if (cpuEntity == null)
            return;

        var cpuView = cpuEntity.getViewComponent().getChildren().get(0);

        if (cpuView.getUserData() instanceof ScaleTransition pulse) {
            pulse.stop();
            cpuView.setScaleX(1.0);
            cpuView.setScaleY(1.0);
        }

        // Reset glow
        if (cpuView instanceof javafx.scene.layout.StackPane stack) {
            for (var child : stack.getChildren()) {
                if (child instanceof Rectangle rect && rect.getStroke() != null) {
                    DropShadow glow = new DropShadow();
                    glow.setColor(ACCENT_PRIMARY);
                    glow.setRadius(30);
                    glow.setSpread(0.2);
                    rect.setEffect(glow);
                }
            }
        }
    }

    public boolean isAnimating() {
        return animating;
    }

    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Gets the count of active (non-terminated) process entities.
     */
    public int getActiveProcessCount() {
        return processEntities.size();
    }
}
