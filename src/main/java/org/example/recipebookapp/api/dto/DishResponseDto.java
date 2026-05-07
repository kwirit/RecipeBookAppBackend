package org.example.recipebookapp.api.dto;


import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class DishResponseDto {
    private Long id;
    private String name;
    private List<String> photos;
    private Double calories;
    private Double proteins;
    private Double fats;
    private Double carbs;
    private Double portionSize;
    private String category;
    private Set<String> flags;
    private List<IngredientResponseDto> ingredients;
    private String createdAt;
    private String updatedAt;

    @Data
    public static class IngredientResponseDto {
        private Long productId;
        private String productName;
        private Double quantityInGrams;
    }
}