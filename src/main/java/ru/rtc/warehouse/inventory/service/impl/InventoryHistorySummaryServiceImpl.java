package ru.rtc.warehouse.inventory.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.InventoryHistorySummaryService;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;
import ru.rtc.warehouse.inventory.spec.InventoryHistorySpecifications;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сводная статистика по истории инвентаризаций.
 * <ul>
 *   <li>total — количество записей</li>
 *   <li>uniqueProducts — число уникальных SKU</li>
 *   <li>discrepancies — количество записей со статусом != OK</li>
 *   <li>avgZoneScanMinutes — среднее (created_at - scanned_at) в минутах (PostgreSQL)</li>
 * </ul>
 * Для avg используется нативный SQL из-за EXTRACT(EPOCH ...).
 */
@Service
@RequiredArgsConstructor
public class InventoryHistorySummaryServiceImpl implements InventoryHistorySummaryService {

    private final EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public HistorySummaryDTO summarize(InventoryHistorySearchRequest rq) {
        // null-safe: пустые фильтры допустимы
        InventoryHistorySearchRequest request = (rq == null) ? new InventoryHistorySearchRequest() : rq;

        // Единая спецификация для count'ов
        Specification<InventoryHistory> spec = InventoryHistorySpecifications.build(request);

        long total = safeCount(spec);
        long uniqueProducts = safeCountDistinctProduct(spec);
        long discrepancies = safeCountWithStatusNotOk(spec);
        Double avgMinutes = avgMinutesNative(request);

        return HistorySummaryDTO.builder()
                .total(total)
                .uniqueProducts(uniqueProducts)
                .discrepancies(discrepancies)
                .avgZoneScanMinutes(avgMinutes)
                .build();
    }

    /* =========================
       Criteria-based counters
       ========================= */

    private long safeCount(Specification<InventoryHistory> spec) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(InventoryHistory.class);
        var p = (spec == null) ? null : spec.toPredicate(root, cq, cb);
        cq.select(cb.count(root));
        if (p != null) cq.where(p);
        Long v = em.createQuery(cq).getSingleResult();
        return v == null ? 0L : v;
    }

    private long safeCountDistinctProduct(Specification<InventoryHistory> spec) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(InventoryHistory.class);
        var p = (spec == null) ? null : spec.toPredicate(root, cq, cb);
        cq.select(cb.countDistinct(root.get("product")));
        if (p != null) cq.where(p);
        Long v = em.createQuery(cq).getSingleResult();
        return v == null ? 0L : v;
    }

    private long safeCountWithStatusNotOk(Specification<InventoryHistory> spec) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(InventoryHistory.class);

        // базовые фильтры из Specifications
        var base = (spec == null) ? null : spec.toPredicate(root, cq, cb);

        // join к статусу и сравнение по коду
        var st = root.join("status", jakarta.persistence.criteria.JoinType.LEFT);
        var notOk = cb.notEqual(
                st.get("code"),
                ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode.OK.name()
        );

        cq.select(cb.count(root));
        cq.where(base == null ? notOk : cb.and(base, notOk));

        Long v = em.createQuery(cq).getSingleResult();
        return v == null ? 0L : v;
    }


    /* =========================
       Native AVG minutes (PostgreSQL)
       ========================= */

    /**
     * Среднее по (created_at - scanned_at) в минутах.
     * Реализовано нативно под Postgres:
     *   avg(extract(epoch from (ih.created_at - ih.scanned_at))/60.0)
     * Фильтры синхронизированы с REST: from/to, zones, statuses, categories, q, robots.
     */
    private Double avgMinutesNative(InventoryHistorySearchRequest rq) {
        StringBuilder sql = new StringBuilder(
                "select avg(extract(epoch from (ih.created_at - ih.scanned_at))/60.0) " +
                        "from inventory_history ih where 1=1");

        Map<String, Object> params = new HashMap<>();

        // период
        if (rq.getFrom() != null) {
            sql.append(" and ih.scanned_at >= :from");
            params.put("from", Timestamp.from(rq.getFrom()));
        }
        if (rq.getTo() != null) {
            sql.append(" and ih.scanned_at < :to");
            params.put("to", Timestamp.from(rq.getTo()));
        }

        // зоны
        if (rq.getZones() != null && !rq.getZones().isEmpty()) {
            sql.append(" and ih.zone in (:zones)");
            params.put("zones", rq.getZones());
        }

        if (rq.getStatuses() != null && !rq.getStatuses().isEmpty()) {
            List<String> statusNames = rq.getStatuses().stream()
                    .map(ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode::name)
                    .toList();

            sql.append(" and exists (select 1 from inventory_status s " +
                    "            where s.id = ih.status_id and s.code in (:st))");
            params.put("st", statusNames);
        }

        // категории
        if (rq.getCategories() != null && !rq.getCategories().isEmpty()) {
            sql.append(" and ih.product_id in (select p.id from products p where p.category in (:cats))");
            params.put("cats", rq.getCategories());
        }

        // q: p.sku_code OR p.name OR r.robot_code
        if (rq.getQ() != null && !rq.getQ().isBlank()) {
            sql.append(" and (exists (select 1 from products p " +
                    "              where p.id = ih.product_id " +
                    "                and (lower(p.sku_code) like :q or lower(p.name) like :q)) " +
                    "       or exists (select 1 from robots r " +
                    "              where r.id = ih.robot_id and lower(r.robot_code) like :q))");
            params.put("q", "%" + rq.getQ().toLowerCase() + "%");
        }

        // robots
        if (rq.getRobots() != null && !rq.getRobots().isEmpty()) {
            sql.append(" and ih.robot_id in (select r.id from robots r where r.robot_code in (:rb))");
            params.put("rb", rq.getRobots());
        }

        Query q = em.createNativeQuery(sql.toString());
        params.forEach(q::setParameter);
        Object v = q.getSingleResult();
        return (v == null) ? null : ((Number) v).doubleValue();
    }
}
