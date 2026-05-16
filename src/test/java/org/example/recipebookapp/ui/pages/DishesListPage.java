package org.example.recipebookapp.ui.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.List;

public class DishesListPage extends BasePage {

    private final By tableId = By.id("dish-table");
    private final By tbodyId = By.id("dish-tbody");
    private final By addDishLink = By.cssSelector("a[href='#/dishes/new']");
    private final By filterName = By.id("d-filter-name");
    private final By filterCategory = By.id("d-filter-category");
    private final By filterVegan = By.id("d-filter-vegan");
    private final By filterGluten = By.id("d-filter-gluten");
    private final By filterSugar = By.id("d-filter-sugar");
    private final By pagination = By.id("dish-pagination");

    public DishesListPage(WebDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public DishesListPage navigateTo() {
        driver.get("http://localhost:8080/#/dishes");
        wait.until(ExpectedConditions.visibilityOfElementLocated(tbodyId));
        return this;
    }

    public DishFormPage openNewDishForm() {
        waitForAndClick(addDishLink);
        wait.until(ExpectedConditions.urlContains("#/dishes/new"));
        return new DishFormPage(driver, wait);
    }

    public DishesListPage filterByName(String name) {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(filterName));
        el.clear();
        el.sendKeys(name);
        waitForLoading();
        return this;
    }

    public DishesListPage filterByCategory(String categoryRu) {
        selectByText(filterCategory, categoryRu);
        waitForLoading();
        return this;
    }

    public DishesListPage toggleFlag(String flag) {
        By locator = switch(flag.toUpperCase()) {
            case "VEGAN" -> filterVegan;
            case "GLUTEN_FREE" -> filterGluten;
            case "SUGAR_FREE" -> filterSugar;
            default -> throw new IllegalArgumentException("Unknown flag: " + flag);
        };
        waitForAndClick(locator);
        waitForLoading();
        return this;
    }

    public boolean isDishVisible(String name) {
        List<WebElement> rows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("#dish-tbody tr.dish-row")));
        return rows.stream().anyMatch(row -> row.getText().contains(name));
    }

    public String getFirstDishCategory() {
        WebElement firstRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("#dish-tbody tr.dish-row td:nth-child(3)")));
        return firstRow.getText().trim();
    }

    public DishFormPage openEditFirstDish() {
        WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#dish-tbody tr.dish-row a.btn-icon")));
        editBtn.click();
        wait.until(ExpectedConditions.urlContains("#/dishes/edit/"));
        return new DishFormPage(driver, wait);
    }

    public void viewDishDetail(String name) {
        List<WebElement> rows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("#dish-tbody tr.dish-row")));
        WebElement target = rows.stream()
                .filter(r -> r.getText().contains(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Dish not found: " + name));
        target.click();
        wait.until(ExpectedConditions.urlContains("#/dishes/"));
    }

    private void waitForLoading() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-overlay")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(tbodyId));
    }
}