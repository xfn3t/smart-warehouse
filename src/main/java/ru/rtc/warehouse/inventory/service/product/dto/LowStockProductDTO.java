package ru.rtc.warehouse.inventory.service.product.dto;

public interface LowStockProductDTO {
	String getProductName();
	String getProductCode();
	Integer getMinStock();
	Integer getQuantity();
	Integer getReplenish();
}