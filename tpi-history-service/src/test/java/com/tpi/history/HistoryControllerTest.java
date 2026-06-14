package com.tpi.history;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void shouldRegisterAndRetrieveHistory() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"eventType\": \"BUY_EXECUTED\",
                                  \"userId\": \"user-1\",
                                  \"payloadJson\": {
                                    \"symbol\": \"NVDA\",
                                    \"amountArs\": 100.0
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("BUY_EXECUTED"));

        mockMvc.perform(get("/users/user-1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user-1"));
    }
}

