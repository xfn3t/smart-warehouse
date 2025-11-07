package ru.rtc.warehouse.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.service.dto.ProductWarehouseDTO;
import ru.rtc.warehouse.warehouse.mapper.WarehouseMapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		uses = {WarehouseMapper.class},
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductWarehouseMapper {

	ProductWarehouseDTO toDto(ProductWarehouse entity);

	List<ProductWarehouseDTO> toDtoList(List<ProductWarehouse> entities);
}