package ru.rtc.warehouse.inventory.repository;


import io.micrometer.common.lang.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;


/**
 * Репозиторий истории инвентаризаций с поддержкой Specifications.
 */
@Repository
public interface InventoryHistoryRepository extends
        JpaRepository<InventoryHistory, Long>, JpaSpecificationExecutor<InventoryHistory> {

    @EntityGraph(attributePaths = {"product", "robot"})
    Page<InventoryHistory> findAll(@Nullable Specification<InventoryHistory> spec, Pageable pageable);
}
