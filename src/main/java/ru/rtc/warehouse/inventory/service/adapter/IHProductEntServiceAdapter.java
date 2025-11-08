package ru.rtc.warehouse.inventory.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.ProductEntityService;

@Service
@RequiredArgsConstructor
public class IHProductEntServiceAdapter {

	private final ProductEntityService productEntityService;

	public Product findByCode(String code) {
		return productEntityService.findBySkuCode(code);
	}

	public Product save(Product product) {
		return productEntityService.save(product);
	}

	public Product findByNameAndCategory(String name, String category) {
		return productEntityService.findByNameAndCategory(name, category);
	}

}
