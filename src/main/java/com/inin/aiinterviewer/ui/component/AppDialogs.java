package com.inin.aiinterviewer.ui.component;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/** Common modal recipes built on {@link AppDialog}. */
public final class AppDialogs {

    private AppDialogs() {
    }

    public static Optional<String> textInput(
            Window owner,
            String title,
            String heading,
            String fieldLabel,
            String promptText
    ) {
        AppDialog<String> dialog = new AppDialog<>(owner, title, heading,
                "使用清晰、容易识别的名称，后续可直接用于筛选和方案配置。",
                AppDialog.Tone.INFORMATION);
        Label label = new Label(fieldLabel);
        label.getStyleClass().add("app-dialog-field-label");
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("app-dialog-field");
        VBox fieldGroup = new VBox(8, label, field);
        dialog.setBody(fieldGroup);
        dialog.addCancelAction("取消");
        Button confirm = dialog.addAction("确定", () -> field.getText().trim(), AppDialog.ActionStyle.PRIMARY);
        confirm.disableProperty().bind(Bindings.createBooleanBinding(
                () -> field.getText() == null || field.getText().trim().isEmpty(),
                field.textProperty()));
        dialog.setInitialFocus(field);
        return dialog.showAndWait().filter(value -> !value.isBlank());
    }

    public static boolean confirm(
            Window owner,
            String title,
            String heading,
            String message,
            String confirmLabel,
            boolean destructive
    ) {
        AppDialog<Boolean> dialog = new AppDialog<>(owner, title, heading, message,
                destructive ? AppDialog.Tone.DANGER : AppDialog.Tone.WARNING);
        dialog.addCancelAction("取消");
        dialog.addAction(confirmLabel, () -> true,
                destructive ? AppDialog.ActionStyle.DANGER : AppDialog.ActionStyle.PRIMARY);
        return dialog.showAndWait().orElse(false);
    }

    public static void showMessage(
            Window owner,
            String title,
            String heading,
            String message,
            AppDialog.Tone tone
    ) {
        AppDialog<Boolean> dialog = new AppDialog<>(owner, title, heading, message, tone);
        dialog.addAction("知道了", () -> true, AppDialog.ActionStyle.PRIMARY);
        dialog.showAndWait();
    }
}
