package org.example.recipebookapp.ui.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ProductsListPage extends BasePage {
    private final By addBtn = By.cssSelector(".card-header .btn-primary");
    private final By productGrid = By.id("product-grid");
    private final By filterName = By.id("filter-name");
    private final By filterCategory = By.id("filter-category");

    public ProductsListPage(WebDriver driver, WebDriverWait wait) { super(driver, wait); }

    public void navigateTo() { driver.get("http://localhost:8080/#/products"); }

    public ProductFormPage openNewProductForm() {
        waitForAndClick(addBtn);
        return new ProductFormPage(driver, wait);
    }

    public boolean isProductVisible(String name) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(productGrid));
        return driver.findElements(By.cssSelector(".product-card-title")).stream()
                .anyMatch(el -> el.getText().equals(name));
    }
}
