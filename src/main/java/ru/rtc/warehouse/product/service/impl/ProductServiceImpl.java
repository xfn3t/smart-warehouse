package ru.rtc.warehouse.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.mapper.ProductMapper;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.repository.ProductRepository;
import ru.rtc.warehouse.product.service.ProductService;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final ProductMapper productMapper;

	public void save(ProductCreateRequest productCreateRequest) {

		Product product = productMapper.toEntity(productCreateRequest);
		productRepository.save(product);
	}
}
