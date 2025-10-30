package ru.rtc.warehouse.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.service.InventoryHistoryStatistic;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InventoryHistoryAdapter {

	private final InventoryHistoryStatistic ihStatistic;

	public BigDecimal avgDailySales() {
		return ihStatistic.avgDailySales();
	}

	public BigDecimal seasonalFactor() {
		return ihStatistic.seasonalFactor();
	}
}
