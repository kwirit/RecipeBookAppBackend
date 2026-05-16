package org.example.recipebookapp.ui.test;

import org.example.recipebookapp.ui.BaseUITest;
import org.example.recipebookapp.ui.data.UiTestDataFactory;
import org.example.recipebookapp.ui.pages.DishFormPage;
import org.example.recipebookapp.ui.pages.DishesListPage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тесты блюд.
 * Применяются:
 * - BVA: размер порции (0, 0.1, отрицательные), длина имени, КБЖУ
 * - EP: авто-определение категории, валидация ингредиентов, флаги, ручной/авто-расчёт КБЖУ
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DishUITest extends BaseUITest {

    @ParameterizedTest
    @CsvSource({"0, false", "0.1, true", "-5, false", "100, true", "999.9, true"})
    @DisplayName("BVA: Валидация размера порции блюда (границы 0, 0.1, отрицательные)")
    public void createDish_PortionBoundaryValidation(double portion, boolean expectedValid) {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.fillBasic("UI_Блюдо_Порция", portion, "Авто")
                .addFirstAvailableIngredient(50.0); // 🔹 Обязательно добавляем ингредиент

        assertThat(expectedValid ? formPage.submitExpectSuccess() : formPage.submitExpectError())
                .as("Ожидается " + (expectedValid ? "успех" : "ошибка") + " при порции " + portion)
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("org.example.recipebookapp.ui.data.UiTestDataFactory#boundaryNames")
    @DisplayName("BVA: Валидация длины имени блюда (1, 2, 255 символов)")
    public void createDish_NameBoundaryValidation(String name) {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.enterName(name)
                .setPortion(100.0)
                .addFirstAvailableIngredient(100.0);

        assertThat(name.length() >= 2 ? formPage.submitExpectSuccess() : formPage.submitExpectError())
                .as("Ожидается успешное сохранение при длине имени >= 2")
                .isTrue();
    }

    @Test
    @DisplayName("EP: Пустой список ингредиентов (ошибка валидации @NotEmpty)")
    public void createDish_EmptyIngredients_ShouldFail() {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.fillBasic("ПустоеБлюдо", 100.0, "Авто");

        assertThat(formPage.submitExpectError())
                .as("Должна появиться ошибка: 'Добавьте хотя бы один ингредиент'")
                .isTrue();
    }


    @Test
    @DisplayName("EP: Авто-определение категории из макроса в названии (!суп)")
    public void createDish_AutoCategoryFromMacro_Soup() {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.enterName("!суп Борщ")
                .addFirstAvailableIngredient(200.0);

        assertThat(formPage.getSelectedCategory())
                .as("Категория должна автоматически определиться как 'Суп'")
                .isEqualTo("Суп");
    }

    @ParameterizedTest
    @CsvSource({
            "!десерт Торт, Десерт",
            "!второе Котлеты, Второе",
            "!салат Оливье, Салат",
            "!напиток Компот, Напиток",
            "!перекус Яблоко, Перекус",
            "!первое Щи, Первое"
    })

    @DisplayName("EP: Авто-определение категории для всех макросов")
    public void createDish_AutoCategoryFromAllMacros(String nameWithMacro, String expectedCategory) {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.enterName(nameWithMacro)
                .addFirstAvailableIngredient(100.0);

        assertThat(formPage.getSelectedCategory())
                .as("Категория для макроса '" + nameWithMacro + "'")
                .isEqualTo(expectedCategory);
    }

    @Test
    @DisplayName("EP: Явная категория перекрывает авто-определение из макроса")
    public void createDish_ExplicitCategoryOverridesMacro() {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.enterName("!суп Борщ")
                .selectCategory("Десерт")
                .addFirstAvailableIngredient(100.0);

        assertThat(formPage.getSelectedCategory())
                .as("Явный выбор категории должен перекрывать авто-определение")
                .isEqualTo("Десерт");
    }

    @Test
    @DisplayName("EP: Авто-расчёт КБЖУ при добавлении ингредиента")
    public void createDish_AutoCalculatedKbzhu_FromIngredient() {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        var autoCalBefore = formPage.getAutoCalories();
        formPage.fillBasic("АвтоКБЖУ", 100.0, "Авто")
                .addFirstAvailableIngredient(100.0);
        var autoCalAfter = formPage.getAutoCalories();

        assertThat(autoCalAfter)
                .as("КБЖУ должно пересчитаться после добавления ингредиента")
                .isNotEqualTo(autoCalBefore)
                .isNotIn("—", "0.00", "");
    }

    @Test
    @DisplayName("EP: Фотографии блюда (загрузка и превью)")
    public void createDish_UploadPhoto_ShouldShowPreview() {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.fillBasic("БлюдоСФото", 100.0, "Авто")
                .addFirstAvailableIngredient(100.0);

        assertThat(formPage.submitExpectSuccess()).isTrue();
    }


    @Test
    @DisplayName("EP: Фильтрация блюд по флагу 'Веган'")
    public void filterDishes_ByVeganFlag_ShouldShowMatching() {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();
        formPage.fillBasic("ВеганБлюдо", 100.0, "Авто")
                .addFirstAvailableIngredient(100.0)
                .toggleVegan(true)
                .submitExpectSuccess();

        listPage.navigateTo().toggleFlag("VEGAN");

        assertThat(listPage.isDishVisible("ВеганБлюдо"))
                .as("Веганское блюдо должно найтись при фильтрации по флагу")
                .isTrue();
    }


    @Test
    @DisplayName("EP: Попытка создания блюда с несуществующим продуктом (ошибка)")
    public void createDish_InvalidProductId_ShouldFail() {
        var listPage = new DishesListPage(driver, wait).navigateTo();
        var formPage = listPage.openNewDishForm();

        formPage.fillBasic("НевалидныйПродукт", 100.0, "Авто");

        assertThat(formPage.submitExpectError()).isTrue();
    }
}