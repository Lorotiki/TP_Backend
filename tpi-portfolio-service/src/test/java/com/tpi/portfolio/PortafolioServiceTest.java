package com.tpi.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.tpi.portfolio.repository.CuentaRepository;
import com.tpi.portfolio.repository.MovimientoDineroRepository;
import com.tpi.portfolio.repository.PosicionRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortafolioServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimientoDineroRepository movimientoDineroRepository;

    @Autowired
    private PosicionRepository posicionRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @BeforeEach
    void cleanDatabase() {
        movimientoDineroRepository.deleteAll();
        posicionRepository.deleteAll();
        cuentaRepository.deleteAll();
    }


    @Test
    void shouldCreateCuentaOnFirstPortfolioFetch() throws Exception {
        mockMvc.perform(get("/users/user-test/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-test"))
                .andExpect(jsonPath("$.balanceArs").value(0.00));
    }

    @Test
    void shouldDepositoFunds() throws Exception {
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

    @Test
    void shouldLoadBalanceFromPortfolioEndpoint() throws Exception {
        mockMvc.perform(post("/users/user-test/portfolio/balance")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"amountArs\": 2500.00,
                                  \"referenceId\": \"balance-1\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceArs").value(2500.00));
    }
}

