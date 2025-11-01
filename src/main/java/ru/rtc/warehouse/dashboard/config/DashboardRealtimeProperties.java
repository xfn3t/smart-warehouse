package ru.rtc.warehouse.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Настройки для «живых» (realtime) виджетов дашборда.
 *
 * <p>Бин привязывается к префиксу {@code app.dashboard} и управляет
 * временем жизни (TTL) ключей в Redis, используемых для метрик.</p>
 *
 * <h2>Пример конфигурации</h2>
 * <pre>
 * app:
 *   dashboard:
 *     ttl:
 *       minute-series-seconds: 4000
 *       checked-day-days: 3
 * </pre>
 *
 * <p>Значения подходят для разработки. Для продакшена имеет смысл уменьшить TTL,
 * чтобы не держать лишние ключи и экономить память Redis.</p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.dashboard")
public class DashboardRealtimeProperties {

    /**
     * Группа настроек TTL, определяющих срок хранения минутных «бакетов»
     * и суточных счётчиков в Redis.
     */
    private Ttl ttl = new Ttl();

    /**
     * Времена жизни (TTL) для метрик, складываемых в Redis.
     */
    @Getter
    @Setter
    public static class Ttl {

        /**
         * TTL для поминутных «бакетов» активности (в секундах).
         * <p>Используется для графика «сканирования по минутам». Значение должно
         * покрывать максимальную ширину окна графика + небольшой запас, но не быть
         * избыточным, чтобы в Redis не накапливались устаревшие ключи.</p>
         */
        private int minuteSeriesSeconds = 4000;

        /**
         * TTL для счётчика «проверено за день» (в днях).
         * <p>Поскольку это суточный счётчик, TTL задаётся в днях — так естественнее
         * управлять экспирацией исторических значений.</p>
         */
        private int checkedDayDays = 3;
    }
}
