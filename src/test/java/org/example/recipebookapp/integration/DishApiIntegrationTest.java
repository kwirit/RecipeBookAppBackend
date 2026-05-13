package org.example.recipebookapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.recipebookapp.api.dto.DishCreateDto;
import org.example.recipebookapp.api.dto.DishResponseDto;
import org.example.recipebookapp.api.dto.ProductCreateDto;
import org.example.recipebookapp.database.entity.enums.CookingRequirement;
import org.example.recipebookapp.database.entity.enums.DishCategory;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.example.recipebookapp.database.entity.enums.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h1>Интеграционные тесты API: Блюда</h1>
 * <p>Тестирует полный цикл работы с блюдами, включая автоматический расчет КБЖУ на порцию.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("🍲 Интеграционные тесты: Блюда")
class DishApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long chickenId;
    private Long oilId;
    private Long waterId;

    /**
     * Подготовка тестовых данных: создание продуктов перед каждым тестом.
     */
    @BeforeEach
    void setUpProducts() throws Exception {
        chickenId = createProduct("Куриное филе", 110.0, 23.0, 1.0, 0.0);
        oilId = createProduct("Оливковое масло", 900.0, 0.0, 100.0, 0.0);
        waterId = createProduct("Вода", 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Вспомогательный метод для создания продукта через API.
     * @return ID созданного продукта
     */
    private Long createProduct(String name, double cal, double pro, double fat, double carb) throws Exception {
        ProductCreateDto dto = new ProductCreateDto();
        dto.setName(name);
        dto.setCalories(cal);
        dto.setProteins(pro);
        dto.setFats(fat);
        dto.setCarbs(carb);
        dto.setCategory(ProductCategory.MEAT);
        dto.setCookingRequirement(CookingRequirement.READY_TO_EAT);
        dto.setFlags(Set.of());

        String content = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(content).get("id").asLong();
    }

    /**
     * Вспомогательный метод для создания DTO ингредиента.
     */
    private DishCreateDto.IngredientDto ing(Long productId, double grams) {
        DishCreateDto.IngredientDto ing = new DishCreateDto.IngredientDto();
        ing.setProductId(productId);
        ing.setQuantityInGrams(grams);
        return ing;
    }

    // ==========================================
    // ЭКВИВАЛЕНТНОЕ РАЗБИЕНИЕ (EP)
    // ==========================================
    @Nested
    @DisplayName("📦 EP: Классы входных данных")
    class EquivalencePartitioningTests {

        @Test
        @DisplayName("EP-1: Авто-расчет КБЖУ на порцию (calories=null)")
        void shouldAutoCalculateKbzhuPerPortion() throws Exception {
            // Дано: Блюдо из 100г курицы (110 ккал). Порция 100г.
            // Ожидаемо: 110 ккал на порцию.
            DishCreateDto dto = new DishCreateDto();
            dto.setName("Куриная грудка");
            dto.setPortionSize(100.0);
            dto.setCategory(DishCategory.SECOND_COURSE);
            dto.setIngredients(List.of(ing(chickenId, 100.0)));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calories").value(110.0))
                    .andExpect(jsonPath("$.proteins").value(23.0));
        }

        @Test
        @DisplayName("EP-2: Ручное переопределение КБЖУ")
        void shouldOverrideKbzhuManually() throws Exception {
            DishCreateDto dto = new DishCreateDto();
            dto.setName("Ручное блюдо");
            dto.setPortionSize(100.0);
            dto.setCalories(999.0); // Ручное значение
            dto.setIngredients(List.of(ing(chickenId, 100.0)));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calories").value(999.0));
        }

        @Test
        @DisplayName("EP-3: Ошибка при пустом списке ингредиентов")
        void shouldRejectEmptyIngredients() throws Exception {
            DishCreateDto dto = new DishCreateDto();
            dto.setName("Пустое");
            dto.setPortionSize(100.0);
            dto.setIngredients(List.of()); // Пусто

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-4: Ошибка при несуществующем продукте")
        void shouldRejectNonExistentProduct() throws Exception {
            DishCreateDto dto = new DishCreateDto();
            dto.setName("Ошибка");
            dto.setPortionSize(100.0);
            dto.setIngredients(List.of(ing(99999L, 100.0)));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound()); // EntityNotFoundException -> 404
        }

        @Test
        @DisplayName("EP-5: Авто-определение категории по макросу")
        void shouldDetectCategoryFromMacro() throws Exception {
            DishCreateDto dto = new DishCreateDto();
            dto.setName("!суп Борщ");
            dto.setPortionSize(250.0);
            dto.setIngredients(List.of(ing(waterId, 200.0), ing(chickenId, 50.0)));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.category").value("SOUP"))
                    .andExpect(jsonPath("$.name").value("Борщ"));
        }
    }

    // ==========================================
    // АНАЛИЗ ГРАНИЧНЫХ ЗНАЧЕНИЙ (BVA)
    // ==========================================
    @Nested
    @DisplayName("📏 BVA: Границы веса, порции и округления")
    class BoundaryValueAnalysisTests {

        @ParameterizedTest
        @CsvSource({
                "100.0, 100.0, 110.0", // Порция 100, Вес 100 -> 110 ккал
                "50.0, 100.0, 55.0",   // Порция 50, Вес 100 -> 55 ккал (половина)
                "200.0, 100.0, 220.0", // Порция 200, Вес 100 -> 220 ккал (в двойне)
                "100.0, 50.0, 55.0",   // Порция 100, Вес 50 -> 55 ккал (половина веса)
                "10.0, 100.0, 11.0"    // Малая порция
        })
        @DisplayName("BVA-1: Расчет КБЖУ на порцию при разных весах и размерах порции")
        void shouldCalculateKbzhuPerPortion(double portion, double weight, double expectedCal) throws Exception {
            DishCreateDto dto = new DishCreateDto();
            dto.setName("Тест веса");
            dto.setPortionSize(portion);
            dto.setIngredients(List.of(ing(chickenId, weight)));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calories").value(expectedCal));
        }

        @Test
        @DisplayName("BVA-2: Округление до 2 знаков")
        void shouldRoundCorrectly() throws Exception {
            // Курица 33.333 ккал/100г. Вес 100г. Порция 100г.
            // Итог: 33.333 -> 33.33
            Long weirdChicken = createProduct("Странная курица", 33.333, 0,0,0);

            DishCreateDto dto = new DishCreateDto();
            dto.setName("Округление");
            dto.setPortionSize(100.0);
            dto.setIngredients(List.of(ing(weirdChicken, 100.0)));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calories").value(33.33));
        }

        @Test
        @DisplayName("BVA-3: Сложный расчет с несколькими ингредиентами")
        void shouldCalculateComplexDish() throws Exception {
            // 100г Курицы (110 ккал) + 10г Масла (90 ккал) = 200 ккал всего.
            // Общий вес = 110г.
            // Порция = 110г.
            // Коэффициент = 110/110 = 1.0.
            // Итог = 200.0 ккал.
            DishCreateDto dto = new DishCreateDto();
            dto.setName("Жареная курица");
            dto.setPortionSize(110.0);
            dto.setIngredients(List.of(
                    ing(chickenId, 100.0),
                    ing(oilId, 10.0)
            ));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calories").value(200.0))
                    .andExpect(jsonPath("$.fats").value(11.0)); // 1г + 10г
        }
    }

    // ==========================================
    // CRUD И БИЗНЕС-ЛОГИКА
    // ==========================================
    @Nested
    @DisplayName("🔄 Полный цикл CRUD")
    class CrudTests {

        @Test
        @DisplayName("CS-1: Create -> Read -> Update -> Delete")
        void fullCrudLifecycle() throws Exception {
            // 1. Create
            DishCreateDto createDto = new DishCreateDto();
            createDto.setName("Борщ");
            createDto.setPortionSize(300.0);
            createDto.setIngredients(List.of(ing(waterId, 250.0), ing(chickenId, 50.0)));

            String createdJson = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Long id = objectMapper.readTree(createdJson).get("id").asLong();

            // Проверка расчета:
            // Water 0 + Chicken (50g * 1.1) = 55 ккал всего.
            // Weight 300g. Portion 300g. Factor 1.0.
            // Result 55.0.
            assertThat(objectMapper.readTree(createdJson).get("calories").asDouble()).isEqualTo(55.0);

            // 2. Read
            mockMvc.perform(get("/api/dishes/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Борщ"));

            // 3. Update
            DishCreateDto updateDto = new DishCreateDto();
            updateDto.setName("Борщ v2");
            updateDto.setPortionSize(300.0);
            updateDto.setIngredients(List.of(ing(chickenId, 100.0))); // Только курица

            mockMvc.perform(put("/api/dishes/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Борщ v2"))
                    .andExpect(jsonPath("$.calories").value(330.0)); // 100г курицы = 110 ккал. Порция 300, вес 100. Factor 3.0. 110*3 = 330?
            // Стоп. Weight=100. Portion=300. Factor = 300/100 = 3.
            // Total Cal = 110. Result = 330.0.
            // Исправим ожидание:
            // Если мы хотим 110 ккал на порцию, нужно либо увеличить вес до 300, либо уменьшить порцию до 100.
            // В данном случае: 100г ингредиентов в 300г порции. Это "разбавленное" блюдо.
            // Тест проверяет именно математику сервиса.

            // 4. Delete
            mockMvc.perform(delete("/api/dishes/{id}", id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/dishes/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("CS-2: Удаление продукта, используемого в блюде (409 Conflict)")
        void shouldReturnConflictWhenDeletingUsedProduct() throws Exception {
            // Создаем блюдо
            DishCreateDto dishDto = new DishCreateDto();
            dishDto.setName("Суп");
            dishDto.setPortionSize(100.0);
            dishDto.setIngredients(List.of(ing(chickenId, 50.0)));

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dishDto)))
                    .andExpect(status().isOk());

            // Пытаемся удалить курицу
            mockMvc.perform(delete("/api/products/{id}", chickenId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.conflictingDishes").isArray());
        }

        @Test
        @DisplayName("CS-3: Пагинация и фильтрация")
        void paginationAndFiltering() throws Exception {
            // Создаем 3 супа
            for (int i = 1; i <= 3; i++) {
                DishCreateDto dto = new DishCreateDto();
                dto.setName("Суп " + i);
                dto.setPortionSize(100.0);
                dto.setIngredients(List.of(ing(waterId, 100.0)));
                mockMvc.perform(post("/api/dishes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isOk());
            }

            // Фильтр по имени
            mockMvc.perform(get("/api/dishes")
                            .param("nameSearch", "Суп")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(3));
        }
    }
}