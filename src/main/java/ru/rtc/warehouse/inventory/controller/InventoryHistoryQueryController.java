package ru.rtc.warehouse.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.util.QuickRangeResolver;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * REST-контроллер поиска по истории инвентаризаций.
 * Поддерживает фильтры, быстрые диапазоны, пагинацию и сортировку.
 */
@RestController
@RequestMapping("/api/inventory/history")
@Tag(name = "Inventory History")
@RequiredArgsConstructor
public class InventoryHistoryQueryController {

    private final InventoryHistoryQueryService queryService;

    // Белый список сортируемых полей и маппинг алиасов -> путь в entity
    private static final Map<String, String> SORT_MAP = Map.ofEntries(
            Map.entry("scannedAt", "scannedAt"),
            Map.entry("zone", "zone"),
            Map.entry("rowNumber", "rowNumber"),
            Map.entry("shelfNumber", "shelfNumber"),
            Map.entry("status", "status"),
            Map.entry("quantity", "quantity"),
            // «читаемые» поля с переходом на связи
            Map.entry("robotCode", "robot.code"),
            Map.entry("skuCode", "product.code"),
            Map.entry("productName", "product.name")
    );

    /**
     * Поиск по историческим данным.
     *
     * @param rq    фильтры (период, зоны, статусы, категории, q, роботы)
     * @param page  номер страницы (0-based)
     * @param size  размер страницы (20/50/100)
     * @param sort  поле сортировки (см. SORT_MAP)
     * @param order направление сортировки ASC|DESC
     */
    @Operation(summary = "Поиск по историческим данным")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = HistoryPageDTO.class)))
    @ApiResponse(responseCode = "400", description = "Некорректные параметры периода")
    @GetMapping
    public ResponseEntity<HistoryPageDTO> findHistory(
            @ParameterObject InventoryHistorySearchRequest rq,

            @Parameter(description = "Номер страницы (0-based)", example = "0")
            @Min(0) Integer page,

            @Parameter(description = "Размер страницы (20|50|100)", example = "20")
            @Min(1) @Max(100) Integer size,

            @Parameter(
                    description = "Поле сортировки",
                    schema = @Schema(
                            allowableValues = {
                                    "scannedAt","zone","rowNumber","shelfNumber",
                                    "status","quantity","robotCode","skuCode","productName"
                            },
                            defaultValue = "scannedAt",
                            example = "scannedAt"
                    )
            )
            String sort,

            @Parameter(
                    description = "Направление сортировки",
                    schema = @Schema(allowableValues = {"ASC","DESC"}, defaultValue = "DESC", example = "DESC")
            )
            String order
    ) {
        if (rq != null && rq.getQuick() != null && (rq.getFrom() != null || rq.getTo() != null)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Укажите либо quick, либо from/to — одновременно нельзя. " +
                            "Если заданы from/to, параметр quick будет проигнорирован.");
        }
        // Диапазон дат: quick → from/to, если явные from/to не заданы
        if (rq != null && rq.getFrom() == null && rq.getTo() == null && rq.getQuick() != null) {
            var zone = ZoneId.systemDefault(); // ← Исправлено: ZoneId вместо TimeZone
            var range = QuickRangeResolver.resolve(rq.getQuick(), zone); // [from; to) в UTC
            rq.setFrom(range[0]);
            rq.setTo(range[1]);
        }
        // Валидация from/to
        if (rq != null && rq.getFrom() != null && rq.getTo() != null) {
            Instant from = rq.getFrom(), to = rq.getTo();
            if (!from.isBefore(to)) {
                throw new ResponseStatusException(BAD_REQUEST, "'from' must be < 'to'");
            }
        }

        // Пагинация: только 20/50/100 (по ТЗ)
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        if (s != 20 && s != 50 && s != 100) s = 20;

        // Сортировка: белый список + алиасы
        String requestedSort = StringUtils.hasText(sort) ? sort : "scannedAt";
        String sortPath = SORT_MAP.getOrDefault(requestedSort, "scannedAt");
        Sort.Direction dir = "DESC".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(p, s, Sort.by(dir, sortPath));

        // Поиск
        Page<InventoryHistoryDTO> pageDto = Optional.ofNullable(queryService.search(rq, pageable))
                .orElse(Page.empty(pageable));

        HistoryPageDTO result = HistoryPageDTO.builder()
                .total(pageDto.getTotalElements())
                .page(pageDto.getNumber())
                .size(pageDto.getSize())
                .items(pageDto.getContent())
                .build();

        return ResponseEntity.ok(result);
    }
}