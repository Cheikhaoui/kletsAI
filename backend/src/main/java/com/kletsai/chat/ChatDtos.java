package com.kletsai.chat;

import com.kletsai.ai.TutorReply;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record ScenarioView(Scenario id, String titleNl, String titleEn, String description) {
        static ScenarioView from(Scenario s) {
            return new ScenarioView(s, s.titleNl(), s.titleEn(), s.description());
        }
    }

    public record StartConversationRequest(@NotNull Scenario scenario) {
    }

    public record SendMessageRequest(@NotBlank @Size(max = 1000) String text) {
    }

    public record ConversationSummary(UUID id, Scenario scenario, String title, Instant createdAt) {
        static ConversationSummary from(Conversation c) {
            return new ConversationSummary(c.getId(), c.getScenario(), c.getTitle(), c.getCreatedAt());
        }
    }

    /**
     * USER messages have {@code text} and the {@code corrections} the tutor gave for them.
     * ASSISTANT messages have {@code lines}.
     */
    public record MessageView(UUID id, ChatMessage.Role role, String text, List<TutorReply.Line> lines,
                              List<TutorReply.Correction> corrections, Instant createdAt) {
    }

    public record ConversationDetail(UUID id, Scenario scenario, String title, Instant createdAt,
                                     List<MessageView> messages) {
    }

    /** The result of sending one message: the learner's message (with corrections) and the tutor's answer. */
    public record TurnResult(MessageView learner, MessageView tutor) {
    }
}
