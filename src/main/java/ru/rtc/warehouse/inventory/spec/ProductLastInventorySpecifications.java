package ru.rtc.warehouse.inventory.spec;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.List;

public class ProductLastInventorySpecifications {

	public static Specification<InventoryHistory> buildProductLastInventorySpec(
			String warehouseCode,
			List<String> categories,
			List<String> statusCodes,
			String searchQuery,
			List<String> robots) {

		return notDeleted()
				.and(productNotDeleted())
				.and(byWarehouseCode(warehouseCode))
				.and(byCategories(categories))
				.and(byStatusCodes(statusCodes))
				.and(bySearchQuery(searchQuery))
				.and(byRobots(robots))
				.and(isLatestForProduct(warehouseCode));
	}

	public static Specification<InventoryHistory> notDeleted() {
		return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
	}

	public static Specification<InventoryHistory> productNotDeleted() {
		return (root, query, cb) -> {
			Join<Object, Object> productJoin = root.join("product", JoinType.INNER);
			return cb.isFalse(productJoin.get("isDeleted"));
		};
	}

	public static Specification<InventoryHistory> byWarehouseCode(String warehouseCode) {
		return (root, query, cb) -> {
			if (!StringUtils.hasText(warehouseCode)) {
				return cb.conjunction();
			}
			return cb.equal(root.get("warehouse").get("code"), warehouseCode);
		};
	}

	public static Specification<InventoryHistory> byCategories(List<String> categories) {
		return (root, query, cb) -> {
			if (CollectionUtils.isEmpty(categories)) {
				return cb.conjunction();
			}
			Join<Object, Object> productJoin = root.join("product", JoinType.INNER);
			return productJoin.get("category").in(categories);
		};
	}

	public static Specification<InventoryHistory> byStatusCodes(List<String> statusCodes) {
		return (root, query, cb) -> {
			if (CollectionUtils.isEmpty(statusCodes)) {
				return cb.conjunction();
			}
			Join<Object, Object> statusJoin = root.join("status", JoinType.INNER);
			return statusJoin.get("code").as(String.class).in(statusCodes);
		};
	}

	public static Specification<InventoryHistory> bySearchQuery(String searchQuery) {
		return (root, query, cb) -> {
			if (!StringUtils.hasText(searchQuery)) {
				return cb.conjunction();
			}

			String likePattern = "%" + searchQuery.toLowerCase() + "%";

			Join<Object, Object> productJoin = root.join("product", JoinType.INNER);
			Join<Object, Object> robotJoin = root.join("robot", JoinType.LEFT);

			Predicate skuPredicate = cb.like(cb.lower(productJoin.get("skuCode")), likePattern);
			Predicate namePredicate = cb.like(cb.lower(productJoin.get("name")), likePattern);
			Predicate robotPredicate = cb.like(cb.lower(robotJoin.get("code")), likePattern);

			return cb.or(skuPredicate, namePredicate, robotPredicate);
		};
	}

	public static Specification<InventoryHistory> byRobots(List<String> robots) {
		return (root, query, cb) -> {
			if (CollectionUtils.isEmpty(robots)) {
				return cb.conjunction();
			}
			Join<Object, Object> robotJoin = root.join("robot", JoinType.LEFT);
			return robotJoin.get("code").in(robots);
		};
	}

	public static Specification<InventoryHistory> isLatestForProduct(String warehouseCode) {
		return (root, query, cb) -> {
			// Подзапрос для получения ID последних записей по каждому продукту
			Subquery<Long> subquery = query.subquery(Long.class);
			Root<InventoryHistory> subRoot = subquery.from(InventoryHistory.class);

			subquery.select(cb.max(subRoot.get("id")));
			subquery.where(
					cb.equal(subRoot.get("product").get("id"), root.get("product").get("id")),
					cb.equal(subRoot.get("warehouse").get("code"), warehouseCode),
					cb.isFalse(subRoot.get("isDeleted"))
			);

			return cb.equal(root.get("id"), subquery);
		};
	}

	// Альтернативный вариант - по максимальному scannedAt (если предпочитаешь этот подход)
	public static Specification<InventoryHistory> isLatestForProductByScannedAt(String warehouseCode) {
		return (root, query, cb) -> {
			Subquery<LocalDateTime> subquery = query.subquery(LocalDateTime.class);
			Root<InventoryHistory> subRoot = subquery.from(InventoryHistory.class);

			// Явно указываем тип для scannedAt
			Path<LocalDateTime> scannedAtPath = subRoot.get("scannedAt");
//			subquery.select(cb.max(scannedAtPath));
			subquery.where(
					cb.equal(subRoot.get("product").get("id"), root.get("product").get("id")),
					cb.equal(subRoot.get("warehouse").get("code"), warehouseCode),
					cb.isFalse(subRoot.get("isDeleted"))
			);

			return cb.equal(root.get("scannedAt"), subquery);
		};
	}
}