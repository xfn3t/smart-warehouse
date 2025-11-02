package ru.rtc.warehouse.inventory.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
public class InventoryHistoryPageRequest {

	@Schema(description = "Номер страницы (0-based)", example = "0", defaultValue = "0")
	@Min(0)
	private Integer page = 0;

	@Schema(description = "Размер страницы (20|50|100)", example = "20", defaultValue = "20")
	@Min(1) @Max(100)
	private Integer size = 20;

	@Schema(
			description = "Поле сортировки",
			allowableValues = {"scannedAt", "zone", "rowNumber", "shelfNumber", "status", "quantity", "robotCode", "skuCode", "productName"},
			example = "scannedAt",
			defaultValue = "scannedAt"
	)
	private String sort = "scannedAt";

	@Schema(
			description = "Направление сортировки",
			allowableValues = {"ASC", "DESC"},
			example = "DESC",
			defaultValue = "DESC"
	)
	private String order = "DESC";

	public Pageable toPageable() {
		int actualSize = (size != 20 && size != 50 && size != 100) ? 20 : size;
		Sort.Direction direction = "ASC".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
		return PageRequest.of(page, actualSize, Sort.by(direction, sort));
	}
}