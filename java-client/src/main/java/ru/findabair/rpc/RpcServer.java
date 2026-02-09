package ru.findabair.rpc;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RpcServer {

    private static final String QUEUE_NAME = "rpc_queue";

    // Эмуляция базы данных пользователей
    private static final Map<String, String> USERS = Map.of(
            "john13", "{\"name\": \"John Everline\", \"born\": 1992, \"city\": \"New York\"}",
            "pink_kitty", "{\"name\": \"Kate Pink\",     \"born\": 1995, \"city\": \"London\"}",
            "h4x0r", "{\"name\": \"Max Hacker\",    \"born\": 1989, \"city\": \"Berlin\"}"
    );

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setVirtualHost("/");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // Объявляем очередь, в которую клиент будет слать запросы
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            // Один запрос за раз — не берём следующий, пока не обработали текущий
            channel.basicQos(1);

            log.info("[x] Ожидание RPC-запросов...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties requestProps = delivery.getProperties();

                String correlationId = requestProps.getCorrelationId();
                String replyTo = requestProps.getReplyTo();
                String body = new String(delivery.getBody(), StandardCharsets.UTF_8);

                log.info("[.] Получен запрос | correlationId={} | body={}", correlationId, body);

                // Обработка: ищем пользователя по логину
                String login = body.replaceAll(".*\"login\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                String response = USERS.getOrDefault(login, "{\"error\": \"user not found\"}");

                // Формируем properties ответа — возвращаем тот же correlationId
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                        .correlationId(correlationId)
                        .build();

                // Отправляем ответ через дефолтный обменник в очередь replyTo
                channel.basicPublish(
                        "", // дефолтный обменник
                        replyTo,  // routing key = название очереди клиента
                        replyProps,
                        response.getBytes(StandardCharsets.UTF_8)
                );

                // Подтверждаем обработку сообщения
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                log.info("[.] Ответ отправлен | correlationId={}", correlationId);
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            log.error("Ошибка при выполнении", e);
        }

    }
}

