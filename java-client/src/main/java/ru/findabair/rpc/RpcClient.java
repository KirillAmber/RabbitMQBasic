package ru.findabair.rpc;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
public class RpcClient {

    private static final String RPC_QUEUE = "rpc_queue";

    private final Channel channel;
    private final Connection connection;
    private final String replyQueueName;

    // Хранилище correlationId -> Future с ответом
    private final ConcurrentMap<String, CompletableFuture<String>> pendingRequests
            = new ConcurrentHashMap<>();

    public RpcClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setVirtualHost("/");
        factory.setUsername("guest");
        factory.setPassword("guest");

        connection = factory.newConnection();
        channel = connection.createChannel();

        // Создаём эксклюзивную очередь для ответов с именем, сгенерированным RabbitMQ
        // exclusive=true  — только это соединение видит очередь
        // autoDelete=true — очередь удалится при закрытии соединения
        replyQueueName = channel.queueDeclare("", false, true, true, null).getQueue();
        log.info("[i] Очередь для ответов: {}", replyQueueName);

        // Слушаем очередь ответов
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String correlationId = delivery.getProperties().getCorrelationId();
            String response = new String(delivery.getBody(), StandardCharsets.UTF_8);

            log.info("[<] Получен ответ | correlationId={} | body={}", correlationId, response);

            // Находим Future по correlationId и завершаем его результатом
            CompletableFuture<String> future = pendingRequests.remove(correlationId);
            if (future != null) {
                future.complete(response);
            }
        };

        channel.basicConsume(replyQueueName, true, deliverCallback, consumerTag -> {
        });
    }

    /**
     * Отправляет RPC-запрос и возвращает Future с ответом.
     * Можно вызывать несколько раз параллельно — каждый запрос
     * идентифицируется своим correlationId.
     */
    public CompletableFuture<String> call(String body) throws IOException {
        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo(replyQueueName)
                .build();

        log.info("[>] Отправка запроса | correlationId={} | body={}", correlationId, body);

        channel.basicPublish("", RPC_QUEUE, props, body.getBytes(StandardCharsets.UTF_8));

        return future;
    }

    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public static void main(String[] args) throws Exception {
        RpcClient client = new RpcClient();

        // Несколько запросов параллельно
        String[] logins = {"john13", "pink_kitty", "h4x0r", "unknown_user"};

        CompletableFuture<?>[] futures = new CompletableFuture[logins.length];

        for (int i = 0; i < logins.length; i++) {
            String request = "{\"login\": \"" + logins[i] + "\"}";
            futures[i] = client.call(request)
                    .orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(ex -> "Timeout или ошибка: " + ex.getMessage());
        }

        // Ждём все ответы
        CompletableFuture.allOf(futures).join();

        for (CompletableFuture<?> f : futures) {
            log.info("[=] Результат: {}", f.get());
        }
        client.close();
    }
}
