package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CandidateProfileListItemDto;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class ProfileController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CandidateProfileService profileService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;

    @FXML private TableView<CandidateProfileListItemDto> profileTable;
    @FXML private TableColumn<CandidateProfileListItemDto, String> resumeColumn;
    @FXML private TableColumn<CandidateProfileListItemDto, String> nameColumn;
    @FXML private TableColumn<CandidateProfileListItemDto, String> roleColumn;
    @FXML private TableColumn<CandidateProfileListItemDto, String> skillsColumn;
    @FXML private TableColumn<CandidateProfileListItemDto, String> sourceColumn;
    @FXML private TableColumn<CandidateProfileListItemDto, String> statusColumn;
    @FXML private TableColumn<CandidateProfileListItemDto, LocalDateTime> updateTimeColumn;
    @FXML private Label summaryLabel;
    @FXML private Button editButton;

    public ProfileController(
            CandidateProfileService profileService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator
    ) {
        this.profileService = profileService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
    }

    @FXML
    private void initialize() {
        resumeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().resumeName()));
        nameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(fallback(cell.getValue().candidateName())));
        roleColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(fallback(cell.getValue().targetRole())));
        skillsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(String.join("、", cell.getValue().skills())));
        sourceColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(sourceText(cell.getValue().source())));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().confirmed() ? "已确认" : "待确认"));
        updateTimeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().updateTime()));
        updateTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : TIME_FORMAT.format(value));
            }
        });
        editButton.disableProperty().bind(profileTable.getSelectionModel().selectedItemProperty().isNull());
        profileTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && profileTable.getSelectionModel().getSelectedItem() != null) {
                editSelected();
            }
        });
        refresh();
    }

    @FXML
    private void refresh() {
        var profiles = profileService.list(sessionState.requireCurrentUser().id());
        profileTable.getItems().setAll(profiles);
        long confirmed = profiles.stream().filter(CandidateProfileListItemDto::confirmed).count();
        summaryLabel.setText("共 " + profiles.size() + " 份画像 · 已确认 " + confirmed + " 份");
    }

    @FXML
    private void editSelected() {
        CandidateProfileListItemDto selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/resume-detail-view.fxml", "编辑候选人画像", selected.resumeId());
        }
    }

    private String sourceText(ProfileSource source) {
        return switch (source) {
            case AI -> "AI 生成";
            case LOCAL_DRAFT -> "本地草稿";
            case MANUAL -> "人工编辑";
        };
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "待补充" : value;
    }
}
