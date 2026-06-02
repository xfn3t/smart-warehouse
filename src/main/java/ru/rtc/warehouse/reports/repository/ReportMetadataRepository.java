package ru.rtc.warehouse.reports.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.reports.model.ReportMetadata;

@Repository
public interface ReportMetadataRepository
    extends JpaRepository<ReportMetadata, Long>
{
    Optional<ReportMetadata> findByReportUidAndIsDeletedFalse(UUID reportUid);

    @Query(
        """
        SELECT rm FROM ReportMetadata rm
        JOIN FETCH rm.warehouse w
        WHERE rm.user.id = :userId
          AND rm.isDeleted = false
        ORDER BY rm.createdAt DESC
        """
    )
    List<ReportMetadata> findAllByUserId(@Param("userId") Long userId);

    @Query(
        """
        SELECT rm FROM ReportMetadata rm
        JOIN FETCH rm.warehouse w
        WHERE rm.user.id = :userId
          AND w.code = :warehouseCode
          AND rm.isDeleted = false
        ORDER BY rm.createdAt DESC
        """
    )
    List<ReportMetadata> findAllByUserIdAndWarehouseCode(
        @Param("userId") Long userId,
        @Param("warehouseCode") String warehouseCode
    );
}
