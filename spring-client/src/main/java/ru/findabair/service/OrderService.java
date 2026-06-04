package ru.findabair.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.findabair.config.RabbitConfig;
import ru.findabair.model.OrderMessage;

@Service
public class OrderService {

    private final RabbitTemplate rabbitTemplate;

    public OrderService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrder(OrderMessage order) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDERS_KEY,
                order
        );
    }

    public void sendOrder(OrderMessage order, String traceId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDERS_KEY,
                order,
                message -> {
                    message.getMessageProperties()
                            .setHeader("X-Trace-Id", traceId);
                    return message;
                }
        );
    }

    public void processOrder(OrderMessage order) {
        if (order.getOrderId() == null || order.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid order: " + order);
        }
        System.out.println("Processing order: " + order);
    }
}
