package com.inin.aiinterviewer.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;

/** Product-styled multi-select whose options live in an anchored popup. */
public final class AppMultiSelect<T> extends Button {

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObservableSet<T> selectedItems = FXCollections.observableSet(new LinkedHashSet<>());
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "请选择");
    private final StringProperty popupTitle = new SimpleStringProperty(this, "popupTitle", "选择项目");
    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this, "converter");
    private final Label valueLabel = new Label();
    private final Label countLabel = new Label();
    private final TextField searchField = new TextField();
    private final VBox optionHost = new VBox(4);
    private final VBox popupShell = new VBox(10);
    private final FontIcon arrowIcon = new FontIcon("mdi2c-chevron-down");
    private final Popup popup = new Popup();

    public AppMultiSelect() {
        getStyleClass().add("app-multi-select");
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setMaxWidth(Double.MAX_VALUE);
        setAccessibleText("多选选择器");

        valueLabel.getStyleClass().add("app-multi-select-value");
        valueLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        arrowIcon.setIconSize(18);
        arrowIcon.getStyleClass().add("app-multi-select-arrow");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonContent = new HBox(8, valueLabel, spacer, arrowIcon);
        buttonContent.setAlignment(Pos.CENTER_LEFT);
        buttonContent.setMouseTransparent(true);
        buttonContent.prefWidthProperty().bind(widthProperty().subtract(28));
        setGraphic(buttonContent);

        buildPopup();
        setOnAction(event -> {
            if (popup.isShowing()) popup.hide();
            else showPopup();
        });
        var showing = javafx.css.PseudoClass.getPseudoClass("showing");
        popup.setOnShown(event -> pseudoClassStateChanged(showing, true));
        popup.setOnHidden(event -> pseudoClassStateChanged(showing, false));
        items.addListener((ListChangeListener<T>) change -> {
            selectedItems.retainAll(items);
            rebuildOptions();
            updateDisplay();
        });
        selectedItems.addListener((SetChangeListener<T>) change -> {
            updateDisplay();
            updateCount();
        });
        promptText.addListener((observable, oldValue, value) -> updateDisplay());
        popupTitle.addListener((observable, oldValue, value) -> updateCount());
        converter.addListener((observable, oldValue, value) -> {
            rebuildOptions();
            updateDisplay();
        });
        updateDisplay();
    }

    public ObservableList<T> getItems() { return items; }
    public ObservableSet<T> getSelectedItems() { return selectedItems; }

    public void setSelectedItems(Collection<? extends T> values) {
        selectedItems.clear();
        if (values != null) values.stream().filter(items::contains).forEach(selectedItems::add);
        rebuildOptions();
    }

    public String getPromptText() { return promptText.get(); }
    public void setPromptText(String value) { promptText.set(value); }
    public StringProperty promptTextProperty() { return promptText; }
    public String getPopupTitle() { return popupTitle.get(); }
    public void setPopupTitle(String value) { popupTitle.set(value); }
    public StringProperty popupTitleProperty() { return popupTitle; }
    public StringConverter<T> getConverter() { return converter.get(); }
    public void setConverter(StringConverter<T> value) { converter.set(value); }
    public ObjectProperty<StringConverter<T>> converterProperty() { return converter; }
    public boolean isPopupShowing() { return popup.isShowing(); }
    public String getDisplayText() { return valueLabel.getText(); }

    /** Exposes the rendered popup surface for native JavaFX snapshot verification. */
    public Region getPopupContent() { return popupShell; }

    private void buildPopup() {
        Label title = new Label();
        title.textProperty().bind(popupTitle);
        title.getStyleClass().add("app-multi-select-title");
        countLabel.getStyleClass().add("app-multi-select-count");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(8, title, headerSpacer, countLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("搜索分类");
        searchField.getStyleClass().add("app-multi-select-search");
        searchField.textProperty().addListener((observable, oldValue, value) -> rebuildOptions());
        ScrollPane optionsScroll = new ScrollPane(optionHost);
        optionsScroll.setFitToWidth(true);
        optionsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        optionsScroll.setPrefViewportHeight(188);
        optionsScroll.getStyleClass().add("app-multi-select-scroll");

        Button clear = new Button("清空选择");
        clear.getStyleClass().add("app-multi-select-clear");
        clear.setOnAction(event -> {
            selectedItems.clear();
            rebuildOptions();
        });
        Button done = new Button("完成");
        done.getStyleClass().add("app-multi-select-done");
        done.setOnAction(event -> popup.hide());
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, clear, footerSpacer, done);
        footer.setAlignment(Pos.CENTER_LEFT);

        popupShell.getChildren().setAll(header, searchField, optionsScroll, footer);
        popupShell.getStyleClass().add("app-multi-select-popup");
        popupShell.setMinWidth(300);
        popupShell.setPrefWidth(340);
        popupShell.setMaxWidth(420);
        URL stylesheet = AppMultiSelect.class.getResource("/css/app.css");
        if (stylesheet != null) popupShell.getStylesheets().add(stylesheet.toExternalForm());
        popup.getContent().setAll(popupShell);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setAutoFix(true);
        rebuildOptions();
    }

    private void showPopup() {
        if (getScene() == null || getScene().getWindow() == null) return;
        Bounds bounds = localToScreen(getBoundsInLocal());
        if (bounds == null) return;
        popupShell.setPrefWidth(Math.max(360, Math.min(420, getWidth() + 24)));
        searchField.clear();
        rebuildOptions();
        popup.show(this, bounds.getMinX(), bounds.getMaxY() + 6);
        searchField.requestFocus();
    }

    private void rebuildOptions() {
        String query = searchField.getText() == null ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
        optionHost.getChildren().clear();
        for (T item : items) {
            String text = displayText(item);
            if (!query.isBlank() && !text.toLowerCase(Locale.ROOT).contains(query)) continue;
            CheckBox option = new CheckBox(text);
            option.setSelected(selectedItems.contains(item));
            option.setMaxWidth(Double.MAX_VALUE);
            option.getStyleClass().add("app-multi-select-option");
            option.setOnAction(event -> {
                if (option.isSelected()) selectedItems.add(item);
                else selectedItems.remove(item);
            });
            optionHost.getChildren().add(option);
        }
        if (optionHost.getChildren().isEmpty()) {
            Label empty = new Label(items.isEmpty() ? "暂无可选分类" : "没有匹配的分类");
            empty.getStyleClass().add("app-multi-select-empty");
            optionHost.getChildren().add(empty);
        }
        updateCount();
    }

    private void updateDisplay() {
        if (selectedItems.isEmpty()) {
            valueLabel.setText(getPromptText());
            valueLabel.getStyleClass().remove("has-selection");
            return;
        }
        String names = items.stream().filter(selectedItems::contains).limit(2)
                .map(this::displayText).reduce((left, right) -> left + "、" + right).orElse("");
        valueLabel.setText(selectedItems.size() == 1 ? names : "已选择 " + selectedItems.size() + " 个分类 · " + names);
        if (!valueLabel.getStyleClass().contains("has-selection")) valueLabel.getStyleClass().add("has-selection");
    }

    private void updateCount() { countLabel.setText("已选 " + selectedItems.size() + " / " + items.size()); }

    private String displayText(T item) {
        if (item == null) return "";
        StringConverter<T> currentConverter = getConverter();
        return currentConverter == null ? item.toString() : currentConverter.toString(item);
    }
}
