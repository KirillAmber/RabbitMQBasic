package ru.findabair;


import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class SpringBootRabbitApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootRabbitApplication.class, args);
    }
}
