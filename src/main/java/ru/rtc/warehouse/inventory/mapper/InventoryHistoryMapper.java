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
	@Mapping(target = "robot", source = "robotCode", qualifiedByName = "mapRobotCodeToRobot")
	@Mapping(target = "product", source = "productCode", qualifiedByName = "mapProductCodeToProduct")
	@Mapping(target = "createdAt", ignore = true)
	InventoryHistory toEntity(InventoryHistoryCreateRequest dto);

	@Named("mapRobotCodeToRobot")
	default Robot mapRobotCodeToRobot(String code) {
		if (code == null) return null;
		return Robot.builder()
				.code(code)
				.build();
	}

	@Named("mapProductCodeToProduct")
	default Product mapProductCodeToProduct(String code) {
		if (code == null) return null;
		return Product.builder()
				.code(code)
				.build();
	}
}
