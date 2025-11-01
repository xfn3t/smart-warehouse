package ru.rtc.warehouse.location.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.repository.LocationRepository;
import ru.rtc.warehouse.location.service.LocationEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationEntityServiceImpl implements LocationEntityService {

	private final LocationRepository repository;

	@Override
	public void save(Location location) {
		repository.save(location);
	}

	@Override
	public void update(Location location) {
		repository.save(location);
	}

	@Override
	public List<Location> findAll() {
		return repository.findAll();
	}

	@Override
	public Location findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Local not found"));
	}

	@Override
	public void delete(Long id) {
		repository.deleteById(id);
	}

	@Override
	public List<Location> saveAll(List<Location> locations) {
		return repository.saveAll(locations);
	}

	@Override
	public List<Location> findByWarehouse(Warehouse warehouse) {
		return repository.findByWarehouse(warehouse);
	}

}
