package ru.findabair.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import ru.findabair.model.OrderMessage;

@Service
@Slf4j
public class OrderSendService {

    private final StreamBridge streamBridge;

    public OrderSendService(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /**
     * Простая отправка без заголовков.
     */
    public void sendOrder(OrderMessage order) {
        streamBridge.send("orderEvents-out-0", order);
        log.info("Order sent: {}", order.getOrderId());
    }

    /**
     * Отправка с заголовками трассировки.
     */
    public void sendOrder(OrderMessage order, String traceId) {
        var message = MessageBuilder
                .withPayload(order)
                .setHeader("X-Trace-Id", traceId)
                .setHeader("X-Source", "order-service")
                .build();

        streamBridge.send("orderEvents-out-0", message);
        log.info("Order sent: {}, traceId: {}",
                order.getOrderId(), traceId);
    }

    /**
     * Динамическая маршрутизация по типу заказа.
     */
    public void sendOrderDynamic(OrderMessage order) {
        String destination = switch (order.getType()) {
            case "EXPRESS"   -> "orders.express";
            case "WHOLESALE" -> "orders.wholesale";
            default          -> "orders.standard";
        };

        streamBridge.send(destination, order);
        log.info("Order sent to {}: {}",
                destination, order.getOrderId());
    }
}
