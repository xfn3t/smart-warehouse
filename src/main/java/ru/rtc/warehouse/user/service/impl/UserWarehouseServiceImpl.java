package ru.rtc.warehouse.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.mapper.UserMapper;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.UserEntityService;
import ru.rtc.warehouse.user.service.UserService;
import ru.rtc.warehouse.user.service.UserWarehouseService;
import ru.rtc.warehouse.user.service.dto.UserDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserWarehouseServiceImpl implements UserWarehouseService {

	private final UserService userService;
	private final UserEntityService userEntityService;
	private final WarehouseEntityService warehouseEntityService;
	private final UserMapper userMapper;

	@Override
	@Transactional
	public UserDTO createUserForWarehouse(String warehouseCode, UserCreateRequest request) {

		UserDTO userDTO = userService.save(request);

		// Находим склад
		Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
		if (warehouse == null) {
			throw new NotFoundException("Warehouse not found: " + warehouseCode);
		}

		// Привязываем пользователя к складу (ManyToMany)
		User user = new User();
		user.setId(userDTO.getId());
		Set<Warehouse> warehouses = new HashSet<>();
		warehouses.add(warehouse);
		user.setWarehouses(warehouses);

		warehouse.getUsers().add(user);

		warehouseEntityService.save(warehouse);

		return userDTO;
	}

	@Override
	public List<UserDTO> findAllByWarehouse(String warehouseCode) {
		return userMapper.toDtoList(userEntityService.findAllByWarehouse(warehouseCode));
	}
}
