package ru.rtc.warehouse.user.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.user.model.User;

public interface UserEntityService extends CrudEntityService<User, Long> {
	User findByEmail(String email);
	boolean existByEmail(String email);
	User saveUser(User user);
}
