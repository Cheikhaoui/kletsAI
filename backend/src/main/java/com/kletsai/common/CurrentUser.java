package com.kletsai.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The one place that knows who the current user is.
 * Today it returns the seeded single user; when login is added, read the id from the security context here.
 */
@Component
public class CurrentUser {

    private final UUID id;

    public CurrentUser(@Value("${kletsai.single-user-id}") UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
