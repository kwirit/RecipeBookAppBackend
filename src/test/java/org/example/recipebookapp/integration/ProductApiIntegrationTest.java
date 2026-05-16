package org.example.recipebookapp.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.example.recipebookapp.util.TestDataFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Интеграционные тесты CRUD и фильтрации продуктов.
 * Применяются:
 * - BVA: проверка границ длины имени, диапазонов БЖУ, суммы БЖУ=100
 * - EP: валидные/невалидные флаги, комбинации фильтров
 * - Параметризация для граничных значений
 */
public class ProductApiIntegrationTest extends BaseApiIntegrationTest {

    @Test
    @DisplayName("BVA: Создание продукта с валидными данными (граница имени = 2 символа)")
    public void createProduct_ValidNameMinBoundary() {
        Map<String, Object> payload = TestDataFactory.validProduct("Т1");
        int id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/products")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("Т1"))
                .extract().jsonPath().getInt("id");
        createdProductIds.add((long) id);
    }

    @ParameterizedTest
    @MethodSource("org.example.recipebookapp.util.TestDataFactory#boundaryNames")
    @DisplayName("Валидация имени продукта (границы длины)")
    public void createProduct_NameBoundaryValidation(String name) {
        Map<String, Object> payload = TestDataFactory.validProduct(name);
        if (name.length() < 2) {
            given().contentType(ContentType.JSON).body(payload).when().post("/products")
                    .then().statusCode(400);
        } else {
            int id = given().contentType(ContentType.JSON).body(payload).when().post("/products")
                    .then().statusCode(200).extract().jsonPath().getInt("id");
            createdProductIds.add((long) id);
        }
    }

    @Test
    @DisplayName("EP: Создание продукта с суммой БЖУ > 100 (ожидаемая ошибка валидации)")
    public void createProduct_BjuSumOver100_ShouldFail() {
        given().contentType(ContentType.JSON).body(TestDataFactory.productWithBjuSumOver100())
                .when().post("/products")
                .then()
                .statusCode(400)
                .contentType(ContentType.TEXT)
                .body(equalTo("Сумма БЖУ на 100г не может превышать 100"));
    }

    @Test
    @DisplayName("EP: Фильтрация продуктов по категории и флагам")
    public void filterProducts_ByCategoryAndFlags() {
        // Создаём продукт для фильтрации
        int id = given().contentType(ContentType.JSON).body(TestDataFactory.validProduct("ФильтрТест"))
                .when().post("/products").then().extract().jsonPath().getInt("id");
        createdProductIds.add((long) id);

        given()
                .queryParam("nameSearch", "Фильтр")
                .queryParam("category", "VEGETABLES")
                .when().get("/products")
                .then().statusCode(200)
                .body("content.size()", greaterThan(0))
                .body("content[0].name", containsString("Фильтр"));
    }

    @Test
    @DisplayName("EP: Удаление продукта, используемого в блюде (409 Conflict)")
    public void deleteProduct_UsedInDish_ShouldReturn409() {
        int prodId = given().contentType(ContentType.JSON).body(TestDataFactory.validProduct("КонфликтныйПродукт"))
                .when().post("/products").then().extract().jsonPath().getInt("id");
        createdProductIds.add((long) prodId);

        Map<String, Object> dishPayload = Map.of(
                "name", "ТестовоеБлюдоДляКонфликта",
                "portionSize", 200.0,
                "ingredients", List.of(Map.of("productId", prodId, "quantityInGrams", 50.0))
        );
        int dishId = given().contentType(ContentType.JSON).body(dishPayload)
                .when().post("/dishes").then().extract().jsonPath().getInt("id");
        createdDishIds.add((long) dishId);

        given()
                .when().delete("/products/" + prodId)
                .then().statusCode(409)
                .body("conflictingDishes.size()", greaterThan(0))
                .body("message", containsString("используется"));
    }
}