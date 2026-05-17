package com.fraudengine.api.controller;

import com.fraudengine.api.dto.EvaluationResponse;
import com.fraudengine.api.dto.TransactionRequest;
import com.fraudengine.domain.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResponse> evaluate(
            @Valid @RequestBody TransactionRequest request) {
        EvaluationResponse response = transactionService.evaluate(request, false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate")
    public ResponseEntity<EvaluationResponse> simulate(
            @Valid @RequestBody TransactionRequest request) {
        EvaluationResponse response = transactionService.evaluate(request, true);
        return ResponseEntity.ok(response);
    }
}