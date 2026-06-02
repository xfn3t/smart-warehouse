package ru.rtc.warehouse.reports.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "report_metadata")
@Getter
@Setter
@NoArgsConstructor
public class ReportMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_uid", nullable = false, unique = true)
    private UUID reportUid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sku_codes", columnDefinition = "jsonb")
    private List<String> skuCodes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;
}
