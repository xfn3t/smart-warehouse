package ru.rtc.warehouse.reports.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
public class RobotActivityRowMapper implements RowMapper<RobotActivityReportDTO> {

    @Override
    public RobotActivityReportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        RobotActivityReportDTO dto = new RobotActivityReportDTO();
        dto.setRobotId(rs.getLong("robot_id"));
        dto.setRobotCode(rs.getString("robot_code"));
        dto.setWarehouseCode(rs.getString("warehouse_code"));
        dto.setWarehouseName(rs.getString("warehouse_name"));
        dto.setRobotStatus(rs.getString("robot_status"));
        dto.setBatteryLevel(rs.getInt("battery_level"));
        Timestamp lastUpdate = rs.getTimestamp("last_update");
        if (lastUpdate != null) {
            dto.setLastUpdate(lastUpdate.toLocalDateTime());
        }
        dto.setTotalScans(rs.getLong("total_scans"));
        Timestamp lastScan = rs.getTimestamp("last_scan_at");
        if (lastScan != null) {
            dto.setLastScanAt(lastScan.toLocalDateTime());
        }
        dto.setOkScans(rs.getLong("ok_scans"));
        dto.setLowStockScans(rs.getLong("low_stock_scans"));
        dto.setCriticalScans(rs.getLong("critical_scans"));
        dto.setTotalDifference(rs.getLong("total_difference"));
        return dto;
    }
}
