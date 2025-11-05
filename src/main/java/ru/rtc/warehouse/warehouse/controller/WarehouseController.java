package ru.rtc.warehouse.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseUpdateRequest;
import ru.rtc.warehouse.warehouse.service.WarehouseService;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@RequiresOwnership(codeParam = "warehouseCode", entityType = RequiresOwnership.EntityType.WAREHOUSE)
public class WarehouseController {

	private final WarehouseService warehouseService;

	@GetMapping
	public List<WarehouseDTO> getUserWarehouses(Authentication authentication) {
		return warehouseService.findByUserId(getUserId(authentication));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void createWarehouse(@RequestBody WarehouseCreateRequest warehouseCreateRequest, Authentication authentication) {
		warehouseService.save(warehouseCreateRequest, getUserId(authentication));
	}

	@PutMapping("/{warehouseCode}")
	@ResponseStatus(HttpStatus.OK)
	public void updateWarehouse(
			@RequestBody WarehouseUpdateRequest warehouseUpdateRequest,
			@PathVariable String warehouseCode
	) {
		warehouseService.update(warehouseUpdateRequest, warehouseCode);
	}

	@DeleteMapping("/{warehouseCode}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteWarehouse(@PathVariable String warehouseCode) {
		warehouseService.delete(warehouseCode);
	}

	private Long getUserId(Authentication authentication) {
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		return userDetails.getUser().getId();
	}
}
