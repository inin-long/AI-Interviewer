package com.inin.aiinterviewer.application.mapper;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.domain.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {
    public UserDto toDto(UserEntity entity) {
        return new UserDto(entity.getId(), entity.getUsername(), entity.getNickname(), entity.getCreateTime());
    }
}

