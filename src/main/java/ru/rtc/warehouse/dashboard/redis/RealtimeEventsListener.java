package ru.rtc.warehouse.dashboard.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import ru.rtc.warehouse.dashboard.events.InventoryHistoryCreatedEvent;
import ru.rtc.warehouse.dashboard.events.RobotSnapshotEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;

/**
 * Слушатель доменных событий: после коммита фиксирует метрики в Redis.
 */
@Component
@RequiredArgsConstructor
public class RealtimeEventsListener {

    private final RealtimeMetricsWriter writer;
    private final SimpMessagingTemplate broker;
    private final RealtimeStatsService stats;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InventoryHistoryCreatedEvent e) {
        writer.onHistoryCreated(e.getHistory());
        broker.convertAndSend("/topic/realtime", stats.getStats()); // мгновенно пушим обновление
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RobotSnapshotEvent e) {
        writer.onRobotSnapshot(e.getRobot());
        broker.convertAndSend("/topic/realtime", stats.getStats());
    }
}
