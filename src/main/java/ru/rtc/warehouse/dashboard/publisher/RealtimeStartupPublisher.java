package ru.rtc.warehouse.dashboard.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.dashboard.redis.RealtimeMetricsWriter;
import ru.rtc.warehouse.dashboard.service.adapter.DashboardInventoryHistoryEntServiceAdapter;
import ru.rtc.warehouse.dashboard.service.adapter.DashboardRobotEntServiceAdapter;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;
import ru.rtc.warehouse.dashboard.service.adapter.WarehouseEntServiceAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Component
@RequiredArgsConstructor
public class RealtimeStartupPublisher {

	private final RealtimeStatsService statsService;
	private final SimpMessagingTemplate broker;
	private final RealtimeMetricsWriter writer;
	private final WarehouseEntServiceAdapter warehouseService;

	private final DashboardRobotEntServiceAdapter robotService;

	private final DashboardInventoryHistoryEntServiceAdapter inventoryHistoryService;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional(readOnly = true)
	public void onStartup() {
		// --- Инициализация роботов ---
		robotService.findAll().forEach(writer::onRobotSnapshot);

		// --- Инициализация inventory history ---
		inventoryHistoryService.findAll().forEach(writer::onHistoryCreated);

		// --- Пушим начальные данные по каждому складу ---
		warehouseService.findAll()
				.stream()
				.map(Warehouse::getCode)
				.distinct()
				.forEach(code ->
						broker.convertAndSend("/topic/realtime/" + code, statsService.getStats(code))
				);
	}
}