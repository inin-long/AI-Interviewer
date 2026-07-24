package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CandidateProfileListItemDto;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class ProfileController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SORT_LATEST = "最近更新";
    private static final String SORT_EARLIEST = "最早更新";
    private static final String SORT_NAME = "按姓名";

    private final CandidateProfileService profileService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;

    /* ListView (replaces old TableView) */
    @FXML private ListView<CandidateProfileListItemDto> profileList;

    /* Stats */
    @FXML private Label totalCountLabel;
    @FXML private Label confirmedCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label aiCountLabel;

    /* Toolbar */
    @FXML private ToggleButton allFilterBtn;
    @FXML private ToggleButton confirmedFilterBtn;
    @FXML private ToggleButton pendingFilterBtn;
    @FXML private TextField searchField;
    @FXML private AppSelect<String> sortSelect;
    @FXML private Button editButton;

    private List<CandidateProfileListItemDto> allProfiles = List.of();

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
        sortSelect.getItems().setAll(SORT_LATEST, SORT_EARLIEST, SORT_NAME);
        sortSelect.setValue(SORT_LATEST);
        sortSelect.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        setupListView();
        setupSelection();
        refresh();
    }

    private void setupListView() {
        profileList.setCellFactory(list -> new ListCell<>() {
            private final VBox root = new VBox(8);
            private final HBox topRow = new HBox(10);
            private final FontIcon sourceIcon = new FontIcon();
            private final Label nameLabel = new Label();
            private final Label roleBadge = new Label();
            private final Label statusBadge = new Label();
            private final HBox metaRow = new HBox(16);
            private final Label resumeLabel = new Label();
            private final Label skillsLabel = new Label();
            private final Label timeLabel = new Label();
            private final Region spacer = new Region();

            {
                root.getStyleClass().add("profile-card");
                root.setPadding(new javafx.geometry.Insets(14, 16, 14, 16));

                // Top row: icon + name + role + status
                topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                sourceIcon.setIconSize(20);
                sourceIcon.getStyleClass().add("profile-card-icon");
                nameLabel.getStyleClass().add("profile-card-name");
                roleBadge.getStyleClass().add("profile-card-role");
                statusBadge.getStyleClass().add("profile-card-status");
                topRow.getChildren().addAll(sourceIcon, nameLabel, roleBadge, spacer, statusBadge);

                // Meta row: resume, skills, time
                metaRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                resumeLabel.getStyleClass().add("profile-card-resume");
                skillsLabel.getStyleClass().add("profile-card-skills");
                timeLabel.getStyleClass().add("profile-card-time");
                metaRow.getChildren().addAll(resumeLabel, skillsLabel, timeLabel);

                root.getChildren().addAll(topRow, metaRow);
            }

            @Override
            protected void updateItem(CandidateProfileListItemDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // Source icon
                switch (item.source()) {
                    case AI -> sourceIcon.setIconLiteral("mdi2b-brain");
                    case LOCAL_DRAFT -> sourceIcon.setIconLiteral("mdi2e-file-edit-outline");
                    case MANUAL -> sourceIcon.setIconLiteral("mdi2h-human-edit");
                }
                sourceIcon.getStyleClass().setAll(List.of("profile-card-icon", "profile-card-icon-" + item.source().name().toLowerCase()));

                nameLabel.setText(fallback(item.candidateName()));
                roleBadge.setText(fallback(item.targetRole()));

                // Status badge
                if (item.confirmed()) {
                    statusBadge.setText("已确认");
                    statusBadge.getStyleClass().setAll(List.of("profile-card-status", "profile-status-confirmed"));
                } else {
                    statusBadge.setText("待确认");
                    statusBadge.getStyleClass().setAll(List.of("profile-card-status", "profile-status-pending"));
                }

                resumeLabel.setText("简历: " + fallback(item.resumeName()));
                String skillsText = item.skills() != null && !item.skills().isEmpty()
                        ? "技能: " + String.join("、", item.skills())
                        : "";
                skillsLabel.setText(skillsText);
                timeLabel.setText(item.updateTime() != null ? TIME_FORMAT.format(item.updateTime()) : "");

                setGraphic(root);
            }
        });
    }

    private void setupSelection() {
        profileList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) ->
                editButton.setDisable(val == null));
        profileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && profileList.getSelectionModel().getSelectedItem() != null) {
                editSelected();
            }
        });
    }

    @FXML
    private void refresh() {
        allProfiles = profileService.list(sessionState.requireCurrentUser().id());
        updateStats();
        applyFilters();
    }

    @FXML
    private void applyFilters() {
        Predicate<CandidateProfileListItemDto> pred = p -> true;

        // Status filter
        if (confirmedFilterBtn.isSelected()) {
            pred = pred.and(CandidateProfileListItemDto::confirmed);
        } else if (pendingFilterBtn.isSelected()) {
            pred = pred.and(p -> !p.confirmed());
        }

        // Search filter
        String keyword = searchField.getText().trim();
        if (!keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            pred = pred.and(p ->
                    fallback(p.candidateName()).toLowerCase().contains(kw)
                            || fallback(p.targetRole()).toLowerCase().contains(kw)
                            || fallback(p.resumeName()).toLowerCase().contains(kw)
                            || p.skills().stream().anyMatch(s -> s.toLowerCase().contains(kw))
            );
        }

        var filtered = allProfiles.stream().filter(pred).collect(Collectors.toList());

        // Sort
        String sortKey = sortSelect.getValue();
        if (sortKey != null) {
            switch (sortKey) {
                case SORT_LATEST -> filtered.sort((a, b) ->
                        b.updateTime().compareTo(a.updateTime()));
                case SORT_EARLIEST -> filtered.sort((a, b) ->
                        a.updateTime().compareTo(b.updateTime()));
                case SORT_NAME -> filtered.sort((a, b) ->
                        fallback(a.candidateName()).compareTo(fallback(b.candidateName())));
            }
        }

        profileList.getItems().setAll(filtered);
    }

    @FXML
    private void editSelected() {
        var selected = profileList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/resume-detail-view.fxml", "编辑候选人画像", selected.resumeId());
        }
    }

    private void updateStats() {
        long total = allProfiles.size();
        long confirmed = allProfiles.stream().filter(CandidateProfileListItemDto::confirmed).count();
        long pending = total - confirmed;
        long aiCount = allProfiles.stream().filter(p -> p.source() == ProfileSource.AI).count();

        totalCountLabel.setText(String.valueOf(total));
        confirmedCountLabel.setText(String.valueOf(confirmed));
        pendingCountLabel.setText(String.valueOf(pending));
        aiCountLabel.setText(String.valueOf(aiCount));
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
