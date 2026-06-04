package ru.findabair.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.findabair.config.RabbitConfig;
import ru.findabair.model.OrderMessage;
import ru.findabair.service.OrderService;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderListener {

    private final OrderService orderService;

    /*@RabbitListener(
            queues = RabbitConfig.ORDERS_QUEUE
    )
    public void handleOrder(
            @Payload OrderMessage order,
            @Header(value = "X-Trace-Id",
                    required = false) String traceId) {

        log.info("Received order: {}, traceId: {}",
                order, traceId);
        System.out.println(order);
    }*/

    @RabbitListener(
            queues = RabbitConfig.ORDERS_QUEUE,
            errorHandler = "orderErrorHandler"
    )
    public void handleOrder(@Payload OrderMessage order,
                            Channel channel,
                            @Header(value = "X-Trace-Id",
                                    required = false) String traceId,
                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            log.info("Received order with manual ack: {}, traceId: {}",
                    order, traceId);
            orderService.processOrder(order);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
