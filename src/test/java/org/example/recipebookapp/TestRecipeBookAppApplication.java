package org.example.recipebookapp;

import org.springframework.boot.SpringApplication;

public class TestRecipeBookAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(RecipeBookAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
