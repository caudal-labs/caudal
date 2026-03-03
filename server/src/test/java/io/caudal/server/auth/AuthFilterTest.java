package io.caudal.server.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.caudal.server.CaudalApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = CaudalApplication.class,
    properties = {
        "caudal.auth.disabled=false",
        "caudal.auth.api-key=test-secret-key",
        "spring.datasource.url=jdbc:h2:mem:authtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
    }
)
@AutoConfigureMockMvc
class AuthFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/focus").param("space", "test"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/focus")
                .param("space", "test")
                .header("Authorization", "Bearer wrong-key"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validBearerToken_passes() throws Exception {
        mockMvc.perform(get("/api/v1/focus")
                .param("space", "test")
                .header("Authorization", "Bearer test-secret-key"))
            .andExpect(status().isOk());
    }

    @Test
    void validTokenPrefix_passes() throws Exception {
        mockMvc.perform(get("/api/v1/focus")
                .param("space", "test")
                .header("Authorization", "Token test-secret-key"))
            .andExpect(status().isOk());
    }

    @Test
    void actuator_bypassesAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }
}
