package ru.rtc.warehouse.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Базовый шаблон для работы со строковыми ключами/значениями.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        StringRedisTemplate tpl = new StringRedisTemplate(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new StringRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.setHashValueSerializer(new StringRedisSerializer());
        return tpl;
    }
}