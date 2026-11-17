package com.kletsai.common;

import com.kletsai.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Boots the app against real Postgres: Flyway runs V1, Hibernate validates, the seeded user is served. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeControllerTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void returnsSeededSingleUser() {
        assertThat(mvc.get().uri("/api/me"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id").isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    void healthIsUp() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status").isEqualTo("UP");
    }
}
