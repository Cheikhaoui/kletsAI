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
@Table(name = "chat_message")
public class ChatMessage {

    public enum Role { USER, ASSISTANT }

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Role role;

    /** USER: what the learner typed. ASSISTANT: the tutor's full JSON reply (lines + corrections). */
    @Column(nullable = false, updatable = false)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(UUID conversationId, Role role, String text, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.role = role;
        this.text = text;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public Role getRole() { return role; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}
