package ru.rtc.warehouse.inventory.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.InventoryHistorySummaryService;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;
import ru.rtc.warehouse.inventory.spec.InventoryHistorySpecifications;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryHistorySummaryServiceImpl implements InventoryHistorySummaryService {

    private final EntityManager em;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public HistorySummaryDTO summarize(InventoryHistorySearchRequest rq) {

        // базовый предикат от наших спецификаций
        Specification<InventoryHistory> spec = InventoryHistorySpecifications.build(rq);

        // total
        long total = count(spec);

        // unique products
        long uniqueProducts = countDistinctProduct(spec);

        // discrepancies: status != OK в рамках того же набора данных
        long discrepancies = countWithStatusNotOk(spec);

        // среднее время (мин) = avg(created_at - scanned_at) — Postgres-специфика
        Double avgMinutes = avgMinutesNative(rq);

        return HistorySummaryDTO.builder()
                .total(total)
                .uniqueProducts(uniqueProducts)
                .discrepancies(discrepancies)
                .avgZoneScanMinutes(avgMinutes)
                .build();
    }

    private long count(Specification<InventoryHistory> spec) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(InventoryHistory.class);
        var p = spec.toPredicate(root, cq, cb);
        cq.select(cb.count(root));
        if (p != null) cq.where(p);
        return em.createQuery(cq).getSingleResult();
    }

    private long countDistinctProduct(Specification<InventoryHistory> spec) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(InventoryHistory.class);
        var p = spec.toPredicate(root, cq, cb);
        cq.select(cb.countDistinct(root.get("product")));
        if (p != null) cq.where(p);
        return em.createQuery(cq).getSingleResult();
    }

    private long countWithStatusNotOk(Specification<InventoryHistory> spec) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(InventoryHistory.class);
        var p = spec.toPredicate(root, cq, cb);
        var notOk = cb.notEqual(root.get("status"), InventoryHistoryStatus.OK);
        cq.select(cb.count(root)).where(p == null ? notOk : cb.and(p, notOk));
        return em.createQuery(cq).getSingleResult();
    }

    /**
     * Среднее по (created_at - scanned_at) в минутах.
     * Реализовано нативно под Postgres: avg(extract(epoch from (created_at - scanned_at))/60.0)
     * Фильтры те же, что и для поиска.
     */
    private Double avgMinutesNative(InventoryHistorySearchRequest rq) {
        StringBuilder sql = new StringBuilder(
                "select avg(extract(epoch from (ih.created_at - ih.scanned_at))/60.0) " +
                        "from inventory_history ih where 1=1 ");

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

        // статусы (СПИСОК)
        if (rq.getStatuses() != null && !rq.getStatuses().isEmpty()) {
            sql.append(" and ih.status in (:st)");
            // передаём список строк, а не enum
            params.put("st", rq.getStatuses().stream().map(Enum::name).toList());
        }

        // категории
        if (rq.getCategories() != null && !rq.getCategories().isEmpty()) {
            sql.append(" and ih.product_id in (select p.id from products p where p.category in (:cats))");
            params.put("cats", rq.getCategories());
        }

        // q: product.sku_code OR product.name OR robot.robot_code
        if (rq.getQ() != null && !rq.getQ().isBlank()) {
            sql.append(" and (exists (select 1 from products p " +
                    "where p.id = ih.product_id " +
                    "and (lower(p.sku_code) like :q or lower(p.name) like :q)) " +   // <-- sku_code
                    "or exists (select 1 from robots r " +
                    "where r.id = ih.robot_id and lower(r.robot_code) like :q))");
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
        return v == null ? null : ((Number) v).doubleValue();
    }
}
