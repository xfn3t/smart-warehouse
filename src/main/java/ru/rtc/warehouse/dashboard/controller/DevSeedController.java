package ru.rtc.warehouse.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.dashboard.service.InventoryHistoryCommandService;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryStatusRepository;
import ru.rtc.warehouse.product.repository.ProductRepository;
import ru.rtc.warehouse.robot.repository.RobotRepository;
import ru.rtc.warehouse.warehouse.repository.WarehouseRepository;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevSeedController {

    private final InventoryHistoryCommandService cmd;
    private final WarehouseRepository whRepo;
    private final RobotRepository robotRepo;
    private final ProductRepository productRepo;
    private final InventoryHistoryStatusRepository statusRepo; // если у тебя другая таблица — подключи нужную

    @PostMapping("/seed-last-hour")
    public ResponseEntity<Map<String,Object>> seed(@RequestParam(defaultValue = "60") int minutes,
                                                   @RequestParam(defaultValue = "2") int perMinute) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime to = LocalDateTime.now(zone).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime from = to.minusMinutes(Math.max(1, minutes) - 1);

        var wh = whRepo.findAll(); var rb = robotRepo.findAll(); var pr = productRepo.findAll();
        var ok = statusRepo.findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode.OK).orElse(null);
        var low = statusRepo.findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode.LOW_STOCK).orElse(null);
        var crit = statusRepo.findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode.CRITICAL).orElse(null);
        Random rnd = new Random();

        int created = 0;
        LocalDateTime cur = from;
        while (!cur.isAfter(to)) {
            for (int i = 0; i < perMinute; i++) {
                var h = new InventoryHistory();
                h.setWarehouse(wh.get(rnd.nextInt(wh.size())));
                h.setRobot(rb.get(rnd.nextInt(rb.size())));
                h.setProduct(pr.get(rnd.nextInt(pr.size())));
                h.setZone(1 + rnd.nextInt(120));
                h.setRowNumber(1 + rnd.nextInt(200));
                h.setShelfNumber(1 + rnd.nextInt(20));

                int expected = 10 + rnd.nextInt(40);
                int qty = Math.max(0, expected + (-6 + rnd.nextInt(13)));
                int diff = qty - expected;
                h.setExpectedQuantity(expected);
                h.setQuantity(qty);
                h.setDifference(diff);

                if ((qty == 0 && expected > 0) || diff <= -10) h.setStatus(crit);
                else if (diff < 0) h.setStatus(low);
                else h.setStatus(ok);

                h.setScannedAt(cur);
                h.setCreatedAt(cur.plusMinutes(1 + rnd.nextInt(3)));
                h.setDeleted(false);

                cmd.create(h); // через доменный сервис — это зажжёт writer
                created++;
            }
            cur = cur.plusMinutes(1);
        }

        return ResponseEntity.ok(Map.of("from", from.toString(), "to", to.toString(), "created", created));
    }
}
