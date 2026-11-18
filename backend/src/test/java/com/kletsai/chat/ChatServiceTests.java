package com.kletsai.chat;

import com.kletsai.TestcontainersConfiguration;
import com.kletsai.ai.TutorClient;
import com.kletsai.ai.TutorClient.Turn;
import com.kletsai.ai.TutorReply;
import com.kletsai.ai.TutorReply.Category;
import com.kletsai.ai.TutorReply.Correction;
import com.kletsai.ai.TutorReply.Line;
import com.kletsai.ai.TutorUnavailableException;
import com.kletsai.chat.ChatDtos.ConversationDetail;
import com.kletsai.chat.ChatDtos.TurnResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ChatServiceTests.FakeTutorConfig.class})
class ChatServiceTests {

    @TestConfiguration
    static class FakeTutorConfig {
        @Bean
        @Primary
        FakeTutorClient fakeTutorClient() {
            return new FakeTutorClient();
        }
    }

    @Autowired ChatService chat;
    @Autowired FakeTutorClient tutor;
    @Autowired ConversationRepository conversations;
    @Autowired MockMvcTester mvc;

    @BeforeEach
    void reset() {
        tutor.reset();
    }

    private static TutorReply reply(String nl, String en, Correction... corrections) {
        return new TutorReply(List.of(new Line(nl, en)), List.of(corrections));
    }

    @Test
    void startingAConversationStoresTheTutorsOpening() {
        tutor.willReply(reply("Goedemorgen! Waar wilt u naartoe?", "Good morning! Where do you want to go?"));

        ConversationDetail c = chat.start(Scenario.TRAIN_TICKET);

        assertThat(c.title()).isEqualTo("Een kaartje kopen");
        assertThat(c.messages()).singleElement().satisfies(m -> {
            assertThat(m.role()).isEqualTo(ChatMessage.Role.ASSISTANT);
            assertThat(m.lines()).extracting(Line::nl).containsExactly("Goedemorgen! Waar wilt u naartoe?");
        });
        assertThat(tutor.calls.getFirst().learnerText()).isNull();
        assertThat(tutor.calls.getFirst().scenarioInstructions()).contains("NS employee");
    }

    @Test
    void sendingAMessageReturnsCorrectionsAndSendsHistory() {
        ConversationDetail c = chat.start(Scenario.TRAIN_TICKET);
        Correction wordOrder = new Correction("Ik wil gaan naar Amsterdam", "Ik wil naar Amsterdam gaan",
                "The infinitive goes to the end.", Category.WORD_ORDER, null);
        tutor.willReply(reply("Een enkele reis of een retour?", "A single or a return?", wordOrder));

        TurnResult turn = chat.send(c.id(), "  Ik wil gaan naar Amsterdam ");

        assertThat(turn.learner().text()).isEqualTo("Ik wil gaan naar Amsterdam");
        assertThat(turn.learner().corrections()).containsExactly(wordOrder);
        assertThat(turn.tutor().lines()).extracting(Line::en).containsExactly("A single or a return?");

        FakeTutorClient.Call call = tutor.calls.getLast();
        assertThat(call.learnerText()).isEqualTo("Ik wil gaan naar Amsterdam");
        assertThat(call.history()).singleElement()
                .satisfies(t -> assertThat(t.role()).isEqualTo(Turn.Role.TUTOR));

        // Reloading shows the correction under the learner's message.
        ConversationDetail reloaded = chat.get(c.id());
        assertThat(reloaded.messages()).hasSize(3);
        assertThat(reloaded.messages().get(1).corrections()).containsExactly(wordOrder);
    }

    @Test
    void nothingIsSavedWhenTheTutorFails() {
        ConversationDetail c = chat.start(Scenario.DOCTOR);
        tutor.willFail(new TutorUnavailableException("down"));

        assertThatThrownBy(() -> chat.send(c.id(), "Ik heb hoofdpijn")).isInstanceOf(TutorUnavailableException.class);

        assertThat(chat.get(c.id()).messages()).hasSize(1);
    }

    @Test
    void tutorFailureIs503ForTheApp() {
        ConversationDetail c = chat.start(Scenario.FREE);
        tutor.willFail(new TutorUnavailableException("Could not reach the AI tutor"));

        assertThat(mvc.post().uri("/api/conversations/{id}/messages", c.id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Hallo\"}"))
                .hasStatus(503)
                .bodyJson().extractingPath("$.detail").isEqualTo("Could not reach the AI tutor");
    }

    @Test
    void scenariosAreListed() {
        assertThat(mvc.get().uri("/api/scenarios"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asArray()
                .containsExactly("FREE", "TRAIN_TICKET", "DOCTOR", "COLLEAGUE");
    }

    @Test
    void fakeIsTheActiveTutor(@Autowired TutorClient active) {
        assertThat(active).isSameAs(tutor);
    }
}
