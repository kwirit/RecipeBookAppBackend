package org.example.recipebookapp.integration;

import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Тесты загрузки файлов.
 * Применяется EP: валидное изображение, файл не изображение, большой файл.
 */
public class FileApiIntegrationTest extends BaseApiIntegrationTest {

    /**
     * Создаёт минимальный валидный PNG-файл (1x1 пиксель, прозрачный).
     */
    private byte[] createMinimalPng() {
        String minimalPngBase64 =
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        return Base64.getDecoder().decode(minimalPngBase64);
    }


    @Test
    @DisplayName("EP: Загрузка файла не-изображения (ошибка 400/500)")
    public void uploadFile_NonImage_ShouldFail() throws IOException {
        File tempFile = File.createTempFile("test_doc", ".txt");
        Files.writeString(tempFile.toPath(), "This is not an image");

        byte[] fileBytes = Files.readAllBytes(tempFile.toPath());
        given()
                .multiPart("file", tempFile.getName(), fileBytes, "text/plain")  // ✅ byte[] + contentType
                .when()
                .post("/api/files/upload")
                .then()
                .statusCode(not(200)); // 400 или 500

        tempFile.delete();
    }

    @Test
    @DisplayName("BVA: Загрузка файла >20MB (ошибка валидации)")
    public void uploadFile_TooLarge_ShouldFail() throws IOException {
        // 🔹 Создаём большой файл (21 МБ)
        File largeFile = File.createTempFile("large_test", ".jpg");
        // Записываем 21 МБ нулей (быстро, без реального заполнения)
        try (var fos = new java.io.FileOutputStream(largeFile)) {
            byte[] chunk = new byte[1024 * 1024]; // 1 МБ
            for (int i = 0; i < 21; i++) {
                fos.write(chunk);
            }
        }

        // 🔹 Вариант 3: MultiPartSpecification для полного контроля
        byte[] largeBytes = Files.readAllBytes(largeFile.toPath());
        MultiPartSpecification largePart = new MultiPartSpecBuilder(largeBytes)
                .fileName(largeFile.getName())
                .controlName("file")
                .mimeType("image/jpeg")
                .build();

        given()
                .multiPart(largePart)
                .when()
                .post("/api/files/upload")
                .then()
                .statusCode(not(200)); // 400 или 500

        largeFile.delete();
    }
}