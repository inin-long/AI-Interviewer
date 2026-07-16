package com.inin.aiinterviewer.domain.model;

import java.io.Serializable;

public record ScenarioConstraint(
        String code,
        String description,
        boolean hard,
        boolean active
) implements Serializable {
    public ScenarioConstraint {
        code = code == null ? "" : code.strip();
        description = description == null ? "" : description.strip();
    }
}
