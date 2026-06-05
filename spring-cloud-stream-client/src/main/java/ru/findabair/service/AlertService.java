package ru.findabair.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertService {

    public void sendAlert(String message) {
        log.warn("ALERT: {}", message);
    }
}
