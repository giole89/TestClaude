package com.finai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point dell'applicazione FINAI.
 *
 * <p>L'applicazione usa thread virtuali (Java 21) configurati in application.yml
 * tramite {@code spring.threads.virtual.enabled=true}.</p>
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class FinaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinaiApplication.class, args);
    }
}
