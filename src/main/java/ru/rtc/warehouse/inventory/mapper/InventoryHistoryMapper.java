package ru.rtc.warehouse.inventory.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.ProductEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.RobotEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.robot.model.Robot;

import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class InventoryHistoryMapper {

	private final RobotEntServiceAdapter robotService;
	private final ProductEntServiceAdapter productService;

	public InventoryHistoryDTO toDto(InventoryHistory entity) {
		if (entity == null) return null;

		InventoryHistoryDTO dto = new InventoryHistoryDTO();
		dto.setId(entity.getId());

		dto.setRobotCode(entity.getRobot() != null ? entity.getRobot().getCode() : null);
		if (entity.getProduct() != null) {
			dto.setSkuCode(entity.getProduct().getCode());     // <-- код (sku)
			dto.setProductName(entity.getProduct().getName()); // <-- название
		}

		dto.setQuantity(entity.getQuantity());
		dto.setZone(entity.getZone());
		dto.setRowNumber(entity.getRowNumber());
		dto.setShelfNumber(entity.getShelfNumber());
		dto.setStatus(entity.getStatus());
		dto.setScannedAt(entity.getScannedAt());
		dto.setCreatedAt(entity.getCreatedAt());
		return dto;
	}

	public List<InventoryHistoryDTO> toDtoList(List<InventoryHistory> entities) {
		if (entities == null) return List.of();
		return entities.stream().map(this::toDto).collect(Collectors.toList());
	}

	public InventoryHistory toEntity(InventoryHistoryDTO dto) {
		if (dto == null) return null;

		InventoryHistory entity = new InventoryHistory();
		entity.setId(dto.getId());

		entity.setRobot(findRobot(dto.getRobotCode()));
		entity.setProduct(findProductBySku(dto.getSkuCode())); // <-- по skuCode

		entity.setQuantity(dto.getQuantity());
		entity.setZone(dto.getZone());
		entity.setRowNumber(dto.getRowNumber());
		entity.setShelfNumber(dto.getShelfNumber());
		entity.setStatus(dto.getStatus());
		entity.setScannedAt(dto.getScannedAt());
		entity.setCreatedAt(dto.getCreatedAt());
		return entity;
	}

	public InventoryHistory toEntity(InventoryHistoryCreateRequest r) {
		InventoryHistory e = new InventoryHistory();

		Robot robot = new Robot();
		robot.setCode(r.getRobotCode());
		e.setRobot(robot);

		Product product = new Product();
		product.setCode(r.getProductCode()); // поле запроса у тебя называется productCode — ок
		e.setProduct(product);

		e.setQuantity(r.getQuantity());
		e.setZone(r.getZone());
		e.setRowNumber(r.getRowNumber());
		e.setShelfNumber(r.getShelfNumber());
		e.setStatus(r.getStatus());
		e.setScannedAt(r.getScannedAt());
		return e;
	}

	public InventoryHistory toEntity(InventoryHistoryUpdateRequest r) {
		if (r == null) return null;

		InventoryHistory e = new InventoryHistory();

		Robot robot = new Robot();
		robot.setCode(r.getRobotCode());
		e.setRobot(robot);

		Product product = new Product();
		product.setCode(r.getProductCode()); // аналогично
		e.setProduct(product);

		e.setQuantity(r.getQuantity());
		e.setZone(r.getZone());
		e.setRowNumber(r.getRowNumber());
		e.setShelfNumber(r.getShelfNumber());
		e.setStatus(r.getStatus());
		e.setScannedAt(r.getScannedAt());
		return e;
	}

	public List<InventoryHistory> toEntityList(List<InventoryHistoryDTO> dtos) {
		if (dtos == null) return List.of();
		return dtos.stream().map(this::toEntity).collect(Collectors.toList());
	}

	private Robot findRobot(String code) {
		if (code == null) return null;
		return robotService.findByCode(code);
	}

	private Product findProductBySku(String skuCode) { // явное имя для читаемости
		if (skuCode == null) return null;
		return productService.findByCode(skuCode); // сервис ищет по коду товара (sku_code)
	}
}

