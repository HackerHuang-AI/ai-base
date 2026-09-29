package com.ai.base.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.ai.base.infrastructure.persistence.mapper")
@SpringBootApplication(scanBasePackages = "com.ai.base")
public class AiBaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiBaseApplication.class, args);
    }
}

