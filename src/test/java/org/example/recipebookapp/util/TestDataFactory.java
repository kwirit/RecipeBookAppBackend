package org.example.recipebookapp.util;

import java.util.*;

public final class TestDataFactory {

    // ==================== PRODUCT ====================
    public static Map<String, Object> validProduct(String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("calories", 150.0);
        map.put("proteins", 10.0);
        map.put("fats", 5.0);
        map.put("carbs", 20.0); // сумма = 35 <= 100
        map.put("category", "VEGETABLES");
        map.put("cookingRequirement", "READY_TO_EAT");
        map.put("flags", new HashSet<>(List.of("VEGAN"))); // mutable Set
        map.put("photos", new ArrayList<>()); // mutable List
        return map;
    }

    public static Map<String, Object> productWithBjuSumOver100() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Тест_Продукт_БЖУ");
        map.put("calories", 999.0);
        map.put("proteins", 50.0);
        map.put("fats", 40.0);
        map.put("carbs", 20.0); // сумма = 110 > 100
        map.put("category", "MEAT");
        map.put("cookingRequirement", "SEMI_FINISHED");
        map.put("flags", new HashSet<>());
        return map;
    }

    // BVA для порции/веса
    public static List<Object> boundaryPortions() {
        return List.of(0.0, 0.1, -5.0, 100.0);
    }

    // BVA для длины имени
    public static List<Object> boundaryNames() {
        return List.of("А", "Аа", "АаБбВвГгДдЕеЖжЗзИиКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя");
    }

    public static Map<String, Object> validDish(String name, List<Map<String, Object>> ingredients, Long productId) {
        List<Map<String, Object>> mutableIngredients = new ArrayList<>();
        if (ingredients == null || ingredients.isEmpty()) {
            Map<String, Object> ing = new HashMap<>();
            ing.put("productId", productId);
            ing.put("quantityInGrams", 150.0);
            mutableIngredients.add(ing);
        } else {
            // Копируем в mutable-список
            for (Map<String, Object> ing : ingredients) {
                mutableIngredients.add(new HashMap<>(ing));
            }
        }

        Map<String, Object> dish = new HashMap<>();
        dish.put("name", name);
        dish.put("portionSize", 300.0);
        dish.put("ingredients", mutableIngredients);
        dish.put("category", "SOUP");
        dish.put("flags", new HashSet<>());
        return dish;
    }

    // EP: комбинации флагов
    public static List<Set<String>> flagPartitions() {
        return List.of(
                new HashSet<>(),
                new HashSet<>(List.of("VEGAN")),
                new HashSet<>(List.of("GLUTEN_FREE", "SUGAR_FREE")),
                new HashSet<>(List.of("VEGAN", "GLUTEN_FREE", "SUGAR_FREE"))
        );
    }
}