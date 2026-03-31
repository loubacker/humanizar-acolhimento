package com.humanizar.acolhimento.infrastructure.controller.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class AcolhimentoExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new AcolhimentoExceptionHandler())
                .build();
    }

    @Test
    void shouldMapAcolhimentoExceptionToStandardJson() throws Exception {
        mockMvc.perform(get("/test/advice"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.reasonCode").value("DUPLICATE_PATIENT"))
                .andExpect(jsonPath("$.message").value("Acolhimento duplicado"))
                .andExpect(jsonPath("$.correlationId").value("corr-123"))
                .andExpect(jsonPath("$.path").value("/test/advice"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/advice")
        String fail() {
            throw new AcolhimentoException(
                    ReasonCode.DUPLICATE_PATIENT,
                    "corr-123",
                    "Acolhimento duplicado");
        }
    }
}
