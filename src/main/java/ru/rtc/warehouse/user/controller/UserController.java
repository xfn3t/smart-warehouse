package ru.rtc.warehouse.user.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.service.UserWarehouseService;
import ru.rtc.warehouse.user.service.dto.UserDTO;

@RestController
@RequestMapping("/api/{warehouseCode}/users")
@RequiredArgsConstructor
@RequiresOwnership(
    codeParam = "warehouseCode",
    entityType = RequiresOwnership.EntityType.WAREHOUSE
)
public class UserController {

    private final UserWarehouseService userWarehouseService;

    /** List users assigned to this warehouse — ADMIN only */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getAllWarehouseUser(
        @PathVariable String warehouseCode
    ) {
        return userWarehouseService.findAllByWarehouse(warehouseCode);
    }

    /** Register a new user for this warehouse — ADMIN only */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public void register(
        @PathVariable String warehouseCode,
        @Valid @RequestBody UserCreateRequest request
    ) {
        userWarehouseService.createUserForWarehouse(warehouseCode, request);
    }
}
