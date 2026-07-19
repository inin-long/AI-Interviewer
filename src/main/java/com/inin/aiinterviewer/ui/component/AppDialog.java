package com.inin.aiinterviewer.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Reusable in-product modal with branded chrome, content host and action bar.
 * Feature code supplies only copy, content and result-producing actions.
 */
public class AppDialog<R> extends Dialog<R> {

    public enum Tone {
        NEUTRAL("mdi2s-shape-outline"),
        INFORMATION("mdi2i-information-outline"),
        WARNING("mdi2a-alert-outline"),
        DANGER("mdi2a-alert-circle-outline");

        private final String iconLiteral;

        Tone(String iconLiteral) {
            this.iconLiteral = iconLiteral;
        }
    }

    public enum ActionStyle {
        PRIMARY("app-dialog-primary-button"),
        SECONDARY("app-dialog-secondary-button"),
        DANGER("app-dialog-danger-button");

        private final String styleClass;

        ActionStyle(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    private final VBox bodyHost = new VBox(16);
    private final HBox actionBar = new HBox(10);
    private final Label supportingLabel = new Label();
    private Node initialFocus;

    public AppDialog(Window owner, String windowTitle, String heading) {
        this(owner, windowTitle, heading, null, Tone.NEUTRAL);
    }

    public AppDialog(Window owner, String windowTitle, String heading, String supportingText, Tone tone) {
        Objects.requireNonNull(windowTitle, "windowTitle");
        Objects.requireNonNull(heading, "heading");
        Objects.requireNonNull(tone, "tone");

        if (owner != null) {
            initOwner(owner);
            initModality(Modality.WINDOW_MODAL);
        } else {
            initModality(Modality.APPLICATION_MODAL);
        }
        initStyle(StageStyle.TRANSPARENT);
        setTitle(windowTitle);

        DialogPane dialogPane = new DialogPane();
        dialogPane.getStyleClass().add("app-dialog-pane");
        URL stylesheet = AppDialog.class.getResource("/css/app.css");
        if (stylesheet != null) dialogPane.getStylesheets().add(stylesheet.toExternalForm());

        Label brand = new Label("AI");
        brand.getStyleClass().add("app-dialog-brand");
        Label windowTitleLabel = new Label(windowTitle);
        windowTitleLabel.getStyleClass().add("app-dialog-window-title");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeButton = new Button();
        closeButton.setGraphic(new FontIcon("mdi2c-close"));
        closeButton.getStyleClass().add("app-dialog-close-button");
        closeButton.setAccessibleText("关闭弹窗");
        closeButton.setCancelButton(true);
        closeButton.setOnAction(event -> cancel());
        HBox header = new HBox(10, brand, windowTitleLabel, headerSpacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-dialog-header");

        FontIcon toneIcon = new FontIcon(tone.iconLiteral);
        toneIcon.setIconSize(20);
        toneIcon.getStyleClass().addAll("app-dialog-tone-icon", "tone-" + tone.name().toLowerCase());
        Label headingLabel = new Label(heading);
        headingLabel.setWrapText(true);
        headingLabel.getStyleClass().add("app-dialog-heading");
        HBox headingRow = new HBox(9, toneIcon, headingLabel);
        headingRow.setAlignment(Pos.CENTER_LEFT);

        supportingLabel.setWrapText(true);
        supportingLabel.getStyleClass().add("app-dialog-supporting");
        bodyHost.getChildren().add(headingRow);
        setSupportingText(supportingText);
        bodyHost.getStyleClass().add("app-dialog-body");

        actionBar.setAlignment(Pos.CENTER_RIGHT);
        actionBar.getStyleClass().add("app-dialog-actions");

        VBox shell = new VBox(header, bodyHost, actionBar);
        shell.getStyleClass().add("app-dialog-shell");
        shell.setMinWidth(440);
        shell.setPrefWidth(480);
        shell.setMaxWidth(520);
        dialogPane.setContent(shell);
        dialogPane.setPrefWidth(508);
        dialogPane.setMaxSize(548, Region.USE_PREF_SIZE);
        // JavaFX only permits abnormal closing (window close, Esc or close())
        // when the DialogPane exposes a cancel-capable ButtonType. Keep that
        // semantic button hidden because the product action bar is rendered
        // by this component itself.
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        Node nativeCancel = dialogPane.lookupButton(ButtonType.CANCEL);
        nativeCancel.setVisible(false);
        nativeCancel.setManaged(false);
        nativeCancel.getStyleClass().add("app-dialog-native-cancel");
        setDialogPane(dialogPane);

        setOnShown(event -> {
            Scene scene = dialogPane.getScene();
            if (scene != null) {
                scene.setFill(Color.TRANSPARENT);
                scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                    if (keyEvent.getCode() == KeyCode.ESCAPE) {
                        keyEvent.consume();
                        cancel();
                    }
                });
            }
            if (initialFocus != null) initialFocus.requestFocus();
        });
    }

    public void setSupportingText(String text) {
        supportingLabel.setText(text == null ? "" : text);
        if (supportingLabel.getText().isBlank()) {
            bodyHost.getChildren().remove(supportingLabel);
        } else if (!bodyHost.getChildren().contains(supportingLabel)) {
            bodyHost.getChildren().add(1, supportingLabel);
        }
    }

    public void setBody(Node content) {
        Objects.requireNonNull(content, "content");
        bodyHost.getChildren().removeIf(node -> node.getStyleClass().contains("app-dialog-feature-content"));
        content.getStyleClass().add("app-dialog-feature-content");
        bodyHost.getChildren().add(content);
    }

    public Button addAction(String label, Supplier<? extends R> resultSupplier, ActionStyle style) {
        Objects.requireNonNull(resultSupplier, "resultSupplier");
        Button button = actionButton(label, style);
        button.setDefaultButton(style == ActionStyle.PRIMARY || style == ActionStyle.DANGER);
        button.setOnAction(event -> setResult(resultSupplier.get()));
        actionBar.getChildren().add(button);
        return button;
    }

    public Button addCancelAction(String label) {
        Button button = actionButton(label, ActionStyle.SECONDARY);
        button.setCancelButton(true);
        button.setOnAction(event -> cancel());
        actionBar.getChildren().add(button);
        return button;
    }

    public void setInitialFocus(Node node) {
        initialFocus = node;
    }

    private Button actionButton(String label, ActionStyle style) {
        Button button = new Button(Objects.requireNonNull(label, "label"));
        button.getStyleClass().addAll("app-dialog-button", style.styleClass);
        return button;
    }

    private void cancel() {
        setResult(null);
        close();
    }
}
