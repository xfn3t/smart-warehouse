package ru.rtc.warehouse.inventory.mapper;

import org.mapstruct.*;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.robot.model.Robot;

@Mapper(componentModel = "spring",
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InventoryHistoryMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "robot", source = "robotId", qualifiedByName = "mapRobotIdToRobot")
	@Mapping(target = "product", source = "productId", qualifiedByName = "mapProductIdToProduct")
	@Mapping(target = "createdAt", ignore = true)
	InventoryHistory toEntity(InventoryHistoryCreateRequest dto);

	@Named("mapRobotIdToRobot")
	default Robot mapRobotIdToRobot(Long robotId) {
		if (robotId == null) return null;
		return Robot.builder()
				.id("RB-" + robotId)
				.build();
	}

	@Named("mapProductIdToProduct")
	default Product mapProductIdToProduct(String productId) {
		if (productId == null) return null;
		return Product.builder()
				.id(productId)
				.build();
	}
}
