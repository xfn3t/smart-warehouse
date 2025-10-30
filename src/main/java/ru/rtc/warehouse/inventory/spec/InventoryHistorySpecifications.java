package ru.rtc.warehouse.inventory.spec;


import jakarta.persistence.criteria.JoinType;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Строитель динамических JPA-спецификаций для фильтров истории.
 */
@NoArgsConstructor
public final class InventoryHistorySpecifications {

    /**
     * Собирает единую спецификацию на основе всех заданных фильтров.
     */
    public static Specification<InventoryHistory> build(InventoryHistorySearchRequest rq) {
        Specification<InventoryHistory> spec = Specification.allOf();
        if (rq == null) return spec;

        // Период по scannedAt (если в сущности LocalDateTime — конвертируй из Instant)
        if (rq.getFrom() != null) {
            var from = LocalDateTime.ofInstant(rq.getFrom(), ZoneOffset.UTC);
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("scannedAt"), from));
        }
        if (rq.getTo() != null) {
            var to = LocalDateTime.ofInstant(rq.getTo(), ZoneOffset.UTC);
            spec = spec.and((root, cq, cb) -> cb.lessThan(root.get("scannedAt"), to));
        }

        if (!CollectionUtils.isEmpty(rq.getZones())) {
            spec = spec.and((root, cq, cb) -> root.get("zone").in(rq.getZones()));
        }

        if (!CollectionUtils.isEmpty(rq.getStatuses())) {
            spec = spec.and((root, q, cb) -> {
                var st = root.join("status", JoinType.LEFT); // сущность InventoryHistoryStatus
                return st.get("code").in(rq.getStatuses());  // code — enum
            });
        }

        // Категории (только product)
        if (!CollectionUtils.isEmpty(rq.getCategories())) {
            spec = spec.and((root, cq, cb) -> {
                var product = root.join("product", JoinType.LEFT);
                return product.get("category").in(rq.getCategories());
            });
        }
        // Строковый поиск q: Product.code OR Product.name OR Robot.code (case-insensitive)
        if (StringUtils.hasText(rq.getQ())) {
            spec = spec.and((root, cq, cb) -> {
                var product = root.join("product", JoinType.LEFT);
                var robot = root.join("robot", JoinType.LEFT);
                String like = "%" + rq.getQ().toLowerCase() + "%";
                cq.distinct(true); // важно: избегаем дублей из-за join'ов
                return cb.or(
                        cb.like(cb.lower(product.get("code")), like),
                        cb.like(cb.lower(product.get("name")), like),
                        cb.like(cb.lower(robot.get("code")), like)
                );
            });
        }

        // Фильтр по роботам — по robotCode (а не просто code)
        if (!CollectionUtils.isEmpty(rq.getRobots())) {
            spec = spec.and((root, cq, cb) ->
                    root.join("robot", JoinType.LEFT)
                            .get("code").in(rq.getRobots()));
        }

        // На всякий случай обеспечим distinct даже если q не задан
        spec = spec.and((root, cq, cb) -> { cq.distinct(true); return cb.conjunction(); });

        return spec;
    }
}