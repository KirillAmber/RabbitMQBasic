package ru.findabair.function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.findabair.model.OrderConfirmation;
import ru.findabair.model.OrderMessage;

import java.util.function.Function;

/**
 * Composing функций: validateOrder|confirmOrder.
 *
 * Псевдонимы биндингов через function.bindings:
 *   validateOrder|confirmOrder-in-0  -> orderPipelineInput
 *   validateOrder|confirmOrder-out-0 -> orderPipelineOutput
 */
@Slf4j
@Configuration
public class OrderPipelineFunctions {

    @Bean
    public Function<OrderMessage, OrderMessage> validateOrder() {
        return order -> {
            log.info("Validating order: {}",
                    order.getOrderId());
            if (order.getOrderId() == null
                    || order.getOrderId().isBlank()) {
                throw new IllegalArgumentException(
                        "orderId must not be blank");
            }
            if (order.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid quantity: " + order.getQuantity());
            }
            if (order.getProduct() == null
                    || order.getProduct().isBlank()) {
                throw new IllegalArgumentException(
                        "product must not be blank");
            }
            log.info("Order {} passed validation",
                    order.getOrderId());
            return order;
        };
    }
}