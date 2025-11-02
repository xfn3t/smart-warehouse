package ru.rtc.warehouse.inventory.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.ProductEntityService;

@Service
@RequiredArgsConstructor
public class ProductEntServiceAdapter {

	private final ProductEntityService productEntityService;

	public Product findByCode(String code) {
		return productEntityService.findByCode(code);
	}

	public Product save(Product product) {
		return productEntityService.save(product);
	}

}
