package ru.rtc.warehouse.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.user.model.Role;
import ru.rtc.warehouse.user.repository.RoleRepository;
import ru.rtc.warehouse.user.service.RoleService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

	private final RoleRepository repository;

	@Override
	public void save(Role role) {
		repository.save(role);
	}

	@Override
	public void update(Role role) {
		repository.save(role);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> findAll() {
		return repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Role findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Role not found"));
	}

	@Override
	public void delete(Long id) {
		repository.deleteById(id);
	}

	@Override
	public Role findByCode(Role.RoleCode code) {
		return repository.findByCode(code);
	}
}
