package io.caudal.server.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.caudal.server.CaudalApplication;
import org.junit.jupiter.api.BeforeEach;
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
        "spring.datasource.url=jdbc:h2:mem:querytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
    }
)

@AutoConfigureMockMvc
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "space": "qtest",
                  "events": [
                    {"src": "a", "dst": "b", "weight": 3.0},
                    {"src": "a", "dst": "c", "weight": 1.0},
                    {"src": "b", "dst": "d", "weight": 2.0}
                  ]
                }
                """));
    }

    @Test
    void focus_returnsRankedItems() throws Exception {
        mockMvc.perform(get("/api/v1/focus")
                .param("space", "qtest")
                .param("k", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].id").isString())
            .andExpect(jsonPath("$.items[0].score").isNumber())
            .andExpect(jsonPath("$.asOf").isString());
    }

    @Test
    void focus_emptySpace_returnsEmptyItems() throws Exception {
        mockMvc.perform(get("/api/v1/focus")
                .param("space", "nonexistent")
                .param("k", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void next_returnsOutgoing() throws Exception {
        mockMvc.perform(get("/api/v1/next")
                .param("space", "qtest")
                .param("src", "a")
                .param("k", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.items[0].score").isNumber());
    }

    @Test
    void next_noOutgoing_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/next")
                .param("space", "qtest")
                .param("src", "d")
                .param("k", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void pathways_returnsPaths() throws Exception {
        mockMvc.perform(post("/api/v1/pathways")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "space": "qtest",
                      "start": "a",
                      "k": 5,
                      "mode": "fast"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths").isArray())
            .andExpect(jsonPath("$.topEntities").isArray())
            .andExpect(jsonPath("$.asOf").isString());
    }

    @Test
    void response_neverExposesInternalTerms() throws Exception {
        String response = mockMvc.perform(get("/api/v1/focus")
                .param("space", "qtest")
                .param("k", "10"))
            .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response)
            .doesNotContain("tau", "pheromone", "alpha", "decayPerBucket", "bucket", "ants", "maxSteps");
    }
}
