package ru.rtc.warehouse.user.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.user.model.Role;
import ru.rtc.warehouse.user.model.Role.RoleCode;

public interface RoleService extends CrudEntityService<Role, Long> {
	Role findByCode(RoleCode code);
}
