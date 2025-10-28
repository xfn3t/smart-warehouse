package ru.rtc.warehouse.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "app.dashboard")
public class DashboardRealtimeProperties {
    /**
     * Таймзона для вычисления "начала дня" при инкременте счётчика "проверено сегодня".
     * Может быть переопределена параметром запроса при чтении (в контроллере).
     */
    private String timezone = "UTC";

    private Ttl ttl = new Ttl();

    @Getter @Setter
    public static class Ttl {
        /** TTL поминутных бакетов активности. */
        private int minuteSeriesSeconds = 4000;
        /** TTL дневного счётчика "проверено сегодня", в днях. */
        private int checkedDayDays = 3;
    }
}