package ru.rtc.warehouse.dashboard.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class RealtimePushScheduler {

    private final RealtimeStatsService statsService;
    private final SimpMessagingTemplate broker;

    // каждые 5 секунд шлём свежий срез
    @Scheduled(fixedRateString = "${app.dashboard.push-interval-ms:5000}")
    public void push() {
        RealtimeStatsDTO dto = statsService.getStats();
        broker.convertAndSend("/topic/realtime", dto);
    }
}
