package com.buulgyeong.forexanalyzer.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {

    @Bean
    public Dotenv dotenv() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        System.out.println(">>> Loaded .env entries:");
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        return dotenv;
    }
}