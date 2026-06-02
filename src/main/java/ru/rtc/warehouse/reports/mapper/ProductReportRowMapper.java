package ru.rtc.warehouse.reports.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.ProductReportDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
public class ProductReportRowMapper implements RowMapper<ProductReportDTO> {

    @Override
    public ProductReportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProductReportDTO dto = new ProductReportDTO();
        dto.setProductId(rs.getLong("product_id"));
        dto.setSkuCode(rs.getString("sku_code"));
        dto.setProductName(rs.getString("product_name"));
        dto.setCategory(rs.getString("category"));
        dto.setWarehouseCode(rs.getString("warehouse_code"));
        dto.setWarehouseName(rs.getString("warehouse_name"));
        dto.setMinStock(rs.getInt("min_stock"));
        dto.setOptimalStock(rs.getInt("optimal_stock"));
        int currentQty = rs.getInt("current_quantity");
        dto.setCurrentQuantity(rs.wasNull() ? null : currentQty);
        int expectedQty = rs.getInt("expected_quantity");
        dto.setExpectedQuantity(rs.wasNull() ? null : expectedQty);
        int diff = rs.getInt("difference");
        dto.setDifference(rs.wasNull() ? null : diff);
        dto.setInventoryStatus(rs.getString("inventory_status"));
        Timestamp lastScan = rs.getTimestamp("last_scanned_at");
        if (lastScan != null) {
            dto.setLastScannedAt(lastScan.toLocalDateTime());
        }
        return dto;
    }
}
