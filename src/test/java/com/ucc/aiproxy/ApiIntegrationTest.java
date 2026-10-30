package com.ucc.aiproxy;

import com.ucc.aiproxy.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.ai.mock-latency-ms=0")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String PROMPT = "{\"prompt\":\"What is the proxy pattern?\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserAccountRepository repository;

    @BeforeEach
    void clean() {
        repository.clear();
    }

    @Test
    void generateGoesThroughChainAndUpdatesStatus() throws Exception {
        mvc.perform(post("/api/ai/generate").header("X-User-Id", "student")
                        .contentType(MediaType.APPLICATION_JSON).content(PROMPT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").isNotEmpty())
                .andExpect(jsonPath("$.totalTokens").isNumber());

        mvc.perform(get("/api/quota/status").header("X-User-Id", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.requestsThisMinute").value(1))
                .andExpect(jsonPath("$.tokenLimit").value(50000));
    }

    @Test
    void eleventhRequestReturns429WithRetryAfter() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/ai/generate").header("X-User-Id", "spammer")
                    .contentType(MediaType.APPLICATION_JSON).content(PROMPT)).andExpect(status().isOk());
        }
        mvc.perform(post("/api/ai/generate").header("X-User-Id", "spammer")
                        .contentType(MediaType.APPLICATION_JSON).content(PROMPT))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void upgradeAndHistory() throws Exception {
        mvc.perform(post("/api/quota/upgrade").header("X-User-Id", "buyer")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plan\":\"PRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.requestsPerMinuteLimit").value(60));

        mvc.perform(post("/api/quota/upgrade").header("X-User-Id", "buyer")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plan\":\"FREE\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/quota/history").header("X-User-Id", "buyer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void missingUserHeaderOrEmptyPromptIsBadRequest() throws Exception {
        mvc.perform(post("/api/ai/generate").contentType(MediaType.APPLICATION_JSON).content(PROMPT))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ai/generate").header("X-User-Id", "x")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
