package com.pokesoft;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/*
    Verze 1.5
    - razeni podle pravdepodobnosti a kurzu

    Verze 1.4
    - oprava zapisu do DB ' na ''
    - zmena razeni podle profitu

    Verze 1.3
    - minimalni profit 1.1
    - vyber ze vsech kurzu, nejen nejmensiho

    Verze 1.2
    - pridan nahodny pocet zapasu
    - pridany kurzy 10 a 02
 */


public class Main {

    public static Config config;

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        Gson gson = new Gson();
        config = gson.fromJson(new JsonReader(new FileReader(args[0])), Config.class);
        System.setProperty("webdriver.gecko.driver", "/home/daniel/IdeaProjects/milionar/lib/selenium/geckodriver");
        WebDriver driver = new FirefoxDriver();

        Random rng = new Random();
//nabidky
        String numberOfMatches = Integer.toString(4975 + rng.ints(0, 100).findFirst().getAsInt() * 50);
        String baseUrl = "https://www.tipsport.cz/kurzy/zitra?limit=" + numberOfMatches;
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
        numberOfMatches = Integer.toString(4975 + rng.ints(0, 100).findFirst().getAsInt() * 50);
        baseUrl = "https://www.tipsport.cz/vysledky?timeFilter=form.period.today.yesterday&limit=" + numberOfMatches;
        driver.get(baseUrl);

        sportClass = new By.ByClassName("o-superSportRow");
        webElements = (ArrayList<WebElement>) driver.findElements(sportClass);
        ArrayList<com.pokesoft.Zapas> vysledky = new ArrayList<>();

        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        for (WebElement element : webElements) {
            vysledky.addAll(parseSportElement(element));
        }
        driver.close();

//zapis do DB
        writeToDB(vysledky);

//hledani k-neighbours
        String url = "jdbc:postgresql://10.0.1.43/tipsport";
        String user = "tipsport";
        String password = "heslo";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        ArrayList<ZapasProfit> zapasProfitList = new ArrayList<>();

        for (Zapas z : nabidky) {
            String queryString = "SELECT * FROM vysledky WHERE " +
                    "sport = '" + z.getSport().replace("'", "''") + "' AND " +
                    "liga = '" + z.getLiga().replace("'", "''") + "'";
            ResultSet rs = stmt.executeQuery(queryString);

            ArrayList<Point> points = new ArrayList<>();

            while (rs.next()) {
                float delta1 = z.getKurz1() - rs.getFloat("jedna");
                float delta10 = z.getKurz10() - rs.getFloat("jednanula");
                float delta0 = z.getKurz0() - rs.getFloat("nula");
                float delta02 = z.getKurz02() - rs.getFloat("nuladva");
                float delta2 = z.getKurz2() - rs.getFloat("dva");
                Float delta = (float) Math.sqrt(delta1 * delta1 + delta10 * delta10 + delta0 * delta0 + delta02 * delta02 + delta2 * delta2);
                String result = "0";
                if (rs.getInt("domaci") > rs.getInt("hoste")) {
                    result = "1";
                }
                if (rs.getInt("domaci") < rs.getInt("hoste")) {
                    result = "2";
                }

                if (points.size() == 10) {
                    replaceMax(points, delta, result);
                } else {
                    points.add(new Point(delta, result));
                }
            }

            if (points.size() == 10) {
                float probability1 = coutProbability(points, "1");
                float probability10 = coutProbability(points, "10");
                float probability0 = coutProbability(points, "0");
                float probability02 = coutProbability(points, "02");
                float probability2 = coutProbability(points, "2");

                float profit1 = probability1 * z.getKurz1();
                float profit10 = probability10 * z.getKurz10();
                float profit0 = probability0 * z.getKurz0();
                float profit02 = probability02 * z.getKurz02();
                float profit2 = probability2 * z.getKurz2();

                float minProfit = 1.1f;

                if (profit1 > minProfit) {
                    zapasProfitList.add(new ZapasProfit(z, profit1, "1"));
                }
                if (profit10 > minProfit) {
                    zapasProfitList.add(new ZapasProfit(z, profit10, "10"));
                }
                if (profit0 > minProfit) {
                    zapasProfitList.add(new ZapasProfit(z, profit0, "0"));
                }
                if (profit02 > minProfit) {
                    zapasProfitList.add(new ZapasProfit(z, profit02, "02"));
                }
                if (profit2 > minProfit) {
                    zapasProfitList.add(new ZapasProfit(z, profit2, "2"));
                }
            }
        }
        Collections.sort(zapasProfitList);
        for (ZapasProfit zp : zapasProfitList) {
            System.out.println(zp.getZapas().getLiga() + " " + zp.getZapas().getZapas() + " " + zp.getZapas().getDatum());
            if (zp.getResult().equals("1")) {
                System.out.println("1 > " + zp.getZapas().getKurz1() + " " + zp.profit);
            }
            if (zp.getResult().equals("10")) {
                System.out.println("10 > " + zp.getZapas().getKurz10() + " " + zp.profit);
            }
            if (zp.getResult().equals("0")) {
                System.out.println("0 > " + zp.getZapas().getKurz0() + " " + zp.profit);
            }
            if (zp.getResult().equals("02")) {
                System.out.println("02 > " + zp.getZapas().getKurz02() + " " + zp.profit);
            }
            if (zp.getResult().equals("2")) {
                System.out.println("2 > " + zp.getZapas().getKurz2() + " " + zp.profit);
            }
        }
    }

    private static float coutProbability(ArrayList<Point> points, String result) {
        float returnValue;
        float totalDelta = 0;
        float totalScore = 0;
        for (Point p : points) {
            totalDelta += 1 / (1 + p.getDelta());
            if (result.contains(p.getResult())) {
                totalScore += 1 / (1 + p.getDelta());
            }
        }
        returnValue = totalScore / totalDelta;
        return returnValue;
    }

    private static void replaceMax(ArrayList<Point> points, Float delta, String result) {
        float m = points.get(0).getDelta();
        int index = 0;

        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).getDelta() > m) {
                m = points.get(i).getDelta();
                index = i;
            }
        }

        if (m > delta) {
            points.set(index, new Point(delta, result));
        }
    }

    public static void writeToDB(ArrayList<Zapas> vysledky) {
        String url = "jdbc:postgresql://10.0.1.43/tipsport";
        String user = "tipsport";
        String password = "heslo";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        for (Zapas v : vysledky) {
            if (v.getHoste() != null && v.getDomaci() != null) {
                String query = "SELECT * FROM vysledky WHERE " +
                        "zapas = '" + v.getZapas().replace("'", "''") + "'" +
                        " AND datum = '" + v.getDatum() + "'";
                System.out.println(query);
                Boolean zapasVDB = false;
                ResultSet rs;
                try {
                    rs = stmt.executeQuery(query);
                    if (rs.next()) {
                        zapasVDB = true;
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                if (!zapasVDB) {
                    String insertString = "INSERT INTO vysledky (liga, sport, datum, zapas, jedna, jednanula, nula, nuladva, " +
                            "dva, domaci, hoste) " +
                            "VALUES (" +
                            "'" + v.getLiga().replace("'", "''") + "', " +
                            "'" + v.getSport().replace("'", "''") + "', " +
                            "'" + v.getDatum() + "', " +
                            "'" + v.getZapas().replace("'", "''") + "', " +
                            v.getKurz1().toString() + ", " +
                            v.getKurz10().toString() + ", " +
                            v.getKurz0().toString() + ", " +
                            v.getKurz02().toString() + ", " +
                            v.getKurz2().toString() + ", " +
                            v.getDomaci().toString() + ", " +
                            v.getHoste().toString() + ")";
                    try {
                        stmt.executeUpdate(insertString);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    System.out.println(insertString);
                }
            }
        }
        try {
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
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
                if (liga.contains("\n")) {
                    liga = liga.substring(0, liga.indexOf("\n"));
                }
            }
            if (classString.equals("o-matchRow")) {
                Zapas zapas = new Zapas();
                String zapasJmeno = zalElement.findElement(new By.ByClassName("o-matchRow__leftSide")).getText();

                System.out.println(liga + " " + zapasJmeno);

                zapas.setZapas(zapasJmeno);

                WebElement kurzyElement = zalElement.findElement(new By.ByClassName("o-matchRow__rightSideInner"));
                String datum = kurzyElement.findElement(new By.ByClassName("o-matchRow__dateClosed")).getText();
                if (datum.length() == 14) {
                    datum = datum.substring(0, 9) + " " + datum.substring(9);
                } else if (datum.length() == 15){
                    datum = datum.substring(0, 10) + " " + datum.substring(10);
                } else {
                    datum = datum.substring(0, 8) + " " + datum.substring(8);
                }

                ArrayList<WebElement> kurzyElementList = (ArrayList<WebElement>) kurzyElement.findElements(new By.ByClassName("btnRate"));
                if (kurzyElementList.size() == 5) {
                    ArrayList<Float> kurzyList = new ArrayList<>();
                    for (WebElement kurz : kurzyElementList) {
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
                    if (resultElement != null) {
                        resultString = resultElement.getText();
                        if (resultString.contains(" ")) {
                            resultString = resultString.substring(0, resultString.indexOf(" "));
                        }

                        try {
                            if (resultString.contains(":")) {
                                String[] results = resultString.split(":");
                                String domaciString = results[0];
                                String hosteString = results[1];
                                zapas.setHoste(Integer.valueOf(hosteString));
                                zapas.setDomaci(Integer.valueOf(domaciString));
                                System.out.print(domaciString + ":" + hosteString + ">");
                            }
                        } catch (Exception e) {
                            System.out.println("chyba vysledku");
                        }
                    }

                    returnValue.add(zapas);
                    System.out.println(sport + " " + liga + " " + zapasJmeno + " ");
                }
            }
        }
        return returnValue;
    }

    public static class Point {
        private Float delta;
        private String result;

        public Point(Float delta, String result) {
            this.delta = delta;
            this.result = result;
        }

        public Float getDelta() {
            return delta;
        }

        public void setDelta(Float delta) {
            this.delta = delta;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    public static class ZapasProfit implements Comparable {
        private Zapas zapas;
        private float profit;
        private String result;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public ZapasProfit(Zapas zapas, float profit, String result) {
            this.zapas = zapas;
            this.profit = profit;
            this.result = result;
        }

        public Zapas getZapas() {
            return zapas;
        }

        public void setZapas(Zapas zapas) {
            this.zapas = zapas;
        }

        public float getProfit() {
            return profit;
        }

        public void setProfit(float profit) {
            this.profit = profit;
        }

        @Override
        public int compareTo(@NotNull Object o) {
            ZapasProfit zp = (ZapasProfit) o;
            float zpKurz = 0;
            float myKurz = 0;

            if (zp.getResult().equals("1")) {
                zpKurz = zp.getZapas().getKurz1();
            }
            if (zp.getResult().equals("10")) {
                zpKurz = zp.getZapas().getKurz10();
            }
            if (zp.getResult().equals("0")) {
                zpKurz = zp.getZapas().getKurz0();
            }
            if (zp.getResult().equals("02")) {
                zpKurz = zp.getZapas().getKurz02();
            }
            if (zp.getResult().equals("2")) {
                zpKurz = zp.getZapas().getKurz2();
            }

            if (this.getResult().equals("1")) {
                myKurz = this.getZapas().getKurz1();
            }
            if (this.getResult().equals("10")) {
                myKurz = this.getZapas().getKurz10();
            }
            if (this.getResult().equals("0")) {
                myKurz = this.getZapas().getKurz0();
            }
            if (this.getResult().equals("02")) {
                myKurz = this.getZapas().getKurz02();
            }
            if (this.getResult().equals("2")) {
                myKurz = this.getZapas().getKurz2();
            }

            float myProb = this.profit / myKurz;
            float zpProb = zp.profit / zpKurz;


            int returnValue = 0;
/*            if (zp.profit > this.profit) {
                returnValue = 1;
            }
            if (zp.profit < this.profit) {
                returnValue = -1;
            }*/

/*
            if (zpKurz > myKurz) {
                returnValue = 1;
            }
            if (zpKurz < myKurz) {
                returnValue = -1;
            }*/

            if (zpProb > myProb) {
                returnValue = 1;
            }
            if (zpKurz < myKurz) {
                returnValue = -1;
            }
            if (zpProb == myProb) {
                if (zpKurz > myKurz) {
                    return 1;
                }
                if (zpKurz == myKurz) {
                    return 0;
                }
                if (zpKurz < myKurz) {
                    return -1;
                }
            }

            return returnValue;
        }
    }
}
