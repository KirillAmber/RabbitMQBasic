package ru.findabair.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderErrorHandler implements RabbitListenerErrorHandler {

    @Override
    public Object handleError(Message amqpMessage,
                              org.springframework.messaging.Message<?> message,
                              ListenerExecutionFailedException exception) {
        log.error("Failed to process message: {}, error: {}",
                amqpMessage.getMessageProperties().getMessageId(),
                exception.getCause().getMessage());

        // Можно сохранить в БД, отправить алерт и т.д.

        throw exception; // пробрасываем дальше чтобы сработал retry/DLQ
    }
}
