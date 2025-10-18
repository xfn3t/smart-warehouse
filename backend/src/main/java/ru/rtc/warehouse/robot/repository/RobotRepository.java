package ru.rtc.warehouse.robot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.robot.model.Robot;

@Repository
public interface RobotRepository extends JpaRepository<Robot, String> {
	@Query("SELECT MAX(CAST(SUBSTRING(r.id, 4) AS int)) FROM Robot r WHERE r.id LIKE 'RB-%'")
	Integer findMaxRobotNumber();
}
