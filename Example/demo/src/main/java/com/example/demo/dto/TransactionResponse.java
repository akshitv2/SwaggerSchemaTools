package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class TransactionResponse {

    private String transactionId;

    private String status;

    private BigDecimal amount;

    private OffsetDateTime timestamp;

    public TransactionResponse(String transactionId, String status, BigDecimal amount, OffsetDateTime timestamp) {
        this.transactionId = transactionId;
        this.status = status;
        this.amount = amount;
        this.timestamp = timestamp;
    }

}