package org.example.recipebookapp.ui.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.List;

public class ProductFormPage extends BasePage {
    // Locators
    private final By nameInput = By.id("p-name");
    private final By categorySelect = By.id("p-category");
    private final By cookingSelect = By.id("p-cooking");
    private final By calInput = By.id("p-cal");
    private final By proInput = By.id("p-pro");
    private final By fatInput = By.id("p-fat");
    private final By carbInput = By.id("p-carb");
    private final By veganCb = By.id("p-vegan");
    private final By saveBtn = By.cssSelector("form#product-form button[type='submit']");
    private final By successToast = By.cssSelector(".toast.success");
    private final By errorModal = By.id("confirm-modal");

    public ProductFormPage(WebDriver driver, WebDriverWait wait) { super(driver, wait); }

    public ProductFormPage fillProduct(String name, String category, String cooking,
                                       double cal, double pro, double fat, double carb,
                                       boolean vegan) {
        type(nameInput, name);
        new Select(driver.findElement(categorySelect)).selectByVisibleText(category);
        new Select(driver.findElement(cookingSelect)).selectByValue(cooking);
        type(calInput, String.valueOf(cal));
        type(proInput, String.valueOf(pro));
        type(fatInput, String.valueOf(fat));
        type(carbInput, String.valueOf(carb));
        if (vegan && !driver.findElement(veganCb).isSelected()) {
            driver.findElement(veganCb).click();
        }
        return this;
    }

    public boolean submitExpectSuccess() {
        waitForAndClick(saveBtn);
        return isVisible(successToast);
    }

    public boolean submitExpectError() {
        waitForAndClick(saveBtn);
        return isVisible(errorModal);
    }
}