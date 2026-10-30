package com.ucc.aiproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiProxyPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiProxyPlatformApplication.class, args);
    }
}
