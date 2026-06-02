package ru.rtc.warehouse.reports.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.DiscrepancySummaryDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
public class DiscrepancySummaryRowMapper implements RowMapper<DiscrepancySummaryDTO> {

    @Override
    public DiscrepancySummaryDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        DiscrepancySummaryDTO dto = new DiscrepancySummaryDTO();
        dto.setWarehouseCode(rs.getString("warehouse_code"));
        dto.setWarehouseName(rs.getString("warehouse_name"));
        dto.setSkuCode(rs.getString("sku_code"));
        dto.setProductName(rs.getString("product_name"));
        dto.setScanCount(rs.getLong("scan_count"));
        dto.setTotalAbsDiscrepancy(rs.getLong("total_abs_discrepancy"));
        dto.setAvgDiscrepancy(rs.getBigDecimal("avg_discrepancy"));
        dto.setMaxDiscrepancy(rs.getLong("max_discrepancy"));
        Timestamp lastDisc = rs.getTimestamp("last_discrepancy_at");
        if (lastDisc != null) {
            dto.setLastDiscrepancyAt(lastDisc.toLocalDateTime());
        }
        return dto;
    }
}
