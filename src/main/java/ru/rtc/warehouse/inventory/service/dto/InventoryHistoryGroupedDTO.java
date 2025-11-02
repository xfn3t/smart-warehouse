package ru.rtc.warehouse.inventory.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistoryGroupedDTO {
	private String skuCode;
	private List<InventoryHistoryDTO> history;
}