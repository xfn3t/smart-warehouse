package ru.rtc.warehouse.reports.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.DailyAggregationDTO;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class DailyAggregationRowMapper implements RowMapper<DailyAggregationDTO> {

    @Override
    public DailyAggregationDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        DailyAggregationDTO dto = new DailyAggregationDTO();
        dto.setWarehouseId(rs.getLong("warehouse_id"));
        dto.setWarehouseCode(rs.getString("warehouse_code"));
        dto.setScanDate(rs.getDate("scan_date").toLocalDate());
        dto.setTotalScans(rs.getLong("total_scans"));
        dto.setTotalQuantity(rs.getLong("total_quantity"));
        dto.setTotalDifference(rs.getLong("total_difference"));
        dto.setTotalAbsDifference(rs.getLong("total_abs_difference"));
        dto.setUniqueProductsScanned(rs.getLong("unique_products_scanned"));
        dto.setUniqueRobots(rs.getLong("unique_robots"));
        return dto;
    }
}
