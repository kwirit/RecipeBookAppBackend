package org.example.recipebookapp.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.recipebookapp.database.entity.enums.CookingRequirement;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.example.recipebookapp.database.entity.enums.ProductCategory;

import java.util.List;
import java.util.Set;

@Data
public class ProductCreateDto {
    @NotBlank @Size(min = 2)
    private String name;
    @Size(max = 5)
    private List<String> photos;
    @NotNull @Min(0)
    private Double calories;
    @NotNull @Min(0) @Max(100)
    private Double proteins;
    @NotNull @Min(0) @Max(100)
    private Double fats;
    @NotNull @Min(0) @Max(100)
    private Double carbs;
    private String composition;
    @NotNull
    private ProductCategory category;
    @NotNull
    private CookingRequirement cookingRequirement;
    private Set<NutritionFlag> flags;
}