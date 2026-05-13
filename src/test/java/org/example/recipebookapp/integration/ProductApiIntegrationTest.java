package org.example.recipebookapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.recipebookapp.api.dto.ProductCreateDto;
import org.example.recipebookapp.api.dto.ProductResponseDto;
import org.example.recipebookapp.database.entity.enums.CookingRequirement;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.example.recipebookapp.database.entity.enums.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h1>Интеграционные тесты API: Продукты</h1>
 * <p>Тестирует полный стек Controller → Service → Repository → In-Memory DB.
 * Применяет техники тест-дизайна: Эквивалентное разбиение (EP) и Анализ граничных значений (BVA).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("📦 Интеграционные тесты: Продукты")
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;



    private ProductCreateDto validBaseDto() {
        ProductCreateDto d = new ProductCreateDto();
        d.setName("Базовый продукт");
        d.setCalories(100.0);
        d.setProteins(10.0);
        d.setFats(5.0);
        d.setCarbs(20.0);
        d.setCategory(ProductCategory.VEGETABLES);
        d.setCookingRequirement(CookingRequirement.READY_TO_EAT);
        d.setFlags(EnumSet.noneOf(NutritionFlag.class));
        return d;
    }

    /**
     * Подготовка данных перед каждым тестом.
     * В интеграционных тестах @Transactional гарантирует автоматический rollback после каждого теста.
     */
    @BeforeEach
    void setUp() {
    }

    // ==========================================
    // ЭКВИВАЛЕНТНОЕ РАЗБИЕНИЕ (EP)
    // ==========================================
    @Nested
    @DisplayName("🔍 EP: Классы входных данных")
    class EquivalencePartitioningTests {

        @Test
        @DisplayName("EP-1: Валидный класс → Успешное создание продукта")
        void shouldCreateProduct_WhenDataIsValid() throws Exception {
            ProductCreateDto dto = validBaseDto();

            String json = mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString();

            ProductResponseDto created = mapper.readValue(json, ProductResponseDto.class);
            assertThat(created.getName()).isEqualTo("Базовый продукт");
            assertThat(created.getCalories()).isEqualTo(100.0);
            assertThat(created.getCategory()).isEqualTo("VEGETABLES");
        }

        @Test
        @DisplayName("EP-2: Невалидный класс (имя < 2 символов) → Ошибка валидации")
        void shouldRejectProduct_WhenNameTooShort() throws Exception {
            ProductCreateDto dto = validBaseDto();
            dto.setName("А"); // Нарушает @Size(min = 2)

            mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.name", notNullValue()))
                    .andDo(print());
        }

        @Test
        @DisplayName("EP-3: Невалидный класс (сумма БЖУ > 100) → Бизнес-ошибка")
        void shouldRejectProduct_WhenBjuSumExceeds100() throws Exception {
            ProductCreateDto dto = validBaseDto();
            dto.setProteins(40.0);
            dto.setFats(40.0);
            dto.setCarbs(40.0); // Сумма 120 > 100

            mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isInternalServerError()) // ValidationException → 500 (или 400, зависит от @ControllerAdvice)
                    .andExpect(jsonPath("$.message", containsString("Сумма БЖУ")));
        }

        @Test
        @DisplayName("EP-4: Невалидный класс (отсутствуют обязательные поля) → 400")
        void shouldRejectProduct_WhenRequiredFieldsMissing() throws Exception {
            ProductCreateDto dto = new ProductCreateDto();
            dto.setName("Тест без обязательных");
            // category, cookingRequirement, proteins, fats, carbs, calories = null

            mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==========================================
    // АНАЛИЗ ГРАНИЧНЫХ ЗНАЧЕНИЙ (BVA)
    // ==========================================
    @Nested
    @DisplayName("📏 BVA: Граничные значения полей")
    class BoundaryValueAnalysisTests {

        @ParameterizedTest
        @CsvSource({
                "0.0, 0.0, 0.0",       // Граница min: 0
                "33.33, 33.33, 33.34", // Граница max-epsilon: 99.99
                "34.0, 33.0, 33.0",    // Граница max: 100.0
                "0.01, 0.0, 0.0"       // Минимально допустимое > 0
        })
        @DisplayName("BVA-1: Границы значений БЖУ (0..100, сумма ≤100)")
        void shouldAcceptBoundaryBjuValues(Double p, Double f, Double c) throws Exception {
            ProductCreateDto dto = validBaseDto();
            dto.setProteins(p);
            dto.setFats(f);
            dto.setCarbs(c);

            mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.proteins").value(p));
        }

        @Test
        @DisplayName("BVA-2: Выход за верхнюю границу БЖУ (100.01) → Ошибка")
        void shouldRejectWhenExceedsMaxBju() throws Exception {
            ProductCreateDto dto = validBaseDto();
            dto.setProteins(100.01); // @Max(100)

            mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @CsvSource({
                "1, bad",      // min-1: нарушает @Size(min=2)
                "2, ok",       // min: валидно
                "255, ok"      // max: валидно (length 255)
        })
        @DisplayName("BVA-3: Границы длины названия (min=2)")
        void shouldValidateNameLengthBoundary(int length, String expected) throws Exception {
            ProductCreateDto dto = validBaseDto();
            dto.setName("A".repeat(length));

            MvcResult result = mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().is(expected.equals("ok") ? 200 : 400))
                    .andReturn();

            if (expected.equals("ok")) {
                assertThat(result.getResponse().getContentAsString()).contains("\"name\":\"" + "A".repeat(length) + "\"");
            }
        }
    }

    // ==========================================
    // CRUD & БИЗНЕС-ЛОГИКА
    // ==========================================
    @Nested
    @DisplayName("🔄 Полный цикл и сценарии")
    class BusinessLogicTests {

        @Test
        @DisplayName("CS-1: Обновление продукта → изменение полей и БЖУ")
        void shouldUpdateProduct_WhenDataIsValid() throws Exception {
            // 1. Создаём
            String json = mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validBaseDto())))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            Long id = mapper.readTree(json).get("id").asLong();

            // 2. Обновляем
            ProductCreateDto updateDto = validBaseDto();
            updateDto.setName("Обновлённый продукт");
            updateDto.setCalories(200.0);
            updateDto.setProteins(20.0);
            updateDto.setFats(10.0);
            updateDto.setCarbs(0.0);

            mvc.perform(put("/api/products/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Обновлённый продукт"))
                    .andExpect(jsonPath("$.calories").value(200.0));
        }

        @Test
        @DisplayName("CS-2: Удаление свободного продукта → 204")
        void shouldDeleteUnusedProduct() throws Exception {
            String json = mvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validBaseDto())))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            Long id = mapper.readTree(json).get("id").asLong();

            mvc.perform(delete("/api/products/{id}", id))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/products/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("CS-3: Пагинация и фильтрация списка")
        void shouldPaginateAndFilterProducts() throws Exception {
            for (int i = 1; i <= 3; i++)
                mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validBaseDto())));

            mvc.perform(get("/api/products").param("page", "0").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }
    }
}
