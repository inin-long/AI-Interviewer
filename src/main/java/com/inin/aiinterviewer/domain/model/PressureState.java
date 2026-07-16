package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.PressureLevel;

import java.io.Serializable;

public record PressureState(
        PressureLevel level,
        int consecutivePressureTurns,
        String lastTargetKey,
        int repeatedTargetTurns,
        boolean sufficientEvidence,
        boolean lowered,
        boolean safetyAdjusted,
        String reason
) implements Serializable {

    public PressureState {
        level = level == null ? PressureLevel.STANDARD : level;
        consecutivePressureTurns = Math.max(0, consecutivePressureTurns);
        lastTargetKey = lastTargetKey == null ? "" : lastTargetKey.strip();
        repeatedTargetTurns = Math.max(0, repeatedTargetTurns);
        reason = reason == null ? "" : reason.strip();
    }

    public static PressureState initial() {
        return new PressureState(
                PressureLevel.STANDARD, 0, "", 0,
                false, false, false, "尚未执行压力控制");
    }
}
