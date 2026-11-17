package com.kletsai.flashcard;

import com.kletsai.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CardControllerTests {

    @Autowired
    MockMvcTester mvc;

    private static final String NEW_CARD = """
            {"cardType":"WORD","dutch":"  appel ","english":"apple","article":"DE",
             "exampleNl":"Ik eet een appel.","exampleEn":"I eat an apple."}
            """;

    @Test
    void createsCardDueImmediately() {
        assertThat(mvc.post().uri("/api/cards").contentType(MediaType.APPLICATION_JSON).content(NEW_CARD))
                .hasStatus(201)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.dutch").isEqualTo("appel");
                    assertThat(json).extractingPath("$.source").isEqualTo("MANUAL");
                    assertThat(json).extractingPath("$.repetitions").isEqualTo(0);
                });
    }

    @Test
    void rejectsDuplicateIgnoringCase() {
        mvc.post().uri("/api/cards").contentType(MediaType.APPLICATION_JSON).content(NEW_CARD).exchange();

        assertThat(mvc.post().uri("/api/cards").contentType(MediaType.APPLICATION_JSON)
                .content(NEW_CARD.replace("appel", "APPEL")))
                .hasStatus(409);
    }

    @Test
    void rejectsBlankDutch() {
        assertThat(mvc.post().uri("/api/cards").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardType\":\"WORD\",\"dutch\":\" \",\"english\":\"x\"}"))
                .hasStatus(400);
    }

    @Test
    void seedDeckIsDueAndStatsCountIt() {
        assertThat(mvc.get().uri("/api/cards/review-deck?days=0"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()").asNumber().satisfies(n -> assertThat(n.intValue()).isGreaterThan(20));

        assertThat(mvc.get().uri("/api/cards/stats"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.dueNow").asNumber().satisfies(n -> assertThat(n.intValue()).isGreaterThan(20));
    }

    @Test
    void unknownCardIs404ProblemDetail() {
        assertThat(mvc.get().uri("/api/cards/00000000-0000-0000-0000-00000000dead"))
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.status").isEqualTo(404);
    }

    @Test
    void syncEndpointValidatesGrade() {
        assertThat(mvc.post().uri("/api/reviews/sync").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        [{"id":"8f0c2a43-6a43-4c7e-9a3b-2b1ad3f1a001","cardId":"8f0c2a43-6a43-4c7e-9a3b-2b1ad3f1a002",
                          "grade":7,"reviewedAt":"2026-11-18T07:00:00Z"}]
                        """))
                .hasStatus(400);
    }
}
