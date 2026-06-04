package ru.findabair.function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import ru.findabair.model.OrderConfirmation;
import ru.findabair.model.OrderMessage;
import ru.findabair.service.OrderService;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class OrderFunctions {

    private final OrderService orderService;

    /**
     * Основной потребитель — обрабатывает заказы.
     * Получает заголовки через Message<T>.
     */
    @Bean
    public Consumer<Message<OrderMessage>> processOrder() {
        return message -> {
            OrderMessage order = message.getPayload();
            String traceId = (String) message.getHeaders()
                    .get("X-Trace-Id");
            log.info("Processing order: {}, traceId: {}",
                    order, traceId);
            orderService.process(order);
        };
    }

    /**
     * Аудит — подписан на тот же exchange что и processOrder,
     * но с другой group, поэтому получает копию каждого сообщения.
     */
    @Bean
    public Consumer<OrderMessage> auditOrder() {
        return order -> {
            log.info("Auditing order: {}", order);
            orderService.audit(order);
        };
    }

    /**
     * Трансформация — получает OrderMessage,
     * возвращает OrderConfirmation в orders.confirmations.
     */
    @Bean
    public Function<OrderMessage, OrderConfirmation> confirmOrder() {
        return order -> {
            log.info("Confirming order: {}", order.getOrderId());
            return new OrderConfirmation(
                    "CONF-" + order.getOrderId(),
                    order.getOrderId(),
                    "CONFIRMED"
            );
        };
    }

    /**
     * Слушатель DLQ — логирует и сохраняет проблемные сообщения.
     */
    @Bean
    public Consumer<Message<OrderMessage>> processOrderDlq() {
        return message -> {
            OrderMessage order = message.getPayload();
            Map<String, Object> headers = message.getHeaders();
            Object xDeath = headers.get("x-death");

            log.error(
                    "Dead letter received. Order: {}, x-death: {}",
                    order, xDeath);

            orderService.saveFailedOrder(order,
                    xDeath != null ? xDeath.toString() : "unknown");
        };
    }
}
