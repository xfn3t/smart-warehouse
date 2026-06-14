package ru.rtc.warehouse.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportFileDTO {
    private byte[] bytes;
    private String contentType;
    private String reportType;

    public boolean isExcel() {
        return reportType != null && reportType.contains("EXCEL");
    }
}
