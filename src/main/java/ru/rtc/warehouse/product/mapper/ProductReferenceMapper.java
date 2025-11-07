package ru.rtc.warehouse.product.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.product.model.Product;

@Component
public class ProductReferenceMapper {

	@Named("mapProductToString")
	public String mapProductToString(Product product) {
		return product != null ? product.getSkuCode() : null;
	}

	@Named("mapStringToProduct")
	public Product mapStringToProduct(String skuCode) {
		if (skuCode == null) return null;
		Product product = new Product();
		product.setSkuCode(skuCode);
		return product;
	}
}