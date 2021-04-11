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
import java.util.concurrent.TimeUnit;


public class Main {
    public static Config config;

    public static void main(String[] args) throws IOException, InterruptedException {
        Gson gson = new Gson();
        config = gson.fromJson(new JsonReader(new FileReader(args[0])), Config.class);
        System.setProperty("webdriver.gecko.driver", "/home/daniel/IdeaProjects/milionar/lib/selenium/geckodriver");
        WebDriver driver = new FirefoxDriver();

        String baseUrl = "https://www.tipsport.cz/kurzy/zitra?limit=500";

        driver.get(baseUrl);
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        ArrayList<com.pokesoft.Zapas> zapasy = new ArrayList<>();
        By sportClass = new By.ByClassName("o-superSportRow");
        ArrayList<WebElement> webElements = (ArrayList<WebElement>) driver.findElements(sportClass);
        for (WebElement element : webElements) {
            String e = element.getText();
            zapasy.addAll(parseSportElement(element));
        }

        for(Zapas zapas : zapasy) {
            String zapasJson = gson.toJson(zapas, Zapas.class);
            System.out.println(zapasJson);
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

        System.out.println(zapasyElement.getText());

        By zapasNeboLigaBy = new By.ByXPath(".//div[@class = 'o-competitionRow' or @class = 'o-matchRow']");
        ArrayList<WebElement> zapasyAligyElements = (ArrayList<WebElement>) zapasyElement.findElements(zapasNeboLigaBy);

        String liga = "";
        for (WebElement zalElement : zapasyAligyElements) {
            String classString = zalElement.getAttribute("class");
            if (classString.equals("o-competitionRow")) {
                liga = zalElement.findElement(new By.ByClassName("o-competitionRow__left")).getText();
                System.out.println(liga);
            }
            if (classString.equals("o-matchRow")) {
                Zapas zapas = new Zapas();
                String zapasJmeno = zalElement.findElement(new By.ByClassName("o-matchRow__leftSide")).getText();

                System.out.println(sport + " " + liga + " " + zapasJmeno);
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
                    returnValue.add(zapas);
                }
            }
        }
        return returnValue;
    }
}
