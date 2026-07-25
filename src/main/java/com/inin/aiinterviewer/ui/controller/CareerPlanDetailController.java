package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CareerPlanDto;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Scope("prototype")
public class CareerPlanDetailController implements ContextAwareController<CareerPlanDto> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContentNavigator navigator;

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label createdLabel;
    @FXML private Label currentRoleLabel;
    @FXML private Label targetRoleLabel;
    @FXML private Label backgroundLabel;
    @FXML private MarkdownView markdownView;

    public CareerPlanDetailController(ContentNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void initializeContext(CareerPlanDto plan) {
        if (plan == null) {
            titleLabel.setText("职业规划详情");
            subtitleLabel.setText("未找到规划内容");
            markdownView.setMarkdown("暂无可展示的职业规划。");
            return;
        }
        String target = text(plan.targetRole(), "职业发展规划");
        titleLabel.setText(target);
        subtitleLabel.setText("从当前定位到目标岗位的完整发展路线");
        createdLabel.setText(plan.createTime() == null ? "保存时间未知" : TIME_FORMAT.format(plan.createTime()));
        currentRoleLabel.setText(text(plan.currentRole(), "未填写"));
        targetRoleLabel.setText(target);
        backgroundLabel.setText(background(plan));
        markdownView.setMarkdown(text(plan.planMarkdown(), "暂无可展示的职业规划。"));
    }

    @FXML
    private void back() {
        navigator.back();
    }

    private String background(CareerPlanDto plan) {
        String industry = text(plan.industry(), "");
        String experience = text(plan.experienceYears(), "");
        if (industry.isBlank() && experience.isBlank()) return "未填写";
        if (industry.isBlank()) return experience;
        if (experience.isBlank()) return industry;
        return industry + " · " + experience;
    }

    private String text(String value, String fallback) {
        return Optional.ofNullable(value).map(String::strip).filter(item -> !item.isBlank()).orElse(fallback);
    }
}
