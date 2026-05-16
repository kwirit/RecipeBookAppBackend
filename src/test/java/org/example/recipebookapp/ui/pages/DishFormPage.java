package org.example.recipebookapp.ui.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.time.Duration;
import java.util.List;

public class DishFormPage extends BasePage {

    private final By formId = By.id("dish-form");
    private final By nameInput = By.id("d-name");
    private final By categorySelect = By.id("d-category");
    private final By portionInput = By.id("d-portion");
    private final By addIngredientBtn = By.id("add-ingredient");
    private final By ingredientsList = By.id("ingredients-list");
    private final By photoInput = By.id("d-photo-file");
    private final By veganCb = By.id("d-vegan");
    private final By glutenCb = By.id("d-gluten");
    private final By sugarCb = By.id("d-sugar");
    private final By submitBtn = By.cssSelector("#dish-form button[type='submit']");
    private final By successToast = By.cssSelector(".toast.success");
    private final By errorModal = By.id("confirm-modal");
    private final By calInput = By.id("d-cal");
    private final By proInput = By.id("d-pro");
    private final By fatInput = By.id("d-fat");
    private final By carbInput = By.id("d-carb");

    public DishFormPage(WebDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public DishFormPage waitForForm() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(formId));
        // 🔹 Ждём, пока загрузится кэш продуктов для селекта ингредиентов
        wait.until(driver -> {
            Select s = new Select(driver.findElement(By.cssSelector(".ing-select")));
            return s.getOptions().size() > 1; // >1 т.к. первый — "Выберите продукт"
        });
        return this;
    }

    public DishFormPage enterName(String name) {
        type(nameInput, name);
        // 🔹 Ждём сработки debounce (350ms в app.js для авто-категории)
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    public DishFormPage selectCategory(String categoryRuOrAuto) {
        selectByText(categorySelect, categoryRuOrAuto);
        return this;
    }

    public DishFormPage setPortion(double portion) {
        type(portionInput, String.valueOf(portion));
        // 🔹 Ждём пересчёта КБЖУ (debounce 150ms)
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    /**
     * Добавляет первый ДОСТУПНЫЙ продукт из кэша.
     * Если кэш пуст — ждёт до 5 секунд.
     */
    public DishFormPage addFirstAvailableIngredient(double weight) {
        waitForAndClick(addIngredientBtn);

        List<WebElement> rows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(".ingredient-row")));
        WebElement lastRow = rows.get(rows.size() - 1);

        Select prodSelect = new Select(lastRow.findElement(By.className("ing-select")));
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        shortWait.until(d -> prodSelect.getOptions().size() > 1);

        if (prodSelect.getOptions().size() > 1) {
            prodSelect.selectByIndex(1);
        } else {
            throw new IllegalStateException("Список продуктов пуст — нельзя добавить ингредиент");
        }

        WebElement weightInput = lastRow.findElement(By.className("ing-weight"));
        weightInput.clear();
        weightInput.sendKeys(String.valueOf(weight));

        try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return this;
    }

    public DishFormPage removeIngredient(int index) {
        List<WebElement> deletes = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.className("delete-ing")));
        deletes.get(index).click();
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    public DishFormPage uploadPhoto(File imageFile) {
        WebElement input = driver.findElement(photoInput);
        input.sendKeys(imageFile.getAbsolutePath());
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#d-photo-previews img")));
        return this;
    }

    public DishFormPage toggleVegan(boolean enable) { setCheckbox(veganCb, enable); return this; }
    public DishFormPage toggleGlutenFree(boolean enable) { setCheckbox(glutenCb, enable); return this; }
    public DishFormPage toggleSugarFree(boolean enable) { setCheckbox(sugarCb, enable); return this; }

    public DishFormPage setKbzhu(Double cal, Double pro, Double fat, Double carb) {
        if (cal != null) type(calInput, String.valueOf(cal));
        if (pro != null) type(proInput, String.valueOf(pro));
        if (fat != null) type(fatInput, String.valueOf(fat));
        if (carb != null) type(carbInput, String.valueOf(carb));
        return this;
    }

    /**
     * Отправка формы с ожиданием успеха.
     * Возвращает true, если появился toast успеха.
     */
    public boolean submitExpectSuccess() {
        // 🔹 1. Ждём, пока кнопка станет кликабельной (форма валидна)
        WebDriverWait btnWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement btn = btnWait.until(ExpectedConditions.elementToBeClickable(submitBtn));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-overlay")));
        } catch (TimeoutException ignored) {}

        // 🔹 4. Проверяем toast успеха (появляется через ~100-300ms)
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return isVisible(successToast);
    }

    /**
     * Отправка формы с ожиданием ошибки валидации.
     * Возвращает true, если появилась модалка ошибки.
     */
    public boolean submitExpectError() {
        WebDriverWait btnWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement btn = btnWait.until(ExpectedConditions.elementToBeClickable(submitBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(errorModal));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public String getAutoCalories() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(calInput)).getAttribute("value");
    }

    public String getSelectedCategory() {
        Select select = new Select(wait.until(ExpectedConditions.elementToBeClickable(categorySelect)));
        return select.getFirstSelectedOption().getText().trim();
    }

    public DishFormPage fillBasic(String name, double portion, String categoryRuOrAuto) {
        enterName(name);
        setPortion(portion);
        selectCategory(categoryRuOrAuto);
        return this;
    }

    private void setCheckbox(By locator, boolean state) {
        WebElement cb = wait.until(ExpectedConditions.elementToBeClickable(locator));
        if (cb.isSelected() != state) cb.click();
    }
}