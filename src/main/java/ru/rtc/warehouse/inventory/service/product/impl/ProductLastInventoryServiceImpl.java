package ru.rtc.warehouse.inventory.service.product.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.inventory.controller.dto.request.ProductLastInventorySearchRequest;
import ru.rtc.warehouse.inventory.service.product.ProductLastInventoryService;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryDTO;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryPageDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLastInventoryServiceImpl
    implements ProductLastInventoryService
{

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public ProductLastInventoryPageDTO getLastInventoryByWarehouse(
        String warehouseCode,
        ProductLastInventorySearchRequest searchRequest,
        Pageable pageable
    ) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        params.add(warehouseCode);

        if (searchRequest != null) {
            if (
                searchRequest.getCategories() != null &&
                !searchRequest.getCategories().isEmpty()
            ) {
                where.append(" AND p.category = ANY(?)");
                params.add(
                    searchRequest.getCategories().toArray(new String[0])
                );
            }
            if (
                searchRequest.getQ() != null && !searchRequest.getQ().isBlank()
            ) {
                where.append(
                    " AND (LOWER(p.sku_code) LIKE LOWER(?) OR LOWER(p.name) LIKE LOWER(?))"
                );
                String q = "%" + searchRequest.getQ() + "%";
                params.add(q);
                params.add(q);
            }
            if (
                searchRequest.getRobots() != null &&
                !searchRequest.getRobots().isEmpty()
            ) {
                where.append(" AND r.robot_code = ANY(?)");
                params.add(searchRequest.getRobots().toArray(new String[0]));
            }
        }

        // Статус теперь вычисляется динамически, фильтруем через HAVING
        StringBuilder statusFilter = new StringBuilder();
        if (
            searchRequest != null &&
            searchRequest.getStatuses() != null &&
            !searchRequest.getStatuses().isEmpty()
        ) {
            List<String> statusNames = searchRequest
                .getStatuses()
                .stream()
                .map(Enum::name)
                .toList();
            // Строим фильтр через HAVING на вычисляемом поле statusCode
            // HAVING будет добавлен после подзапроса
            List<String> placeholders = new ArrayList<>();
            for (String s : statusNames) {
                placeholders.add("?");
                params.add(s);
            }
            statusFilter
                .append(" AND sub.statusCode IN (")
                .append(String.join(",", placeholders))
                .append(")");
        }

        String orderClause = buildOrderClause(pageable.getSort());

        // Внутренний подзапрос: последнее сканирование + вычисляемый статус
        String innerSql = """
            SELECT DISTINCT ON (ih.product_id)
                p.sku_code AS productCode,
                p.name AS productName,
                p.category AS category,
                p.image_url AS imageUrl,
                ih.expected_quantity AS expectedQuantity,
                ih.quantity AS actualQuantity,
                ih.difference AS difference,
                ih.scanned_at AS lastScannedAt,
                CASE
                    WHEN ih.quantity <= pw.min_stock THEN 'CRITICAL'
                    WHEN ih.quantity <= pw.optimal_stock THEN 'LOW_STOCK'
                    ELSE 'OK'
                END AS statusCode,
                r.robot_code AS robotCode,
                ih.zone AS zone,
                ih.row AS row,
                ih.shelf AS shelf
            FROM inventory_history ih
            JOIN products p ON p.id = ih.product_id AND p.is_deleted = false
            JOIN warehouses w ON w.id = ih.warehouse_id AND w.is_deleted = false
            LEFT JOIN product_warehouse pw
                ON pw.product_id = ih.product_id
                AND pw.warehouse_id = ih.warehouse_id
                AND pw.is_deleted = false
            LEFT JOIN robots r ON r.id = ih.robot_id AND r.is_deleted = false
            WHERE w.code = ?
              AND ih.is_deleted = false
              %s
            ORDER BY ih.product_id, ih.scanned_at DESC NULLS LAST
            """.formatted(where);

        // count: оборачиваем в ещё один подзапрос
        String countSql = """
            SELECT COUNT(*)
            FROM (
                %s
            ) sub
            WHERE 1=1 %s
            """.formatted(innerSql, statusFilter);

        Long total = jdbcTemplate.queryForObject(
            countSql,
            Long.class,
            params.toArray()
        );

        // data: оборачиваем + применяем фильтр по статусу + сортировку + пагинацию
        String dataSql = """
            SELECT *
            FROM (
                %s
            ) sub
            WHERE 1=1 %s
            ORDER BY %s
            LIMIT ? OFFSET ?
            """.formatted(innerSql, statusFilter, orderClause);

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageable.getPageSize());
        dataParams.add(pageable.getOffset());

        List<ProductLastInventoryDTO> items = jdbcTemplate.query(
            dataSql,
            this::mapRow,
            dataParams.toArray()
        );

        return ProductLastInventoryPageDTO.builder()
            .total(total != null ? total : 0)
            .page(pageable.getPageNumber())
            .size(pageable.getPageSize())
            .items(items)
            .build();
    }

    private String buildOrderClause(Sort sort) {
        if (sort.isUnsorted()) {
            return "lastScannedAt DESC NULLS LAST";
        }
        StringBuilder sb = new StringBuilder();
        for (Sort.Order order : sort) {
            if (!sb.isEmpty()) sb.append(", ");
            String col = switch (order.getProperty()) {
                case "productCode" -> "productCode";
                case "productName" -> "productName";
                case "actualQuantity" -> "actualQuantity";
                case "lastScannedAt" -> "lastScannedAt";
                case "statusCode" -> "statusCode";
                case "robotCode" -> "robotCode";
                default -> "lastScannedAt";
            };
            sb.append(col).append(" ").append(order.getDirection().name());
        }
        sb.append(" NULLS LAST");
        return sb.toString();
    }

    private ProductLastInventoryDTO mapRow(ResultSet rs, int rowNum)
        throws SQLException {
        ProductLastInventoryDTO dto = new ProductLastInventoryDTO();
        dto.setProductCode(rs.getString("productCode"));
        dto.setProductName(rs.getString("productName"));
        dto.setCategory(rs.getString("category"));
        dto.setImageUrl(rs.getString("imageUrl"));
        int expected = rs.getInt("expectedQuantity");
        dto.setExpectedQuantity(rs.wasNull() ? null : expected);
        int actual = rs.getInt("actualQuantity");
        dto.setActualQuantity(rs.wasNull() ? null : actual);
        int diff = rs.getInt("difference");
        dto.setDifference(rs.wasNull() ? null : diff);
        java.sql.Timestamp ts = rs.getTimestamp("lastScannedAt");
        dto.setLastScannedAt(ts != null ? ts.toLocalDateTime() : null);
        dto.setStatusCode(rs.getString("statusCode"));
        dto.setRobotCode(rs.getString("robotCode"));
        dto.setZone(rs.getInt("zone"));
        dto.setRow(rs.getInt("row"));
        dto.setShelf(rs.getInt("shelf"));
        return dto;
    }
}
