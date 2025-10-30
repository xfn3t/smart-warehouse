package ru.rtc.warehouse.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.AlreadyExistsException;
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.mapper.UserMapper;
import ru.rtc.warehouse.user.model.Role;
import ru.rtc.warehouse.user.model.Role.RoleCode;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.RoleService;
import ru.rtc.warehouse.user.service.UserEntityService;
import ru.rtc.warehouse.user.service.UserService;
import ru.rtc.warehouse.user.service.dto.UserDTO;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserEntityService userEntityService;
	private final RoleService roleService;
	private final UserMapper userMapper;

	public UserDTO save(UserCreateRequest request) {
		User user = userMapper.toEntity(request);

		if (userEntityService.existByEmail(user.getEmail())) {
			throw new AlreadyExistsException("User already exists");
		}

		// Обрабатываем оба случая: с ролью и без
		Role role;
		if (request.getRole() != null) {
			// Ищем переданную роль в базе данных
			role = roleService.findByCode(RoleCode.from(request.getRole()));
		} else {
			// Роль по умолчанию
			role = roleService.findByCode(RoleCode.VIEWER);
		}

		user.setRole(role); // Устанавливаем найденную роль из БД

		return userMapper.toDto(userEntityService.saveUser(user));
	}
}
