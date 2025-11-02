package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.robot.model.RobotStatus;
import ru.rtc.warehouse.robot.repository.RobotStatusRepository;
import ru.rtc.warehouse.robot.service.RobotStatusService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RobotStatusServiceImpl implements RobotStatusService {

	private final RobotStatusRepository repository;

	@Override
	public RobotStatus save(RobotStatus robotStatus) {
		return repository.save(robotStatus);
	}

	@Override
	public RobotStatus update(RobotStatus robotStatus) {
		return repository.save(robotStatus);
	}

	@Override
	public List<RobotStatus> findAll() {
		return repository.findAll();
	}

	@Override
	public RobotStatus findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Robot status not found"));
	}

	@Override
	public void delete(Long id) {
		repository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public RobotStatus findByCode(RobotStatus.StatusCode status) {
		return repository.findByCode(status)
				.orElseThrow(() -> new NotFoundException("Robot status not found"));
	}
}
