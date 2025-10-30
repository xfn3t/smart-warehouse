package ru.rtc.warehouse.product.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.product.model.Product;

@Component
public class ProductReferenceMapper {

	@Named("mapProductToString")
	public String mapProductToString(Product product) {
		return product != null ? product.getCode() : null;
	}

	@Named("mapStringToProduct")
	public Product mapStringToProduct(String code) {
		if (code == null) return null;
		Product product = new Product();
		product.setCode(code);
		return product;
	}
}