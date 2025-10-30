package ru.rtc.warehouse.location.service;

import ru.rtc.warehouse.location.model.LocationStatus;

public interface LocationStatusEntityService {
	LocationStatus findByStatusCode(LocationStatus.LocationStatusCode locationStatusCode);
	LocationStatus getDefaultStatus();
}
