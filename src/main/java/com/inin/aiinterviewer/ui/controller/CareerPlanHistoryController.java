package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CareerPlanDto;
import com.inin.aiinterviewer.application.service.CareerPlanningService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import jakarta.annotation.Resource;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
public class CareerPlanHistoryController implements ContextAwareController<Object> {

    @Resource
    private ContentNavigator navigator;

    @Resource
    private UserSessionState userSessionState;

    @Resource
    private CareerPlanningService careerPlanningService;

    @FXML
    private ListView<CareerPlanDto> historyList;

    @FXML
    private Label countLabel;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void initializeContext(Object context) {
        // no-op
    }

    @FXML
    public void initialize() {
        Long userId = userSessionState.requireCurrentUser().id();
        List<CareerPlanDto> plans = careerPlanningService.listPlans(userId);

        historyList.setCellFactory(list -> new PlanHistoryCell());

        historyList.getItems().addAll(plans);
        countLabel.setText(plans.size() + " 份");
        historyList.setPlaceholder(emptyState());

        historyList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                CareerPlanDto selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.planMarkdown() != null && !selected.planMarkdown().isBlank()) {
                    openDetail(selected);
                }
            }
        });
    }

    private void openDetail(CareerPlanDto plan) {
        navigator.showSubPage("/fxml/career-plan-detail-view.fxml", "职业规划详情", plan);
    }

    private VBox emptyState() {
        FontIcon icon = new FontIcon("mdi2m-map-outline");
        icon.setIconSize(34);
        icon.getStyleClass().add("history-empty-icon");
        Label title = new Label("还没有职业规划");
        title.getStyleClass().add("history-empty-title");
        Label copy = new Label("返回职业规划页生成第一份发展路线");
        copy.getStyleClass().add("history-empty-copy");
        VBox box = new VBox(8, icon, title, copy);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    @FXML
    private void handleBack() {
        navigator.back();
    }

    private final class PlanHistoryCell extends ListCell<CareerPlanDto> {
        @Override
        protected void updateItem(CareerPlanDto item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(empty || item == null ? null : card(item));
        }

        private HBox card(CareerPlanDto item) {
            FontIcon icon = new FontIcon("mdi2m-map-outline");
            icon.setIconSize(24);
            icon.getStyleClass().add("career-history-row-icon");
            StackPane iconBox = new StackPane(icon);
            iconBox.getStyleClass().add("career-history-icon-box");

            Label title = new Label(Optional.ofNullable(item.targetRole()).filter(value -> !value.isBlank())
                    .orElse("职业发展规划"));
            title.getStyleClass().add("career-history-row-title");

            String currentRole = Optional.ofNullable(item.currentRole()).filter(value -> !value.isBlank())
                    .orElse("当前定位未填写");
            String background = List.of(
                            Optional.ofNullable(item.industry()).orElse(""),
                            Optional.ofNullable(item.experienceYears()).orElse(""))
                    .stream().filter(value -> !value.isBlank()).reduce((left, right) -> left + " · " + right)
                    .orElse("职业背景未填写");
            Label description = new Label(currentRole + "  →  " + title.getText());
            description.getStyleClass().add("career-history-row-description");
            description.setWrapText(true);
            description.setMinWidth(0);
            description.setMaxWidth(Double.MAX_VALUE);
            Label meta = new Label(background + "  ·  " + item.createTime().format(FMT));
            meta.getStyleClass().add("career-history-row-meta");
            VBox identity = new VBox(5, title, description, meta);
            identity.setMinWidth(0);
            identity.setPrefWidth(0);
            HBox.setHgrow(identity, Priority.ALWAYS);

            Button view = new Button("查看规划");
            view.getStyleClass().add("history-row-primary");
            view.setMinWidth(104);
            view.setPrefWidth(104);
            view.setMaxWidth(104);
            view.setTextOverrun(OverrunStyle.CLIP);
            FontIcon viewIcon = new FontIcon("mdi2a-arrow-right");
            viewIcon.setIconSize(16);
            view.setGraphic(viewIcon);
            view.setOnAction(event -> {
                event.consume();
                getListView().getSelectionModel().select(item);
                openDetail(item);
            });
            view.setOnMouseClicked(event -> event.consume());

            HBox card = new HBox(14, iconBox, identity, view);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.getStyleClass().add("career-history-row-card");
            card.setMaxWidth(Double.MAX_VALUE);
            card.prefWidthProperty().bind(historyList.widthProperty().subtract(24));
            return card;
        }
    }
}
