package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryStatistic;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InventoryHistoryStatisticImpl implements InventoryHistoryStatistic {

	private final InventoryHistoryRepository ihRepository;

	@Override
	public BigDecimal avgDailySales() {
		return ihRepository.avgDailySales().orElse(BigDecimal.ZERO);
	}

	@Override
	public BigDecimal seasonalFactor() {
		return ihRepository.seasonalFactor().orElse(BigDecimal.ZERO);
	}
}
