package ru.rtc.warehouse.inventory.controller.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

import java.util.List;

@Data
public class ProductLastInventorySearchRequest {

	@ArraySchema(arraySchema = @Schema(description = "Категории товара"),
			schema = @Schema(example = "network"))
	private List<String> categories;

	@ArraySchema(
			arraySchema = @Schema(description = "Статусы записей"),
			schema = @Schema(
					implementation = ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode.class,
					example = "CRITICAL"
			)
	)
	private List<InventoryHistoryStatus.InventoryHistoryStatusCode> statuses;

	@Schema(description = "Поиск по артикулу товара (Product.code), названию (Product.name) и коду робота (Robot.code); без учета регистра")
	private String q;

	@ArraySchema(arraySchema = @Schema(description = "Коды роботов"),
			schema = @Schema(example = "RB-001"))
	private List<String> robots;
}