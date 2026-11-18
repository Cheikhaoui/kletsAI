package com.kletsai.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Scenario scenario;

    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    public Conversation(UUID userId, Scenario scenario, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.scenario = scenario;
        this.title = scenario.titleNl();
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Scenario getScenario() { return scenario; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
}
