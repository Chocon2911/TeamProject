package com.ossimulator.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.ossimulator.core.Process;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static com.ossimulator.ui.UIConfig.*;

public class ProcessEntityFactory implements EntityFactory {

    public enum EntityType {
        PROCESS,
        CPU_CORE,
        MEMORY_SLOT,
        PARTICLE
    }

    @Spawns("process")
    public Entity spawnProcess(SpawnData data) {
        Process process = data.get("process");

        StackPane card = createProcessCard(process);

        return FXGL.entityBuilder(data)
                .type(EntityType.PROCESS)
                .viewWithBBox(card)
                .with("process", process)
                .with("pid", process.getPid())
                .build();
    }

    private StackPane createProcessCard(Process process) {
        int priority = process.getPriority();
        Color priorityColor = getPriorityColor(priority);

        Rectangle cardBody = new Rectangle(PROCESS_WIDTH, PROCESS_HEIGHT);
        cardBody.setArcWidth(PROCESS_ARC);
        cardBody.setArcHeight(PROCESS_ARC);
        cardBody.setFill(BG_MEDIUM.deriveColor(0, 1, 1.2, 0.9));
        cardBody.setStroke(priorityColor);
        cardBody.setStrokeWidth(PROCESS_BORDER_WIDTH);

        DropShadow glow = new DropShadow();
        glow.setColor(priorityColor);
        glow.setRadius(PROCESS_GLOW_RADIUS * 1.5);
        glow.setSpread(0.6);
        cardBody.setEffect(glow);

        Text pidText = new Text("P" + process.getPid());
        pidText.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_SIZE_LARGE));
        pidText.setFill(TEXT_PRIMARY);

        Glow textGlow = new Glow(0.4);
        pidText.setEffect(textGlow);

        Text nameText = new Text(truncateName(process.getName()));
        nameText.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE_SMALL));
        nameText.setFill(TEXT_SECONDARY);

        Text priorityText = new Text("★".repeat(Math.min(priority, 5)));
        priorityText.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_SIZE_SMALL));
        priorityText.setFill(priorityColor);

        Text stateText = new Text("NEW");
        stateText.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE_SMALL));
        stateText.setFill(STATE_NEW); // White
        stateText.setUserData("stateText"); // Tag for updates

        // Layout
        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER);
        textBox.getChildren().addAll(pidText, nameText, stateText, priorityText);

        StackPane card = new StackPane();
        card.getChildren().addAll(cardBody, textBox);

        return card;
    }

    /**
     * Creates the CPU core visual - a pulsating hexagonal shape.
     */
    @Spawns("cpu")
    public Entity spawnCPU(SpawnData data) {
        StackPane cpuVisual = createCPUVisual();

        return FXGL.entityBuilder(data)
                .type(EntityType.CPU_CORE)
                .viewWithBBox(cpuVisual)
                .build();
    }

    private StackPane createCPUVisual() {
        // Outer glow ring
        Rectangle outerRing = new Rectangle(CPU_RADIUS * 2.5, CPU_RADIUS * 2.5);
        outerRing.setArcWidth(20);
        outerRing.setArcHeight(20);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(ACCENT_PRIMARY.deriveColor(0, 1, 1, 0.3));
        outerRing.setStrokeWidth(2);

        // Main CPU body
        Rectangle cpuBody = new Rectangle(CPU_RADIUS * 2, CPU_RADIUS * 2);
        cpuBody.setArcWidth(15);
        cpuBody.setArcHeight(15);
        cpuBody.setFill(BG_LIGHT);
        cpuBody.setStroke(ACCENT_PRIMARY);
        cpuBody.setStrokeWidth(3);

        // Inner glow
        DropShadow innerGlow = new DropShadow();
        innerGlow.setColor(ACCENT_PRIMARY);
        innerGlow.setRadius(30);
        innerGlow.setSpread(0.2);
        cpuBody.setEffect(innerGlow);

        // CPU Label
        Text cpuLabel = new Text("CPU");
        cpuLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_SIZE_LARGE));
        cpuLabel.setFill(ACCENT_PRIMARY);

        Glow textGlow = new Glow(0.8);
        cpuLabel.setEffect(textGlow);

        // "CORE" sublabel
        Text coreLabel = new Text("◆ CORE ◆");
        coreLabel.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE_SMALL));
        coreLabel.setFill(TEXT_SECONDARY);

        VBox labels = new VBox(5);
        labels.setAlignment(Pos.CENTER);
        labels.getChildren().addAll(cpuLabel, coreLabel);

        StackPane cpuStack = new StackPane();
        cpuStack.getChildren().addAll(outerRing, cpuBody, labels);

        return cpuStack;
    }

    /**
     * Creates a memory slot visual for the memory bar.
     */
    @Spawns("memorySlot")
    public Entity spawnMemorySlot(SpawnData data) {
        boolean occupied = data.get("occupied");
        int slotIndex = data.get("slotIndex");
        Integer pid = data.hasKey("pid") ? data.get("pid") : null;

        StackPane slotVisual = createMemorySlotVisual(occupied, pid);

        return FXGL.entityBuilder(data)
                .type(EntityType.MEMORY_SLOT)
                .viewWithBBox(slotVisual)
                .with("slotIndex", slotIndex)
                .with("occupied", occupied)
                .build();
    }

    private StackPane createMemorySlotVisual(boolean occupied, Integer pid) {
        double slotWidth = (MEMORY_BAR_WIDTH - (MEMORY_SLOT_GAP * 4)) / 5;

        Rectangle slot = new Rectangle(slotWidth, MEMORY_BAR_HEIGHT - 10);
        slot.setArcWidth(8);
        slot.setArcHeight(8);

        if (occupied) {
            slot.setFill(ACCENT_PRIMARY.deriveColor(0, 0.7, 0.8, 0.8));
            slot.setStroke(ACCENT_PRIMARY);

            DropShadow glow = new DropShadow();
            glow.setColor(ACCENT_PRIMARY);
            glow.setRadius(10);
            slot.setEffect(glow);
        } else {
            slot.setFill(BG_MEDIUM.deriveColor(0, 1, 0.8, 0.5));
            slot.setStroke(GRID_LINE);
        }
        slot.setStrokeWidth(1);

        StackPane slotPane = new StackPane();
        slotPane.getChildren().add(slot);

        if (occupied && pid != null) {
            Text pidLabel = new Text("P" + pid);
            pidLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_SIZE_MEDIUM));
            pidLabel.setFill(TEXT_PRIMARY);
            slotPane.getChildren().add(pidLabel);
        }

        return slotPane;
    }

    /**
     * Truncates long process names for display.
     */
    private String truncateName(String name) {
        if (name.length() > 10) {
            return name.substring(0, 8) + "..";
        }
        return name;
    }
}
