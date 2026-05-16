package org.example.recipebookapp.ui.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;

import java.io.File;
import java.time.Duration;

public abstract class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    protected void waitForAndClick(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(text);
    }

    protected String getText(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText();
    }

    protected boolean isVisible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected void selectByText(By locator, String text) {
        WebElement selectElement = wait.until(ExpectedConditions.elementToBeClickable(locator));
        new Select(selectElement).selectByVisibleText(text);
    }

    /**
     * Гарантированно закрывает модальное окно #confirm-modal, если оно открыто.
     * Вызывается перед каждым действием, требующим клика по основным элементам.
     */
    protected void ensureModalClosed() {
        try {
            // 🔹 Короткий таймаут для проверки модалки (чтобы не ждать долго, если её нет)
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(1));

            // 🔹 Проверяем, видима ли модалка (не имеет класса "hidden")
            WebElement modal = shortWait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("confirm-modal")));

            String modalClass = modal.getAttribute("class");
            if (modalClass != null && !modalClass.contains("hidden")) {
                // 🔹 Модалка видима — ищем кнопку закрытия
                WebElement closeBtn = shortWait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#confirm-modal #confirm-cancel, #confirm-modal .btn-primary")));

                // 🔹 Кликаем через JS для надёжности (обходит возможные оверлеи)
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeBtn);

                // 🔹 Ждём, пока модалка скроется
                shortWait.until(ExpectedConditions.invisibilityOf(modal));
            }
        } catch (TimeoutException | NoSuchElementException e) {
            // 🔹 Модалки нет или она уже закрыта — это нормальная ситуация
        } catch (Exception e) {
            // 🔹 Логирование для отладки (можно убрать в продакшене)
            System.out.println("⚠️ ensureModalClosed: " + e.getMessage());
        }
    }

    protected boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Делает скриншот для отладки (сохраняет в build/reports/tests).
     */
    protected void takeDebugScreenshot(String testName) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File("build/reports/tests/debug_" + testName + "_" + System.currentTimeMillis() + ".png");
            dest.getParentFile().mkdirs();
            org.apache.commons.io.FileUtils.copyFile(src, dest);
            System.out.println("📸 Скриншот: " + dest.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("❌ Не удалось сделать скриншот: " + e.getMessage());
        }
    }
}