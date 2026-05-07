package org.example.recipebookapp.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductInUseException extends RuntimeException {
    private final List<String> dishNames;

    public ProductInUseException(List<String> dishNames) {
        super("Невозможно удалить продукт. Он используется в следующих блюдах: " + String.join(", ", dishNames));
        this.dishNames = dishNames;
    }
}