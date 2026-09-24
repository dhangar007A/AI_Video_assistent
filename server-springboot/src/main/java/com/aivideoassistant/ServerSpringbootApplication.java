package com.aivideoassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ServerSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerSpringbootApplication.class, args);
    }
}
