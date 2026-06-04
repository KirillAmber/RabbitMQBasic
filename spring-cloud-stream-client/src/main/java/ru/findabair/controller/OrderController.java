package ru.findabair.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.findabair.model.OrderMessage;
import ru.findabair.service.OrderSendService;
import ru.findabair.service.OrderService;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderSendService orderSendService;

    @PostMapping
    public ResponseEntity<String> createOrder(
            @RequestBody OrderMessage order,
            @RequestHeader(value = "X-Trace-Id",
                    required = false) String traceId) {

        if (traceId != null) {
            orderSendService.sendOrder(order, traceId);
        } else {
            orderSendService.sendOrder(order);
        }

        return ResponseEntity.accepted()
                .body("Order queued: " + order.getOrderId());
    }

    @PostMapping("/dynamic")
    public ResponseEntity<String> createOrderDynamic(
            @RequestBody OrderMessage order) {

        orderSendService.sendOrderDynamic(order);
        return ResponseEntity.accepted()
                .body("Order routed by type: "
                        + order.getOrderId());
    }
}
