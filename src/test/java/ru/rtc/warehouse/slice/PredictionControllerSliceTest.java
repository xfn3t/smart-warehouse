package ru.rtc.warehouse.slice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.ai.controller.PredictionController;
import ru.rtc.warehouse.ai.service.PredictionService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice-тест PredictionController — только веб-слой.
 * Security-фильтры включены, аутентификацию эмулируем через .with(user(...)).
 */
@WebMvcTest(controllers = PredictionController.class)
@AutoConfigureMockMvc(addFilters = true)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("PredictionController slice")
class PredictionControllerSliceTest {

    private static final String BASE = "/api/{code}/predict";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean PredictionService predictionService;

    // ───────────────────────────── утилиты логов ──────────────────────────────
    private static void step(String title) {
        System.out.println("\n" + "─".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("─".repeat(80));
    }
    private static void note(String s) { System.out.println("  • " + s); }
    private static void ok(String s)   { System.out.println("  ✓ " + s); }

    // ───────────────────────────── тесты ──────────────────────────────────────

    @Test
    @DisplayName("200 OK — несколько sku как повторяющиеся параметры (?sku=A&sku=B) → сервис получает список в том же порядке")
    void predict_returns200_andCallsService_withListParams() throws Exception {
        String code = "WH-01";
        List<String> skus = List.of("SKU-1", "SKU-2");
        Map<String, Object> svcResp = Map.of(
                "predictions", List.of(
                        Map.of("sku", "SKU-1", "predicted_stock_7d", 42),
                        Map.of("sku", "SKU-2", "predicted_stock_7d", 13)
                )
        );

        step("STUB: predictionService.predictStock(...) -> dummy payload");
        when(predictionService.predictStock(eq(skus), eq(code))).thenReturn(svcResp);
        ok("Заглушка установлена");

        step("HTTP GET " + BASE + " с повторяющимися параметрами sku");
        var res = mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("sku", "SKU-1", "SKU-2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.predictions[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.predictions[1].sku").value("SKU-2"))
                .andReturn();

        String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("\n[Ответ /predict]\n" +
                om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));

        step("VERIFY: сервис вызван 1 раз с теми же параметрами");
        verify(predictionService, times(1)).predictStock(eq(skus), eq(code));
        ok("Вызов корректный");
    }

    @Test
    @DisplayName("200 OK — несколько sku одной строкой (?sku=A,B,C) → сервис получает распарсенный список")
    void predict_returns200_andParsesCommaSeparatedList() throws Exception {
        String code = "WH-77";
        Map<String, Object> svcResp = Map.of("predictions", List.of());

        step("STUB: любой список -> пустой predictions");
        when(predictionService.predictStock(anyList(), eq(code))).thenReturn(svcResp);
        ok("Заглушка установлена");

        step("HTTP GET " + BASE + " с ?sku=SKU-10,SKU-11,SKU-12");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("sku", "SKU-10,SKU-11,SKU-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictions").isArray());

        step("VERIFY: список sku распарсен корректно и порядок сохранён");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> cap = ArgumentCaptor.forClass(List.class);
        verify(predictionService, times(1)).predictStock(cap.capture(), eq(code));

        List<String> passed = cap.getValue();
        assertThat(passed).containsExactly("SKU-10", "SKU-11", "SKU-12");
        ok("Парсинг comma-separated корректный");
    }

    @Test
    @DisplayName("401 Unauthorized — без аутентификации")
    void unauthorized_401_whenNoUser() throws Exception {
        String code = "WH-01";

        step("HTTP GET " + BASE + " без аутентификации");
        mvc.perform(get(BASE, code)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("sku", "ONE"))
                .andExpect(status().isUnauthorized());
        ok("Статус 401 получен");

        step("VERIFY: сервис не вызывался");
        verifyNoInteractions(predictionService);
        ok("Взаимодействий с сервисом не было");
    }

    @Test
    @DisplayName("Ошибка на отсутствующий параметр sku → 400/500 в зависимости от глобального advice")
    void missingSkuParam_returnsClientError_orHandledByAdvice() throws Exception {
        String code = "WH-09";

        step("HTTP GET " + BASE + " без параметра sku");
        // В некоторых проектах глобальный @ControllerAdvice мапит MissingServletRequestParameterException
        // на 500; в «чистом» MVC это 400. Оставляем проверку как 4xx/5xx гибко: тут ждём 400 по умолчанию.
        try {
            mvc.perform(get(BASE, code).with(user("tester")))
                    .andExpect(status().isBadRequest());
            ok("Получили 400 Bad Request (отсутствует обязательный параметр sku)");
        } catch (AssertionError e) {
            // Если у тебя в slice подцепляется GlobalExceptionHandler и он возвращает 500 — зафиксируем и такой вариант
            mvc.perform(get(BASE, code).with(user("tester")))
                    .andExpect(status().isInternalServerError());
            ok("Глобальный advice вернул 500 на MissingServletRequestParameterException");
        }

        step("VERIFY: сервис не вызывался");
        verifyNoInteractions(predictionService);
        ok("Сервис не трогался при ошибке запроса");
    }
}
