package ru.rtc.warehouse.user.service;

import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.service.dto.UserDTO;

public interface UserService {
	UserDTO save(UserCreateRequest request);
}
