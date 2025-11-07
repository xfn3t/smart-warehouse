package ru.rtc.warehouse.inventory.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryStatusRepository;
import ru.rtc.warehouse.inventory.service.adapter.IHProductEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.adapter.IHRobotEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.robot.model.Robot;

import java.util.List;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class InventoryHistoryMapper {

	@Autowired
	protected IHRobotEntServiceAdapter robotService;
	@Autowired
	protected IHProductEntServiceAdapter productService;
	@Autowired
	private InventoryHistoryStatusRepository statusRepo;

	// -------- Entity -> DTO --------

	@Mappings({
			@Mapping(target = "robotCode", source = "robot.code"),
			@Mapping(target = "skuCode", source = "product.skuCode"), // Использовать skuCode
			@Mapping(target = "productName", source = "product.name"),
			@Mapping(target = "robot", ignore = true),
			@Mapping(target = "product", ignore = true),
			@Mapping(target = "warehouse", ignore = true),
			@Mapping(target = "status", source = "status.code")
	})
	public abstract InventoryHistoryDTO toDto(InventoryHistory entity);

	public abstract List<InventoryHistoryDTO> toDtoList(List<InventoryHistory> entities);

	// DTO -> Entity: разрешаем ссылки по кодам
	@Mappings({
			@Mapping(target = "robot", source = "robotCode", qualifiedByName = "resolveRobot"),
			@Mapping(target = "product", source = "skuCode", qualifiedByName = "resolveProductBySku"),
			@Mapping(target = "status", source = "status", qualifiedByName = "resolveStatus")
	})
	public abstract InventoryHistory toEntity(InventoryHistoryDTO dto);

	@Mappings({
			@Mapping(target = "robot", source = "robotCode", qualifiedByName = "resolveRobot"),
			@Mapping(target = "product", source = "productCode", qualifiedByName = "resolveProductBySku")
	})
	public abstract InventoryHistory toEntity(InventoryHistoryCreateRequest dto);

	@Mappings({
			@Mapping(target = "robot",   source = "robotCode",   qualifiedByName = "resolveRobot"),
			@Mapping(target = "product", source = "productCode", qualifiedByName = "resolveProductBySku"),
			@Mapping(target = "status",  source = "status",      qualifiedByName = "resolveStatus")
	})
	public abstract InventoryHistory toEntity(InventoryHistoryUpdateRequest dto);

	public abstract List<InventoryHistory> toEntityList(List<InventoryHistoryDTO> dtos);

	// Резолверы ссылок
	@Named("resolveRobot")
	protected Robot resolveRobot(String code) {
		return (code == null || code.isBlank()) ? null : robotService.findByCode(code);
	}

	@Named("resolveProductBySku")
	protected Product resolveProductBySku(String sku) {
		return (sku == null || sku.isBlank()) ? null : productService.findByCode(sku);
	}

	@Named("resolveStatus")
	protected InventoryHistoryStatus resolveStatus(InventoryHistoryStatus.InventoryHistoryStatusCode code) {
		if (code == null) return null;
		return statusRepo.findByCode(code)
				.orElseThrow(() -> new IllegalArgumentException("Status not found: " + code));
	}
}