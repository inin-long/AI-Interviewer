package com.inin.aiinterviewer.ui.animation;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;

public final class AuthIllustrationMotion {

    private AuthIllustrationMotion() {
    }

    public static Animation start(Parent showcase) {
        Node illustration = showcase.lookup("#authIllustration");
        if (illustration == null) {
            throw new IllegalStateException("Missing authentication illustration node");
        }

        TranslateTransition floating = new TranslateTransition(Duration.seconds(3.8), illustration);
        floating.setFromY(-7);
        floating.setToY(7);
        floating.setAutoReverse(true);
        floating.setCycleCount(Animation.INDEFINITE);
        floating.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition breathing = new ScaleTransition(Duration.seconds(5.2), illustration);
        breathing.setFromX(0.992);
        breathing.setFromY(0.992);
        breathing.setToX(1.012);
        breathing.setToY(1.012);
        breathing.setAutoReverse(true);
        breathing.setCycleCount(Animation.INDEFINITE);
        breathing.setInterpolator(Interpolator.EASE_BOTH);

        RotateTransition drifting = new RotateTransition(Duration.seconds(6.4), illustration);
        drifting.setFromAngle(-0.7);
        drifting.setToAngle(0.7);
        drifting.setAutoReverse(true);
        drifting.setCycleCount(Animation.INDEFINITE);
        drifting.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition motion = new ParallelTransition(floating, breathing, drifting);
        showcase.sceneProperty().addListener((observable, previous, current) -> {
            if (current == null) motion.pause();
            else motion.play();
        });
        motion.play();
        return motion;
    }
}
