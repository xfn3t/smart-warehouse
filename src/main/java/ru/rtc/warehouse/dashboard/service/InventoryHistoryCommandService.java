package ru.rtc.warehouse.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.dashboard.events.InventoryHistoryCreatedEvent;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;

@Service
@RequiredArgsConstructor
public class InventoryHistoryCommandService {
    private final InventoryHistoryRepository repo;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public InventoryHistory create(InventoryHistory e) {
        InventoryHistory saved = repo.save(e);
        // после коммита — в Redis
        publisher.publishEvent(new InventoryHistoryCreatedEvent(this, saved));
        return saved;
    }
}