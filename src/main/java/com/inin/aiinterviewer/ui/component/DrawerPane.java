package com.inin.aiinterviewer.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

/**
 * Reusable right-side drawer with a modal scrim, shared header and animated lifecycle.
 * Feature views only provide a title and content node.
 */
public class DrawerPane extends StackPane {

    private static final Duration MOTION_DURATION = Duration.millis(190);

    private final Region scrim = new Region();
    private final VBox panel = new VBox();
    private final Label titleLabel = new Label();
    private final StackPane contentHost = new StackPane();
    private ParallelTransition activeTransition;
    private double drawerWidth = 480;

    public DrawerPane() {
        getStyleClass().add("drawer-overlay");
        setVisible(false);
        setManaged(false);

        scrim.getStyleClass().add("drawer-scrim");
        scrim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scrim.setOnMouseClicked(event -> close());

        titleLabel.getStyleClass().add("drawer-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button closeButton = new Button();
        closeButton.setGraphic(new FontIcon("mdi2c-close"));
        closeButton.getStyleClass().add("drawer-close-button");
        closeButton.setAccessibleText("关闭抽屉");
        closeButton.setOnAction(event -> close());

        HBox header = new HBox(12, titleLabel, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 16, 22));
        header.getStyleClass().add("drawer-header");

        contentHost.getStyleClass().add("drawer-content-host");
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        panel.getChildren().setAll(header, contentHost);
        panel.getStyleClass().add("drawer-panel");
        panel.setMaxWidth(Region.USE_PREF_SIZE);
        setDrawerWidth(drawerWidth);
        StackPane.setAlignment(panel, Pos.CENTER_RIGHT);

        getChildren().setAll(scrim, panel);
    }

    public void open(String title, Node content) {
        Objects.requireNonNull(content, "content");
        stopActiveTransition();
        titleLabel.setText(title == null ? "" : title);
        contentHost.getChildren().setAll(content);
        setManaged(true);
        setVisible(true);
        toFront();

        panel.setTranslateX(drawerWidth);
        scrim.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(MOTION_DURATION, panel);
        slide.setToX(0);
        FadeTransition fade = new FadeTransition(MOTION_DURATION, scrim);
        fade.setToValue(1);
        activeTransition = new ParallelTransition(slide, fade);
        activeTransition.play();
    }

    public void close() {
        if (!isVisible()) return;
        stopActiveTransition();
        TranslateTransition slide = new TranslateTransition(MOTION_DURATION, panel);
        slide.setToX(drawerWidth);
        FadeTransition fade = new FadeTransition(MOTION_DURATION, scrim);
        fade.setToValue(0);
        activeTransition = new ParallelTransition(slide, fade);
        activeTransition.setOnFinished(event -> {
            contentHost.getChildren().clear();
            setVisible(false);
            setManaged(false);
            activeTransition = null;
        });
        activeTransition.play();
    }

    public boolean isOpen() {
        return isVisible();
    }

    public double getDrawerWidth() {
        return drawerWidth;
    }

    public void setDrawerWidth(double drawerWidth) {
        if (drawerWidth < 320) throw new IllegalArgumentException("Drawer width must be at least 320");
        this.drawerWidth = drawerWidth;
        panel.setMinWidth(drawerWidth);
        panel.setPrefWidth(drawerWidth);
    }

    private void stopActiveTransition() {
        if (activeTransition != null) {
            activeTransition.stop();
            activeTransition = null;
        }
    }
}
