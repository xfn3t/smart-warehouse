package ru.rtc.warehouse.user.service;

import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.service.dto.UserDTO;

import java.util.List;

public interface UserWarehouseService {
	UserDTO createUserForWarehouse(String warehouseCode, UserCreateRequest request);
	List<UserDTO> findAllByWarehouse(String warehouseCode);
}
