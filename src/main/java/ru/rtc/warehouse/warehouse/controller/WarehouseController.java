package ru.rtc.warehouse.warehouse.controller.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseUpdateRequest;
import ru.rtc.warehouse.warehouse.service.WarehouseService;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
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

	@PutMapping("/{warehouseId}")
	@ResponseStatus(HttpStatus.OK)
	public void updateWarehouse(
			@RequestBody WarehouseUpdateRequest warehouseUpdateRequest,
			@PathVariable Long warehouseId
	) {
		warehouseService.update(warehouseUpdateRequest, warehouseId);
	}

	private Long getUserId(Authentication authentication) {
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		return userDetails.getUser().getId();
	}
}
