package ru.rtc.warehouse.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findByCode(String code);

	Optional<Product> findByCodeAndWarehouse(String code, Warehouse warehouse);
}
