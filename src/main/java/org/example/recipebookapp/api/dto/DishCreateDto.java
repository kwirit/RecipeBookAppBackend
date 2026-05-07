package org.example.recipebookapp.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.recipebookapp.database.entity.enums.DishCategory;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;

import java.util.List;
import java.util.Set;

@Data
public class DishCreateDto {
    @NotBlank @Size(min = 2)
    private String name;
    @Size(max = 5)
    private List<String> photos;
    private Double calories; // nullable, авто-расчёт если null
    private Double proteins;
    private Double fats;
    private Double carbs;
    @NotNull @Positive
    private Double portionSize;
    private DishCategory category; // nullable, авто-определение из названия если null
    private Set<NutritionFlag> flags;
    @NotEmpty
    private List<IngredientDto> ingredients;

    @Data
    public static class IngredientDto {
        @NotNull
        private Long productId;
        @NotNull @Positive
        private Double quantityInGrams;
    }
}