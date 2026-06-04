package ru.findabair.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmation {
    private String confirmationId;
    private String orderId;
    private String status;
}
