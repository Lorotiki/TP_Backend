package com.tpi.marketdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CotizacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnQuoteForKnownSymbol() throws Exception {
        mockMvc.perform(get("/quotes/nvda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("NVDA"))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.price").value(36000.00));
    }

    @Test
    void shouldReturnNotFoundForUnknownSymbol() throws Exception {
        mockMvc.perform(get("/quotes/unknown"))
                .andExpect(status().isNotFound());
    }


}

