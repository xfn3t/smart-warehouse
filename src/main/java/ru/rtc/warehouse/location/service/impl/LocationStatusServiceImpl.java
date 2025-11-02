package ru.rtc.warehouse.location.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.location.model.LocationStatus;
import ru.rtc.warehouse.location.model.LocationStatus.LocationStatusCode;
import ru.rtc.warehouse.location.repository.LocationStatusRepository;
import ru.rtc.warehouse.location.service.LocationStatusEntityService;

@Service
@RequiredArgsConstructor
public class LocationStatusServiceImpl implements LocationStatusEntityService {

	private final LocationStatusRepository locationStatusRepository;

	@Override
	public LocationStatus getDefaultStatus() {
		return findByStatusCode(LocationStatusCode.RECENT);
	}

	@Override
	public LocationStatus findByStatusCode(LocationStatusCode locationStatusCode) {
		return locationStatusRepository.findByStatusCode(locationStatusCode);
	}
}
