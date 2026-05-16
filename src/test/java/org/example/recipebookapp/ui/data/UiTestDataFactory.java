package org.example.recipebookapp.ui.data;

import java.util.List;
import java.util.Map;

/**
 * Генератор данных для UI-тестов.
 * Применяет:
 * - BVA: длины строк, диапазоны БЖУ, суммы БЖУ, размеры порций
 * - EP: валидные/невалидные комбинации флагов, категорий, состояний формы
 */
public final class UiTestDataFactory {

    // ==================== PRODUCT ====================
    public static Map<String, Object> validProduct(String name) {
        return Map.of(
                "name", name, "category", "Овощи", "cooking", "READY_TO_EAT",
                "cal", 150.0, "pro", 10.0, "fat", 5.0, "carb", 20.0, "vegan", true
        );
    }

    public static List<Object> boundaryNames() {
        // BVA: 1 символ (невалидно), 2 (валидно), 255 (валидно)
        return List.of("А", "Аа", "A".repeat(255));
    }

    public static List<Object> bjuBoundaryCases() {
        // EP + BVA: [pro, fat, carb, expectedValid]
        return List.of(
                List.of(0.0, 0.0, 0.0, true),       // границы минимума
                List.of(30.0, 30.0, 40.0, true),    // сумма = 100 (граница)
                List.of(30.0, 30.0, 40.1, false),   // сумма = 100.1 (переход границы)
                List.of(100.0, 1.0, 0.0, false)     // >100 на одно поле
        );
    }

    // ==================== DISH ====================
    public static List<Object> portionBoundaryCases() {
        // BVA: порция
        return List.of(0.0, 0.1, 100.0, -5.0);
    }
}