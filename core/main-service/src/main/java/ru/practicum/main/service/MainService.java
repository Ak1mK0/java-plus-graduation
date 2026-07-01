package ru.practicum.main.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.exception.ErrorHandler;
import ru.practicum.stat.client.StatsClient;

@SpringBootApplication
@EnableFeignClients(basePackages = "ru.practicum.faign")
@Import({StatsClient.class, ErrorHandler.class})
public class MainService {
    public static void main(String[] args) {
        SpringApplication.run(MainService.class, args);
    }
}
