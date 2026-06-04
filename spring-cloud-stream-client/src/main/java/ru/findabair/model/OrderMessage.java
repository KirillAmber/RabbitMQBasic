package ru.findabair.model;

import lombok.Data;

@Data
public class OrderMessage {
    private String orderId;
    private String customerId;
    private String product;
    private int quantity;
    private String type; // STANDARD, EXPRESS, WHOLESALE
}
