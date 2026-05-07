package org.example.recipebookapp.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.recipebookapp.api.dto.ProductCreateDto;
import org.example.recipebookapp.api.dto.ProductFilterDto;
import org.example.recipebookapp.api.dto.ProductResponseDto;
import org.example.recipebookapp.api.dto.ProductResponseDto;
import org.example.recipebookapp.core.service.DishService;
import org.example.recipebookapp.core.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final DishService dishService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductCreateDto dto) {
        return ResponseEntity.ok(productService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getAll(ProductFilterDto filter,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getAll(filter, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(@PathVariable Long id, @Valid @RequestBody ProductCreateDto dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dishService.checkProductUsage(id); // Проверка перед удалением
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}