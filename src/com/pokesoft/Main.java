package com.pokesoft;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.IOException;


public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("webdriver.gecko.driver", "/home/daniel/IdeaProjects/milionar/lib/selenium/geckodriver");
        WebDriver driver = new FirefoxDriver();

        String baseUrl = "https://www.tipsport.cz/kurzy/zitra?limit=125";

        driver.get(baseUrl);

        String text;
        By body = new By.ByClassName("o-superSportRow");
        text = driver.findElement(body).getText();
        WebElement e = driver.findElement(body);
        System.out.println(text);

        driver.close();

    }
}
