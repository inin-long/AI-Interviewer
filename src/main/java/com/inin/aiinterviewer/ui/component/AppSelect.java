package com.inin.aiinterviewer.ui.component;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Product-styled replacement for a raw JavaFX {@link ComboBox}.
 *
 * <p>The component keeps the ComboBox API so existing controllers and FXML
 * bindings continue to work, while owning the button cell, popup cells and
 * selected-state treatment used throughout the application.</p>
 */
public class AppSelect<T> extends ComboBox<T> {

    public AppSelect() {
        getStyleClass().add("app-select");
        setVisibleRowCount(8);
        setCellFactory(ignored -> new PopupCell());
        setButtonCell(new ButtonCell());
    }

    private String displayText(T item) {
        if (item == null) return "";
        StringConverter<T> converter = getConverter();
        return converter == null ? item.toString() : converter.toString(item);
    }

    private final class ButtonCell extends ListCell<T> {
        private ButtonCell() {
            getStyleClass().add("app-select-button-cell");
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : displayText(item));
            setGraphic(null);
        }
    }

    private final class PopupCell extends ListCell<T> {
        private final FontIcon checkIcon = new FontIcon("mdi2c-check");

        private PopupCell() {
            getStyleClass().add("app-select-popup-cell");
            checkIcon.setIconSize(17);
            checkIcon.getStyleClass().add("app-select-check");
            selectedProperty().addListener((observable, oldValue, selected) -> updateGraphic());
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : displayText(item));
            updateGraphic();
        }

        private void updateGraphic() {
            setGraphic(!isEmpty() && getItem() != null && isSelected() ? checkIcon : null);
        }
    }
}
