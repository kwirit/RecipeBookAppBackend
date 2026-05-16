package org.example.recipebookapp.ui.test;

import org.example.recipebookapp.ui.BaseUITest;
import org.example.recipebookapp.ui.data.UiTestDataFactory;
import org.example.recipebookapp.ui.pages.ProductFormPage;
import org.example.recipebookapp.ui.pages.ProductsListPage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тесты продуктов.
 * Применяются:
 * - BVA: длина имени, границы БЖУ, сумма БЖУ=100
 * - EP: валидные/невалидные сценарии валидации, комбинации флагов
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductUITest extends BaseUITest {

    @Test
    @Order(1)
    @DisplayName("EP: Успешное создание продукта с валидными данными")
    public void createProduct_ValidData_ShouldShowSuccess() {
        ProductsListPage listPage = new ProductsListPage(driver, wait);
        listPage.navigateTo();

        ProductFormPage formPage = listPage.openNewProductForm();
        Map<String, Object> data = UiTestDataFactory.validProduct("UI_Тест_Продукт");
        formPage.fillProduct((String) data.get("name"), (String) data.get("category"),
                (String) data.get("cooking"), (double) data.get("cal"), (double) data.get("pro"),
                (double) data.get("fat"), (double) data.get("carb"), (boolean) data.get("vegan"));

        assertThat(formPage.submitExpectSuccess())
                .as("Должен появиться toast успешного сохранения")
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("org.example.recipebookapp.ui.data.UiTestDataFactory#boundaryNames")
    @Order(2)
    @DisplayName("BVA: Валидация длины имени продукта (1, 2, 255 символов)")
    public void createProduct_NameBoundaryValidation(String name) {
        ProductsListPage listPage = new ProductsListPage(driver, wait);
        listPage.navigateTo();
        ProductFormPage formPage = listPage.openNewProductForm();

        Map<String, Object> data = UiTestDataFactory.validProduct(name);
        formPage.fillProduct((String) data.get("name"), (String) data.get("category"),
                (String) data.get("cooking"), (double) data.get("cal"), (double) data.get("pro"),
                (double) data.get("fat"), (double) data.get("carb"), (boolean) data.get("vegan"));

        assertThat(name.length() >= 2 ? formPage.submitExpectSuccess() : formPage.submitExpectError())
                .as("Ожидается успешное сохранение при длине имени >= 2")
                .isTrue();
    }

    @ParameterizedTest
    @CsvSource({"0.0, 0.0, 0.0, true", "30.0, 30.0, 40.0, true", "30.0, 30.0, 40.1, false", "100.0, 1.0, 0.0, false"})
    @Order(3)
    @DisplayName("BVA/EP: Валидация БЖУ (границы 0, 100, сумма 100/101)")
    public void createProduct_BjuBoundaryValidation(double pro, double fat, double carb, boolean expectedValid) {
        ProductsListPage listPage = new ProductsListPage(driver, wait);
        listPage.navigateTo();
        ProductFormPage formPage = listPage.openNewProductForm();

        Map<String, Object> data = UiTestDataFactory.validProduct("БЖУ_Тест");
        formPage.fillProduct((String) data.get("name"), (String) data.get("category"),
                (String) data.get("cooking"), (double) data.get("cal"), pro, fat, carb, (boolean) data.get("vegan"));

        assertThat(expectedValid ? formPage.submitExpectSuccess() : formPage.submitExpectError())
                .as("Ожидается успешное сохранение при валидных значениях БЖУ")
                .isTrue();
    }
}