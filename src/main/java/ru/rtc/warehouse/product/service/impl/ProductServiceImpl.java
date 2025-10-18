package ru.rtc.warehouse.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.mapper.ProductMapper;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.ProductEntityService;
import ru.rtc.warehouse.product.service.ProductService;
import ru.rtc.warehouse.product.service.dto.ProductDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductEntityService productEntityService;
	private final ProductMapper productMapper;

	@Override
	public void save(ProductCreateRequest productCreateRequest) {
		Product product = productMapper.toEntity(productCreateRequest);
		productEntityService.save(product);
	}

	@Override
	public void update(ProductUpdateRequest updateRequest, Long id) {

		Product product = productEntityService.findById(id);

		String code = updateRequest.getCode();
		String category = updateRequest.getCategory();
		String name = updateRequest.getName();
		Integer minStock = updateRequest.getMinStock();
		Integer optimalStock = updateRequest.getOptimalStock();

		if (code != null) product.setCode(code);
		if (category != null) product.setCategory(category);
		if (name != null) product.setName(name);
		if (minStock != null) product.setMinStock(minStock);
		if (optimalStock != null) product.setOptimalStock(optimalStock);

		productEntityService.update(product);
	}

	@Override
	public ProductDTO findById(Long id) {
		return productMapper.toDto(productEntityService.findById(id));
	}

	@Override
	public ProductDTO findByCode(String code) {
		return productMapper.toDto(productEntityService.findByCode(code));
	}

	@Override
	public List<ProductDTO> findAll() {
		return productMapper.toDtoList(productEntityService.findAll());
	}

	@Override
	public void delete(Long id) {
		productEntityService.delete(id);
	}


}
