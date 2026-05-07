package org.example.recipebookapp.core.specifications;

import jakarta.persistence.criteria.Predicate;
import org.example.recipebookapp.database.entity.Dish;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DishSpecifications {
    public static Specification<Dish> filter(String nameSearch, String category, List<NutritionFlag> flags) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(nameSearch)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameSearch.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (!CollectionUtils.isEmpty(flags)) {
                for (NutritionFlag flag : flags) {
                    predicates.add(cb.isMember(flag, root.get("flags")));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}