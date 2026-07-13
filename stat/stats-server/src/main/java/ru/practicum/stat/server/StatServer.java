package ru.practicum.stat.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.practicum.exception.ErrorHandler;

@SpringBootApplication
@Import({ErrorHandler.class})
public class StatServer {
    public static void main(String[] args) {
        SpringApplication.run(StatServer.class, args);
    }
}
