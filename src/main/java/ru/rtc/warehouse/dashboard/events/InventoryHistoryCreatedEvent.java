package ru.rtc.warehouse.dashboard.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

/**
 * Событие: строка истории инвентаризации создана (после коммита).
 */
@Getter
public class InventoryHistoryCreatedEvent extends ApplicationEvent {
    private final InventoryHistory history;

    public InventoryHistoryCreatedEvent(Object source, InventoryHistory history) {
        super(source);
        this.history = history;
    }
}