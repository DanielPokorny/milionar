package com.pokesoft;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("webdriver.gecko.driver", "/home/daniel/IdeaProjects/milionar/lib/selenium/geckodriver");
        WebDriver driver = new FirefoxDriver();

        String baseUrl = "https://www.tipsport.cz/kurzy/zitra?limit=125";

        driver.get(baseUrl);
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        String text;
        By sportClass = new By.ByClassName("o-superSportRow");
        ArrayList<WebElement> webElements = (ArrayList<WebElement>) driver.findElements(sportClass);
        for (WebElement element : webElements) {
            By sportBy = new By.ByClassName("o-superSportRow__header");
            WebElement sportElement = element.findElement(sportBy);
            String sport = sportElement.getText();
            By competitionBy = new By.ByClassName("o-competitionRow");
            ArrayList<WebElement> competionsElements = element.
            System.out.println(sportElement.getText());
            driver.findElement(By.xpath("//div[contains(@class, 'value') and contains(@class, 'test')]"));
        }

        driver.close();

    }
}
