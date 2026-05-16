package org.example.recipebookapp.integration;

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
 * Интеграционные тесты блюд.
 * Применяются:
 * - BVA: порция (0, отрицательная, минимальная положительная)
 * - EP: авто-расчёт КБЖУ vs ручной, флаги по ингредиентам, пустой список ингредиентов
 * - Параметризация граничных значений порции
 */
public class DishApiIntegrationTest extends BaseApiIntegrationTest {

    private Long productId;

    @BeforeAll
    public void setupDishTests() {
        // Создаём базовый продукт для использования в блюдах
        productId = given().contentType(ContentType.JSON).body(TestDataFactory.validProduct("БазовыйИнгредиент"))
                .when().post("/products").then().extract().jsonPath().getLong("id");
        createdProductIds.add(productId);
    }

    @ParameterizedTest
    @MethodSource("org.example.recipebookapp.util.TestDataFactory#boundaryPortions")
    @DisplayName("BVA: Валидация размера порции (границы)")
    public void createDish_PortionBoundaryValidation(Double portion) {
        Map<String, Object> payload = TestDataFactory.validDish("ТестПорции_" + portion, List.of(), productId);
        payload.put("portionSize", portion);

        if (portion <= 0) {
            given().contentType(ContentType.JSON).body(payload).when().post("/dishes")
                    .then().statusCode(400);
        } else {
            int id = given().contentType(ContentType.JSON).body(payload).when().post("/dishes")
                    .then().statusCode(200).extract().jsonPath().getInt("id");
            createdDishIds.add((long) id);
        }
    }

    @Test
    @DisplayName("EP: Авто-расчёт КБЖУ при создании блюда")
    public void createDish_AutoCalculateKbzhu() {
        Map<String, Object> payload = Map.of(
                "name", "АвтоРасчётБлюдо",
                "portionSize", 200.0,
                "ingredients", List.of(Map.of("productId", productId, "quantityInGrams", 100.0))
        );

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/dishes")
                .then().statusCode(200)
                .body("calories", notNullValue())
                .body("proteins", notNullValue())
                .body("ingredients.size()", equalTo(1));
    }

    @Test
    @DisplayName("EP: Ручная корректировка КБЖУ (override)")
    public void updateDish_ManualKbzhuOverride() {
        int id = given().contentType(ContentType.JSON).body(TestDataFactory.validDish("OverrideТест", List.of(), productId))
                .when().post("/dishes").then().extract().jsonPath().getInt("id");
        createdDishIds.add((long) id);

        Map<String, Object> updatePayload = Map.of(
                "name", "OverrideТест_Изменен",
                "portionSize", 200.0,
                "calories", 500.0,
                "proteins", 20.0,
                "fats", 10.0,
                "carbs", 50.0,
                "ingredients", List.of(Map.of("productId", productId, "quantityInGrams", 100.0))
        );

        given().contentType(ContentType.JSON).body(updatePayload)
                .when().put("/dishes/" + id)
                .then().statusCode(200)
                .body("calories", equalTo(500.0F))
                .body("name", containsString("Изменен"));
    }

    @Test
    @DisplayName("EP: Пустой список ингредиентов при создании (ошибка валидации)")
    public void createDish_EmptyIngredients_ShouldFail() {
        Map<String, Object> payload = Map.of(
                "name", "ПустоеБлюдо",
                "portionSize", 100.0,
                "ingredients", List.of()
        );
        given().contentType(ContentType.JSON).body(payload)
                .when().post("/dishes")
                .then().statusCode(400);
    }
}