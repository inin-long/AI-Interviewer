package com.inin.aiinterviewer.application.task;

import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.CandidateProfileTaskService.CandidateProfileTaskPayload;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Component;

@Component
public class CandidateProfileTaskHandler implements BackgroundTaskHandler {
    private final CandidateProfileService profileService;

    public CandidateProfileTaskHandler(CandidateProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public BackgroundTaskType taskType() {
        return BackgroundTaskType.PROFILE_GENERATE;
    }

    @Override
    public void handle(BackgroundTaskContext context) {
        CandidateProfileTaskPayload payload = context.payload(CandidateProfileTaskPayload.class);
        profileService.generate(context.userId(), payload.resumeId());
    }
}
