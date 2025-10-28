package ru.rtc.warehouse.dashboard.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import ru.rtc.warehouse.dashboard.events.InventoryHistoryCreatedEvent;
import ru.rtc.warehouse.dashboard.events.RobotSnapshotEvent;

/**
 * Слушатель доменных событий: после коммита фиксирует метрики в Redis.
 */
@Component
@RequiredArgsConstructor
public class RealtimeEventsListener {

    private final RealtimeMetricsWriter writer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InventoryHistoryCreatedEvent e) {
        writer.onHistoryCreated(e.getHistory());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RobotSnapshotEvent e) {
        writer.onRobotSnapshot(e.getRobot());
    }
}
