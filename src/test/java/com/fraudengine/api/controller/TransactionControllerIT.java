package com.fraudengine.api.controller;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class TransactionControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("fraudengine")
            .withUsername("fraudengine")
            .withPassword("fraudengine");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldEvaluateTransactionAndReturnDecision() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "txn-it-001",
                                  "userId": "usr-it-123",
                                  "deviceId": "dev-it-abc",
                                  "amount": 500.00,
                                  "merchantId": "mch-it-xyz",
                                  "merchant": "Store IT",
                                  "country": "BR",
                                  "transactionAt": "2025-02-06T14:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-it-001"))
                .andExpect(jsonPath("$.decision").exists())
                .andExpect(jsonPath("$.totalScore").isNumber())
                .andExpect(jsonPath("$.rulesVersion").value("1.0.0"))
                .andExpect(jsonPath("$.simulated").value(false))
                .andExpect(jsonPath("$.rulesEvaluated").isArray());
    }

    @Test
    void shouldEvaluateHighAmountTransactionWithHigherScore() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "txn-it-002",
                                  "userId": "usr-it-456",
                                  "deviceId": "dev-it-def",
                                  "amount": 9500.00,
                                  "merchantId": "mch-it-abc",
                                  "merchant": "Expensive Store",
                                  "country": "BR",
                                  "transactionAt": "2025-02-06T14:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(30))
                .andExpect(jsonPath("$.decision").value("APPROVE"));
    }

    @Test
    void shouldReturnBlockForHighRiskCountryAndHighAmount() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "txn-it-003",
                                  "userId": "usr-it-789",
                                  "deviceId": "dev-it-ghi",
                                  "amount": 9500.00,
                                  "merchantId": "mch-it-xyz",
                                  "merchant": "Store",
                                  "country": "NG",
                                  "transactionAt": "2025-02-06T02:30:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("BLOCK"));
    }

    @Test
    void shouldNotPersistWhenSimulated() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "txn-it-sim-001",
                                  "userId": "usr-sim-123",
                                  "deviceId": "dev-sim-abc",
                                  "amount": 500.00,
                                  "merchantId": "mch-sim-xyz",
                                  "merchant": "Sim Store",
                                  "country": "BR",
                                  "transactionAt": "2025-02-06T14:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulated").value(true));
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "usr-it-123",
                                  "amount": 500.00,
                                  "transactionAt": "2025-02-06T14:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}