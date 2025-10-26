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
		uses = {WarehouseMapper.class},
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

	ProductDTO toDto(Product entity);

	@Mapping(target = "isDeleted", ignore = true)
	Product toEntity(ProductDTO dto);

	List<ProductDTO> toDtoList(List<Product> entities);

	@Mapping(target = "isDeleted", ignore = true)
	Product toEntity(ProductCreateRequest dto);
}