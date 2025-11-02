package ru.rtc.warehouse.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.product.model.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	// Базовые методы
	Optional<Product> findByCodeAndIsDeletedFalse(String code);

	@Query("SELECT p FROM Product p WHERE p.isDeleted = false")
	List<Product> findAllActiveProducts();

	List<Product> findByCategoryAndIsDeletedFalse(String category);

	@Query("SELECT p FROM Product p WHERE p.name LIKE %:name% AND p.isDeleted = false")
	List<Product> findByNameContainingAndIsDeletedFalse(@Param("name") String name);

	boolean existsByCodeAndIsDeletedFalse(String code);
}