package org.example.recipebookapp.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.recipebookapp.api.dto.DishCreateDto;
import org.example.recipebookapp.api.dto.DishResponseDto;
import org.example.recipebookapp.core.service.DishService;
import org.example.recipebookapp.database.entity.enums.NutritionFlag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;

    @PostMapping
    public ResponseEntity<DishResponseDto> create(@Valid @RequestBody DishCreateDto dto) {
        return ResponseEntity.ok(dishService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<DishResponseDto>> getAll(
            @RequestParam(required = false) String nameSearch,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<NutritionFlag> flags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(dishService.getAll(nameSearch, category, flags, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(dishService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DishResponseDto> update(@PathVariable Long id, @Valid @RequestBody DishCreateDto dto) {
        return ResponseEntity.ok(dishService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return ResponseEntity.noContent().build();
    }
}