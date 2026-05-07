package org.example.recipebookapp.core.specifications;

import jakarta.persistence.criteria.Predicate;
import org.example.recipebookapp.api.dto.ProductFilterDto;
import org.example.recipebookapp.database.entity.Product;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecifications {
    public static Specification<Product> fromFilter(ProductFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(filter.getNameSearch())) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.getNameSearch().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getCategory())) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }
            if (StringUtils.hasText(filter.getCookingRequirement())) {
                predicates.add(cb.equal(root.get("cookingRequirement"), filter.getCookingRequirement()));
            }
            if (!CollectionUtils.isEmpty(filter.getFlags())) {
                for (NutritionFlag flag : filter.getFlags()) {
                    predicates.add(cb.isMember(flag, root.get("flags")));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}