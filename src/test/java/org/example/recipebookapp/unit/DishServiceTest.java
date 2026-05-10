package org.example.recipebookapp.unit;

import jakarta.persistence.EntityNotFoundException;
import org.example.recipebookapp.api.dto.DishCreateDto;
import org.example.recipebookapp.api.dto.DishResponseDto;
import org.example.recipebookapp.core.service.DishService;
import org.example.recipebookapp.database.entity.Dish;
import org.example.recipebookapp.database.entity.DishIngredient;
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
import org.junit.jupiter.params.provider.ValueSource;
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
 * <h2>Формула расчёта:</h2>
 * <pre>
 * Для каждого ингредиента:
 *   вклад = значение_продукта_на_100г × (вес_ингредиента_в_граммах / 100)
 *
 * Итоговое значение блюда:
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
 * @author Your Name
 * @version 1.0
 * @since 2024-01-01
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
     * Используется шаблонный метод для избежания дублирования кода.
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
        baseDto.setPortionSize(100.0);
        baseDto.setIngredients(List.of(baseIngredientDto));
        baseDto.setCalories(null); // null = триггер авторасчёта
    }

    /**
     * Вспомогательный метод для настройки мока {@code dishRepository.save()}.
     * Имитирует поведение JPA: присваивает ID и заполняет аудиторские поля.
     * Предотвращает {@code NullPointerException} в методе {@code toDto()}.
     *
     * @return сохранённый объект {@code Dish} с заполненными полями
     */
    private Dish mockSuccessfulSave() {
        when(dishRepository.save(any(Dish.class))).thenAnswer(invocation -> {
            Dish saved = invocation.getArgument(0);
            saved.setId(999L); // Имитация генерации ID базой данных
            saved.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
            saved.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
            return saved;
        });
        return Dish.builder().id(999L).build();
    }

    // =========================================================================
    // ГРУППА ТЕСТОВ: Эквивалентное разбиение (Equivalence Partitioning)
    // =========================================================================
    @Nested
    @DisplayName("Эквивалентное разбиение: классы входных данных")
    class EquivalencePartitioningTests {

        /**
         * <b>Класс эквивалентности #1:</b> Валидные данные, авторасчёт КБЖУ.
         * <p>Условие: {@code calories == null} в DTO, список ингредиентов не пуст,
         * все продукты существуют в БД.</p>
         * <p>Ожидаемый результат: КБЖУ рассчитано автоматически по формуле.</p>
         */
        @Test
        @DisplayName("EP-1: Авторасчёт КБЖУ при calories = null (валидные данные)")
        void shouldAutoCalculateKbzhu_WhenCaloriesIsNull_AndDataIsValid() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(baseProduct));
            mockSuccessfulSave();

            // When: 100г продукта с 100 ккал/100г → ожидаем 100.0 ккал
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
         * <p>Условие: {@code calories != null} в DTO.</p>
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

            // Then: должны сохраниться ручные значения, а не рассчитанные
            assertThat(result.getCalories()).isEqualTo(500.0);
            assertThat(result.getProteins()).isEqualTo(50.0);
            // Жиры и углеводы не были переопределены → рассчитались автоматически
            assertThat(result.getFats()).isEqualTo(5.0);
        }

        /**
         * <b>Класс эквивалентности #3:</b> Пустой список ингредиентов.
         * <p>Условие: {@code ingredients == []}.</p>
         * <p>Ожидаемый результат: КБЖУ = 0.0 (нет вклада от ингредиентов).</p>
         * <p>Примечание: валидация {@code @NotEmpty} на уровне DTO может отклонить
         * такой запрос до входа в сервис, но тест проверяет устойчивость логики.</p>
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
         * <b>Класс эквивалентности #4:</b> Продукт не найден в репозитории.
         * <p>Условие: {@code productRepository.findById(id) returns empty}.</p>
         * <p>Ожидаемый результат: выбрасывается {@code EntityNotFoundException}.</p>
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

            // Убеждаемся, что save не был вызван после ошибки
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
         * При расчёте НА ПОРЦИЮ для ОДНОГО ингредиента вес сокращается в формуле.
         * Итог зависит только от калорийности продукта и размера порции:
         * Result = ProductCal × (PortionSize / 100)
         */
        @ParameterizedTest
        @CsvSource({
                "100.0, 100.0, 100.0",  // Порция 100г → 100 × 1.0 = 100.0
                "50.0,  100.0,  50.0",  // Порция 50г  → 100 × 0.5 = 50.0
                "200.0, 100.0, 200.0",  // Порция 200г → 100 × 2.0 = 200.0
                "100.0, 50.0,  100.0",  // Вес 50г не влияет при 1 ингр. → 100 × 1.0 = 100.0
                "1.0,   100.0,  1.0"    // Порция 1г → 100 × 0.01 = 1.0
        })
        @DisplayName("BVA-1: Корректность расчёта при разном весе ингредиента и порции")
        void shouldCalculateCorrectly_ForDifferentIngredientWeights(
                Double portionSize, Double weight, Double expectedCal) {

            // Given
            baseDto.setPortionSize(portionSize);
            baseIngredientDto.setQuantityInGrams(weight);
            when(productRepository.findById(1L)).thenReturn(Optional.of(baseProduct));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories())
                    .as("Калории для порции %.1f г и веса %.1f г", portionSize, weight)
                    .isEqualTo(expectedCal);
        }

        /**
         * <b>Граничные значения для калорийности продукта:</b>
         * <ul>
         *   <li>{@code 0.0} — продукт без калорий (вода, специи)</li>
         *   <li>{@code 0.001} — очень маленькое значение (проверка округления)</li>
         *   <li>{@code 9999.99} — экстремально высокая калорийность</li>
         * </ul>
         */
        @ParameterizedTest
        @CsvSource({
                "0.0,      0.0",      // Нулевая калорийность
                "0.001,    0.0",      // Округление вниз: 0.001 × 1.0 = 0.001 → 0.0
                "0.005,    0.01",     // Округление вверх: 0.005 → 0.01
                "9999.99, 9999.99"    // Максимальное значение
        })
        @DisplayName("BVA-2: Расчёт при экстремальных значениях калорийности продукта")
        void shouldHandleExtremeProductCalories(
                Double productCalories, Double expectedDishCalories) {

            // Given
            Product extremeProduct = Product.builder()
                    .id(2L)
                    .name("Экстремальный продукт")
                    .calories(productCalories)
                    .proteins(0.0).fats(0.0).carbs(0.0)
                    .build();

            baseIngredientDto.setProductId(2L);
            baseIngredientDto.setQuantityInGrams(100.0); // коэффициент = 1.0

            when(productRepository.findById(2L)).thenReturn(Optional.of(extremeProduct));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories()).isEqualTo(expectedDishCalories);
        }

        /**
         * <b>Тесты на точность округления:</b>
         * <p>Метод использует {@code Math.round(val * 100.0) / 100.0},
         * что реализует округление до 2 знаков по правилам математики
         * (0.005 → 0.01, 0.014 → 0.01, 0.015 → 0.02).</p>
         */
        @ParameterizedTest
        @CsvSource({
                // rawResult, expectedAfterRounding
                "123.454, 123.45",   // Округление вниз
                "123.455, 123.46",   // Округление вверх (полуцелое)
                "0.004,   0.0",      // Очень маленькое → 0
                "0.005,   0.01",     // Граница округления
                "999.994, 999.99",   // Большое число, округление вниз
                "999.995, 1000.0"    // Большое число, округление вверх
        })
        @DisplayName("BVA-3: Проверка алгоритма округления до 2 знаков")
        void shouldRoundCorrectly_ToTwoDecimals(
                Double rawResult, Double expectedRounded) {

            // Given: создаём продукт, чей вклад даст нужное "сырое" значение
            // При весе 100г: calories × 1.0 = rawResult → calories = rawResult
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
    // ГРУППА ТЕСТОВ: Сложные сценарии и интеграция
    // =========================================================================
    @Nested
    @DisplayName("Сложные сценарии: несколько ингредиентов, агрегация")
    class ComplexScenariosTests {

        /**
         * Проверка корректного суммирования вкладов от нескольких ингредиентов
         * с последующим пересчётом на размер порции.
         */
        @Test
        @DisplayName("CS-1: Суммирование КБЖУ от нескольких ингредиентов (расчёт НА ПОРЦИЮ)")
        void shouldSumKbzhu_FromMultipleIngredients() {
            // Given: 3 ингредиента
            // Курица: 200г (110 ккал/100г) → Абс: 220 ккал, 46Б, 2Ж
            // Рис:    150г (130 ккал/100г) → Абс: 195 ккал, 4.05Б, 0.45Ж, 42У
            // Масло:   10г (900 ккал/100г) → Абс:  90 ккал, 0Б,   10Ж
            // Общий вес = 360г. Абсолютные суммы: К=505, Б=50.05, Ж=12.45, У=42.0
            // Порция = 100г. Коэффициент = 100 / 360 ≈ 0.27777...
            // Ожидаемое на порцию: К=140.28, Б=13.9, Ж=3.46, У=11.67

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
            baseDto.setPortionSize(100.0);
            baseDto.setCalories(null); // Авторасчёт

            when(productRepository.findById(1L)).thenReturn(Optional.of(chicken));
            when(productRepository.findById(2L)).thenReturn(Optional.of(rice));
            when(productRepository.findById(3L)).thenReturn(Optional.of(oil));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories()).isEqualTo(140.28);
            assertThat(result.getProteins()).isEqualTo(13.9);
            assertThat(result.getFats()).isEqualTo(3.46);
            assertThat(result.getCarbs()).isEqualTo(11.67);
        }

        /**
         * Проверка устойчивости к ингредиентам с нулевыми значениями КБЖУ.
         * Такие ингредиенты (вода, соль) не должны ломать расчёт.
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
                    createIngredientDto(4L, 500.0), // 500г воды
                    createIngredientDto(5L, 10.0)   // 10г соли
            ));

            when(productRepository.findById(4L)).thenReturn(Optional.of(water));
            when(productRepository.findById(5L)).thenReturn(Optional.of(salt));
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
         * Проверка алгебраического суммирования (включая отрицательные значения)
         * с корректным применением коэффициента порции.
         */
        @Test
        @DisplayName("CS-3: Алгебраическое суммирование (в т.ч. отрицательные значения, расчёт НА ПОРЦИЮ)")
        void shouldSumAlgebraically_EvenWithNegativeValues() {
            // Given: База (100г, 100 ккал/100г) + Баг (100г, -50 ккал/100г)
            // Абсолютная сумма: 50 ккал, 5Б, 3Ж
            // Общий вес: 200г. Порция: 100г. Коэффициент = 0.5
            // Ожидаемое на порцию: 25.0 ккал, 2.5Б, 1.5Ж

            Product buggyProduct = Product.builder().id(6L).name("Баг-продукт")
                    .calories(-50.0).proteins(-5.0).fats(-2.0).carbs(-10.0).build();

            baseDto.setIngredients(List.of(
                    createIngredientDto(1L, 100.0),
                    createIngredientDto(6L, 100.0)
            ));
            baseDto.setPortionSize(100.0);

            when(productRepository.findById(1L)).thenReturn(Optional.of(baseProduct));
            when(productRepository.findById(6L)).thenReturn(Optional.of(buggyProduct));
            mockSuccessfulSave();

            // When
            DishResponseDto result = dishService.create(baseDto);

            // Then
            assertThat(result.getCalories()).isEqualTo(25.0);
            assertThat(result.getProteins()).isEqualTo(2.5);
            assertThat(result.getFats()).isEqualTo(1.5);
        }
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Фабричный метод для создания {@code IngredientDto}.
     * Упрощает чтение тестов и уменьшает дублирование кода.
     *
     * @param productId ID продукта
     * @param weight вес в граммах
     * @return настроенный DTO ингредиента
     */
    private DishCreateDto.IngredientDto createIngredientDto(Long productId, Double weight) {
        DishCreateDto.IngredientDto dto = new DishCreateDto.IngredientDto();
        dto.setProductId(productId);
        dto.setQuantityInGrams(weight);
        return dto;
    }

    /**
     * AssertJ-проверка для сравнения значений КБЖУ с допустимой погрешностью.
     * Используется в тестах, где возможны небольшие расхождения из-за
     * особенностей арифметики с плавающей точкой.
     *
     * @param actual фактическое значение
     * @param expected ожидаемое значение
     * @param delta допустимая погрешность
     */
    private void assertKbzhuEquals(Double actual, Double expected, double delta) {
        assertThat(actual)
                .as("Значение КБЖУ с погрешностью %.4f", delta)
                .isCloseTo(expected, org.assertj.core.data.Percentage.withPercentage(delta * 100 / Math.max(expected, 1)));
    }
}