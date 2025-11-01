package ru.rtc.warehouse.inventory.controller.dto.request;


import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.common.QuickRange;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class InventoryHistorySearchRequest {
    @Schema(example = "2025-10-14T00:00:00Z")
    private Instant from;

    @Schema(example = "2025-10-21T00:00:00Z")
    private Instant to;

    @Schema(description = "Быстрый период", example = "TODAY")
    private QuickRange quick;

    @ArraySchema(arraySchema = @Schema(description = "Зоны склада"),
            schema = @Schema(example = "1"))
    private List<Integer> zones;

    @ArraySchema(
            arraySchema = @Schema(description = "Статусы записей"),
            schema = @Schema(
                    implementation = ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode.class,
                    example = "CRITICAL"
            )
    )
    private List<InventoryHistoryStatus.InventoryHistoryStatusCode> statuses;

    @ArraySchema(arraySchema = @Schema(description = "Категории товара"),
            schema = @Schema(example = "network"))
    private List<String> categories;

    @Schema(description = "Поиск по артикулу товара (Product.code), названию (Product.name) и коду робота (Robot.code); без учёта регистра")
    private String q;

    @ArraySchema(arraySchema = @Schema(description = "Коды роботов"),
            schema = @Schema(example = "RB-001"))
    private List<String> robots;

    // Нормализация списков для @ParameterObject
    public void setCategories(List<String> raw) {
        this.categories = normalizeCommaAware(raw);
    }

    public void setRobots(List<String> raw) {
        this.robots = normalizeCommaAware(raw);
    }

    // zones/statuses приходят уже типизированными — дополнительная нормализация не нужна

    private static List<String> normalizeCommaAware(List<String> raw) {
        if (raw == null) return null;
        List<String> out = new ArrayList<>();
        for (String item : raw) {
            if (item == null) continue;
            // разбиваем каждую строку по запятой и триммим элементы
            Arrays.stream(item.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(out::add);
        }
        // Порядок сохраняется, дубликаты намеренно не удаляются
        return out.isEmpty() ? null : out;
    }
}
