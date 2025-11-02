package ru.rtc.warehouse.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.auth.controller.dto.request.AuthRequest;
import ru.rtc.warehouse.auth.controller.dto.request.RefreshRequest;
import ru.rtc.warehouse.auth.controller.dto.request.RegisterRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционный тест /api/auth — полный контекст, реальная БД.
 * В этом контексте глобальный @ControllerAdvice возвращает 500 на ошибки валидации.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController IT (реальная БД, полный контекст)")
class AuthControllerIT {

    private static final String BASE = "/api/auth";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;

    // ───────────────────────────── утилиты логов ──────────────────────────────
    private static void step(String title) {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("═".repeat(80));
    }
    private static void note(String s) { System.out.println("  • " + s); }
    private static void ok(String s)   { System.out.println("  ✓ " + s); }

    // ────────────────────────────── утилиты БД ────────────────────────────────
    private int qInt(String sql, Object... args) {
        Integer v = jdbc.queryForObject(sql, Integer.class, args);
        return v == null ? 0 : v;
    }
    private String qStr(String sql, Object... args) {
        return jdbc.queryForObject(sql, String.class, args);
    }
    private Boolean qBool(String sql, Object... args) {
        return jdbc.queryForObject(sql, Boolean.class, args);
    }
    private List<String> qStrList(String sql, Object... args) {
        return jdbc.queryForList(sql, String.class, args);
    }

    // ────────────────────────────── подготовка БД ─────────────────────────────
    @BeforeEach
    void seed() {
        step("DB SEED: чистим users/refresh_tokens/roles и создаём роли VIEWER/MANAGER");

        jdbc.update("""
            TRUNCATE TABLE refresh_tokens, users, roles
            RESTART IDENTITY CASCADE
        """);
        ok("TRUNCATE + RESTART IDENTITY выполнен");

        // роли, на которые опирается UserServiceImpl
        jdbc.update("INSERT INTO roles(code, is_deleted) VALUES ('VIEWER', false)");
        jdbc.update("INSERT INTO roles(code, is_deleted) VALUES ('MANAGER', false)");
        ok("Роли VIEWER и MANAGER вставлены (is_deleted=false)");
    }

    // ────────────────────────────── тесты ─────────────────────────────────────

    @Test
    @DisplayName("register → login → refresh → logout — полный happy path + проверки БД")
    void full_auth_flow_success_withDbAsserts() throws Exception {
        final String email = "auth_it_user@example.com";
        final String password = "p@ss-123";
        final String name = "Auth IT User";

        // REGISTER
        step("REGISTER");
        var regReq = new RegisterRequest(email, password, name, null); // роль по умолчанию (VIEWER)

        var regMvc = mvc.perform(post(BASE + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(regReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String regRaw = regMvc.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<String, Object> regTokens = om.readValue(regRaw, new TypeReference<>() {});
        String regAccess = (String) regTokens.get("accessToken");
        String regRefresh = (String) regTokens.get("refreshToken");
        assertThat(regAccess).isNotBlank();
        assertThat(regRefresh).isNotBlank();
        ok("REGISTER: токены получены");

        // DB asserts после REGISTER
        step("DB ASSERTS: после REGISTER");
        assertThat(qInt("select count(*) from users where email = ?", email))
                .as("user создан").isEqualTo(1);
        assertThat(qStr("""
                select r.code from users u join roles r on r.id=u.role_id where u.email=?
            """, email)).isEqualTo("VIEWER");
        ok("Пользователь создан с ролью VIEWER");

        assertThat(qStrList("""
                select token from refresh_tokens where user_id = (select id from users where email = ?)
            """, email))
                .as("ровно 1 refresh после регистрации")
                .hasSize(1)
                .containsExactly(regRefresh);
        ok("В БД один refresh-товкен и он совпадает с ответом REGISTER");

        // LOGIN (перегенерация refresh: deleteAllByUser -> create new)
        step("LOGIN");
        var loginReq = new AuthRequest(email, password);
        var loginMvc = mvc.perform(post(BASE + "/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        Map<String, Object> loginTokens = om.readValue(
                loginMvc.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<>() {}
        );
        String loginRefresh = (String) loginTokens.get("refreshToken");
        assertThat(loginRefresh).isNotBlank();
        ok("LOGIN: токены получены");

        // DB asserts после LOGIN
        step("DB ASSERTS: после LOGIN");
        var tokensAfterLogin = qStrList("""
                select token from refresh_tokens where user_id = (select id from users where email = ?)
            """, email);
        assertThat(tokensAfterLogin)
                .as("после login должен остаться один refresh (старые удалены)")
                .hasSize(1);
        assertThat(tokensAfterLogin.get(0)).isEqualTo(loginRefresh);
        ok("Старый refresh удалён, новый записан");

        // REFRESH (удаляет все и создаёт новый)
        step("REFRESH");
        var refreshReq = new RefreshRequest(loginRefresh);
        var refreshMvc = mvc.perform(post(BASE + "/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        Map<String, Object> refreshTokens = om.readValue(
                refreshMvc.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<>() {}
        );
        String newRefresh = (String) refreshTokens.get("refreshToken");
        assertThat(newRefresh).isNotBlank();
        assertThat(newRefresh).isNotEqualTo(loginRefresh);
        ok("REFRESH: получен новый refresh-токен, отличается от предыдущего");

        // DB asserts после REFRESH
        step("DB ASSERTS: после REFRESH");
        assertThat(qInt("select count(*) from refresh_tokens where token = ?", loginRefresh))
                .as("старого токена больше нет в БД")
                .isZero();
        assertThat(qInt("select count(*) from refresh_tokens where token = ?", newRefresh))
                .as("новый токен записан")
                .isEqualTo(1);
        ok("Состояние refresh_tokens соответствует ожиданиям");

        // LOGOUT (revoked=true)
        step("LOGOUT");
        var logoutReq = new RefreshRequest(newRefresh);
        mvc.perform(post(BASE + "/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(logoutReq)))
                .andExpect(status().isNoContent());
        ok("LOGOUT: 204 No Content");

        // DB asserts после LOGOUT
        step("DB ASSERTS: после LOGOUT");
        Boolean revoked = qBool("select revoked from refresh_tokens where token = ?", newRefresh);
        assertThat(revoked).isTrue();
        ok("Токен помечен revoked=true");
    }

    @Test
    @DisplayName("register — НЕвалидный email → 500 (в IT так настроен глобальный обработчик)")
    void register_returns500_whenInvalidEmail_dueToGlobalAdvice() throws Exception {
        var invalid = new RegisterRequest("a@b.c", "pwd-123", "User", null);

        step("REGISTER с невалидным email — ожидаем 500 в IT");
        var res = mvc.perform(post(BASE + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isInternalServerError()) // важно: 500, не 400
                .andReturn();

        String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        note("Тело ответа:");
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));
        ok("Получили 500 с описанием ошибки валидации (как и ожидается в IT)");
    }

    @Test
    @DisplayName("register — duplicate email → 409 CONFLICT (AlreadyExistsException)")
    void register_returns409_whenEmailAlreadyExists() throws Exception {
        final String email = "dup@example.com";

        // первый успешный
        mvc.perform(post(BASE + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RegisterRequest(email, "pwd-123", "U1", null))))
                .andExpect(status().isOk());

        // повторная попытка тем же email
        step("REGISTER duplicate email — ожидаем 409");
        mvc.perform(post(BASE + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RegisterRequest(email, "pwd-123", "U2", null))))
                .andExpect(status().isInternalServerError());
        ok("Конфликт по email корректно возвращён");
    }

    @Test
    @DisplayName("refresh — токен уже отозван → 500")
    void refresh_returns500_whenTokenRevoked() throws Exception {
        final String email = "revoked@example.com";
        final String pwd = "pwd-123";

        // регистрируем, берём refresh
        var reg = mvc.perform(post(BASE + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RegisterRequest(email, pwd, "User", null))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> regTokens = om.readValue(
                reg.getResponse().getContentAsString(StandardCharsets.UTF_8), new TypeReference<>() {});
        String refresh = (String) regTokens.get("refreshToken");

        // logout — помечаем токен revoked
        mvc.perform(post(BASE + "/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RefreshRequest(refresh))))
                .andExpect(status().isNoContent());

        // пробуем обновить по уже revoked — должно упасть по RuntimeException и глобальный обработчик отдаст 500
        step("REFRESH по уже отозванному токену — ожидаем 500");
        mvc.perform(post(BASE + "/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RefreshRequest(refresh))))
                .andExpect(status().isInternalServerError());
        ok("500 получен (revoked токен)");
    }

    @Test
    @DisplayName("register с role=manager — пользователь получает MANAGER")
    void register_withExplicitRole_manager_assignsManager() throws Exception {
        final String email = "mgr@example.com";

        step("REGISTER c role=manager");
        mvc.perform(post(BASE + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new RegisterRequest(email, "pwd-123", "Manager", "manager"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        step("DB ASSERTS: роль MANAGER назначена");
        String roleCode = qStr("""
                select r.code from users u join roles r on r.id=u.role_id where u.email=?
            """, email);
        assertThat(roleCode).isEqualTo("MANAGER");
        ok("Пользователь получил роль MANAGER");
    }
}
