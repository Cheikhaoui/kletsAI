package com.kletsai.chat;

import com.kletsai.chat.ChatDtos.ConversationDetail;
import com.kletsai.chat.ChatDtos.ConversationSummary;
import com.kletsai.chat.ChatDtos.ScenarioView;
import com.kletsai.chat.ChatDtos.SendMessageRequest;
import com.kletsai.chat.ChatDtos.StartConversationRequest;
import com.kletsai.chat.ChatDtos.TurnResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/scenarios")
    public List<ScenarioView> scenarios() {
        return Arrays.stream(Scenario.values()).map(ScenarioView::from).toList();
    }

    @GetMapping("/conversations")
    public List<ConversationSummary> conversations() {
        return chat.recent().stream().map(ConversationSummary::from).toList();
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDetail start(@Valid @RequestBody StartConversationRequest request) {
        return chat.start(request.scenario());
    }

    @GetMapping("/conversations/{id}")
    public ConversationDetail get(@PathVariable UUID id) {
        return chat.get(id);
    }

    @PostMapping("/conversations/{id}/messages")
    public TurnResult send(@PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {
        return chat.send(id, request.text());
    }
}
