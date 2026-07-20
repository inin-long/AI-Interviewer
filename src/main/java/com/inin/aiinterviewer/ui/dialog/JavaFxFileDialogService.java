package com.inin.aiinterviewer.ui.dialog;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class JavaFxFileDialogService implements FileDialogService {

    @Override
    public Optional<Path> chooseResume(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择简历");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "简历文件 (*.pdf, *.docx, *.md, *.txt)", "*.pdf", "*.docx", "*.md", "*.txt"));
        File selected = chooser.showOpenDialog(owner);
        return selected == null ? Optional.empty() : Optional.of(selected.toPath());
    }

    @Override
    public Optional<Path> chooseKnowledgeDocument(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("上传知识文档");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "知识文档 (*.pdf, *.docx, *.md, *.txt)", "*.pdf", "*.docx", "*.md", "*.txt"));
        File selected = chooser.showOpenDialog(owner);
        return selected == null ? Optional.empty() : Optional.of(selected.toPath());
    }

    @Override
    public Optional<Path> choosePlanIcon(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择方案图标");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "图片文件 (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
        File selected = chooser.showOpenDialog(owner);
        return selected == null ? Optional.empty() : Optional.of(selected.toPath());
    }

    @Override
    public Optional<Path> choosePlanImport(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入面试方案");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("面试方案 (*.json)", "*.json"));
        File selected = chooser.showOpenDialog(owner);
        return selected == null ? Optional.empty() : Optional.of(selected.toPath());
    }

    @Override
    public Optional<Path> choosePlanExport(Window owner, String suggestedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出面试方案");
        chooser.setInitialFileName((suggestedName == null || suggestedName.isBlank()
                ? "interview-plan" : suggestedName.replaceAll("[\\\\/:*?\"<>|]", "_")) + ".json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("面试方案 (*.json)", "*.json"));
        File selected = chooser.showSaveDialog(owner);
        return selected == null ? Optional.empty() : Optional.of(selected.toPath());
    }
}
