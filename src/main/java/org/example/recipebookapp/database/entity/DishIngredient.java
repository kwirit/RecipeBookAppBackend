package org.example.recipebookapp.database.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dish_ingredients", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"dish_id", "product_id"})
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DishIngredient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Double quantityInGrams;
}