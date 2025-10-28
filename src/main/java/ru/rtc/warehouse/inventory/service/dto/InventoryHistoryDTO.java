package ru.rtc.warehouse.inventory.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InventoryHistoryDTO {
	private Long id;
	private String robotCode;
	private String skuCode;
	private String productName;
	private Integer quantity;
	private String zone;
	private Integer rowNumber;
	private Integer shelfNumber;
	private InventoryHistoryStatus status;
	private LocalDateTime scannedAt;
	private LocalDateTime createdAt;
}
