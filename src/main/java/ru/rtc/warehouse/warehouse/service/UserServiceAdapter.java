package ru.rtc.warehouse.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.UserEntityService;

@Service
@RequiredArgsConstructor
public class UserServiceAdapter {

	private final UserEntityService userService;

	public User getUserById(Long userId) {
		return userService.findById(userId);
	}
}
