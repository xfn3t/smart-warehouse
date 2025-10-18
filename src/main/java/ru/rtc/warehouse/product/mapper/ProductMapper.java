package ru.rtc.warehouse.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.dto.ProductDTO;

import java.util.List;

@Mapper(componentModel = "spring",
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

	Product toEntity(ProductCreateRequest dto);
	ProductDTO toDto(Product product);
	void updateEntity(ProductUpdateRequest dto, @MappingTarget Product entity);
	List<Product> toEntityList(List<ProductCreateRequest> dtos);
	List<ProductDTO> toDtoList(List<Product> products);
}