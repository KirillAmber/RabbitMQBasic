package ru.findabair.function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import ru.findabair.service.AlertService;

import java.util.function.Consumer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class GlobalErrorConfig {

    private final AlertService alertService;

    @Bean
    public MessageChannel errorChannel() {
        return new PublishSubscribeChannel();
    }

    @ServiceActivator(inputChannel = "errorChannel")
    public void handleError(ErrorMessage errorMessage) {
        Throwable cause =
                errorMessage.getPayload().getCause();
        Message<?> failed =
                errorMessage.getOriginalMessage();

        String failedBody = failed != null
                && failed.getPayload() instanceof byte[]
                ? new String((byte[]) failed.getPayload())
                : "unknown";

        log.error(
                "Global error. cause={}, body={}",
                cause != null ? cause.getMessage() : "null",
                failedBody
        );

        alertService.sendAlert(
                "Message processing failed: "
                        + (cause != null
                        ? cause.getMessage()
                        : "unknown error")
        );
    }
}
