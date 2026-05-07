package org.example.recipebookapp.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.recipebookapp.api.dto.ProductCreateDto;
import org.example.recipebookapp.api.dto.ProductFilterDto;
import org.example.recipebookapp.api.dto.ProductResponseDto;
import org.example.recipebookapp.core.specifications.ProductSpecifications;
import org.example.recipebookapp.database.entity.Product;
import org.example.recipebookapp.database.repository.ProductRepository;
import org.example.recipebookapp.exception.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    private void validateBjuSum(Double p, Double f, Double c) {
        if (p + f + c > 100.0) {
            throw new ValidationException("Сумма БЖУ на 100г не может превышать 100");
        }
    }

    private ProductResponseDto toDto(Product p) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPhotos(p.getPhotos());
        dto.setCalories(p.getCalories());
        dto.setProteins(p.getProteins());
        dto.setFats(p.getFats());
        dto.setCarbs(p.getCarbs());
        dto.setComposition(p.getComposition());
        dto.setCategory(p.getCategory().name());
        dto.setCookingRequirement(p.getCookingRequirement().name());
        dto.setFlags(p.getFlags().stream().map(Enum::name).collect(Collectors.toSet()));
        dto.setCreatedAt(p.getCreatedAt().toString());
        dto.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return dto;
    }

    @Transactional
    public ProductResponseDto create(ProductCreateDto dto) {
        validateBjuSum(dto.getProteins(), dto.getFats(), dto.getCarbs());

        Product product = Product.builder()
                .name(dto.getName())
                .calories(dto.getCalories())
                .proteins(dto.getProteins())
                .fats(dto.getFats())
                .carbs(dto.getCarbs())
                .composition(dto.getComposition())
                .category(dto.getCategory())
                .cookingRequirement(dto.getCookingRequirement())
                .build();
        product.setPhotos(dto.getPhotos() != null
                ? new ArrayList<>(dto.getPhotos())
                : new ArrayList<>());
        product.setFlags(dto.getFlags() != null
                ? new HashSet<>(dto.getFlags())
                : new HashSet<>());
        return toDto(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAll(ProductFilterDto filter, int page, int size) {
        Sort sort = Sort.unsorted();
        if (filter.getSortBy() != null) {
            Sort.Direction dir = "desc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(dir, filter.getSortBy());
        }
        return productRepository.findAll(ProductSpecifications.fromFilter(filter), PageRequest.of(page, size, sort))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getById(Long id) {
        return productRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
    }

    @Transactional
    public ProductResponseDto update(Long id, ProductCreateDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
        validateBjuSum(dto.getProteins(), dto.getFats(), dto.getCarbs());

        product.setName(dto.getName());
        product.setPhotos(dto.getPhotos() != null
                ? new ArrayList<>(dto.getPhotos())
                : new ArrayList<>());
        product.setCalories(dto.getCalories());
        product.setProteins(dto.getProteins());
        product.setFats(dto.getFats());
        product.setCarbs(dto.getCarbs());
        product.setComposition(dto.getComposition());
        product.setCategory(dto.getCategory());
        product.setCookingRequirement(dto.getCookingRequirement());
        product.setFlags(dto.getFlags() != null
                ? new HashSet<>(dto.getFlags())
                : new HashSet<>());
        return toDto(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Продукт не найден");
        }
        // Проверка использования в блюдах выполняется в DishService или через репозиторий блюд
        // Для чистоты архитектуры делегируем проверку в DishService или вызываем репозиторий блюд здесь
        // Реализовано в DishService.checkProductUsage(id)
        productRepository.deleteById(id);
    }
}