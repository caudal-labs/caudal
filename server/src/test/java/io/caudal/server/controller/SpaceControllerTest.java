package io.caudal.server.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        "spring.datasource.url=jdbc:h2:mem:spacetest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
    }
)
@AutoConfigureMockMvc
class SpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deleteSpace_existingSpace_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "user:alice",
                      "events": [{"src": "user:alice", "dst": "topic:java", "intensity": 1.0}]
                    }
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(delete("/api/v1/spaces/user:alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deleted").value("user:alice"))
            .andExpect(jsonPath("$.asOf").isNotEmpty());
    }

    @Test
    void deleteSpace_unknownSpace_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/spaces/user:unknown"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Space not found: user:unknown"));
    }

    @Test
    void deleteSpace_removesFromMemory() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "delete-test",
                      "events": [{"src": "user:alice", "dst": "topic:spring", "intensity": 3.0}]
                    }
                    """))
            .andExpect(status().isAccepted());

        // Confirm space has data before deletion
        mockMvc.perform(get("/api/v1/focus").param("space", "delete-test").param("k", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").isString());

        mockMvc.perform(delete("/api/v1/spaces/delete-test"))
            .andExpect(status().isOk());

        // After deletion, focus returns empty (space is gone from memory)
        mockMvc.perform(get("/api/v1/focus").param("space", "delete-test").param("k", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void deleteSpace_twice_secondReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "delete-idempotent-test",
                      "events": [{"src": "user:bob", "dst": "topic:java", "intensity": 1.0}]
                    }
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(delete("/api/v1/spaces/delete-idempotent-test"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/spaces/delete-idempotent-test"))
            .andExpect(status().isNotFound());
    }
}
