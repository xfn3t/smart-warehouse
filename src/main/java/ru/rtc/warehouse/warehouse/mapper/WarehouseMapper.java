package ru.rtc.warehouse.warehouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WarehouseMapper {

	@Mapping(source = "warehouseLocation", target = "location")
	WarehouseDTO toDto(Warehouse entity);

	WarehouseDTO toDto(WarehouseCreateRequest warehouseCreateRequest);

	@Mapping(source = "location", target = "warehouseLocation")
	Warehouse toEntity(WarehouseDTO dto);

	@Mapping(source = "location", target = "warehouseLocation")
	Warehouse toEntity(WarehouseCreateRequest request);

	List<WarehouseDTO> toDtoList(List<Warehouse> warehouses);
}