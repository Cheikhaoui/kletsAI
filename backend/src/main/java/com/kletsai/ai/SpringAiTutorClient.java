package com.kletsai.ai;

import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.OutputConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicCacheOptions;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Calls Claude through Spring AI with native structured output, so every reply matches the JSON schema. */
@Component
@EnableConfigurationProperties(AiProperties.class)
public class SpringAiTutorClient implements TutorClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiTutorClient.class);

    /** The API needs a user message first; the tutor's opening line is a reply to this. */
    static final String START_MESSAGE = "[The learner opened this scene. Greet them and start the role-play.]";

    private final ChatModel chatModel;
    private final TutorReplyParser parser;
    private final AiProperties props;
    private final String systemPrompt;
    private final String schema;
    private final boolean apiKeyMissing;

    public SpringAiTutorClient(ChatModel chatModel, TutorReplyParser parser, AiProperties props,
                               @Value("${spring.ai.anthropic.api-key:}") String apiKey) {
        this.chatModel = chatModel;
        this.apiKeyMissing = apiKey.isBlank() || apiKey.equals("not-set") || apiKey.endsWith("...");
        if (apiKeyMissing) {
            log.warn("ANTHROPIC_API_KEY is not set (or is still the .env.example placeholder): chat will return 503.");
        }
        this.parser = parser;
        this.props = props;
        this.systemPrompt = read("prompts/tutor-system.md");
        this.schema = read("prompts/tutor-reply.schema.json");
    }

    @Override
    public TutorReply respond(String scenarioInstructions, List<Turn> history, String learnerText) {
        if (apiKeyMissing) {
            throw new TutorUnavailableException(
                    "The AI tutor isn't set up: ANTHROPIC_API_KEY is missing. Put your key in .env and restart the backend.");
        }
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt + scenarioInstructions));
        messages.add(new UserMessage(START_MESSAGE));
        for (Turn t : history) {
            messages.add(t.role() == Turn.Role.LEARNER
                    ? new UserMessage(t.text())
                    : AssistantMessage.builder().content(t.text()).build());
        }
        if (learnerText != null) {
            messages.add(new UserMessage(learnerText));
        }

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(props.model())
                .maxTokens(props.maxTokens())
                .thinkingAdaptive()
                .effort(OutputConfig.Effort.of(props.effort().toLowerCase(Locale.ROOT)))
                .outputSchema(schema)
                // System prompt + history stay identical between turns, so cache them.
                .cacheOptions(AnthropicCacheOptions.builder().strategy(AnthropicCacheStrategy.CONVERSATION_HISTORY).build())
                .build();

        ChatResponse response;
        try {
            response = chatModel.call(new org.springframework.ai.chat.prompt.Prompt(messages, options));
        } catch (RuntimeException e) {
            log.warn("Claude call failed", e);
            throw new TutorUnavailableException(describe(e), e);
        }

        Generation result = response.getResult();
        String finish = result == null ? null : result.getMetadata().getFinishReason();
        if (finish != null && finish.toLowerCase(Locale.ROOT).contains("refusal")) {
            throw new TutorUnavailableException("The tutor declined to answer this message");
        }
        if (finish != null && finish.toLowerCase(Locale.ROOT).contains("max_tokens")) {
            throw new TutorUnavailableException("The tutor's reply was cut off");
        }
        String text = result == null ? null : result.getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new TutorUnavailableException("The tutor returned an empty reply");
        }
        return parser.parse(text);
    }

    /** A message the learner (and developer) can act on, based on the Anthropic SDK error in the cause chain. */
    static String describe(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof UnauthorizedException || t instanceof PermissionDeniedException) {
                return "The AI tutor isn't set up: ANTHROPIC_API_KEY is missing or invalid. Put your key in .env and restart the backend.";
            }
            if (t instanceof RateLimitException) {
                return "The AI tutor is busy right now. Try again in a minute.";
            }
            if (t instanceof BadRequestException) {
                return "The AI request was rejected: " + t.getMessage();
            }
            if (t instanceof AnthropicIoException) {
                return "Could not reach the AI tutor. Check the internet connection of the backend.";
            }
        }
        return "Could not reach the AI tutor";
    }

    private static String read(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
