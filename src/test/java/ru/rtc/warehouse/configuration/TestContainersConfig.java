package ru.rtc.warehouse.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("smart_warehouse");
    }
    @BeforeAll
    static void start() {
        // инициализация контейнера произойдет автоматически благодаря @Container
    }
}

