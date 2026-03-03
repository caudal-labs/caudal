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
        "caudal.auth.disabled=true",
        "spring.datasource.url=jdbc:h2:mem:authdisabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
    }
)
@AutoConfigureMockMvc
class AuthDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authDisabled_noHeaderNeeded() throws Exception {
        mockMvc.perform(get("/api/v1/focus").param("space", "test"))
            .andExpect(status().isOk());
    }
}
