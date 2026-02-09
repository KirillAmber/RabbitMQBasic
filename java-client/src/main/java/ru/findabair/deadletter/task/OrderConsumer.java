package ru.findabair.deadletter.task;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Consumer для очереди q_orders.
 * <p>
 * Логика подтверждений:
 * нечётный номер заказа → basicAck(tag, false)        — успешно обработан
 * чётный  номер заказа  → basicNack(tag, false, false) — улетает в q_dead_orders
 * <p>
 * БОНУС (USE_MULTIPLE_ACK = true):
 * Нечётные сообщения не подтверждаются сразу. Когда deliveryTag
 * кратен 3 — один basicAck(tag, multiple=true) закрывает все
 * накопленные нечётные теги разом. Чётные уже ушли через nack,
 * поэтому multiple не затронет их повторно.
 */
@Slf4j
public class OrderConsumer {

    private static final boolean USE_MULTIPLE_ACK = false;

    public static void main(String[] args) throws Exception {
        try (Connection connection = RabbitMQSetup.buildFactory().newConnection();
             Channel channel = connection.createChannel()) {

            RabbitMQSetup.declareTopology(channel);

            channel.basicQos(RabbitMQSetup.PREFETCH_COUNT);
            log.info("Consumer запущен. prefetch={}, multipleMode={}",
                    RabbitMQSetup.PREFETCH_COUNT, USE_MULTIPLE_ACK);

            DeliverCallback onDeliver = (consumerTag, delivery) -> {
                String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();

                log.info("[tag={}] Получено: {}", deliveryTag, body);

                int orderNumber = parseOrderNumber(body);

                if (orderNumber % 2 != 0) {
                    try {
                        ackOdd(channel, deliveryTag);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    channel.basicNack(deliveryTag, false, false);
                    log.warn("[tag={}] NACK → DLQ. Заказ {} не обработан.", deliveryTag, body);
                }
            };

            channel.basicConsume(RabbitMQSetup.QUEUE_ORDERS, false, onDeliver, ct -> {
            });

            log.info("Нажмите Enter для выхода...");
            System.in.read();
        }
    }

    private static void ackOdd(Channel channel, long deliveryTag) throws Exception {
        if (!USE_MULTIPLE_ACK) {
            channel.basicAck(deliveryTag, false);
            log.info("[tag={}] ACK.", deliveryTag);
        } else {
            if (deliveryTag % 3 == 0) {
                channel.basicAck(deliveryTag, true);
                log.info("[tag={}] ACK (multiple=true) — подтверждены все теги ≤ {}.",
                        deliveryTag, deliveryTag);
            } else {
                log.info("[tag={}] Нечётный, ждём кратного 3.", deliveryTag);
            }
        }
    }

    private static int parseOrderNumber(String body) {
        try {
            return Integer.parseInt(body.split("#")[1].trim());
        } catch (Exception e) {
            log.error("Не удалось распарсить номер из: '{}'", body);
            return -1;
        }
    }
}
