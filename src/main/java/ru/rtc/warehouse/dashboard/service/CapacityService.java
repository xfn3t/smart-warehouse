package ru.rtc.warehouse.dashboard.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CapacityService {

    @PersistenceContext
    private final EntityManager em;

    /**
     * Считает количество занятых ячеек на складе одним запросом.
     * Ячейка занята, если её последнее сканирование имеет quantity > 0.
     */
    public long countOccupiedCells(String warehouseCode) {
        String sql = """
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT ON (zone, row, shelf) quantity
                FROM inventory_history
                WHERE warehouse_id = (
                    SELECT id FROM warehouses WHERE code = :warehouseCode
                )
                  AND is_deleted = FALSE
                ORDER BY zone, row, shelf, scanned_at DESC
            ) sub
            WHERE quantity > 0
            """;

        var query = em.createNativeQuery(sql);
        query.setParameter("warehouseCode", warehouseCode);
        var result = query.getSingleResult();
        if (result instanceof BigInteger bi) {
            return bi.longValue();
        }
        if (result instanceof Long l) {
            return l;
        }
        return ((Number) result).longValue();
    }
}
