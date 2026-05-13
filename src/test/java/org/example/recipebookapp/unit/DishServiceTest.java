package org.example.recipebookapp.unit;

import jakarta.persistence.EntityNotFoundException;
import org.example.recipebookapp.api.dto.DishCreateDto;
import org.example.recipebookapp.api.dto.DishResponseDto;
import org.example.recipebookapp.core.service.DishService;
import org.example.recipebookapp.database.entity.Dish;
import org.example.recipebookapp.database.entity.Product;
import org.example.recipebookapp.database.repository.DishIngredientRepository;
import org.example.recipebookapp.database.repository.DishRepository;
import org.example.recipebookapp.database.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h1>Unit-тесты для DishService</h1>
 *
 * <p>Данный класс покрывает тестами метод автоматического расчёта КБЖУ блюда
 * {@code calculateKbzhuDish(Dish)}, который вызывается внутри публичных методов
 * {@code create()} и {@code update()}.</p>
 *
 * <h2>Формула расчёта (Абсолютная):</h2>
 * <pre>
 * Для каждого ингредиента:
 *   вклад = значение_продукта_на_100г × (вес_ингредиента_в_граммах / 100)
 *
 * Итоговое значение блюда (Сумма всех ингредиентов):
 *   КБЖУ = round(Σ(вклады всех ингредиентов) × 100) / 100
 * </pre>
 *
 * <h2>Применённые техники тест-дизайна:</h2>
 * <ul>
 *   <li><b>Эквивалентное разбиение (EP)</b>: выделение классов входных данных
 *       (валидные/невалидные ингредиенты, пустой список, ручное переопределение)</li>
 *   <li><b>Анализ граничных значений (BVA)</b>: тестирование значений на границах
 *       допустимых диапазонов (0г, 1г, 100г, большие числа, пограничные случаи округления)</li>
 * </ul>
 *
 * <h2>Инструменты:</h2>
 * <ul>
 *   <li>JUnit 5: {@code @Test}, {@code @ParameterizedTest}, {@code @Nested}, {@code @CsvSource}</li>
 *   <li>Mockito: {@code @Mock}, {@code @InjectMocks}, {@code when/thenAnswer}</li>
 *   <li>AssertJ: fluent assertions для читаемости проверок</li>
 * </ul>
 *
 * @author a.erkalov
 * @version 2.0 (Absolute Calculation)
 * @since 2026-01-05
 */
@ExtendWith(MockitoExtension.class)
class DishServiceTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DishIngredientRepository dishIngredientRepository;

    @InjectMocks
    private DishService dishService;

    private Product baseProduct;
    private DishCreateDto baseDto;
    private DishCreateDto.IngredientDto baseIngredientDto;

    /**
     * Подготовка базовых тестовых данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        // Базовый продукт: 100 ккал, 10 Б, 5 Ж, 20 У на 100г
        baseProduct = Product.builder()
                .id(1L)
                .name("Тестовый продукт")
                .calories(100.0)
                .proteins(10.0)
                .fats(5.0)
                .carbs(20.0)
                .build();

        baseIngredientDto = new DishCreateDto.IngredientDto();
        baseIngredientDto.setProductId(1L);
        baseIngredientDto.setQuantityInGrams(100.0); // Базовый вес 100г

        baseDto = new DishCreateDto();
        baseDto.setName("Тестовое блюдо");
        baseDto.setPortionSize(100.0); // Порция не влияет на абсолютный расчет, но нужна для DTO
        baseDto.setIngredients(List.of(baseIngredientDto));
        baseDto.setCalories(null); // null = триггер авторасчёта
    }

    /**
     * Вспомогательный метод для настройки мока {@code dishRepository.save()}.
     * Имитирует поведение JPA: присваивает ID и заполняет аудиторские поля.
     */
    private Dish mockSuccessfulSave() {
        when(dishRepository.save(any(Dish.class))).thenAnswer(invocation -> {
            Dish saved = invocation.getArgument(0);
            saved.setId(999L);
            saved.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
            saved.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
            return saved;
        });
        return Dish.builder().id(999L).build();
    }

    @Nested
    @DisplayName("Эквивалентное разбиение: классы входных данных")
    class EquivalencePartitioningTests {

        /**
         * <b>Класс эквивалентности #1:</b> Валидные данные, авторасчёт КБЖУ.
         * <p>Ожидаемый результат: КБЖУ рассчитано как сумма вкладов.</p>
         */
        @Test
        @DisplayName("EP-1: Авторасчёт КБЖУ при calories = null (валидные данные)")
        void shouldAutoCalculateKbzhu_WhenCaloriesIsNull_AndDataIsValid() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(baseProduct));
            mockSuccessfulSave();

            // When: 100г продукта с 100 ккал/100г → ожидаем 100.0 ккал (абсолютно)
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories()).isEqualTo(100.0);
            assertThat(result.getProteins()).isEqualTo(10.0);
            assertThat(result.getFats()).isEqualTo(5.0);
            assertThat(result.getCarbs()).isEqualTo(20.0);
            verify(dishRepository, times(1)).save(any(Dish.class));
        }

        /**
         * <b>Класс эквивалентности #2:</b> Ручной ввод КБЖУ.
         * <p>Ожидаемый результат: значение из DTO перезаписывает результат авторасчёта.</p>
         */
        @Test
        @DisplayName("EP-2: Ручное значение КБЖУ имеет приоритет над авторасчётом")
        void shouldUseManualKbzhu_WhenCaloriesIsNotNull() {
            // Given
            baseDto.setCalories(500.0); // Пользователь ввёл вручную
            baseDto.setProteins(50.0);
            when(productRepository.findById(1L)).thenReturn(Optional.of(baseProduct));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories()).isEqualTo(500.0);
            assertThat(result.getProteins()).isEqualTo(50.0);
            // Жиры и углеводы не были переопределены → рассчитались автоматически
            assertThat(result.getFats()).isEqualTo(5.0);
        }

        /**
         * <b>Класс эквивалентности #3:</b> Пустой список ингредиентов.
         * <p>Ожидаемый результат: КБЖУ = 0.0.</p>
         */
        @Test
        @DisplayName("EP-3: КБЖУ = 0 при пустом списке ингредиентов")
        void shouldReturnZeroKbzhu_WhenIngredientsListIsEmpty() {
            // Given
            baseDto.setIngredients(Collections.emptyList());
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories()).isEqualTo(0.0);
            assertThat(result.getProteins()).isEqualTo(0.0);
            assertThat(result.getFats()).isEqualTo(0.0);
            assertThat(result.getCarbs()).isEqualTo(0.0);
        }

        /**
         * <b>Класс эквивалентности #4:</b> Продукт не найден.
         */
        @Test
        @DisplayName("EP-4: Исключение при отсутствии продукта в БД")
        void shouldThrowException_WhenProductNotFound() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> dishService.create(baseDto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Продукт не найден");

            verify(dishRepository, never()).save(any(Dish.class));
        }
    }

    // =========================================================================
    // ГРУППА ТЕСТОВ: Анализ граничных значений (Boundary Value Analysis)
    // =========================================================================
    @Nested
    @DisplayName("Анализ граничных значений: вес и точность расчётов")
    class BoundaryValueAnalysisTests {

        /**
         * Проверка влияния веса ингредиента на абсолютную сумму.
         * Формула: Result = ProductCal × (Weight / 100)
         */
        @ParameterizedTest
        @CsvSource({
                "100.0, 100.0",  // 100г → 100 * 1.0 = 100.0
                "50.0,  50.0",   // 50г  → 100 * 0.5 = 50.0
                "200.0, 200.0",  // 200г → 100 * 2.0 = 200.0
                "1.0,   1.0",    // 1г   → 100 * 0.01 = 1.0
                "0.0,   0.0"     // 0г   → 0
        })
        @DisplayName("BVA-1: Корректность расчёта при разном весе ингредиента")
        void shouldCalculateCorrectly_ForDifferentIngredientWeights(Double weight, Double expectedCal) {

            // Given
            baseIngredientDto.setQuantityInGrams(weight);
            when(productRepository.findById(1L)).thenReturn(Optional.of(baseProduct));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories())
                    .as("Калории для веса %.1f г", weight)
                    .isEqualTo(expectedCal);
        }

        /**
         * <b>Тесты на точность округления:</b>
         */
        @ParameterizedTest
        @CsvSource({
                "123.454, 123.45",   // Округление вниз
                "123.455, 123.46",   // Округление вверх (полуцелое)
                "0.004,   0.0",      // Очень маленькое → 0
                "0.005,   0.01",     // Граница округления
                "999.994, 999.99",   // Большое число, округление вниз
                "999.995, 1000.0"    // Большое число, округление вверх
        })
        @DisplayName("BVA-3: Проверка алгоритма округления до 2 знаков")
        void shouldRoundCorrectly_ToTwoDecimals(Double rawResult, Double expectedRounded) {

            // Given
            Product roundingProduct = Product.builder()
                    .id(3L)
                    .name("Продукт для теста округления")
                    .calories(rawResult)
                    .proteins(0.0).fats(0.0).carbs(0.0)
                    .build();

            baseIngredientDto.setProductId(3L);
            baseIngredientDto.setQuantityInGrams(100.0);

            when(productRepository.findById(3L)).thenReturn(Optional.of(roundingProduct));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories())
                    .as("Округление значения %.3f", rawResult)
                    .isEqualTo(expectedRounded);
        }
    }

    // =========================================================================
    // ГРУППА ТЕСТОВ: Сложные сценарии
    // =========================================================================
    @Nested
    @DisplayName("Сложные сценарии: несколько ингредиентов, агрегация")
    class ComplexScenariosTests {

        /**
         * Проверка корректного суммирования вкладов от нескольких ингредиентов.
         * Расчет АБСОЛЮТНЫЙ (сумма всех ингредиентов).
         */
        @Test
        @DisplayName("CS-1: Суммирование КБЖУ от нескольких ингредиентов (Абсолютный расчет)")
        void shouldSumKbzhu_FromMultipleIngredients() {
            // Given: 3 ингредиента
            // Курица: 200г (110 ккал/100г) → Вклад: 220 ккал
            // Рис:    150г (130 ккал/100г) → Вклад: 195 ккал
            // Масло:   10г (900 ккал/100г) → Вклад:  90 ккал
            // Итого: 220 + 195 + 90 = 505 ккал

            Product chicken = Product.builder().id(1L).name("Курица")
                    .calories(110.0).proteins(23.0).fats(1.0).carbs(0.0).build();
            Product rice = Product.builder().id(2L).name("Рис")
                    .calories(130.0).proteins(2.7).fats(0.3).carbs(28.0).build();
            Product oil = Product.builder().id(3L).name("Масло")
                    .calories(900.0).proteins(0.0).fats(100.0).carbs(0.0).build();

            List<DishCreateDto.IngredientDto> ingredients = List.of(
                    createIngredientDto(1L, 200.0),
                    createIngredientDto(2L, 150.0),
                    createIngredientDto(3L, 10.0)
            );

            baseDto.setIngredients(ingredients);
            baseDto.setCalories(null); // Авторасчёт

            when(productRepository.findById(1L)).thenReturn(Optional.of(chicken));
            when(productRepository.findById(2L)).thenReturn(Optional.of(rice));
            when(productRepository.findById(3L)).thenReturn(Optional.of(oil));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then: Ожидаем абсолютную сумму
            assertThat(result.getCalories()).isEqualTo(505.0);

            // Белки: (23*2) + (2.7*1.5) + 0 = 46 + 4.05 = 50.05
            assertThat(result.getProteins()).isEqualTo(50.05);

            // Жиры: (1*2) + (0.3*1.5) + (100*0.1) = 2 + 0.45 + 10 = 12.45
            assertThat(result.getFats()).isEqualTo(12.45);

            // Углеводы: 0 + (28*1.5) + 0 = 42.0
            assertThat(result.getCarbs()).isEqualTo(42.0);
        }

        /**
         * Проверка устойчивости к ингредиентам с нулевыми значениями КБЖУ.
         */
        @Test
        @DisplayName("CS-2: Ингредиенты с нулевыми КБЖУ не влияют на расчёт")
        void shouldIgnoreZeroKbzhuIngredients() {
            // Given
            Product water = Product.builder().id(4L).name("Вода")
                    .calories(0.0).proteins(0.0).fats(0.0).carbs(0.0).build();
            Product salt = Product.builder().id(5L).name("Соль")
                    .calories(0.0).proteins(0.0).fats(0.0).carbs(0.0).build();

            baseDto.setIngredients(List.of(
                    createIngredientDto(4L, 500.0),
                    createIngredientDto(5L, 10.0)
            ));

            when(productRepository.findById(4L)).thenReturn(Optional.of(water));
            when(productRepository.findById(5L)).thenReturn(Optional.of(salt));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories()).isEqualTo(0.0);
        }
    }

    private DishCreateDto.IngredientDto createIngredientDto(Long productId, Double weight) {
        DishCreateDto.IngredientDto dto = new DishCreateDto.IngredientDto();
        dto.setProductId(productId);
        dto.setQuantityInGrams(weight);
        return dto;
    }
}