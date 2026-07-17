package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.enums.SimulationType;

import java.time.LocalDateTime;

public class ScenarioSessionEntity {
    private String id;
    private Long userId;
    private Long interviewSessionId;
    private SimulationType scenarioType;
    private ScenarioStatus status;
    private String stateJson;
    private int currentRound;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getInterviewSessionId() { return interviewSessionId; }
    public void setInterviewSessionId(Long interviewSessionId) { this.interviewSessionId = interviewSessionId; }
    public SimulationType getScenarioType() { return scenarioType; }
    public void setScenarioType(SimulationType scenarioType) { this.scenarioType = scenarioType; }
    public ScenarioStatus getStatus() { return status; }
    public void setStatus(ScenarioStatus status) { this.status = status; }
    public String getStateJson() { return stateJson; }
    public void setStateJson(String stateJson) { this.stateJson = stateJson; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
