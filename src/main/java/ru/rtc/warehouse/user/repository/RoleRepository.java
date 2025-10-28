package ru.rtc.warehouse.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.user.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
	Role findByCode(Role.RoleCode code);
}
