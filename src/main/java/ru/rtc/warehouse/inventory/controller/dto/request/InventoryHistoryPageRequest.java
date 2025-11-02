package ru.rtc.warehouse.inventory.controller.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Data
public class InventoryHistoryPageRequest {

	@Schema(description = "Номер страницы (0-based)", example = "0", defaultValue = "0")
	private Integer page = 0;

	@Schema(description = "Размер страницы (20|50|100)", example = "20", defaultValue = "20")
	private Integer size = 20;

	@ArraySchema(
			arraySchema = @Schema(
					description = "Повторяемый параметр сортировки вида 'поле,направление'. " +
							"Примеры: sort=productName,asc&sort=scannedAt,desc. " +
							"Если направление не указано — по умолчанию ASC."
			),
			schema = @Schema(example = "scannedAt,desc")
	)
	private List<String> sort;

	public Pageable toPageable() {
		int p = (page == null || page < 0) ? 0 : page;
		int s = normalizeSize(size);
		Sort sortObj = parseSort(sort);
		if (sortObj.isUnsorted()) {
			sortObj = Sort.by(Sort.Direction.DESC, "scannedAt");
		}
		return PageRequest.of(p, s, sortObj);
	}

	private static int normalizeSize(Integer size) {
		if (size == null) return 20;
		return (size == 20 || size == 50 || size == 100) ? size : 20;
	}

	private static Sort parseSort(List<String> raw) {
		if (raw == null || raw.isEmpty()) return Sort.unsorted();
		List<Sort.Order> orders = new ArrayList<>();
		for (String entry : raw) {
			if (entry == null || entry.isBlank()) continue;
			String[] parts = entry.split(",");
			String prop = parts[0].trim();
			if (prop.isEmpty()) continue;

			Sort.Direction dir = Sort.Direction.ASC;
			if (parts.length > 1) {
				String dirRaw = parts[1].trim();
				try {
					dir = Sort.Direction.fromString(dirRaw);
				} catch (IllegalArgumentException ignored) {
					// некорректное направление → по умолчанию ASC
				}
			}
			orders.add(new Sort.Order(dir, prop));
		}
		return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
	}
}