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
    public void on(RobotSnapshotEvent e) {
        var robot = e.getRobot();
        writer.onRobotSnapshot(robot);

        var warehouseCode = robot.getWarehouse().getCode();
        broker.convertAndSend("/topic/realtime/" + warehouseCode, stats.getStats(warehouseCode));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InventoryHistoryCreatedEvent e) {
        var history = e.getHistory();
        writer.onHistoryCreated(history);

        var warehouseCode = history.getWarehouse().getCode();
        broker.convertAndSend("/topic/realtime/" + warehouseCode, stats.getStats(warehouseCode));
    }
}
