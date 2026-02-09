package ru.findabair;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class Consumer {
    // Константы для именования
    private static final String EXCHANGE_NAME = "x_main";
    private static final String QUEUE_NAME = "q_test";
    private static final String ROUTING_KEY = "demo.routing.key";

    public static void main(String[] args) throws Exception {
        // 1. Создаем фабрику соединений
        ConnectionFactory factory = new ConnectionFactory();

        // 2. Устанавливаем параметры подключения
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Создаём обменник, очередь и привязку
            // Важно: параметры должны совпадать с параметрами producer!
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", true, false, null);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

            log.info("Ожидание сообщений...");

            // Обработчик входящих сообщений
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                log.info("Получено сообщение: " + message);
                log.info("Routing key: " + delivery.getEnvelope().getRoutingKey());
                log.info("Consumer tag: " + consumerTag);

                // Подтверждение получения (ack)
                // Если не отправить ack, сообщение останется в очереди
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                log.info("Отправлен ack");
                log.info("---");
            };

            // Начинаем потребление сообщений
            // Параметры basicConsume:
            // - queue: имя очереди
            // - autoAck: автоматическое подтверждение (false = ручное)
            // - callback: обработчик сообщений
            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });

            // Держим приложение запущенным
            log.info("Нажмите Enter для выхода...");
            System.in.read();
        }
    }
}