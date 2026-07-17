package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.AssessmentResultDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.CareerAssessmentService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class CareerHistoryController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CareerAssessmentService assessmentService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private ListView<AssessmentResultDto> resultListView;
    @FXML private Button viewButton;
    @FXML private Button deleteButton;

    public CareerHistoryController(
            CareerAssessmentService assessmentService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.assessmentService = assessmentService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        resultListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AssessmentResultDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String type = item.templateCode().equals("HOLLAND") ? "霍兰德" : "MBTI";
                setText(type + " · " + item.resultCode() + "\n"
                        + TIME_FORMAT.format(item.createTime()));
                setWrapText(true);
            }
        });
        viewButton.disableProperty().bind(resultListView.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(resultListView.getSelectionModel().selectedItemProperty().isNull());
        refresh();
    }

    @FXML
    private void viewSelected() {
        AssessmentResultDto selected = resultListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/career-report-view.fxml", "测评报告", selected.id());
        }
    }

    @FXML
    private void deleteSelected() {
        AssessmentResultDto selected = resultListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "删除这条测评记录？", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("确认删除测评记录");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            assessmentService.deleteResult(sessionState.requireCurrentUser().id(), selected.id());
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }

    private void refresh() {
        resultListView.getItems().setAll(
                assessmentService.listResults(sessionState.requireCurrentUser().id()));
    }
}
