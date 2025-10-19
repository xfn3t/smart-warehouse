package ru.rtc.warehouse.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.AlreadyExistsException;
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.mapper.UserMapper;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.UserEntityService;
import ru.rtc.warehouse.user.service.UserService;
import ru.rtc.warehouse.user.service.dto.UserDTO;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserEntityService userEntityService;
	private final UserMapper userMapper;

	public UserDTO save(UserCreateRequest request) {
		User user = userMapper.toEntity(request);

		if (userEntityService.existByEmail(user.getEmail())) {
			throw new AlreadyExistsException("User already exists");
		}

		if (request.getRole() == null)
			user.setRole(User.Role.VIEWER);

		return userMapper.toDto(userEntityService.saveUser(user));
	}



}
