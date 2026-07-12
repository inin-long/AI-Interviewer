package com.inin.aiinterviewer.ui.state;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.event.UserLoggedInEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class UserSessionState {

    private final AtomicReference<UserDto> currentUser = new AtomicReference<>();

    public Optional<UserDto> currentUser() {
        return Optional.ofNullable(currentUser.get());
    }

    public UserDto requireCurrentUser() {
        UserDto user = currentUser.get();
        if (user == null) {
            throw new IllegalStateException("No local user is currently logged in");
        }
        return user;
    }

    public void logIn(UserDto user) {
        currentUser.set(user);
    }

    @EventListener
    public void onUserLoggedIn(UserLoggedInEvent event) {
        logIn(event.user());
    }

    public void logOut() {
        currentUser.set(null);
    }
}
