package org.example.recipebookapp.api.dto;


import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class ProductResponseDto {
    private Long id;
    private String name;
    private List<String> photos;
    private Double calories;
    private Double proteins;
    private Double fats;
    private Double carbs;
    private String composition;
    private String category;
    private String cookingRequirement;
    private Set<String> flags;
    private String createdAt;
    private String updatedAt;
}