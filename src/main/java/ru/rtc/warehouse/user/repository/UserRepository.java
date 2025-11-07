package ru.rtc.warehouse.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.user.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String username);

	@Query("""
		SELECT u
		FROM User u
		JOIN u.warehouses w
		WHERE w.code = :warehouseCode
	""")
	List<User> findAllByWarehouseCode(@Param("warehouseCode") String warehouseCode);

}
