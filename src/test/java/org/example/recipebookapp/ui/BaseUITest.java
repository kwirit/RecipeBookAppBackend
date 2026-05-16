package org.example.recipebookapp.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseUITest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected static final String BASE_URL = System.getProperty("ui.base.url", "http://localhost:8080/#");
    private static final int MAX_RETRIES = 30;

    @BeforeAll
    public void setupDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--window-size=1920,1080", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    public void tearDownDriver() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    public void navigateHome() {
        waitForAppToBeReady();
        ensureModalClosed();
        driver.get(BASE_URL);
        ensureModalClosed();
    }

    @AfterEach
    public void afterEachTest() {
        ensureModalClosed();
        driver.get(BASE_URL);
    }

    /**
     * Ждёт, пока приложение станет доступно по HTTP.
     * Проверяет корень приложения каждые 1 секунду, максимум 30 попыток.
     */
    private void waitForAppToBeReady() {
        // 🔹 Убираем "/#" из BASE_URL для проверки доступности бэкенда
        String healthUrl = BASE_URL.replace("/#", "").replaceAll("/+$", "");
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(healthUrl).openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setRequestMethod("GET");

                int status = conn.getResponseCode();
                conn.disconnect();

                if (status >= 200 && status < 400) {
                    return;
                }
            } catch (Exception e) {

            }

            attempts++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            if (attempts % 5 == 0) {
                System.out.println("⏳ Ожидание приложения... попытка " + attempts + "/" + MAX_RETRIES);
            }
        }

        throw new RuntimeException(
                "Приложение не стало доступно за " + MAX_RETRIES + " секунд по адресу: " + healthUrl +
                        "\nУбедитесь, что вы запустили: ./gradlew bootRun"
        );
    }

    /**
     * Гарантированно закрывает модальное окно #confirm-modal, если оно открыто.
     */
    protected void ensureModalClosed() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            WebElement modal = shortWait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("confirm-modal")));

            String modalClass = modal.getAttribute("class");
            if (modalClass != null && !modalClass.contains("hidden")) {
                WebElement closeBtn = shortWait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#confirm-modal #confirm-cancel, #confirm-modal .btn-primary")));

                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeBtn);
                shortWait.until(ExpectedConditions.invisibilityOf(modal));
            }
        } catch (TimeoutException | NoSuchElementException e) {
            // Модалки нет или она уже закрыта — это нормально
        } catch (Exception e) {
            System.out.println("⚠️ ensureModalClosed: " + e.getMessage());
        }
    }
}