package ru.findabair;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class Producer {
    // Константы для именования
    private static final String EXCHANGE_NAME = "x_main";
    private static final String QUEUE_NAME = "q_test";
    private static final String ROUTING_KEY = "demo.routing.key";

    public static void main(String[] args) {
        // 1. Создаем фабрику соединений
        ConnectionFactory factory = new ConnectionFactory();

        // 2. Устанавливаем параметры подключения (хост, порт, логин, пароль)
        // По умолчанию: localhost, 5672, guest/guest
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        // Альтернативный способ - использовать URI:
        // factory.setUri("amqp://guest:guest@localhost:5672/");

        // 3. Создаем соединение
        try (Connection connection = factory.newConnection();
             // 4. Создаем канал (Channel) из соединения
             Channel channel = connection.createChannel()) {

            // 5. Создаём обменник (exchange) типа "direct"
            // Параметры:
            // - имя обменника
            // - тип (direct, fanout, topic, headers)
            // - durable (сохранять ли при перезапуске сервера)
            // - autoDelete (удалять ли при отвязке всех очередей)
            // - arguments (дополнительные параметры)
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", true, false, null);
            log.info("Обменник создан: " + EXCHANGE_NAME);

            // 6. Создаём очередь
            // Параметры:
            // - имя очереди
            // - durable (сохранять ли при перезапуске)
            // - exclusive (доступна только для этого соединения)
            // - autoDelete (удалять при отсутствии потребителей)
            // - arguments (дополнительные параметры, например, TTL)
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            log.info("Очередь создана: " + QUEUE_NAME);

            // 7. Привязываем очередь к обменнику с указанным routing key
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
            log.info("Очередь привязана к обменнику с routing key: " + ROUTING_KEY);

            // 8. Отправка сообщений
            // Подготавливаем сообщение
            String message = "Привет, RabbitMQ! Время: " + System.currentTimeMillis();

            // Параметры basicPublish:
            // - exchange: имя обменника (пустая строка = default exchange)
            // - routingKey: ключ маршрутизации
            // - props: свойства сообщения (persistent, priority и т.д.)
            // - body: тело сообщения (byte[])
            channel.basicPublish(
                    EXCHANGE_NAME,              // обменник
                    ROUTING_KEY,                // routing key
                    MessageProperties.PERSISTENT_TEXT_PLAIN, // сообщение сохраняется на диск
                    message.getBytes(StandardCharsets.UTF_8) // тело сообщения
            );

            log.info("Сообщение отправлено: " + message);
            log.info("Обменник: " + EXCHANGE_NAME);
            log.info("Routing key: " + ROUTING_KEY);

            // Небольшая задержка, чтобы сообщение точно ушло
            Thread.sleep(100);

        } catch (Exception e) {
            log.error("Ошибка при выполнении", e);
        }
    }
}