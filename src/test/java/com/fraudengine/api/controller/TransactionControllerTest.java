package com.fraudengine.api.controller;

import com.fraudengine.api.dto.EvaluationResponse;
import com.fraudengine.domain.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldReturn200WithEvaluationResponseForValidRequest() throws Exception {
        EvaluationResponse mockResponse = new EvaluationResponse(
                "txn-001", 30, "APPROVE", "1.0.0", false, List.of(), Instant.now()
        );
        when(transactionService.evaluate(any(), eq(false))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-001"))
                .andExpect(jsonPath("$.decision").value("APPROVE"))
                .andExpect(jsonPath("$.simulated").value(false));
    }

    @Test
    void shouldReturn200WithSimulatedTrueForSimulateEndpoint() throws Exception {
        EvaluationResponse mockResponse = new EvaluationResponse(
                "txn-001", 30, "APPROVE", "1.0.0", true, List.of(), Instant.now()
        );
        when(transactionService.evaluate(any(), eq(true))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/transactions/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulated").value(true));
    }

    @Test
    void shouldReturn400WhenTransactionIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "usr-123",
                                  "deviceId": "dev-abc",
                                  "amount": 500.00,
                                  "merchantId": "mch-xyz",
                                  "merchant": "Store",
                                  "country": "BR",
                                  "transactionAt": "2025-02-06T22:07:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenAmountIsNegative() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "txn-001",
                                  "userId": "usr-123",
                                  "deviceId": "dev-abc",
                                  "amount": -100.00,
                                  "merchantId": "mch-xyz",
                                  "merchant": "Store",
                                  "country": "BR",
                                  "transactionAt": "2025-02-06T22:07:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String buildValidRequestJson() {
        return """
                {
                  "transactionId": "txn-001",
                  "userId": "usr-123",
                  "deviceId": "dev-abc",
                  "amount": 500.00,
                  "merchantId": "mch-xyz",
                  "merchant": "Store XYZ",
                  "country": "BR",
                  "transactionAt": "2025-02-06T22:07:00Z"
                }
                """;
    }
}