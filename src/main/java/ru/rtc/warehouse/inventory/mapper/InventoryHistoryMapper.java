package ru.rtc.warehouse.inventory.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.product.mapper.ProductMapper;
import ru.rtc.warehouse.robot.mapper.RobotMapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		uses = {
				InventoryStatusReferenceMapper.class,
				ProductMapper.class,
				RobotMapper.class
		},
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface InventoryHistoryMapper {

	@Mapping(target = "status", source = "status.code")
	InventoryHistoryDTO toDto(InventoryHistory entity);

	@Mapping(target = "status", source = "status", qualifiedByName = "mapStringToInventoryStatus")
	@Mapping(target = "isDeleted", ignore = true)
	InventoryHistory toEntity(InventoryHistoryDTO dto);

	@Mapping(target = "status", source = "status", qualifiedByName = "mapStringToInventoryStatus")
	@Mapping(target = "isDeleted", ignore = true)
	InventoryHistory toEntity(InventoryHistoryCreateRequest dto);

	List<InventoryHistoryDTO> toDtoList(List<InventoryHistory> entities);
	List<InventoryHistory> toEntityList(List<InventoryHistoryDTO> dtos);
}