package com.kletsai.chat;

import com.kletsai.ai.AiProperties;
import com.kletsai.ai.TutorClient;
import com.kletsai.ai.TutorClient.Turn;
import com.kletsai.ai.TutorReply;
import com.kletsai.ai.TutorReplyParser;
import com.kletsai.chat.ChatDtos.ConversationDetail;
import com.kletsai.chat.ChatDtos.MessageView;
import com.kletsai.chat.ChatDtos.TurnResult;
import com.kletsai.common.CurrentUser;
import com.kletsai.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs chat turns. The Claude call happens outside any DB transaction (it can take seconds);
 * the learner message and the tutor reply are then saved together.
 */
@Service
public class ChatService {

    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final TutorClient tutor;
    private final TutorReplyParser parser;
    private final JsonMapper json;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final int maxHistory;

    public ChatService(ConversationRepository conversations, ChatMessageRepository messages, TutorClient tutor,
                       TutorReplyParser parser, JsonMapper json, CurrentUser currentUser, Clock clock,
                       TransactionTemplate tx, AiProperties aiProperties) {
        this.conversations = conversations;
        this.messages = messages;
        this.tutor = tutor;
        this.parser = parser;
        this.json = json;
        this.currentUser = currentUser;
        this.clock = clock;
        this.tx = tx;
        this.maxHistory = aiProperties.maxHistoryMessages();
    }

    public List<Conversation> recent() {
        return conversations.findTop20ByUserIdOrderByCreatedAtDesc(currentUser.id());
    }

    /** Creates the conversation and asks the tutor for its opening line. Nothing is saved if Claude fails. */
    public ConversationDetail start(Scenario scenario) {
        TutorReply opening = tutor.respond(scenario.instructions(), List.of(), null);
        Instant now = clock.instant();
        return tx.execute(status -> {
            Conversation c = conversations.save(new Conversation(currentUser.id(), scenario, now));
            messages.save(new ChatMessage(c.getId(), ChatMessage.Role.ASSISTANT, json.writeValueAsString(opening), now));
            return detail(c);
        });
    }

    public ConversationDetail get(UUID id) {
        return detail(load(id));
    }

    public TurnResult send(UUID conversationId, String learnerText) {
        Conversation c = load(conversationId);
        String text = learnerText.strip();

        List<ChatMessage> all = messages.findByConversationIdOrderByCreatedAtAsc(c.getId());
        List<Turn> history = all.subList(Math.max(0, all.size() - maxHistory), all.size()).stream()
                .map(m -> new Turn(m.getRole() == ChatMessage.Role.USER ? Turn.Role.LEARNER : Turn.Role.TUTOR, m.getText()))
                .toList();
        // The API expects alternating turns after the synthetic start message, so history must begin with the tutor.
        if (!history.isEmpty() && history.getFirst().role() == Turn.Role.LEARNER) {
            history = history.subList(1, history.size());
        }

        TutorReply reply = tutor.respond(c.getScenario().instructions(), history, text);

        Instant now = clock.instant();
        return tx.execute(status -> {
            ChatMessage learner = messages.save(new ChatMessage(c.getId(), ChatMessage.Role.USER, text, now));
            ChatMessage answer = messages.save(new ChatMessage(c.getId(), ChatMessage.Role.ASSISTANT,
                    json.writeValueAsString(reply), now.plusMillis(1)));
            return new TurnResult(
                    new MessageView(learner.getId(), learner.getRole(), learner.getText(), null, reply.corrections(), learner.getCreatedAt()),
                    new MessageView(answer.getId(), answer.getRole(), null, reply.reply(), null, answer.getCreatedAt()));
        });
    }

    private Conversation load(UUID id) {
        return conversations.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Conversation", id));
    }

    /** Corrections are stored in the tutor reply that follows a learner message, so attach them back to it. */
    private ConversationDetail detail(Conversation c) {
        List<ChatMessage> all = messages.findByConversationIdOrderByCreatedAtAsc(c.getId());
        List<MessageView> views = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            ChatMessage m = all.get(i);
            if (m.getRole() == ChatMessage.Role.ASSISTANT) {
                views.add(new MessageView(m.getId(), m.getRole(), null, parser.parse(m.getText()).reply(), null, m.getCreatedAt()));
            } else {
                List<TutorReply.Correction> corrections = i + 1 < all.size()
                        && all.get(i + 1).getRole() == ChatMessage.Role.ASSISTANT
                        ? parser.parse(all.get(i + 1).getText()).corrections()
                        : List.of();
                views.add(new MessageView(m.getId(), m.getRole(), m.getText(), null, corrections, m.getCreatedAt()));
            }
        }
        return new ConversationDetail(c.getId(), c.getScenario(), c.getTitle(), c.getCreatedAt(), views);
    }
}
