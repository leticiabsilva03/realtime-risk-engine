package com.fraudengine.infrastructure.observability;

import com.fraudengine.api.dto.EvaluationResponse;
import com.fraudengine.domain.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class EvaluationLogger {

    private static final Logger log = LoggerFactory.getLogger(EvaluationLogger.class);

    public void logEvaluation(Transaction tx, EvaluationResponse response, long durationMs) {
        MDC.put("traceId",        generateTraceId(tx.transactionId()));
        MDC.put("transactionId",  tx.transactionId());
        MDC.put("userId",         tx.userId());
        MDC.put("totalScore",     String.valueOf(response.totalScore()));
        MDC.put("decision",       response.decision());
        MDC.put("rulesVersion",   response.rulesVersion());
        MDC.put("durationMs",     String.valueOf(durationMs));
        MDC.put("simulated",      String.valueOf(response.simulated()));

        String rulesTriggered = response.rulesEvaluated().stream()
                .filter(r -> r.triggered())
                .map(r -> r.rule() + "(" + r.score() + ")")
                .reduce((a, b) -> a + "," + b)
                .orElse("none");

        MDC.put("rulesTriggered", rulesTriggered);

        log.info("transaction.evaluated");

        MDC.clear();
    }

    private String generateTraceId(String transactionId) {
        return transactionId + "-" + System.currentTimeMillis();
    }
}