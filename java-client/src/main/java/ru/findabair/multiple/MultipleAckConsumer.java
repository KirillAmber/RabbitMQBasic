package ru.findabair.multiple;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class MultipleAckConsumer {
    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.basicQos(3);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();

                log.info("{} {}", deliveryTag, message);

                if (deliveryTag % 5 == 0) {
                    // Подтверждаем все сообщения до текущего включительно
                    channel.basicAck(deliveryTag, true);
                }
            };

            // autoAck = false, чтобы управлять подтверждениями вручную
            channel.basicConsume("q_drone", false, deliverCallback, consumerTag -> {
            });
        }
    }
}
