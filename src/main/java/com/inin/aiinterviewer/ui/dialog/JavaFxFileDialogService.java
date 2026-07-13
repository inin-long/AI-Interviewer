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
}
