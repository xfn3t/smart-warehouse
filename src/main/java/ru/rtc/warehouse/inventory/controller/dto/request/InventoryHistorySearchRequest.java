package ru.rtc.warehouse.inventory.controller.dto.request;


import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.common.QuickRange;


import java.time.Instant;
import java.util.List;

@Data
public class InventoryHistorySearchRequest {
    @Schema(example = "2025-10-14T00:00:00Z")
    private Instant from;

    @Schema(example = "2025-10-21T00:00:00Z")
    private Instant to;

    @Schema(description = "Быстрый период", example = "TODAY")
    private QuickRange quick;

    @Schema(description = "IANA-таймзона, влияет на пресеты quick",
            example = "Europe/Moscow")
    private String timezone;

    @ArraySchema(arraySchema = @Schema(description = "Зоны склада"),
            schema = @Schema(example = "A"))
    private List<String> zones;

    // ВАЖНО: именно ArraySchema + implementation enum — тогда каждый item будет выпадающим списком
    @ArraySchema(arraySchema = @Schema(description = "Статусы записей"),
            schema = @Schema(implementation = InventoryHistoryStatus.class, example = "CRITICAL"))
    private List<InventoryHistoryStatus> statuses;

    @ArraySchema(arraySchema = @Schema(description = "Категории товара"),
            schema = @Schema(example = "network"))
    private List<String> categories;

    @Schema(description = "Поиск по артикулу товара (Product.code), названию (Product.name) и коду робота (Robot.code); без учёта регистра",
            example = "RB-002")
    private String q;

    @ArraySchema(arraySchema = @Schema(description = "Коды роботов"),
            schema = @Schema(example = "RB-001"))
    private List<String> robots;
}
