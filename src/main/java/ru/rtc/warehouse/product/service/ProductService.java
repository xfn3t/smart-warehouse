package ru.rtc.warehouse.product.service;

import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.service.dto.ProductDTO;

import java.util.List;

public interface ProductService {
	void save(ProductCreateRequest productCreateRequest);
	void update(ProductUpdateRequest updateRequest, Long id);
	ProductDTO findById(Long id);
	ProductDTO findByCode(String code);
	List<ProductDTO> findAll();
	void delete(Long id);
}
