package ru.rtc.warehouse.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.repository.ProductRepository;
import ru.rtc.warehouse.product.service.ProductEntityService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductEntityServiceImpl implements ProductEntityService {

	private final ProductRepository productRepository;

	@Override
	public Product save(Product product) {
		return productRepository.save(product);
	}

	@Override
	public Product update(Product product) {
		return productRepository.save(product);
	}

	@Override
	public List<Product> findAll() {
		return productRepository.findAll();
	}

	@Override
	public Product findById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Product not found"));
	}

	@Override
	public void delete(Long id) {
		productRepository.deleteById(id);
	}

	@Override
	public Product findByCode(String code) {
		return productRepository.findByCodeAndIsDeletedFalse(code)
				.orElseThrow(() -> new NotFoundException("Product not found"));
	}
}
