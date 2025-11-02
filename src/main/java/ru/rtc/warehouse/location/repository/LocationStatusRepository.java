package ru.rtc.warehouse.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.location.model.LocationStatus;
import ru.rtc.warehouse.location.model.LocationStatus.LocationStatusCode;

@Repository
public interface LocationStatusRepository extends JpaRepository<LocationStatus, Long> {
	LocationStatus findByStatusCode(LocationStatusCode locationStatusCode);
}
