package ru.findabair.model;

import lombok.Data;

@Data
public class OrderMessage {
    private String orderId;
    private String product;
    private int quantity;
}
