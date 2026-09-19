package com.example.demo.controllers;

import com.example.demo.dto.TransactionResponse;
import com.example.demo.dto.TransferRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/accounts")
public class BankAccountController {

    @PostMapping("/{sourceAccountId}/transfers")
    public ResponseEntity<TransactionResponse> executeTransfer(
            
            // Path Parameter Schema
            @PathVariable String sourceAccountId,

            // Header Parameter Schema
            @RequestHeader("X-Correlation-ID") String correlationId,

            @RequestParam(defaultValue = "STANDARD") String priority,

            @RequestBody TransferRequest request
    ) {
        // Stubbed response logic
        TransactionResponse stubbedResponse = new TransactionResponse(
                "TXN-" + System.currentTimeMillis(),
                "COMPLETED",
                request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO,
                OffsetDateTime.now()
        );

        return ResponseEntity.ok(stubbedResponse);
    }
}