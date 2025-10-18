package ru.rtc.warehouse.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.product.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
