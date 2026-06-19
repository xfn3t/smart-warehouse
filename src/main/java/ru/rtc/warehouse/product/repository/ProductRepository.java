package ru.rtc.warehouse.product.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByUserIdAndSkuCodeAndIsDeletedFalse(
        Long userId,
        String skuCode
    );

    /**
     * Глобальный поиск по SKU (без userId).
     * Используется в AI/роботных операциях, где пользователь не всегда известен.
     * @deprecated Предпочитайте findByUserIdAndSkuCodeAndIsDeletedFalse
     */
    @Deprecated
    Optional<Product> findBySkuCodeAndIsDeletedFalse(String skuCode);

    /**
     * Безопасный поиск любого продукта по SKU (без userId).
     * Возвращает первый попавшийся, если несколько пользователей имеют один SKU.
     * Используется для операций, не требующих точной привязки к пользователю
     * (например, получение изображения).
     */
    @Query(
        "SELECT p FROM Product p " +
            "WHERE p.skuCode = :skuCode " +
            "AND p.isDeleted = false " +
            "ORDER BY p.id"
    )
    List<Product> findAnyBySkuCodeAndIsDeletedFalse(
        @Param("skuCode") String skuCode
    );

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false")
    List<Product> findAllActiveProducts();

    @Query(
        "SELECT p FROM Product p WHERE p.user.id = :userId AND p.isDeleted = false"
    )
    List<Product> findAllActiveProductsByUserId(@Param("userId") Long userId);

    List<Product> findByCategoryAndIsDeletedFalse(String category);

    @Query(
        "SELECT p FROM Product p WHERE p.name LIKE %:name% AND p.isDeleted = false"
    )
    List<Product> findByNameContainingAndIsDeletedFalse(
        @Param("name") String name
    );

    boolean existsByUserIdAndSkuCodeAndIsDeletedFalse(
        Long userId,
        String skuCode
    );

    @Query(
        "SELECT p FROM Product p WHERE p.name = :name AND p.category = :category AND p.isDeleted = false"
    )
    Optional<Product> findByNameAndCategoryAndIsDeletedFalse(
        @Param("name") String name,
        @Param("category") String category
    );

    @Query(
        "SELECT p FROM Product p " +
            "JOIN p.warehouseParameters pw " +
            "WHERE p.skuCode = :skuCode " +
            "AND p.user.id = :userId " +
            "AND pw.warehouse = :warehouse " +
            "AND p.isDeleted = false " +
            "AND pw.isDeleted = false"
    )
    Optional<Product> findByUserIdAndSkuCodeAndWarehouse(
        @Param("userId") Long userId,
        @Param("skuCode") String skuCode,
        @Param("warehouse") Warehouse warehouse
    );

    /** @deprecated Используйте findByUserIdAndSkuCodeAndWarehouse */
    @Deprecated
    @Query(
        "SELECT p FROM Product p " +
            "JOIN p.warehouseParameters pw " +
            "WHERE p.skuCode = :skuCode " +
            "AND pw.warehouse = :warehouse " +
            "AND p.isDeleted = false " +
            "AND pw.isDeleted = false"
    )
    Optional<Product> findBySkuCodeAndWarehouse(
        @Param("skuCode") String skuCode,
        @Param("warehouse") Warehouse warehouse
    );

    /**
     * Поиск продукта по userId + SKU + коду склада.
     * Учитывает, что один SKU может быть у разных пользователей на одном складе.
     */
    @Query(
        "SELECT p FROM Product p " +
            "JOIN p.warehouseParameters pw " +
            "JOIN pw.warehouse w " +
            "WHERE p.skuCode = :skuCode " +
            "AND p.user.id = :userId " +
            "AND w.code = :warehouseCode " +
            "AND p.isDeleted = false " +
            "AND pw.isDeleted = false"
    )
    Optional<Product> findByUserIdAndSkuCodeAndWarehouseCode(
        @Param("userId") Long userId,
        @Param("skuCode") String skuCode,
        @Param("warehouseCode") String warehouseCode
    );

    /**
     * Поиск любого продукта по SKU + коду склада (без userId).
     * Используется в AI/роботных сценариях, где пользователь неизвестен.
     * Если несколько пользователей имеют один SKU на складе, возвращает первый.
     */
    @Query(
        "SELECT p FROM Product p " +
            "JOIN p.warehouseParameters pw " +
            "JOIN pw.warehouse w " +
            "WHERE p.skuCode = :skuCode " +
            "AND w.code = :warehouseCode " +
            "AND p.isDeleted = false " +
            "AND pw.isDeleted = false " +
            "ORDER BY p.id"
    )
    List<Product> findBySkuCodeAndWarehouseCode(
        @Param("skuCode") String skuCode,
        @Param("warehouseCode") String warehouseCode
    );

    @Query(
        "SELECT DISTINCT p FROM Product p " +
            "JOIN p.warehouseParameters pw " +
            "JOIN pw.warehouse w " +
            "WHERE w.code = :warehouseCode " +
            "AND p.isDeleted = false " +
            "AND pw.isDeleted = false"
    )
    List<Product> findByWarehouseCodeAndIsDeletedFalse(
        @Param("warehouseCode") String warehouseCode
    );

    @Query(
        "SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.isDeleted = false ORDER BY p.category"
    )
    List<String> findDistinctCategories();

    /**
     * Возвращает товары пользователя на его складах с актуальным количеством (последнее сканирование).
     * Один нативный запрос — без N+1.
     */
    @Query(
        value = """
            SELECT
                p.sku_code            AS skuCode,
                p.name                AS productName,
                p.category            AS category,
                p.image_url           AS imageUrl,
                w.code                AS warehouseCode,
                w.name                AS warehouseName,
                latest.quantity       AS quantity,
                latest.zone           AS zone,
                latest.row            AS "row",
                latest.shelf          AS shelf,
                pw.min_stock          AS minStock,
                pw.optimal_stock      AS optimalStock
            FROM products p
            JOIN product_warehouse pw
                ON pw.product_id = p.id
                AND pw.is_deleted = FALSE
            JOIN warehouses w
                ON w.id = pw.warehouse_id
                AND w.is_deleted = FALSE
            JOIN user_warehouses uw
                ON uw.warehouse_id = w.id
                AND uw.user_id = :userId
            LEFT JOIN LATERAL (
                SELECT
                    ih.quantity,
                    ih.zone,
                    ih.row,
                    ih.shelf
                FROM inventory_history ih
                WHERE ih.product_id = p.id
                  AND ih.warehouse_id = w.id
                  AND ih.is_deleted = FALSE
                ORDER BY ih.scanned_at DESC
                LIMIT 1
            ) latest ON TRUE
            WHERE p.user_id = :userId
              AND p.is_deleted = FALSE
            ORDER BY w.code, p.name
        """,
        nativeQuery = true
    )
    List<Object[]> findUserProductsOnWarehouses(@Param("userId") Long userId);
}
