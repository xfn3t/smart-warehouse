package ru.rtc.warehouse.slice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.warehouse.controller.WarehouseController;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.service.WarehouseService;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WarehouseController.class)
@AutoConfigureMockMvc(addFilters = true) //
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WarehouseControllerSliceTest {

    private static final String URL = "/api/warehouse";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean
    WarehouseService warehouseService;

    // ---------- УТИЛИТЫ ДЛЯ ПОНЯТНЫХ ЛОГОВ ----------

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

    // ---------- ФАБРИКИ ТЕСТОВЫХ ДАННЫХ ----------

    private static UserDetailsImpl principalWithUserId(long userId) {
        User u = new User();
        u.setId(userId);
        u.setEmail("u@example.com");
        u.setPassword("pwd");
        return new UserDetailsImpl(u);
    }

    private static WarehouseDTO sampleDto() {
        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(1L);
        dto.setCode("W-001");
        dto.setName("Main Warehouse");
        dto.setZoneMaxSize(2);
        dto.setRowMaxSize(3);
        dto.setShelfMaxSize(4);
        dto.setLocation("Moscow");
        return dto;
    }

    private static WarehouseCreateRequest sampleCreateReq() {
        WarehouseCreateRequest body = new WarehouseCreateRequest();
        body.setCode("NEW-001");
        body.setName("New WH");
        body.setZoneMaxSize(1);
        body.setRowMaxSize(1);
        body.setShelfMaxSize(1);
        body.setLocation("SPB");
        return body;
    }

    // ---------- ТЕСТЫ ----------

    @Nested
    @DisplayName("GET /api/warehouse")
    class GetUserWarehouses {

        @Test
        @DisplayName("возвращает список складов пользователя и дергает сервис с корректным userId")
        void returnsWarehouses_andCallsService_withCorrectUserId() throws Exception {
            long userId = 777L;

            step("STUB: сервис возвращает 1 склад для userId=" + userId);
            WarehouseDTO dto = sampleDto();
            when(warehouseService.findByUserId(userId)).thenReturn(List.of(dto));
            ok("Заглушка установлена");

            step("HTTP GET " + URL + " (аутентифицированный)");
            var res = mvc.perform(get(URL)
                            .with(user(principalWithUserId(userId)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].code").value("W-001"))
                    .andReturn();

            String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
            System.out.println("\n[Ответ контроллера /api/warehouse]\n" +
                    om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));

            step("VERIFY: проверяем вызов сервиса и содержимое ответа");
            verify(warehouseService, times(1)).findByUserId(eq(userId));
            ok("Сервис вызван ровно 1 раз с userId=" + userId);

            assertThat(raw).contains("Main Warehouse");
            ok("Ответ содержит имя склада \"Main Warehouse\"");
        }

        @Test
        @DisplayName("без аутентификации — 401 и сервис не вызывается")
        void unauthorized_401() throws Exception {
            step("HTTP GET " + URL + " (без аутентификации)");
            mvc.perform(get(URL).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
            ok("Статус 401 получен");

            step("VERIFY: сервис не вызывался");
            verifyNoInteractions(warehouseService);
            ok("Взаимодействий с сервисом не было");
        }
    }

    @Nested
    @DisplayName("POST /api/warehouse")
    class CreateWarehouse {

        @Test
        @DisplayName("создает склад — 201 и передает тело + userId в сервис")
        void created_201() throws Exception {
            long userId = 100L;
            var body = sampleCreateReq();

            step("STUB: сервис.save(...) ничего не делает (void)");
            doNothing().when(warehouseService).save(any(WarehouseCreateRequest.class), eq(userId));
            ok("Заглушка установлена");

            step("HTTP POST " + URL + " (аутентифицированный + CSRF)");
            String json = om.writeValueAsString(body);
            note("Тело запроса:");
            System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(json)));

            mvc.perform(post(URL)
                            .with(user(principalWithUserId(userId)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
            ok("Статус 201 Created получен");

            step("VERIFY: проверяем, что сервис вызван с корректными аргументами");
            ArgumentCaptor<WarehouseCreateRequest> reqCap = ArgumentCaptor.forClass(WarehouseCreateRequest.class);
            verify(warehouseService, times(1)).save(reqCap.capture(), eq(userId));
            WarehouseCreateRequest passed = reqCap.getValue();

            assertThat(passed.getCode()).as("code").isEqualTo(body.getCode());
            assertThat(passed.getName()).as("name").isEqualTo(body.getName());
            assertThat(passed.getZoneMaxSize()).as("zoneMaxSize").isEqualTo(body.getZoneMaxSize());
            assertThat(passed.getRowMaxSize()).as("rowMaxSize").isEqualTo(body.getRowMaxSize());
            assertThat(passed.getShelfMaxSize()).as("shelfMaxSize").isEqualTo(body.getShelfMaxSize());
            assertThat(passed.getLocation()).as("location").isEqualTo(body.getLocation());
            ok("Все поля тела запроса переданы сервису корректно");
        }

        @Test
        @DisplayName("без CSRF — 403 и сервис не вызывается")
        void forbidden_403_without_csrf() throws Exception {
            long userId = 100L;
            var body = sampleCreateReq();

            step("HTTP POST " + URL + " (аутентифицированный, НО без CSRF)");
            mvc.perform(post(URL)
                            .with(user(principalWithUserId(userId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
            ok("Статус 403 Forbidden получен");

            step("VERIFY: сервис не вызывался");
            verify(warehouseService, times(0)).save(any(), any());
            ok("Взаимодействий с сервисом не было");
        }
    }
}
