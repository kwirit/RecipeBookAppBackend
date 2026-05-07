package org.example.recipebookapp.api.dto;

import lombok.Data;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;

import java.util.List;
import java.util.Set;

@Data
public class ProductFilterDto {
    private String nameSearch;
    private String category;
    private String cookingRequirement;
    private Set<NutritionFlag> flags;
    private String sortBy; // name, calories, proteins, fats, carbs
    private String sortDir; // asc, desc
}