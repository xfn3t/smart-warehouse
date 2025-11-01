// src/test/java/ru/rtc/warehouse/configuration/TestRedisContainerConfig.java
package ru.rtc.warehouse.configuration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestRedisContainerConfig {

    @Bean
    @ServiceConnection(name = "redis") // подсказываем Boot, что это Redis
    RedisContainer redis() {
        return new RedisContainer(DockerImageName.parse("redis:7-alpine"));
    }
}
