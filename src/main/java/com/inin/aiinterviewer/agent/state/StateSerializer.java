package com.inin.aiinterviewer.agent.state;

public interface StateSerializer {
    String serialize(InterviewState state);

    InterviewState deserialize(String json);
}

