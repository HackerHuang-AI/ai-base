package com.ai.base.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ai.base")
public class AiBaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiBaseApplication.class, args);
    }
}

