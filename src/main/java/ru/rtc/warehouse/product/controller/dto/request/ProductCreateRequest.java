package ru.rtc.warehouse.product.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest {
	@NotNull(message = "Product name is required")
	@Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
	private String name;

	@Size(max = 100, message = "Category must not exceed 100 characters")
	private String category;

	@NotNull(message = "Minimum stock is required")
	private Integer minStock;

	@NotNull(message = "Optimal stock is required")
	private Integer optimalStock;
}