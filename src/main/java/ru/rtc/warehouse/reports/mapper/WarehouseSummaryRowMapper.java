package ru.rtc.warehouse.reports.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.WarehouseSummaryReportDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
public class WarehouseSummaryRowMapper implements RowMapper<WarehouseSummaryReportDTO> {

    @Override
    public WarehouseSummaryReportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        WarehouseSummaryReportDTO dto = new WarehouseSummaryReportDTO();
        dto.setWarehouseId(rs.getLong("warehouse_id"));
        dto.setWarehouseCode(rs.getString("warehouse_code"));
        dto.setWarehouseName(rs.getString("warehouse_name"));
        dto.setTotalQuantity(rs.getLong("total_quantity"));
        dto.setUniqueSkuCount(rs.getLong("unique_sku_count"));
        dto.setTotalDiscrepancy(rs.getLong("total_discrepancy"));
        dto.setTotalAbsDiscrepancy(rs.getLong("total_abs_discrepancy"));
        Timestamp lastScan = rs.getTimestamp("last_scan_at");
        if (lastScan != null) {
            dto.setLastScanAt(lastScan.toLocalDateTime());
        }
        dto.setCriticalCount(rs.getLong("critical_count"));
        dto.setLowStockCount(rs.getLong("low_stock_count"));
        dto.setReportCreatedAt(LocalDateTime.now());
        return dto;
    }
}
