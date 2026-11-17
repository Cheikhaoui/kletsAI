package com.kletsai.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class MeController {

    public record MeResponse(UUID id, String displayName) {
    }

    private final CurrentUser currentUser;
    private final AppUserRepository users;

    public MeController(CurrentUser currentUser, AppUserRepository users) {
        this.currentUser = currentUser;
        this.users = users;
    }

    @GetMapping
    public MeResponse me() {
        AppUser user = users.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
        return new MeResponse(user.getId(), user.getDisplayName());
    }
}
