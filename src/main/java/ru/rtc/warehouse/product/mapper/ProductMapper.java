package ru.rtc.warehouse.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.dto.ProductDTO;
import ru.rtc.warehouse.warehouse.mapper.WarehouseMapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		uses = {WarehouseMapper.class, ProductWarehouseMapper.class},
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

	@Mapping(target = "code", source = "skuCode")
	ProductDTO toDto(Product entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "skuCode", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "warehouseParameters", ignore = true)
	@Mapping(target = "inventoryHistory", ignore = true)
	Product toEntity(ProductDTO dto);

	List<ProductDTO> toDtoList(List<Product> entities);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "skuCode", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "warehouseParameters", ignore = true)
	@Mapping(target = "inventoryHistory", ignore = true)
	Product toEntity(ProductCreateRequest dto);
}