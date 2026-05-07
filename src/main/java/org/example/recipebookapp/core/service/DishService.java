package org.example.recipebookapp.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.recipebookapp.api.dto.DishCreateDto;
import org.example.recipebookapp.api.dto.DishResponseDto;
import org.example.recipebookapp.core.specifications.DishSpecifications;
import org.example.recipebookapp.database.entity.Dish;
import org.example.recipebookapp.database.entity.DishIngredient;
import org.example.recipebookapp.database.entity.Product;
import org.example.recipebookapp.database.entity.enums.DishCategory;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.example.recipebookapp.database.repository.DishRepository;
import org.example.recipebookapp.database.repository.ProductRepository;
import org.example.recipebookapp.exception.ProductInUseException;
import org.example.recipebookapp.exception.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {
    private final DishRepository dishRepository;
    private final ProductRepository productRepository;

    private static final Pattern MACRO_PATTERN = Pattern.compile("!(десерт|первое|второе|напиток|салат|суп|перекус)", Pattern.CASE_INSENSITIVE);
    private static final Map<String, DishCategory> MACRO_MAP = Map.of(
            "десерт", DishCategory.DESSERT, "первое", DishCategory.FIRST_COURSE,
            "второе", DishCategory.SECOND_COURSE, "напиток", DishCategory.DRINK,
            "салат", DishCategory.SALAD, "суп", DishCategory.SOUP, "перекус", DishCategory.SNACK
    );

    private void validateBjuPer100g(Double p, Double f, Double c, Double portion) {
        double sumPer100g = (p + f + c) / portion * 100;
        if (sumPer100g > 100.0) {
            throw new ValidationException("Сумма БЖУ на 100г блюда не может превышать 100");
        }
    }

    private String[] parseMacro(String name) {
        Matcher m = MACRO_PATTERN.matcher(name);
        String cleanedName = name;
        DishCategory detected = null;
        if (m.find()) {
            String macro = m.group(1).toLowerCase();
            detected = MACRO_MAP.get(macro);
            cleanedName = name.replaceFirst(Pattern.quote(m.group(0)), "").trim();
        }
        return new String[]{cleanedName, detected != null ? detected.name() : null};
    }

    private Set<NutritionFlag> computeAllowedFlags(List<DishIngredient> ingredients) {
        if (ingredients.isEmpty()) return EnumSet.noneOf(NutritionFlag.class);
        Set<NutritionFlag> allowed = EnumSet.allOf(NutritionFlag.class);
        for (DishIngredient ing : ingredients) {
            allowed.retainAll(ing.getProduct().getFlags());
            if (allowed.isEmpty()) break;
        }
        return allowed;
    }

    private void calculateKbzhuDish(Dish dish) {
        double cal = 0, prot = 0, fat = 0, carb = 0;
        for (DishIngredient ing : dish.getIngredients()) {
            Product p = ing.getProduct();
            double ratio = ing.getQuantityInGrams() / 100.0;
            cal += p.getCalories() * ratio;
            prot += p.getProteins() * ratio;
            fat += p.getFats() * ratio;
            carb += p.getCarbs() * ratio;
        }
        dish.setCalories(Math.round(cal * 100.0) / 100.0);
        dish.setProteins(Math.round(prot * 100.0) / 100.0);
        dish.setFats(Math.round(fat * 100.0) / 100.0);
        dish.setCarbs(Math.round(carb * 100.0) / 100.0);
    }

    private DishResponseDto toDto(Dish d) {
        DishResponseDto dto = new DishResponseDto();
        dto.setId(d.getId());
        dto.setName(d.getName());
        dto.setPhotos(d.getPhotos());
        dto.setCalories(d.getCalories());
        dto.setProteins(d.getProteins());
        dto.setFats(d.getFats());
        dto.setCarbs(d.getCarbs());
        dto.setPortionSize(d.getPortionSize());
        dto.setCategory(d.getCategory().name());
        dto.setFlags(d.getFlags().stream().map(Enum::name).collect(Collectors.toSet()));
        dto.setIngredients(d.getIngredients().stream().map(ing -> {
            DishResponseDto.IngredientResponseDto iDto = new DishResponseDto.IngredientResponseDto();
            iDto.setProductId(ing.getProduct().getId());
            iDto.setProductName(ing.getProduct().getName());
            iDto.setQuantityInGrams(ing.getQuantityInGrams());
            return iDto;
        }).collect(Collectors.toList()));
        dto.setCreatedAt(d.getCreatedAt().toString());
        dto.setUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : null);
        return dto;
    }

    @Transactional
    public DishResponseDto create(DishCreateDto dto) {
        String[] parsed = parseMacro(dto.getName());
        String finalName = parsed[0];
        DishCategory finalCategory = dto.getCategory() != null ? dto.getCategory() :
                (parsed[1] != null ? DishCategory.valueOf(parsed[1]) : DishCategory.SECOND_COURSE);

        Dish dish = Dish.builder()
                .name(finalName)
                .photos(dto.getPhotos() != null ? new ArrayList<>(dto.getPhotos()) : new ArrayList<>())
                .flags(dto.getFlags() != null ? new HashSet<>(dto.getFlags()) : new HashSet<>())
                .portionSize(dto.getPortionSize())
                .category(finalCategory)
                .build();

        List<DishIngredient> ingredients = new ArrayList<>();
        for (DishCreateDto.IngredientDto iDto : dto.getIngredients()) {
            Product product = productRepository.findById(iDto.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Продукт ID " + iDto.getProductId() + " не найден"));
            ingredients.add(DishIngredient.builder().dish(dish).product(product).quantityInGrams(iDto.getQuantityInGrams()).build());
        }
        dish.setIngredients(ingredients);

        calculateKbzhuDish(dish);
        // Разрешаем ручную корректировку, если переданы
        if (dto.getCalories() != null) dish.setCalories(dto.getCalories());
        if (dto.getProteins() != null) dish.setProteins(dto.getProteins());
        if (dto.getFats() != null) dish.setFats(dto.getFats());
        if (dto.getCarbs() != null) dish.setCarbs(dto.getCarbs());

        validateBjuPer100g(dish.getProteins(), dish.getFats(), dish.getCarbs(), dish.getPortionSize());

        Set<NutritionFlag> allowed = computeAllowedFlags(dish.getIngredients());
        Set<NutritionFlag> requested = dto.getFlags() != null ? new HashSet<>(dto.getFlags()) : new HashSet<>();
        requested.retainAll(allowed);
        dish.setFlags(requested);

        return toDto(dishRepository.save(dish));
    }

    @Transactional(readOnly = true)
    public Page<DishResponseDto> getAll(String nameSearch, String category, List<NutritionFlag> flags, int page, int size) {
        return dishRepository.findAll(DishSpecifications.filter(nameSearch, category, flags), PageRequest.of(page, size))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public DishResponseDto getById(Long id) {
        return dishRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Блюдо не найдено"));
    }

    @Transactional
    public DishResponseDto update(Long id, DishCreateDto dto) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Блюдо не найдено"));

        String[] parsed = parseMacro(dto.getName());
        dish.setName(parsed[0]);
        dish.setCategory(dto.getCategory() != null ? dto.getCategory() :
                (parsed[1] != null ? DishCategory.valueOf(parsed[1]) : dish.getCategory()));
        dish.setPhotos(dto.getPhotos() != null ? new ArrayList<>(dto.getPhotos()) : new ArrayList<>());
        dish.setPortionSize(dto.getPortionSize());

        dish.getIngredients().clear();
        for (DishCreateDto.IngredientDto iDto : dto.getIngredients()) {
            Product product = productRepository.findById(iDto.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Продукт ID " + iDto.getProductId() + " не найден"));
            dish.addIngredient(DishIngredient.builder().product(product).quantityInGrams(iDto.getQuantityInGrams()).build());
        }

        calculateKbzhuDish(dish);
        if (dto.getCalories() != null) dish.setCalories(dto.getCalories());
        if (dto.getProteins() != null) dish.setProteins(dto.getProteins());
        if (dto.getFats() != null) dish.setFats(dto.getFats());
        if (dto.getCarbs() != null) dish.setCarbs(dto.getCarbs());

        validateBjuPer100g(dish.getProteins(), dish.getFats(), dish.getCarbs(), dish.getPortionSize());

        Set<NutritionFlag> allowed = computeAllowedFlags(dish.getIngredients());
        Set<NutritionFlag> requested = dto.getFlags() != null ? new HashSet<>(dto.getFlags()) : new HashSet<>();
        requested.retainAll(allowed);
        dish.setFlags(requested);

        return toDto(dishRepository.save(dish));
    }

    @Transactional
    public void delete(Long id) {
        if (!dishRepository.existsById(id)) throw new EntityNotFoundException("Блюдо не найдено");
        dishRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public void checkProductUsage(Long productId) {
        List<Dish> dishes = dishRepository.findByIngredients_ProductId(productId);
        if (!dishes.isEmpty()) {
            List<String> names = dishes.stream().map(Dish::getName).collect(Collectors.toList());
            throw new ProductInUseException(names);
        }
    }
}