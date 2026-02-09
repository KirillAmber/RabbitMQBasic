package ru.findabair.deadletter;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class DroneConsumer {

    public static final String QUEUE_NAME = "q_drone_quorum";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.basicQos(3); // prefetch count = 3

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();

                if (delivery.getEnvelope().isRedeliver()) {
                    log.info("{} {}, Redelivered", deliveryTag, message);
                    // Подтверждаем, чтобы не зациклить
                    channel.basicAck(deliveryTag, false);
                } else {
                    channel.basicNack(deliveryTag, false, true); // requeue = true
                    log.info("{} {}", deliveryTag, message);
                }
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });

            // Держим приложение запущенным
            log.info("Нажмите Enter для выхода...");
            System.in.read();
        }
    }
}
