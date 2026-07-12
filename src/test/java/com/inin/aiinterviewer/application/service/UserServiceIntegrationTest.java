package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.entity.UserEntity;
import com.inin.aiinterviewer.infrastructure.database.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void registersAndAuthenticatesLocalUserWithHashedPassword() {
        UserDto registered = userService.register("mahoo.dev", "Mahoo", "safe-password");

        assertThat(registered.id()).isPositive();
        assertThat(registered.username()).isEqualTo("mahoo.dev");
        assertThat(registered.nickname()).isEqualTo("Mahoo");

        UserEntity entity = userMapper.findByUsername("MAHOO.DEV").orElseThrow();
        assertThat(entity.getPasswordHash()).startsWith("$2");
        assertThat(entity.getPasswordHash()).isNotEqualTo("safe-password");

        UserDto loggedIn = userService.login("Mahoo.Dev", "safe-password");
        assertThat(loggedIn.id()).isEqualTo(registered.id());
    }

    @Test
    void rejectsDuplicateUsernameAndInvalidCredentials() {
        userService.register("unique-user", "Unique", "safe-password");

        assertThatThrownBy(() -> userService.register("UNIQUE-USER", "Other", "safe-password"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);

        assertThatThrownBy(() -> userService.login("unique-user", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_INVALID_CREDENTIALS);
    }
}

