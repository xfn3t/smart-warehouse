package ru.rtc.warehouse.warehouse.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Positive;

@Getter
@Setter
public class WarehouseCreateRequest {

	@NotBlank(message = "Code is required")
	@Size(max = 50, message = "Code must not exceed 50 characters")
	private String code;

	@NotBlank(message = "Name is required")
	@Size(max = 255, message = "Name must not exceed 255 characters")
	private String name;

	@NotNull(message = "Zone max size is required")
	@Positive(message = "Zone max size must be positive")
	private Integer zoneMaxSize;

	@NotNull(message = "Row max size is required")
	@Positive(message = "Row max size must be positive")
	private Integer rowMaxSize;

	@NotNull(message = "Shelf max size is required")
	@Positive(message = "Shelf max size must be positive")
	private Integer shelfMaxSize;

	@Size(max = 255, message = "Location must not exceed 255 characters")
	private String location;
}