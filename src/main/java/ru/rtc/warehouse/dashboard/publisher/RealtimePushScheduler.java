package ru.rtc.warehouse.dashboard.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;
import ru.rtc.warehouse.dashboard.service.WarehouseEntServiceAdapter;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class RealtimePushScheduler {

    private final RealtimeStatsService statsService;
    private final SimpMessagingTemplate broker;
    private final WarehouseEntServiceAdapter warehouseService;

    // каждые 30 секунд шлём свежий срез
    @Scheduled(fixedRateString = "${app.dashboard.push-interval-ms:30000}")
    public void push() {
        warehouseService.findAll().forEach(w -> {
            String warehouseCode = w.getCode();
            broker.convertAndSend("/topic/realtime/" + warehouseCode,
                    statsService.getStats(warehouseCode));
        });
    }
}
