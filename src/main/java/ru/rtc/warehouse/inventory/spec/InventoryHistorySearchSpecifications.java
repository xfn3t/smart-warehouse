package ru.rtc.warehouse.inventory.spec;

import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

public class InventoryHistorySearchSpecifications {

    public static Specification<InventoryHistory> build(
        Long warehouseId,
        InventoryHistorySearchRequest rq
    ) {
        return notDeleted()
            .and(byWarehouse(warehouseId))
            .and(byDateRange(rq.getFrom(), rq.getTo()))
            .and(byZones(rq.getZones()))
            .and(byStatuses(rq.getStatuses()))
            .and(byCategories(rq.getCategories()))
            .and(bySearchQuery(rq.getQ()))
            .and(byRobots(rq.getRobots()))
            .and(productNotDeleted())
            .and(withActiveProductWarehouse());
    }

    private static Specification<InventoryHistory> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    private static Specification<InventoryHistory> byWarehouse(
        Long warehouseId
    ) {
        return (root, query, cb) -> {
            if (warehouseId == null) return null;
            return cb.equal(root.get("warehouse").get("id"), warehouseId);
        };
    }

    private static Specification<InventoryHistory> byDateRange(
        Instant from,
        Instant to
    ) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;

            Path<LocalDateTime> scannedAt = root.get("scannedAt");
            List<Predicate> predicates = new ArrayList<>();

            if (from != null) {
                LocalDateTime fromDate = from
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                predicates.add(cb.greaterThanOrEqualTo(scannedAt, fromDate));
            }
            if (to != null) {
                LocalDateTime toDate = to
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                predicates.add(cb.lessThan(scannedAt, toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<InventoryHistory> byZones(
        List<Integer> zones
    ) {
        return (root, query, cb) -> {
            if (zones == null || zones.isEmpty()) return null;
            return root.get("zone").in(zones);
        };
    }

    private static Specification<InventoryHistory> byStatuses(
        List<InventoryHistoryStatus.InventoryHistoryStatusCode> statuses
    ) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return null;
            Join<Object, Object> statusJoin = root.join(
                "status",
                JoinType.INNER
            );
            return statusJoin.get("code").in(statuses);
        };
    }

    private static Specification<InventoryHistory> byCategories(
        List<String> categories
    ) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) return null;
            Join<Object, Object> productJoin = root.join(
                "product",
                JoinType.INNER
            );
            return productJoin.get("category").in(categories);
        };
    }

    private static Specification<InventoryHistory> byRobots(
        List<String> robots
    ) {
        return (root, query, cb) -> {
            if (robots == null || robots.isEmpty()) return null;
            Join<Object, Object> robotJoin = root.join("robot", JoinType.LEFT);
            return robotJoin.get("code").in(robots);
        };
    }

    private static Specification<InventoryHistory> bySearchQuery(
        String searchQuery
    ) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(searchQuery)) return null;

            String likePattern = "%" + searchQuery.toLowerCase() + "%";

            Join<Object, Object> productJoin = root.join(
                "product",
                JoinType.INNER
            );
            Join<Object, Object> robotJoin = root.join("robot", JoinType.LEFT);

            return cb.or(
                cb.like(cb.lower(productJoin.get("skuCode")), likePattern),
                cb.like(cb.lower(productJoin.get("name")), likePattern),
                cb.like(cb.lower(robotJoin.get("code")), likePattern)
            );
        };
    }

    /**
     * Исключает записи, где сам продукт помечен как удалённый.
     */
    private static Specification<InventoryHistory> productNotDeleted() {
        return (root, query, cb) ->
            cb.isFalse(root.get("product").get("isDeleted"));
    }

    /**
     * Фильтрует только те записи инвентаризации, у которых связь
     * product_warehouse активна (не удалена со склада).
     */
    private static Specification<
        InventoryHistory
    > withActiveProductWarehouse() {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<?> pwRoot = subquery.from(
                ru.rtc.warehouse.product.model.ProductWarehouse.class
            );

            subquery.select(cb.literal(1L));
            subquery.where(
                cb.equal(
                    pwRoot.get("product").get("id"),
                    root.get("product").get("id")
                ),
                cb.equal(
                    pwRoot.get("warehouse").get("id"),
                    root.get("warehouse").get("id")
                ),
                cb.isFalse(pwRoot.get("isDeleted"))
            );

            return cb.exists(subquery);
        };
    }
}
