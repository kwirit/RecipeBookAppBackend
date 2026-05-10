package org.example.recipebookapp.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.example.recipebookapp.database.entity.Dish;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ProductInUseException extends RuntimeException {
    private final List<DishInfo> conflictingDishes;

    public ProductInUseException(List<Dish> dishes) {
        super("Продукт используется в блюдах");
        this.conflictingDishes = dishes.stream()
                .map(d -> new DishInfo(d.getId(), d.getName(), d.getCategory().toString()))
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class DishInfo {
        private Long id;
        private String name;
        private String category;
    }
}