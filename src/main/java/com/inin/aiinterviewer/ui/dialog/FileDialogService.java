package com.inin.aiinterviewer.ui.dialog;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Boundary for native file dialogs. UI workflow tests can replace this service
 * with a deterministic path provider without automating operating-system windows.
 */
public interface FileDialogService {

    Optional<Path> chooseResume(Window owner);

    Optional<Path> chooseKnowledgeDocument(Window owner);

    default Optional<Path> choosePlanIcon(Window owner) {
        return Optional.empty();
    }

    default Optional<Path> choosePlanImport(Window owner) {
        return Optional.empty();
    }

    default Optional<Path> choosePlanExport(Window owner, String suggestedName) {
        return Optional.empty();
    }
}
