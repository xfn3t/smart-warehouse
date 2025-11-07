package ru.rtc.warehouse.product.controller.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateRequest {
	@Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
	private String name;

	@Size(max = 100, message = "Category must not exceed 100 characters")
	private String category;
}