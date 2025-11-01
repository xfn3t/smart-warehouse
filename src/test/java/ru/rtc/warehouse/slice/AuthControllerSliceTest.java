package ru.rtc.warehouse.slice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import ru.rtc.warehouse.auth.controller.AuthController;
import ru.rtc.warehouse.auth.controller.dto.request.AuthRequest;
import ru.rtc.warehouse.auth.controller.dto.request.RefreshRequest;
import ru.rtc.warehouse.auth.controller.dto.request.RegisterRequest;
import ru.rtc.warehouse.auth.controller.dto.response.AuthResponse;
import ru.rtc.warehouse.auth.service.AuthService;
import ru.rtc.warehouse.exception.AlreadyExistsException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // без security-фильтров: тестируем только web-слой + advice
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthControllerSliceTest {

    private static final String BASE = "/api/auth";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean AuthService authService;

    // ——— утилиты для читабельных логов ———
    private static void step(String title) {
        System.out.println("\n" + "─".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("─".repeat(80));
    }
    private static void note(String message) { System.out.println("  • " + message); }
    private static void ok(String message)   { System.out.println("  ✓ " + message); }

    // Статус, который сейчас возвращает твой GlobalExceptionHandler для ошибок валидации.
    private static ResultMatcher validationErrorStatus() {
        return status().isInternalServerError();
    }

    private static AuthResponse sampleTokens() {
        return AuthResponse.builder()
                .accessToken("access-111")
                .refreshToken("refresh-222")
                .accessTokenExpiresInSeconds(3600L)
                .build();
    }

    // ==================== REGISTER ====================

    @Test
    @DisplayName("POST /api/auth/register — 200 OK при валидном теле; сервис вызван с корректными полями")
    void register_returns200_whenValid_callsServiceWithBody() throws Exception {
        final String URL = BASE + "/register";
        var req = new RegisterRequest("valid.user@example.com","pwd-123","User Name","viewer");

        step("STUB: authService.register(...) -> токены");
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleTokens());
        ok("Заглушка установлена");

        step("HTTP POST " + URL);
        String json = om.writeValueAsString(req);
        note("Тело запроса:");
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(json)));

        var res = mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("access-111"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-222"))
                .andReturn();

        String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("\n[Ответ /register]\n" +
                om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));

        step("VERIFY: сервис вызван с корректным телом");
        var captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService, times(1)).register(captor.capture());
        var passed = captor.getValue();

        assertThat(passed.getEmail()).isEqualTo(req.getEmail());
        assertThat(passed.getPassword()).isEqualTo(req.getPassword());
        assertThat(passed.getName()).isEqualTo(req.getName());
        assertThat(passed.getRole()).isEqualTo(req.getRole());
        ok("Поля переданы корректно");
    }

    @Test
    @DisplayName("POST /api/auth/register — 500 из GlobalExceptionHandler при невалидном email; сервис не вызывается")
    void register_returns500_whenInvalidEmail_dueToGlobalAdvice() throws Exception {
        final String URL = BASE + "/register";
        String body = om.writeValueAsString(Map.of(
                "email","a@b.c",  // короткий и невалидный
                "password","pwd-123",
                "name","User"
        ));

        step("HTTP POST " + URL + " с невалидным email");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(validationErrorStatus()); // сейчас 500

        verify(authService, never()).register(any());
        ok("Сервис не вызывался");
    }

    @Test
    @DisplayName("POST /api/auth/register — 500 из GlobalExceptionHandler при пароле короче 5")
    void register_returns500_whenPasswordTooShort_dueToGlobalAdvice() throws Exception {
        final String URL = BASE + "/register";
        String body = om.writeValueAsString(Map.of(
                "email","valid.user@example.com",
                "password","1234", // < 5
                "name","User"
        ));

        step("HTTP POST " + URL + " с коротким паролем");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(validationErrorStatus()); // сейчас 500

        verify(authService, never()).register(any());
        ok("Сервис не вызывался");
    }

    @Test
    @DisplayName("POST /api/auth/register — 500 из GlobalExceptionHandler при пароле > 20")
    void register_returns500_whenPasswordTooLong_dueToGlobalAdvice() throws Exception {
        final String URL = BASE + "/register";
        String longPwd = "x".repeat(21);
        String body = om.writeValueAsString(Map.of(
                "email","valid.user@example.com",
                "password", longPwd,
                "name","User"
        ));

        step("HTTP POST " + URL + " с длинным паролем");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(validationErrorStatus()); // сейчас 500

        verify(authService, never()).register(any());
        ok("Сервис не вызывался");
    }

    @Test
    @DisplayName("POST /api/auth/register — 500 из GlobalExceptionHandler при name=null")
    void register_returns500_whenNameNull_dueToGlobalAdvice() throws Exception {
        final String URL = BASE + "/register";
        String body = "{\"email\":\"valid.user@example.com\",\"password\":\"pwd-123\",\"name\":null}";

        step("HTTP POST " + URL + " с name=null");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(validationErrorStatus()); // сейчас 500

        verify(authService, never()).register(any());
        ok("Сервис не вызывался");
    }

    // ==================== LOGIN ====================

    @Test
    @DisplayName("POST /api/auth/login — 200 OK, сервис вызывается с email+password")
    void login_returns200_andCallsService() throws Exception {
        final String URL = BASE + "/login";
        var req = new AuthRequest("valid.user@example.com", "pwd-123");

        step("STUB: authService.login(...) -> токены");
        when(authService.login(eq(req.getEmail()), eq(req.getPassword()))).thenReturn(sampleTokens());
        ok("Заглушка установлена");

        step("HTTP POST " + URL);
        var json = om.writeValueAsString(req);
        var res = mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("access-111"))
                .andReturn();

        String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("\n[Ответ /login]\n" +
                om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));

        step("VERIFY: сервис вызван 1 раз с email+password");
        verify(authService, times(1)).login(eq(req.getEmail()), eq(req.getPassword()));
        ok("Вызов корректный");
    }

    // ==================== REFRESH ====================

    @Test
    @DisplayName("POST /api/auth/refresh — 200 OK, возвращает новую пару токенов")
    void refresh_returns200_andNewTokens() throws Exception {
        final String URL = BASE + "/refresh";
        var req = new RefreshRequest("refresh-abc");
        var resp = AuthResponse.builder()
                .accessToken("access-new")
                .refreshToken("refresh-new")
                .accessTokenExpiresInSeconds(3600L)
                .build();

        step("STUB: authService.refreshAccessToken(...) -> новые токены");
        when(authService.refreshAccessToken(eq("refresh-abc"))).thenReturn(resp);
        ok("Заглушка установлена");

        step("HTTP POST " + URL);
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-new"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-new"));

        step("VERIFY: сервис вызван с корректным refreshToken");
        verify(authService, times(1)).refreshAccessToken(eq("refresh-abc"));
        ok("Вызов корректный");
    }

    // ==================== LOGOUT ====================

    @Test
    @DisplayName("POST /api/auth/logout — 204 No Content; refresh помечается отозванным")
    void logout_returns204_andCallsService() throws Exception {
        final String URL = BASE + "/logout";
        var req = new RefreshRequest("refresh-to-revoke");

        step("STUB: authService.logout(...) — void");
        doNothing().when(authService).logout(eq("refresh-to-revoke"));
        ok("Заглушка установлена");

        step("HTTP POST " + URL);
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        step("VERIFY: сервис вызван 1 раз с правильным токеном");
        verify(authService, times(1)).logout(eq("refresh-to-revoke"));
        ok("Вызов корректный");
    }

    // ---------- REGISTER: invalid role -> 500 из GlobalExceptionHandler ----------
    @Test
    @DisplayName("POST /api/auth/register — 500 при невалидной роли (invalid enum), сервис бросает RuntimeException")
    void register_returns500_whenRoleInvalid_dueToGlobalAdvice() throws Exception {
        final String url = BASE + "/register";

        // тело с невалидной ролью
        var bodyJson = """
      {
        "email":"valid.user@example.com",
        "password":"pwd-123",
        "name":"User Name",
        "role":"badrole"
      }
      """;

        step("STUB: authService.register(...) -> RuntimeException(\"Invalid role: badrole\")");
        when(authService.register(argThat(req -> "badrole".equalsIgnoreCase(req.getRole()))))
                .thenThrow(new RuntimeException("Invalid role: badrole"));
        ok("Заглушка установлена");

        step("HTTP POST " + url + " с невалидной ролью");
        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isInternalServerError()); // 500 от GlobalExceptionHandler

        step("VERIFY: сервис вызван 1 раз с ролью badrole");
        var captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService, times(1)).register(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("badrole");
        ok("Вызов корректный");
    }

    // ---------- REGISTER: уже существует -> 409 ----------
    @Test
    @DisplayName("POST /api/auth/register — 409 CONFLICT если пользователь уже существует")
    void register_returns409_whenUserAlreadyExists() throws Exception {
        final String URL = BASE + "/register";
        var req = new RegisterRequest("exists@example.com", "pwd-123", "User", "viewer");

        step("STUB: authService.register(...) -> AlreadyExistsException");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AlreadyExistsException("User already exists"));
        ok("Заглушка установлена");

        step("HTTP POST " + URL + " с email уже существующего пользователя");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict()); // 409

        verify(authService, times(1)).register(any(RegisterRequest.class));
        ok("Сервис вызван и вернул 409 через advice");
    }

    // ---------- REGISTER: лишние поля игнорируются ----------
    @Test
    @DisplayName("POST /api/auth/register — лишние поля в JSON игнорируются (200 OK)")
    void register_ignoresUnknownFields_ok() throws Exception {
        final String URL = BASE + "/register";
        // добавим поле, которого нет в DTO
        var body = """
      {
        "email":"valid.user@example.com",
        "password":"pwd-123",
        "name":"User Name",
        "role":"viewer",
        "unexpected":"value",
        "nested":{"x":1}
      }
      """;

        step("STUB: authService.register(...) -> токены");
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleTokens());
        ok("Заглушка установлена");

        step("HTTP POST " + URL + " с лишними полями");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-111"));

        // убедимся, что лишние поля не попали в RegisterRequest (джексон их отбрасывает)
        var captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        var passed = captor.getValue();
        assertThat(passed.getEmail()).isEqualTo("valid.user@example.com");
        assertThat(passed.getName()).isEqualTo("User Name");
        assertThat(passed.getRole()).isEqualTo("viewer");
        ok("Лишние поля проигнорированы, полезные — считаны корректно");
    }

    // ---------- REFRESH: пустой токен -> сейчас 500 ----------
    @Test
    @DisplayName("POST /api/auth/refresh — пустой refreshToken -> 500 от GlobalExceptionHandler")
    void refresh_returns500_whenTokenBlank_dueToMissingValidation() throws Exception {
        final String URL = BASE + "/refresh";
        var body = "{\"refreshToken\":\"   \"}";

        step("STUB: authService.refreshAccessToken(any) -> RuntimeException");
        when(authService.refreshAccessToken(anyString()))
                .thenThrow(new RuntimeException("Invalid refresh token"));
        ok("Заглушка установлена");

        step("HTTP POST " + URL + " с пустым токеном");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(validationErrorStatus()); // сейчас 500

        verify(authService, times(1)).refreshAccessToken(anyString());
        ok("Вызов был, отдали 500");
    }

    // ---------- LOGOUT: без тела -> сейчас 500 ----------
    @Test
    @DisplayName("POST /api/auth/logout — без тела запроса -> 500 (Jackson/validation -> advice)")
    void logout_returns500_whenBodyMissing() throws Exception {
        final String URL = BASE + "/logout";

        step("HTTP POST " + URL + " без тела");
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)) // без content
                .andExpect(validationErrorStatus()); // Jackson бросит, advice вернёт 500

        verifyNoInteractions(authService);
        ok("Сервис не вызывался");
    }
}
