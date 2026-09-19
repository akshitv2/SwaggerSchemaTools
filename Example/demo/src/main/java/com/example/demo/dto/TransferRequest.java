package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {


    private String destinationAccountId;

    private BigDecimal amount;


    private String currency;

    private String memo;
}