package ru.rtc.warehouse.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findBySkuCodeAndIsDeletedFalse(String skuCode);

	@Query("SELECT p FROM Product p WHERE p.isDeleted = false")
	List<Product> findAllActiveProducts();

	List<Product> findByCategoryAndIsDeletedFalse(String category);

	@Query("SELECT p FROM Product p WHERE p.name LIKE %:name% AND p.isDeleted = false")
	List<Product> findByNameContainingAndIsDeletedFalse(@Param("name") String name);

	boolean existsBySkuCodeAndIsDeletedFalse(String skuCode);

	@Query("SELECT p FROM Product p WHERE p.name = :name AND p.category = :category AND p.isDeleted = false")
	Optional<Product> findByNameAndCategoryAndIsDeletedFalse(@Param("name") String name,
															 @Param("category") String category);

	@Query("SELECT p FROM Product p " +
			"JOIN p.warehouseParameters pw " +
			"WHERE p.skuCode = :skuCode " +
			"AND pw.warehouse = :warehouse " +
			"AND p.isDeleted = false " +
			"AND pw.isDeleted = false")
	Optional<Product> findBySkuCodeAndWarehouse(
			@Param("skuCode") String skuCode,
			@Param("warehouse") Warehouse warehouse);
}