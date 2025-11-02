package ru.rtc.warehouse.inventory.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLastInventoryPageDTO {
	private long total;
	private int page;
	private int size;
	private List<ProductLastInventoryDTO> items;
}