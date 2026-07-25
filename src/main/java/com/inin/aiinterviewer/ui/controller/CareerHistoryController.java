package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.AssessmentResultDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.CareerAssessmentService;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
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
    @FXML private Label countLabel;

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
        resultListView.setCellFactory(list -> new AssessmentHistoryCell());
        resultListView.setPlaceholder(emptyState());
        resultListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) viewSelected();
        });
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
        if (!AppDialogs.confirm(
                resultListView.getScene() == null ? null : resultListView.getScene().getWindow(),
                "删除测评记录",
                "确认删除测评记录",
                "将永久删除这条职业测评记录，此操作无法撤销。",
                "删除",
                true)) return;
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
        var results = assessmentService.listResults(sessionState.requireCurrentUser().id());
        resultListView.getItems().setAll(results);
        countLabel.setText(results.size() + " 份");
    }

    private VBox emptyState() {
        FontIcon icon = new FontIcon("mdi2c-clipboard-outline");
        icon.setIconSize(34);
        icon.getStyleClass().add("history-empty-icon");
        Label title = new Label("还没有测评记录");
        title.getStyleClass().add("history-empty-title");
        Label copy = new Label("完成一次霍兰德或 MBTI 测评后，报告会显示在这里");
        copy.getStyleClass().add("history-empty-copy");
        VBox box = new VBox(8, icon, title, copy);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private final class AssessmentHistoryCell extends ListCell<AssessmentResultDto> {
        @Override
        protected void updateItem(AssessmentResultDto item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(empty || item == null ? null : card(item));
        }

        private HBox card(AssessmentResultDto item) {
            boolean holland = "HOLLAND".equals(item.templateCode());
            FontIcon icon = new FontIcon(holland ? "mdi2c-compass-outline" : "mdi2b-brain");
            icon.setIconSize(24);
            icon.getStyleClass().add("career-history-row-icon");
            StackPane iconBox = new StackPane(icon);
            iconBox.getStyleClass().addAll("career-history-icon-box",
                    holland ? "career-history-icon-holland" : "career-history-icon-mbti");

            String type = holland ? "霍兰德职业兴趣测评" : "MBTI 性格类型测评";
            Label title = new Label(type);
            title.getStyleClass().add("career-history-row-title");
            Label code = new Label(item.resultCode() == null ? "结果待生成" : item.resultCode());
            code.getStyleClass().add("career-result-code");
            HBox titleRow = new HBox(9, title, code);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label description = new Label(holland
                    ? "职业兴趣类型与优势方向分析"
                    : "性格偏好、决策方式与协作风格分析");
            description.getStyleClass().add("career-history-row-description");
            Label date = new Label(item.createTime() == null ? "测评时间未知"
                    : "完成于 " + TIME_FORMAT.format(item.createTime()));
            date.getStyleClass().add("career-history-row-meta");
            VBox identity = new VBox(5, titleRow, description, date);
            identity.setMinWidth(0);
            HBox.setHgrow(identity, Priority.ALWAYS);

            Button view = new Button("查看报告");
            view.getStyleClass().add("history-row-primary");
            view.setOnAction(event -> {
                event.consume();
                getListView().getSelectionModel().select(item);
                viewSelected();
            });
            Button delete = new Button("删除");
            delete.getStyleClass().addAll("history-row-action", "history-row-danger");
            delete.setOnAction(event -> {
                event.consume();
                getListView().getSelectionModel().select(item);
                deleteSelected();
            });
            view.setOnMouseClicked(event -> event.consume());
            delete.setOnMouseClicked(event -> event.consume());
            HBox actions = new HBox(8, view, delete);
            actions.setAlignment(Pos.CENTER_RIGHT);

            HBox card = new HBox(14, iconBox, identity, actions);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.getStyleClass().add("career-history-row-card");
            card.setMaxWidth(Double.MAX_VALUE);
            card.prefWidthProperty().bind(resultListView.widthProperty().subtract(24));
            return card;
        }
    }
}
