package ru.rtc.warehouse.inventory.service.product.dto;

import java.time.LocalDateTime;

public interface ProductWithLastInventoryProjection {
	String getProductCode();
	String getProductName();
	String getCategory();
	Integer getExpectedQuantity();
	Integer getActualQuantity();
	Integer getDifference();
	LocalDateTime getLastScannedAt();
	String getStatusCode();
	String getRobotCode();
}