package ru.rtc.warehouse.reports.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReportResponseDTO {
    private UUID reportUid;
    private String warehouseCode;
    private String warehouseName;
    private String reportType;
    private List<String> skuCodes;
    private LocalDateTime createdAt;
    private String downloadUrl;
    private String pdfBase64;
}
