package ru.rtc.warehouse.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.model.Product;

@Mapper(componentModel = "spring",
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "minStock", source = "minStock")
	@Mapping(target = "optimalStock", source = "optimalStock")
	Product toEntity(ProductCreateRequest dto);

	@Mapping(target = "id", ignore = true)
	void updateEntity(@MappingTarget Product product, ProductCreateRequest dto);
}