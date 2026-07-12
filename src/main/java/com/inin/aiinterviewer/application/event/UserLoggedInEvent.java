package com.inin.aiinterviewer.application.event;

import com.inin.aiinterviewer.application.dto.UserDto;

public record UserLoggedInEvent(UserDto user) {
}

