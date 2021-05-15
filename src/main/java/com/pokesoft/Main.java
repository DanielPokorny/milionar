package com.pokesoft;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;


public class Main {
    public static Config config;

    public static void main(String[] args) throws IOException, InterruptedException {
        Gson gson = new Gson();
        config = gson.fromJson(new JsonReader(new FileReader(args[0])), Config.class);
        System.setProperty("webdriver.gecko.driver", "/home/daniel/IdeaProjects/milionar/lib/selenium/geckodriver");
        WebDriver driver = new FirefoxDriver();

//nabidky
        String baseUrl = "https://www.tipsport.cz/kurzy/zitra?limit=125";
        driver.get(baseUrl);
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        ArrayList<com.pokesoft.Zapas> nabidky = new ArrayList<>();
        By sportClass = new By.ByClassName("o-superSportRow");
        ArrayList<WebElement> webElements = (ArrayList<WebElement>) driver.findElements(sportClass);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        for (WebElement element : webElements) {
            nabidky.addAll(parseSportElement(element));
        }
        driver.manage().timeouts().implicitlyWait(120, TimeUnit.SECONDS);

// vysledky
        baseUrl = "https://www.tipsport.cz/vysledky?timeFilter=form.period.today.yesterday&limit=125";
        driver.get(baseUrl);

        sportClass = new By.ByClassName("o-superSportRow");
        webElements = (ArrayList<WebElement>) driver.findElements(sportClass);
        ArrayList<com.pokesoft.Zapas> vysledky = new ArrayList<>();

        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        for (WebElement element : webElements) {
            vysledky.addAll(parseSportElement(element));
        }

        driver.close();
    }

    public static Collection<? extends com.pokesoft.Zapas> parseSportElement(WebElement element) {
        ArrayList<com.pokesoft.Zapas> returnValue = new ArrayList<>();

        By sportBy = new By.ByClassName("o-superSportRow__header");
        WebElement sportElement = element.findElement(sportBy);
        String sport = sportElement.getText();

        By zapasyBy = new By.ByClassName("o-superSportRow__body");
        WebElement zapasyElement = element.findElement(zapasyBy);

        By zapasNeboLigaBy = new By.ByXPath(".//div[@class = 'o-competitionRow' or @class = 'o-matchRow']");
        ArrayList<WebElement> zapasyAligyElements = (ArrayList<WebElement>) zapasyElement.findElements(zapasNeboLigaBy);

        String liga = "";
        for (WebElement zalElement : zapasyAligyElements) {
            String classString = zalElement.getAttribute("class");
            if (classString.equals("o-competitionRow")) {
                liga = zalElement.findElement(new By.ByClassName("o-competitionRow__left")).getText();
                if(liga.contains("\n")) {
                    liga = liga.substring(0, liga.indexOf("\n"));
                }
            }
            if (classString.equals("o-matchRow")) {
                Zapas zapas = new Zapas();
                String zapasJmeno = zalElement.findElement(new By.ByClassName("o-matchRow__leftSide")).getText();

                zapas.setZapas(zapasJmeno);

                WebElement kurzyElement = zalElement.findElement(new By.ByClassName("o-matchRow__rightSideInner"));
                String datum = kurzyElement.findElement(new By.ByClassName("o-matchRow__dateClosed")).getText();

                ArrayList<WebElement> kurzyElementList = (ArrayList<WebElement>) kurzyElement.findElements(new By.ByClassName("btnRate"));
                if(kurzyElementList.size() == 5) {
                    ArrayList<Float> kurzyList = new ArrayList<>();
                    for(WebElement kurz : kurzyElementList) {
                        Float k = 1F;
                        try {
                            k = Float.parseFloat(kurz.getText());
                        } catch (Exception e) {

                        }
                        kurzyList.add(k);
                    }
                    zapas.setKurz1(kurzyList.get(0));
                    zapas.setKurz10(kurzyList.get(1));
                    zapas.setKurz0(kurzyList.get(2));
                    zapas.setKurz02(kurzyList.get(3));
                    zapas.setKurz2(kurzyList.get(4));

                    zapas.setDatum(datum);
                    zapas.setLiga(liga);
                    zapas.setSport(sport);

                    WebElement resultElement = null;
                    try {
                        resultElement = zalElement.findElement(new By.ByXPath(".//div[@class = 'o-matchRow__result']"));
                    } catch (Exception e) {

                    }
                    String resultString = "";
                    if(resultElement != null) {
                        resultString = resultElement.getText();
                    }
                    returnValue.add(zapas);
                    System.out.println(sport + " " + liga + " " + zapasJmeno + " " + resultString);
                }
            }
        }
        return returnValue;
    }
}
