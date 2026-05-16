package org.example.recipebookapp.integration;

import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс для интеграционных тестов.
 * - @SpringBootTest поднимает реальный Spring Context с веб-сервером
 * - Тесты работают против реального API (без моков/изоляций)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseApiIntegrationTest {

    @LocalServerPort
    protected int port;

    protected String baseUrl;
    protected static final List<Long> createdProductIds = new ArrayList<>();
    protected static final List<Long> createdDishIds = new ArrayList<>();


    @BeforeAll
    public void setupRestAssured() {
        baseUrl = "http://localhost:" + port + "/api";
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    public void cleanupTestData() {
        for (Long id : new ArrayList<>(createdDishIds)) {
            try {
                RestAssured.given()
                        .when().delete("/dishes/" + id)
                        .then().statusCode(anyOf(equalTo(204), equalTo(404))); // 404 — если уже удалено
            } catch (Exception e) {
                // Логируем, но не прерываем очистку
                System.err.println("⚠️ Не удалось удалить блюдо " + id + ": " + e.getMessage());
            }
        }

        for (Long id : new ArrayList<>(createdProductIds)) {
            try {
                RestAssured.given()
                        .when().delete("/products/" + id)
                        .then().statusCode(anyOf(equalTo(204), equalTo(404), equalTo(409))); // 409 — если где-то ещё используется
            } catch (Exception e) {
                System.err.println("⚠️ Не удалось удалить продукт " + id + ": " + e.getMessage());
            }
        }

        // 3. Очищаем списки
        createdDishIds.clear();
        createdProductIds.clear();
    }
}