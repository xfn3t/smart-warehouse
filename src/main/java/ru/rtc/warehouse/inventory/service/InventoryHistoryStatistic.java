package ru.rtc.warehouse.inventory.service;

import java.math.BigDecimal;

public interface InventoryHistoryStatistic {
	BigDecimal avgDailySales();
	BigDecimal seasonalFactor();
}
