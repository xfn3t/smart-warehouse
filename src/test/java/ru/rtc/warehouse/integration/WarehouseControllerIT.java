package ru.rtc.warehouse.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.user.model.Role;
import ru.rtc.warehouse.user.model.User;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarehouseControllerIT {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper om;

    // --- Тестовые данные
    private static final long ROLE_ID   = 1L;
    private static final String ROLE_CODE = "ADMIN"; // важно: enum не содержит USER

    private static final long U_ID   = 100L;
    private static final long W1_ID  = 200L;
    private static final long W2_ID  = 201L;

    private static final String W1_CODE = "WH-200";
    private static final String W2_CODE = "WH-201";

    // --- Утилиты для понятных логов в консоли
    private static void step(String title) {
        System.out.println("\n" + "─".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("─".repeat(80));
    }

    private static void note(String message) {
        System.out.println("  • " + message);
    }

    private static void ok(String message) {
        System.out.println("  ✓ " + message);
    }

    private UserDetailsImpl principal(long id, String roleCode) {
        User u = new User();
        u.setId(id);
        u.setEmail("u@example.com");
        u.setPassword("pwd");

        Role r = new Role();
        r.setId(ROLE_ID);
        r.setCode(Role.RoleCode.valueOf(roleCode));
        u.setRole(r);

        return new UserDetailsImpl(u);
    }

    @BeforeEach
    void seed() {
        step("SEED: чистим БД и подготавливаем тестовые данные");
        jdbc.update("""
            TRUNCATE TABLE user_warehouses, location, warehouses, users, roles
            RESTART IDENTITY CASCADE
        """);

        jdbc.update("INSERT INTO roles(id, code, is_deleted) VALUES (?, ?, false)", ROLE_ID, ROLE_CODE);

        jdbc.update("""
            INSERT INTO users(id, email, password_hash, name, role_id, is_deleted)
            VALUES (?, 'u@x', 'p', 'U', ?, false)
        """, U_ID, ROLE_ID);

        jdbc.update("""
            INSERT INTO warehouses(id, code, zone_max_size, row_max_size, shelf_max_size, name, location, is_deleted)
            VALUES (?, ?, 1, 1, 1, ?, ?, false)
        """, W1_ID, W1_CODE, "W200", "CityA");

        jdbc.update("""
            INSERT INTO warehouses(id, code, zone_max_size, row_max_size, shelf_max_size, name, location, is_deleted)
            VALUES (?, ?, 1, 1, 1, ?, ?, false)
        """, W2_ID, W2_CODE, "W201", "CityB");

        jdbc.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", U_ID, W1_ID);
        jdbc.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", U_ID, W2_ID);

        Integer whCount = jdbc.queryForObject("SELECT COUNT(*) FROM warehouses", Integer.class);
        Integer uwCount = jdbc.queryForObject("SELECT COUNT(*) FROM user_warehouses", Integer.class);
        note("warehouses: " + whCount + ", user_warehouses: " + uwCount);
        ok("Начальное состояние БД подготовлено");
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/warehouse — возвращает только склады пользователя из тестовой БД")
    void getUserWarehouses_fromDb_ok() throws Exception {
        step("Шаг 1: вызываем GET /api/warehouse и проверяем JSON-ответ");

        var mvcResult = mvc.perform(get("/api/warehouse")
                        .with(user(principal(U_ID, ROLE_CODE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String raw = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("\n[Ответ контроллера /api/warehouse]\n"
                + om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));

        List<Map<String, Object>> items = om.readValue(
                mvcResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {}
        );

        assertThat(items)
                .as("Должны получить ровно два склада пользователя")
                .hasSize(2)
                .extracting(m -> (String) m.get("code"))
                .containsExactlyInAnyOrder(W1_CODE, W2_CODE);
        ok("Ответ содержит коды " + W1_CODE + " и " + W2_CODE);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/warehouse — создаёт склад, генерирует локации и линкует к пользователю")
    void createWarehouse_persists_locations_and_links_ok() throws Exception {
        step("Шаг 2: отправляем POST /api/warehouse (ADMIN) и ждём 201 Created");

        String requestJson = """
            {
              "code": "NEW-001",
              "name": "New WH",
              "zoneMaxSize": 2,
              "rowMaxSize": 2,
              "shelfMaxSize": 3,
              "location": "SPB"
            }
            """;
        note("Запрос:");
        System.out.println(requestJson);

        mvc.perform(post("/api/warehouse")
                        .with(user(principal(U_ID, "ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        ok("Контроллер вернул 201 Created");

        step("Шаг 3: проверяем БД после создания склада");
        Long newId = jdbc.queryForObject(
                "SELECT id FROM warehouses WHERE code = ?",
                Long.class, "NEW-001"
        );
        assertThat(newId).as("Склад NEW-001 должен сохраниться").isNotNull();
        ok("Склад сохранён: id=" + newId);

        // Ожидаемое число локаций = zoneMaxSize * rowMaxSize * shelfMaxSize = 2 * 2 * 3 = 12
        int expectedLocations = 2 * 2 * 3;
        Integer actualLocations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM location WHERE warehouse_id = ?",
                Integer.class, newId
        );
        note("Сгенерировано локаций: " + actualLocations + " (ожидалось " + expectedLocations + ")");
        assertThat(actualLocations).as("Должно сгенерироваться " + expectedLocations + " локаций").isEqualTo(expectedLocations);
        ok("Количество локаций корректно");

        Integer linkCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_warehouses WHERE user_id = ? AND warehouse_id = ?",
                Integer.class, U_ID, newId
        );
        assertThat(linkCount).as("Созданный склад должен быть привязан к пользователю").isEqualTo(1);
        ok("Связь user_warehouses создана (user_id=" + U_ID + ", warehouse_id=" + newId + ")");

        step("ИТОГО: POST создал склад, локации и связал с пользователем — всё ок ✅");
    }
}
