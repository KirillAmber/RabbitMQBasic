package ru.findabair.deadletter.task;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Точка входа для подготовки топологии и отправки сообщений.
 * <p>
 * Топология:
 * x_direct  (direct) ──► q_orders  ──[nack/requeue=false]──► x_dead (direct) ──► q_dead_orders
 * <p>
 * Запускать перед OrderConsumer.
 */
@Slf4j
public class RabbitMQSetup {

    // ── Подключение ──────────────────────────────────────────────────────────
    static final String HOST = "localhost";
    static final int PORT = 5672;
    static final String USERNAME = "guest";
    static final String PASSWORD = "guest";

    // ── Обменники ────────────────────────────────────────────────────────────
    static final String EXCHANGE_ORDERS = "x_direct";
    static final String EXCHANGE_DEAD = "x_dead";

    // ── Очереди ──────────────────────────────────────────────────────────────
    static final String QUEUE_ORDERS = "q_orders";
    static final String QUEUE_DEAD = "q_dead_orders";

    // ── Routing keys ─────────────────────────────────────────────────────────
    static final String RK_ORDERS = "orders";
    static final String RK_DEAD = "dead";

    // ── Consumer ─────────────────────────────────────────────────────────────
    static final int PREFETCH_COUNT = 3;
    static final int MESSAGE_COUNT = 10;

    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        try (Connection connection = buildFactory().newConnection();
             Channel channel = connection.createChannel()) {

            declareTopology(channel);
            sendOrders(channel);
        }
    }

    // ── Топология ─────────────────────────────────────────────────────────────

    static void declareTopology(Channel channel) throws Exception {
        // 1. DLX — объявляем первым, так как q_orders на него ссылается
        channel.exchangeDeclare(EXCHANGE_DEAD, BuiltinExchangeType.DIRECT, true);

        channel.queueDeclare(QUEUE_DEAD, true, false, false, null);
        channel.queueBind(QUEUE_DEAD, EXCHANGE_DEAD, RK_DEAD);

        // 2. Основной обменник и очередь с привязкой к DLX
        channel.exchangeDeclare(EXCHANGE_ORDERS, BuiltinExchangeType.DIRECT, true);

        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DEAD);
        args.put("x-dead-letter-routing-key", RK_DEAD);

        channel.queueDeclare(QUEUE_ORDERS, true, false, false, args);
        channel.queueBind(QUEUE_ORDERS, EXCHANGE_ORDERS, RK_ORDERS);

        log.info("Топология готова: {}→{} (DLX: {}→{})",
                EXCHANGE_ORDERS, QUEUE_ORDERS, EXCHANGE_DEAD, QUEUE_DEAD);
    }

    // ── Producer ──────────────────────────────────────────────────────────────

    static void sendOrders(Channel channel) throws Exception {
        AMQP.BasicProperties persistent = new AMQP.BasicProperties.Builder()
                .deliveryMode(2)
                .build();

        for (int i = 1; i <= MESSAGE_COUNT; i++) {
            String body = "Order #" + i;
            channel.basicPublish(EXCHANGE_ORDERS, RK_ORDERS, persistent,
                    body.getBytes(StandardCharsets.UTF_8));
            log.info("→ Отправлено: {}", body);
        }
        log.info("Отправлено {} сообщений.", MESSAGE_COUNT);
    }

    // ── Фабрика ───────────────────────────────────────────────────────────────

    static ConnectionFactory buildFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        return factory;
    }
}
