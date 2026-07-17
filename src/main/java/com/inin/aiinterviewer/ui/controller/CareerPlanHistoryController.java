package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CareerPlanDto;
import com.inin.aiinterviewer.application.service.CareerPlanningService;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import jakarta.annotation.Resource;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.scene.Scene;

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

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void initializeContext(Object context) {
        // no-op
    }

    @FXML
    public void initialize() {
        Long userId = userSessionState.requireCurrentUser().id();
        List<CareerPlanDto> plans = careerPlanningService.listPlans(userId);

        historyList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CareerPlanDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label title = new Label(item.targetRole() != null ? item.targetRole() : "职业规划");
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                Label date = new Label(item.createTime().format(FMT));
                date.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
                HBox box = new HBox(10, title, new Region(), date);
                setGraphic(box);
            }
        });

        historyList.getItems().addAll(plans);

        historyList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                CareerPlanDto selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.planMarkdown() != null && !selected.planMarkdown().isBlank()) {
                    showDetail(selected);
                }
            }
        });
    }

    private void showDetail(CareerPlanDto plan) {
        Stage stage = new Stage();
        stage.setTitle("职业规划详情 - " + (plan.targetRole() != null ? plan.targetRole() : "规划"));
        stage.initOwner(historyList.getScene().getWindow());

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        MarkdownView md = new MarkdownView();
        md.setMarkdown(plan.planMarkdown());
        scroll.setContent(md);

        Scene scene = new Scene(scroll, 750, 550);
        stage.setScene(scene);
        stage.showAndWait();
    }

    @FXML
    private void handleBack() {
        navigator.back();
    }
}
