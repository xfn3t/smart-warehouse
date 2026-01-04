package ru.rtc.warehouse.inventory.service.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryHistoryQueryHelper {

	private final EntityManager em;

	public long count(Specification<InventoryHistory> spec) {
		return executeCountQuery(spec, false);
	}

	public long countDistinctProducts(Specification<InventoryHistory> spec) {
		return executeCountQuery(spec, true);
	}

	public long countWithStatusNotOk(Specification<InventoryHistory> spec) {
		Specification<InventoryHistory> notOkSpec = spec.and(notOkStatus());
		return executeCountQuery(notOkSpec, false);
	}

	public Double calculateAvgMinutesSafe(Specification<InventoryHistory> spec) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<InventoryHistory> cq = cb.createQuery(InventoryHistory.class);
		Root<InventoryHistory> root = cq.from(InventoryHistory.class);

		Predicate predicate = spec.toPredicate(root, cq, cb);
		if (predicate != null) {
			cq.where(predicate);
		}

		cq.select(root);

		try {
			List<InventoryHistory> results = em.createQuery(cq).getResultList();

			if (results.isEmpty()) {
				return null;
			}

			// Вычисляем среднюю разницу в минутах в Java
			double totalMinutes = results.stream()
					.mapToLong(history -> {
						if (history.getScannedAt() == null || history.getCreatedAt() == null) {
							return 0L;
						}
						java.time.Duration duration = java.time.Duration.between(
								history.getScannedAt(),
								history.getCreatedAt()
						);
						return duration.toMinutes();
					})
					.sum();

			return totalMinutes / results.size();
		} catch (Exception e) {
			return null;
		}
	}

	public List<InventoryHistory> findAll(Specification<InventoryHistory> spec) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<InventoryHistory> cq = cb.createQuery(InventoryHistory.class);
		Root<InventoryHistory> root = cq.from(InventoryHistory.class);

		root.fetch("product", JoinType.LEFT);
		root.fetch("robot", JoinType.LEFT);
		root.fetch("location", JoinType.LEFT);
		root.fetch("status", JoinType.LEFT);
		root.fetch("warehouse", JoinType.LEFT);

		Predicate predicate = spec.toPredicate(root, cq, cb);
		if (predicate != null) {
			cq.where(predicate);
		}

		return em.createQuery(cq).getResultList();
	}

	public <T> TypedQuery<T> createQuery(Specification<InventoryHistory> spec, Class<T> resultClass) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(resultClass);
		Root<InventoryHistory> root = cq.from(InventoryHistory.class);

		Predicate predicate = spec.toPredicate(root, cq, cb);
		if (predicate != null) {
			cq.where(predicate);
		}

		return em.createQuery(cq);
	}

	private long executeCountQuery(Specification<InventoryHistory> spec, boolean distinctProduct) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<InventoryHistory> root = cq.from(InventoryHistory.class);

		if (distinctProduct) {
			cq.select(cb.countDistinct(root.get("product")));
		} else {
			cq.select(cb.count(root));
		}

		Predicate predicate = spec.toPredicate(root, cq, cb);
		if (predicate != null) {
			cq.where(predicate);
		}

		Long result = em.createQuery(cq).getSingleResult();
		return result == null ? 0L : result;
	}

	private static Specification<InventoryHistory> notOkStatus() {
		return (root, query, cb) -> {
			Join<Object, Object> statusJoin = root.join("status", JoinType.INNER);
			return cb.notEqual(
					statusJoin.get("code"),
					InventoryHistoryStatus.InventoryHistoryStatusCode.OK
			);
		};
	}
}