package ru.rtc.warehouse.robot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.robot.model.RobotStatus;

import java.util.Optional;

@Repository
public interface RobotStatusRepository extends JpaRepository<RobotStatus, Long> {
	Optional<RobotStatus> findByCode(RobotStatus.StatusCode code);
}
