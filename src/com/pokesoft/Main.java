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

            By zapasyBy = new By.ByClassName("o-superSportRow__body");
            WebElement zapasyElement = element.findElement(zapasyBy);

            By zapasNeboLigaBy = new By.ByXPath("//div[@class = 'o-competitionRow' or @class = 'o-matchRow']");
            ArrayList<WebElement> zapasyAligyElements = (ArrayList<WebElement>) zapasyElement.findElements(zapasNeboLigaBy);
            for (WebElement zalElement : zapasyAligyElements) {
                String classString = zalElement.getAttribute("class");
                if (classString.equals("o-competitionRow")) {
                    String liga = zalElement.findElement(new By.ByClassName("o-competitionRow__left")).getText();
                    System.out.println(liga);
                }
                if (classString.equals("o-matchRow")) {
                    Zapas zapas = new Zapas();
                    String zapasJmeno = zalElement.findElement(new By.ByClassName("o-matchRow__leftSide")).getText();

                    System.out.println(zapasJmeno);
                    zapas.setZapas(zapasJmeno);

                    WebElement kurzyElement = zalElement.findElement(new By.ByClassName("o-matchRow__rightSideInner"));

                    ArrayList<WebElement> kurzyElementList = (ArrayList<WebElement>) kurzyElement.findElements(new By.ByClassName("btnRate"));
                    for(WebElement kurz : kurzyElementList) {
                        System.out.println(kurz.getText());
                    }
                }
            }
            System.out.println(zapasyElement.getText());
        }

        driver.close();

    }
}
