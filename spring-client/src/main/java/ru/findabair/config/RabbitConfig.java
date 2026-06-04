package ru.findabair.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.stereotype.Component;

@Configuration
@Slf4j
public class RabbitConfig {
    public static final String ORDERS_QUEUE = "q_orders";
    public static final String ORDERS_EXCHANGE = "x_orders";
    public static final String ORDERS_KEY = "orders.created";

    // Dead Letter
    public static final String ORDERS_DLQ_NAME = "orders_dlq";
    public static final String ORDERS_DL_EXCHANGE_NAME = "orders_dlx";
    public static final String ORDERS_DL_ROUTING_KEY = "orders.dead";

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(ORDERS_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDERS_DL_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ORDERS_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public Binding ordersBinding(Queue ordersQueue,
                                 DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(ordersQueue)
                .to(ordersExchange)
                .with(ORDERS_KEY);
    }


    // Dead Letter Exchange и очередь
// Dead Letter Exchange и очередь
    @Bean
    public Queue ordersDeadQueue() {
        return QueueBuilder.durable(ORDERS_DLQ_NAME).build();
    }

    @Bean
    public DirectExchange ordersDeadExchange() {
        return new DirectExchange(ORDERS_DL_EXCHANGE_NAME);
    }

    @Bean
    public Binding ordersDeadBinding(Queue ordersDeadQueue,
                                     DirectExchange ordersDeadExchange) {
        return BindingBuilder
                .bind(ordersDeadQueue)
                .to(ordersDeadExchange)
                .with(ORDERS_DL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);

        // Брокер подтвердил получение сообщения
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message not confirmed by broker: {}", cause);
            }
        });

        // Брокер не смог доставить сообщение ни в одну очередь
        template.setReturnsCallback(returned -> {
            log.error("Message returned: {} -> {}",
                    returned.getRoutingKey(),
                    returned.getMessage());
        });

        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        RetryOperationsInterceptor retryInterceptor =
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .backOffOptions(1000, 2.0, 10000)
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build();

        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
