package com.tpi.portfolio;

import org.junit.jupiter.api.BeforeEach;
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
class PortfolioServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CashMovementRepository cashMovementRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void cleanDatabase() {
        cashMovementRepository.deleteAll();
        positionRepository.deleteAll();
        accountRepository.deleteAll();
    }


    @Test
    void shouldCreateAccountOnFirstPortfolioFetch() throws Exception {
        mockMvc.perform(get("/users/user-test/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-test"))
                .andExpect(jsonPath("$.balanceArs").value(0.00));
    }

    @Test
    void shouldDepositFunds() throws Exception {
        mockMvc.perform(post("/users/user-test/deposits")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"amountArs\": 1000.50,
                                  \"referenceId\": \"dep-1\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceArs").value(1000.50));
    }
}

