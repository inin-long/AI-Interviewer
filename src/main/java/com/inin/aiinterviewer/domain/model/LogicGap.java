package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.LogicGapType;

import java.io.Serializable;
import java.util.List;

public record LogicGap(
        LogicGapType type,
        String description,
        double severity,
        List<String> relatedClaimIds
) implements Serializable {
    public LogicGap {
        description = description == null ? "" : description.strip();
        relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
    }
}
