package ru.rtc.warehouse.product.service;

import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.service.dto.ProductDTO;

import java.util.List;

public interface ProductService {
	ProductDTO create(String warehouseCode, ProductCreateRequest productCreateRequest);
	ProductDTO update(ProductUpdateRequest updateRequest, String productCode);
	ProductDTO findByCode(String productCode);
	List<ProductDTO> findAll();
	void delete(String productCode);
}