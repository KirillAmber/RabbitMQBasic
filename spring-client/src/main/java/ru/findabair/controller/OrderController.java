package ru.findabair.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.findabair.model.OrderMessage;
import ru.findabair.service.OrderService;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<String> createOrder(
            @RequestBody OrderMessage order) {
        orderService.sendOrder(order);
        return ResponseEntity.accepted()
                .body("Order sent: "
                        + order.getOrderId());
    }

    @PostMapping("/createWithTrace")
    public ResponseEntity<String> createOrderWithTrace(
            @RequestBody OrderMessage order) {
        orderService.sendOrder(order, UUID.randomUUID().toString());
        return ResponseEntity.accepted()
                .body("Order sent: "
                        + order.getOrderId());
    }
}
