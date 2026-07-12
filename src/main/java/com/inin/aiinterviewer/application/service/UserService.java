package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.event.UserLoggedInEvent;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.mapper.UserDtoMapper;
import com.inin.aiinterviewer.domain.entity.UserEntity;
import com.inin.aiinterviewer.infrastructure.database.mapper.UserMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserDtoMapper dtoMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(
            UserMapper userMapper,
            UserDtoMapper dtoMapper,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userMapper = userMapper;
        this.dtoMapper = dtoMapper;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserDto register(String username, String nickname, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedNickname = normalizeNickname(nickname, normalizedUsername);
        validatePassword(password);

        if (userMapper.findByUsername(normalizedUsername).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(normalizedUsername);
        entity.setNickname(normalizedNickname);
        entity.setPasswordHash(passwordEncoder.encode(password));

        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserEntity saved = userMapper.findById(entity.getId())
                .orElseThrow(() -> new IllegalStateException("New user could not be reloaded"));
        return dtoMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDto login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        UserEntity entity = userMapper.findByUsername(normalizedUsername)
                .filter(user -> password != null && passwordEncoder.matches(password, user.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_INVALID_CREDENTIALS));

        UserDto user = dtoMapper.toDto(entity);
        eventPublisher.publishEvent(new UserLoggedInEvent(user));
        return user;
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String normalized = username.trim();
        if (!normalized.matches("[A-Za-z0-9_.-]{3,64}")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return normalized;
    }

    private String normalizeNickname(String nickname, String fallback) {
        String normalized = nickname == null ? "" : nickname.trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        if (normalized.length() > 64) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
}

