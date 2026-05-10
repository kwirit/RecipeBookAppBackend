package org.example.recipebookapp.database.repository;

import org.example.recipebookapp.database.entity.DishIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DishIngredientRepository extends JpaRepository<DishIngredient, Long> {
    Optional<DishIngredient> findByDishIdAndProductId(Long dishId, Long productId);
}
