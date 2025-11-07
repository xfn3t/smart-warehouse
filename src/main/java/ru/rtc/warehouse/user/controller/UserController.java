package ru.rtc.warehouse.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.service.UserWarehouseService;
import ru.rtc.warehouse.user.service.dto.UserDTO;

import java.util.List;

@RestController
@RequestMapping("/api/{warehouseCode}/users")
@RequiredArgsConstructor
@RequiresOwnership(codeParam = "warehouseCode", entityType = RequiresOwnership.EntityType.WAREHOUSE)
public class UserController {

	private final UserWarehouseService userWarehouseService;

	@GetMapping
	public List<UserDTO> getAllWarehouseUser(@PathVariable String warehouseCode) {
		return userWarehouseService.findAllByWarehouse(warehouseCode);
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public void register(
			@PathVariable String warehouseCode,
			@Valid @RequestBody UserCreateRequest request) {
		userWarehouseService.createUserForWarehouse(warehouseCode, request);
	}
}
