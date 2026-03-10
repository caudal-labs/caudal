package io.caudal.server.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.caudal.server.CaudalApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = CaudalApplication.class,
    properties = {
        "caudal.auth.disabled=true",
        "spring.datasource.url=jdbc:h2:mem:eventtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
    }
)
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ingestEvents_validPayload_returnsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "test-space",
                      "events": [
                        {"src": "user:alice", "dst": "topic:java", "intensity": 1.0, "type": "interaction"},
                        {"src": "user:bob", "dst": "topic:spring", "intensity": 2.0}
                      ]
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.accepted").value(2))
            .andExpect(jsonPath("$.asOf").isNotEmpty());
    }

    @Test
    void ingestEvents_missingSpace_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "events": [{"src": "a", "dst": "b", "intensity": 1.0}]
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void ingestEvents_emptyEvents_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "test",
                      "events": []
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void ingestEvents_withModulations_returnsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "mod-test",
                      "events": [
                        {"src": "user:alice", "dst": "topic:bikes", "intensity": 5.0},
                        {"src": "user:alice", "dst": "topic:math", "intensity": 5.0}
                      ],
                      "modulations": [
                        {"entity": "topic:bikes", "attention": 0.1, "decay": 50}
                      ]
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.accepted").value(2));
    }

    @Test
    void ingestEvents_withModulations_affectsFocus() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "mod-focus-test",
                      "events": [
                        {"src": "user:x", "dst": "topic:suppress-me", "intensity": 5.0},
                        {"src": "user:x", "dst": "topic:keep-me", "intensity": 5.0}
                      ],
                      "modulations": [
                        {"entity": "topic:suppress-me", "attention": 0.0}
                      ]
                    }
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/focus")
                .param("space", "mod-focus-test")
                .param("k", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id == 'topic:suppress-me')]").doesNotExist());
    }

    @Test
    void modulate_standaloneEndpoint_returnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "standalone-mod",
                      "events": [{"src": "a", "dst": "b", "intensity": 1.0}]
                    }
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/modulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "standalone-mod",
                      "modulations": [
                        {"entity": "b", "attention": 0.5, "decay": 100}
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied").value(1))
            .andExpect(jsonPath("$.asOf").isNotEmpty());
    }

    @Test
    void modulate_missingSpace_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/modulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "modulations": [{"entity": "x", "attention": 0.5}]
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void modulate_emptyModulations_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/modulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "test",
                      "modulations": []
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void ingestThenFocus_returnsIngested() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "focus-test",
                      "events": [
                        {"src": "agent:1", "dst": "entity:foo", "intensity": 5.0}
                      ]
                    }
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/focus")
                .param("space", "focus-test")
                .param("k", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.items[0].id").isString())
            .andExpect(jsonPath("$.items[0].score").isNumber())
            .andExpect(jsonPath("$.asOf").isNotEmpty());
    }

    @Test
    void ingestEvents_negativeIntensity_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                  {
                    "space": "test",
                    "events": [{"src": "a", "dst": "b", "intensity": -1.0}]
                  }
                  """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestEvents_zeroIntensity_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                  {
                    "space": "test",
                    "events": [{"src": "a", "dst": "b", "intensity": 0.0}]
                  }
                  """))
                .andExpect(status().isBadRequest());
    }

}
