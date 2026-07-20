package com.inin.aiinterviewer.ui.component;

import com.inin.aiinterviewer.application.dto.CandidateProfileDto;
import com.inin.aiinterviewer.application.dto.ResumeDetailDto;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Candidate portrait preview used inside the shared right-side {@link DrawerPane}.
 * The view deliberately owns presentation only; the resume controller supplies
 * the current data and the actions that close the resume-to-interview loop.
 */
public final class ResumeProfileDrawerView extends BorderPane {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VBox content = new VBox(14);
    private final Button generateButton = actionButton("生成候选人画像", "mdi2s-star-four-points-outline", "resume-drawer-generate-button");
    private final Button editButton = actionButton("编辑画像", "mdi2p-pencil-outline", "resume-drawer-outline-button");
    private final Button confirmButton = actionButton("确认并保存画像", "mdi2c-check-circle-outline", "resume-drawer-primary-button");
    private final Button interviewButton = actionButton("进入模拟面试", "mdi2p-play", "resume-drawer-primary-button");

    public ResumeProfileDrawerView() {
        getStyleClass().add("resume-profile-drawer");
        content.setPadding(new Insets(18, 20, 22, 20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("resume-drawer-scroll");
        setCenter(scrollPane);

        HBox actions = new HBox(9, editButton, confirmButton, interviewButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(14, 18, 15, 18));
        actions.getStyleClass().add("resume-drawer-actions");
        HBox.setHgrow(editButton, Priority.ALWAYS);
        HBox.setHgrow(confirmButton, Priority.ALWAYS);
        HBox.setHgrow(interviewButton, Priority.ALWAYS);
        setBottom(actions);
    }

    public void render(ResumeDetailDto detail, CandidateProfileDto profile, boolean generating) {
        Objects.requireNonNull(detail, "detail");
        content.getChildren().clear();
        content.getChildren().add(profileHeader(detail, profile));

        if (profile == null) {
            content.getChildren().add(emptyProfile(detail, generating));
            editButton.setDisable(true);
            confirmButton.setDisable(true);
            interviewButton.setDisable(true);
            confirmButton.setText("确认并保存画像");
            return;
        }

        CandidateProfileContent candidate = profile.content();
        VBox summarySection = section("mdi2s-star-four-points-outline", "AI 总结",
                bodyText(value(candidate.summary(), "画像摘要待补充")));
        summarySection.getStyleClass().add("resume-drawer-ai-summary");
        VBox.setVgrow(summarySection, Priority.ALWAYS);
        content.getChildren().addAll(
                section("mdi2s-star-four-points-outline", "核心技能", skillCloud(candidate.skills())),
                section("mdi2b-briefcase-outline", "项目经历", bulletList(candidate.projects(), "暂未提取项目经历")),
                facts(candidate),
                section("mdi2b-bullseye-arrow", "适配岗位", bodyText(value(candidate.targetRole(), "待补充目标岗位"))),
                summarySection
        );

        editButton.setDisable(false);
        confirmButton.setDisable(false);
        confirmButton.setText("确认并保存画像");
        interviewButton.setDisable(!profile.confirmed());
    }

    public void setOnGenerate(Runnable action) { generateButton.setOnAction(event -> action.run()); }
    public void setOnEdit(Runnable action) { editButton.setOnAction(event -> action.run()); }
    public void setOnConfirm(Runnable action) { confirmButton.setOnAction(event -> action.run()); }
    public void setOnInterview(Runnable action) { interviewButton.setOnAction(event -> action.run()); }

    private HBox profileHeader(ResumeDetailDto detail, CandidateProfileDto profile) {
        Image avatarImage = new Image(Objects.requireNonNull(
                getClass().getResource("/images/resume/candidate-avatar.png")).toExternalForm(),
                160, 160, true, true);
        ImageView avatar = new ImageView(avatarImage);
        avatar.setFitWidth(64);
        avatar.setFitHeight(64);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);
        avatar.setClip(new Circle(32, 32, 32));
        avatar.getStyleClass().add("resume-drawer-avatar");

        String name = profile == null ? candidateName(detail.resume().originalName())
                : value(profile.content().fullName(), candidateName(detail.resume().originalName()));
        String role = profile == null ? "画像尚未生成"
                : value(profile.content().targetRole(), "目标岗位待补充");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("resume-drawer-name");
        Label roleLabel = new Label(role + (profile == null ? "" : "  ·  " + value(profile.content().yearsExperience(), "经验待补充")));
        roleLabel.getStyleClass().add("resume-drawer-role");
        Label updateLabel = new Label("更新于 " + TIME_FORMAT.format(
                profile == null ? detail.resume().updateTime() : profile.updateTime()));
        updateLabel.getStyleClass().add("resume-drawer-updated");
        VBox identity = new VBox(4, nameLabel, roleLabel, updateLabel);
        HBox.setHgrow(identity, Priority.ALWAYS);

        ProgressIndicator quality = new ProgressIndicator(profile == null ? 0.34 : profile.confirmed() ? 0.92 : 0.76);
        quality.setPrefSize(48, 48);
        quality.setMinSize(48, 48);
        quality.getStyleClass().add("resume-quality-indicator");
        Label qualityLabel = new Label("解析质量");
        qualityLabel.getStyleClass().add("resume-quality-label");
        VBox score = new VBox(3, qualityLabel, quality);
        score.setAlignment(Pos.CENTER);

        HBox header = new HBox(13, avatar, identity, score);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("resume-drawer-profile-header");

        VBox wrapper = new VBox(header);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.getStyleClass().add("resume-drawer-profile-block");
        HBox container = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return container;
    }

    private VBox emptyProfile(ResumeDetailDto detail, boolean generating) {
        FontIcon icon = new FontIcon(generating ? "mdi2l-loading" : "mdi2a-account-plus-outline");
        icon.setIconSize(34);
        icon.getStyleClass().add("resume-empty-profile-icon");
        Label title = new Label(generating ? "正在生成候选人画像" : "等待生成候选人画像");
        title.getStyleClass().add("resume-empty-profile-title");
        String message = generating
                ? "系统正在分析技能、项目经历和岗位匹配度，完成后会自动刷新当前 Drawer。"
                : detail.resume().status().name().equals("COMPLETED")
                ? "简历文本已经提取完成。生成画像后即可核对信息、确认保存并进入模拟面试。"
                : "简历仍在解析，解析完成后才可生成候选人画像。";
        Label description = new Label(message);
        description.setWrapText(true);
        description.getStyleClass().add("resume-empty-profile-description");
        VBox box = new VBox(12, icon, title, description);
        if (!generating && detail.resume().status().name().equals("COMPLETED")) {
            box.getChildren().add(generateButton);
        }
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("resume-empty-profile");
        return box;
    }

    private VBox section(String iconLiteral, String title, Region body) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(16);
        icon.getStyleClass().add("resume-drawer-section-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("resume-drawer-section-title");
        HBox heading = new HBox(8, icon, titleLabel);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(10, heading, body);
        section.getStyleClass().add("resume-drawer-section");
        return section;
    }

    private FlowPane skillCloud(List<String> skills) {
        FlowPane cloud = new FlowPane(7, 7);
        cloud.getStyleClass().add("resume-skill-cloud");
        List<String> visibleSkills = skills == null ? List.of() : skills.stream().limit(10).toList();
        if (visibleSkills.isEmpty()) visibleSkills = List.of("技能待补充");
        for (String skill : visibleSkills) {
            Label chip = new Label(skill);
            chip.getStyleClass().add("resume-skill-chip");
            cloud.getChildren().add(chip);
        }
        return cloud;
    }

    private VBox bulletList(List<String> values, String emptyText) {
        VBox list = new VBox(7);
        List<String> visibleValues = values == null ? List.of() : values.stream().limit(3).toList();
        if (visibleValues.isEmpty()) visibleValues = List.of(emptyText);
        for (String value : visibleValues) {
            FontIcon bullet = new FontIcon("mdi2c-circle-small");
            bullet.setIconSize(16);
            bullet.getStyleClass().add("resume-project-bullet");
            Label label = new Label(value);
            label.setWrapText(true);
            label.getStyleClass().add("resume-drawer-body");
            HBox row = new HBox(4, bullet, label);
            row.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(label, Priority.ALWAYS);
            list.getChildren().add(row);
        }
        return list;
    }

    private VBox facts(CandidateProfileContent candidate) {
        VBox experience = factCard("mdi2a-account-tie-outline", "工作年限",
                value(candidate.yearsExperience(), "待补充"), first(candidate.experience(), "经历待补充"));
        experience.setId("resumeExperienceFactCard");
        VBox education = factCard("mdi2s-school-outline", "教育背景",
                value(candidate.education(), "待补充"), "候选人简历提取");
        education.setId("resumeEducationFactCard");
        VBox cards = new VBox(10, experience, education);
        cards.setId("resumeFacts");
        cards.setFillWidth(true);
        cards.getStyleClass().add("resume-drawer-facts");
        return cards;
    }

    private VBox factCard(String iconLiteral, String labelText, String valueText, String hintText) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(19);
        icon.getStyleClass().add("resume-fact-icon");
        Label label = new Label(labelText);
        label.getStyleClass().add("resume-fact-label");
        HBox heading = new HBox(7, icon, label);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label value = new Label(valueText);
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        value.getStyleClass().add("resume-fact-value");
        Label hint = new Label(hintText);
        hint.setWrapText(true);
        hint.getStyleClass().add("resume-fact-hint");
        VBox card = new VBox(5, heading, value, hint);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("resume-fact-card");
        return card;
    }

    private VBox bodyText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("resume-drawer-body");
        VBox box = new VBox(label);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Button actionButton(String text, String iconLiteral, String styleClass) {
        Button button = new Button(text, new FontIcon(iconLiteral));
        button.getStyleClass().add(styleClass);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private String candidateName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "候选人";
        String stem = fileName.replaceFirst("\\.[^.]+$", "");
        int separator = stem.indexOf('_');
        return separator > 0 ? stem.substring(0, separator) : stem;
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String first(List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : values.getFirst();
    }
}
