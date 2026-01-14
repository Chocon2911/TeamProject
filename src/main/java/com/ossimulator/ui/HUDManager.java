package com.ossimulator.ui;

import com.ossimulator.kernel.Kernel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static com.ossimulator.ui.UIConfig.*;

public class HUDManager {

    private final HBox root;
    private final Text timeText;
    private final Text dispatchText;
    private final Text preemptText;
    private final Text completedText;
    private final Text queueText;

    public HUDManager() {
        root = new HBox(0);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10, 25, 10, 25));

        root.setStyle(
                "-fx-background-color: rgba(240, 242, 245, 0.95); " +
                        "-fx-background-radius: 0 0 25 25; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");

        timeText = makeStatValue("0ms");
        dispatchText = makeStatValue("0");
        preemptText = makeStatValue("0");
        completedText = makeStatValue("0/5");
        queueText = makeStatValue("0");

        root.getChildren().addAll(
                createStatCard("TIME", timeText),
                createSeparator(),
                createStatCard("DISPATCHES", dispatchText),
                createSeparator(),
                createStatCard("PREEMPTS", preemptText),
                createSeparator(),
                createStatCard("COMPLETED", completedText),
                createSeparator(),
                createStatCard("QUEUE", queueText));

    }

    public void bind(double windowWidth) {
        root.translateXProperty().bind(root.widthProperty().divide(-2).add(windowWidth / 2.0));
    }

    public void update(Kernel kernel) {
        timeText.setText(kernel.getSimulationTime() + "ms");
        dispatchText.setText(String.valueOf(kernel.getDispatchCount()));
        preemptText.setText(String.valueOf(kernel.getPreemptCount()));
        completedText.setText(kernel.getTerminatedCount() + "/" + kernel.getTotalProcessCount());
        queueText.setText(String.valueOf(kernel.getScheduler().size()));
    }

    public HBox getRoot() {
        return root;
    }

    private Text makeStatValue(String s) {
        Text t = new Text(s);
        t.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 18));
        t.setFill(Color.web("#2E3440"));
        return t;
    }

    private VBox createStatCard(String labelStr, Text valueText) {
        VBox card = new VBox(2);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(5, 20, 5, 20));

        Text label = new Text(labelStr);
        label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 10));
        label.setFill(Color.web("#808080"));

        card.getChildren().addAll(valueText, label);
        return card;
    }

    private Rectangle createSeparator() {
        Rectangle sep = new Rectangle(1, 40);
        sep.setFill(Color.web("#D1D5DB"));
        return sep;
    }
}
