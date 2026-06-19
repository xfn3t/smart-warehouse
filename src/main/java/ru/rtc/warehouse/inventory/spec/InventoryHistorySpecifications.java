package ru.rtc.warehouse.inventory.spec;

import jakarta.persistence.criteria.*;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

public class InventoryHistorySpecifications {

    public static Specification<InventoryHistory> buildProductLastInventorySpec(
        String warehouseCode,
        List<String> categories,
        List<InventoryHistoryStatus.InventoryHistoryStatusCode> statuses,
        String searchQuery,
        List<String> robots
    ) {
        return notDeleted()
            .and(byWarehouseCode(warehouseCode))
            .and(byCategories(categories))
            .and(byStatuses(statuses))
            .and(bySearchQuery(searchQuery))
            .and(byRobots(robots))
            .and(productNotDeleted())
            .and(isLatestForProduct());
    }

    public static Specification<InventoryHistory> productNotDeleted() {
        return (root, query, cb) ->
            cb.isFalse(root.get("product").get("isDeleted"));
    }

    public static Specification<InventoryHistory> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<InventoryHistory> byWarehouseCode(
        String warehouseCode
    ) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(warehouseCode)) {
                return cb.conjunction();
            }
            return cb.equal(root.get("warehouse").get("code"), warehouseCode);
        };
    }

    public static Specification<InventoryHistory> byCategories(
        List<String> categories
    ) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(categories)) {
                return cb.conjunction();
            }
            Join<Object, Object> productJoin = root.join(
                "product",
                JoinType.INNER
            );
            return productJoin.get("category").in(categories);
        };
    }

    public static Specification<InventoryHistory> byStatuses(
        List<InventoryHistoryStatus.InventoryHistoryStatusCode> statuses
    ) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(statuses)) {
                return cb.conjunction();
            }
            Join<Object, Object> statusJoin = root.join(
                "status",
                JoinType.INNER
            );
            return statusJoin.get("code").in(statuses);
        };
    }

    public static Specification<InventoryHistory> bySearchQuery(
        String searchQuery
    ) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(searchQuery)) {
                return cb.conjunction();
            }

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

    public static Specification<InventoryHistory> byRobots(
        List<String> robots
    ) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(robots)) {
                return cb.conjunction();
            }
            Join<Object, Object> robotJoin = root.join("robot", JoinType.LEFT);
            return robotJoin.get("code").in(robots);
        };
    }

    public static Specification<InventoryHistory> isLatestForProduct() {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<InventoryHistory> subRoot = subquery.from(
                InventoryHistory.class
            );

            subquery.select(cb.max(subRoot.get("id")));
            subquery.groupBy(subRoot.get("product").get("id"));
            subquery.where(
                cb.equal(
                    subRoot.get("warehouse").get("code"),
                    root.get("warehouse").get("code")
                ),
                cb.isFalse(subRoot.get("isDeleted"))
            );

            return cb.in(root.get("id")).value(subquery);
        };
    }
}
