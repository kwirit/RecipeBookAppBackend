package org.example.recipebookapp.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.recipebookapp.database.entity.enums.DishCategory;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "dishes")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Dish {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dish_photos", joinColumns = @JoinColumn(name = "dish_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Column(nullable = false)
    private Double calories;

    @Column(nullable = false)
    private Double proteins;

    @Column(nullable = false)
    private Double fats;

    @Column(nullable = false)
    private Double carbs;

    @Column(nullable = false)
    private Double portionSize; // в граммах

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DishCategory category;

    @ElementCollection(targetClass = NutritionFlag.class)
    @CollectionTable(name = "dish_flags", joinColumns = @JoinColumn(name = "dish_id"))
    @Column(name = "flag")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<NutritionFlag> flags = new HashSet<>();

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DishIngredient> ingredients = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addIngredient(DishIngredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setDish(this);
    }
}