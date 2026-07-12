package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonStateSerializerTest {

    @Test
    void roundTripsCurrentStateVersion() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        InterviewState state = new InterviewState(
                InterviewState.CURRENT_VERSION,
                12,
                34,
                InterviewStage.INTRODUCTION,
                List.of(),
                "请简单介绍一下自己。",
                null,
                null,
                null,
                null,
                Map.of("durationMinutes", 45),
                ""
        );

        InterviewState restored = serializer.deserialize(serializer.serialize(state));

        assertThat(restored).isEqualTo(state);
    }
}

