package ru.findabair.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.findabair.model.OrderMessage;

@Service
@Slf4j
public class OrderService {


    public void process(OrderMessage order) {
        if (order.getOrderId() == null
                || order.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid order: " + order);
        }
        log.info("Order processed: {}", order.getOrderId());
    }

    public void audit(OrderMessage order) {
        log.info("Order audited: {}", order.getOrderId());
    }

    public void saveFailedOrder(OrderMessage order,
                                String reason) {
        log.error("Saving failed order: {}, reason: {}",
                order.getOrderId(), reason);
        // В реальном приложении — сохранение в БД
    }
}
