package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.DomainPackDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.DomainPackService;
import com.inin.aiinterviewer.domain.entity.DomainPackEntity;
import com.inin.aiinterviewer.domain.model.DomainPack;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Scope("prototype")
public class DomainPackManagerController {

    private final DomainPackService domainPackService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private ListView<DomainPackDto> packListView;
    @FXML private TextField nameField;
    @FXML private TextField roleField;
    @FXML private TextField industryField;
    @FXML private TextArea knowledgeArea;
    @FXML private TextArea jdArea;
    @FXML private Label statusLabel;
    @FXML private Button deleteButton;

    public DomainPackManagerController(
            DomainPackService domainPackService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.domainPackService = domainPackService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        packListView.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(DomainPackDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String badge = DomainPackEntity.SOURCE_USER.equals(item.source()) ? "（自建）" : "（内置）";
                    setText(item.displayName() + " · v" + item.version() + "  " + badge);
                }
            }
        });
        loadPacks();
    }

    /** 由计划编辑器在打开对话框前调用，预填 JD 文本。 */
    public void prefill(String jobDescription) {
        if (jobDescription != null && !jobDescription.isBlank()) {
            jdArea.setText(jobDescription);
        }
    }

    private void loadPacks() {
        try {
            List<DomainPackDto> packs = domainPackService.list();
            packListView.getItems().setAll(packs);
            deleteButton.setDisable(packs.isEmpty());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void onCreate() {
        try {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String knowledge = knowledgeArea.getText() == null ? "" : knowledgeArea.getText();
            List<String> lines = Arrays.stream(knowledge.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .limit(12)
                    .toList();
            if (name.isEmpty()) {
                status("请填写知识包名称");
                return;
            }
            if (lines.isEmpty()) {
                status("请至少填写一条领域知识要点（每行一条）");
                return;
            }
            List<DomainPack.CompetencyDefinition> competencies = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String code = "CUST_" + (i + 1);
                String label = line.length() > 24 ? line.substring(0, 24) : line;
                competencies.add(new DomainPack.CompetencyDefinition(code, label, line, 0.8, List.of()));
            }
            String role = (roleField.getText() == null || roleField.getText().isBlank())
                    ? "user" : roleField.getText().trim();
            String industry = (industryField.getText() == null || industryField.getText().isBlank())
                    ? null : industryField.getText().trim();
            DomainPack pack = new DomainPack(null, role, industry, "1.0.0", name,
                    competencies, List.of(), List.of(), List.of(), List.of(), List.of());
            domainPackService.saveUserPack(pack);
            status("已保存知识包：" + name);
            nameField.clear();
            roleField.clear();
            industryField.clear();
            knowledgeArea.clear();
            loadPacks();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void onAiGenerate() {
        try {
            String jd = jdArea.getText() == null ? "" : jdArea.getText();
            String name = (nameField.getText() == null || nameField.getText().isBlank())
                    ? null : nameField.getText().trim();
            domainPackService.generateFromJobDescription(jd, name);
            status("AI 已生成并保存知识包");
            jdArea.clear();
            loadPacks();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void onDeleteSelected() {
        try {
            DomainPackDto selected = packListView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status("请先选择一个知识包");
                return;
            }
            if (!DomainPackEntity.SOURCE_USER.equals(selected.source())) {
                status("内置知识包不可删除");
                return;
            }
            domainPackService.deleteUserPack(selected.id());
            status("已删除：" + selected.displayName());
            loadPacks();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void status(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
