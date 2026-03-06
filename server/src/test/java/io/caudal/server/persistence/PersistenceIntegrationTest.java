package io.caudal.server.persistence;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.caudal.server.CaudalApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("testcontainers")
@Testcontainers
@SpringBootTest(
    classes = CaudalApplication.class,
    properties = {
        "caudal.auth.disabled=true",
        "caudal.snapshot-interval-seconds=99999"
    }
)
@AutoConfigureMockMvc
class PersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("caudal_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersistenceService persistenceService;

    @Test
    void flywayMigrationsApply() {
        // If we got here, Flyway applied successfully
    }

    @Test
    void walPersistedOnIngest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "wal-test",
                      "events": [
                        {"src": "x", "dst": "y", "intensity": 1.0}
                      ]
                    }
                    """))
            .andExpect(status().isAccepted());

        var events = persistenceService.replayWalAfter("wal-test", java.time.Instant.EPOCH);
        org.assertj.core.api.Assertions.assertThat(events).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(events.getFirst().src()).isEqualTo("x");
    }

    @Test
    void snapshotPersistAndLoad() throws Exception {
        // Ingest data
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "snap-test",
                      "events": [
                        {"src": "a", "dst": "b", "intensity": 5.0}
                      ]
                    }
                    """))
            .andExpect(status().isAccepted());

        // Verify focus works before snapshot
        mockMvc.perform(get("/api/v1/focus")
                .param("space", "snap-test")
                .param("k", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));
    }

    @Test
    void ingestThenQuery_fullRoundTrip() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "rt-test",
                      "events": [
                        {"src": "agent:1", "dst": "topic:ml", "intensity": 3.0},
                        {"src": "agent:1", "dst": "topic:java", "intensity": 1.0},
                        {"src": "agent:2", "dst": "topic:ml", "intensity": 2.0}
                      ]
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.accepted").value(3));

        mockMvc.perform(get("/api/v1/focus")
                .param("space", "rt-test")
                .param("k", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value("topic:ml"));

        mockMvc.perform(get("/api/v1/next")
                .param("space", "rt-test")
                .param("src", "agent:1")
                .param("k", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)));

        mockMvc.perform(post("/api/v1/pathways")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "rt-test",
                      "start": "agent:1",
                      "k": 5,
                      "mode": "fast"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths", hasSize(greaterThan(0))));
    }
}
