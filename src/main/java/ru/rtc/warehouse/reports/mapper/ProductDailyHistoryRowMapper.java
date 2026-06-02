package ru.rtc.warehouse.reports.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.ProductDailyHistoryDTO;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ProductDailyHistoryRowMapper implements RowMapper<ProductDailyHistoryDTO> {

    @Override
    public ProductDailyHistoryDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProductDailyHistoryDTO dto = new ProductDailyHistoryDTO();
        dto.setSkuCode(rs.getString("sku_code"));
        dto.setProductName(rs.getString("product_name"));
        dto.setScanDate(rs.getDate("scan_date").toLocalDate());
        dto.setDailyQuantity(rs.getLong("daily_quantity"));
        dto.setDailyDifference(rs.getLong("daily_difference"));
        return dto;
    }
}
