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
import java.util.concurrent.TimeUnit;

//todo nacitani nahodneho poctu zapasu

public class Main {

    public static Config config;

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        Gson gson = new Gson();
        config = gson.fromJson(new JsonReader(new FileReader(args[0])), Config.class);
        System.setProperty("webdriver.gecko.driver", "/home/daniel/IdeaProjects/milionar/lib/selenium/geckodriver");
        WebDriver driver = new FirefoxDriver();

//nabidky
        String baseUrl = "https://www.tipsport.cz/kurzy/zitra?limit=10000";
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
        baseUrl = "https://www.tipsport.cz/vysledky?timeFilter=form.period.today.yesterday&limit=10000";
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
                    "sport = '" + z.getSport() + "' AND " +
                    "liga = '" + z.getLiga() + "'";
            ResultSet rs = stmt.executeQuery(queryString);

            ArrayList<Point> points = new ArrayList<>();

            while (rs.next()) {
                float delta1 = z.getKurz1() - rs.getFloat("jedna");
                float delta10 = z.getKurz10() - rs.getFloat("jednanula");
                float delta0 = z.getKurz0() - rs.getFloat("nula");
                float delta02 = z.getKurz02() - rs.getFloat("nuladva");
                float delta2 = z.getKurz2() - rs.getFloat("dva");
                Float delta = (float) Math.sqrt(delta1 * delta1 + delta10 * delta10 + delta0 * delta0 + delta02 * delta02 + delta2 * delta2);
                Integer result = 0;
                if (rs.getInt("domaci") > rs.getInt("hoste")) {
                    result = 1;
                }
                if (rs.getInt("domaci") < rs.getInt("hoste")) {
                    result = 2;
                }

                if (points.size() == 10) {
                    replaceMax(points, delta, result);
                } else {
                    points.add(new Point(delta, result));
                }
            }

            if (points.size() == 10) {
                float probability1 = coutProbability(points, 1);
                float probability0 = coutProbability(points, 0);
                float probability2 = coutProbability(points, 2);

                float profit1 = probability1 * z.getKurz1();
                float profit0 = probability0 * z.getKurz0();
                float profit2 = probability2 * z.getKurz2();

                float minKurz = Math.min(z.getKurz0(), Math.min(z.getKurz1(), z.getKurz2()));

                if (profit1 > 1 && z.getKurz1() == minKurz) {
                    zapasProfitList.add(new ZapasProfit(z, profit1, 1));
                }
                if (profit0 > 1 && z.getKurz0() == minKurz) {
                    zapasProfitList.add(new ZapasProfit(z, profit0, 0));
                }
                if (profit2 > 1 && z.getKurz2() == minKurz) {
                    zapasProfitList.add(new ZapasProfit(z, profit2, 2));
                }
            }
        }
        Collections.sort(zapasProfitList);
        for (ZapasProfit zp : zapasProfitList) {
            System.out.println(zp.getZapas().getLiga() + " " + zp.getZapas().getZapas() + " " + zp.getZapas().getDatum());
            if (zp.getResult() == 0) {
                System.out.println("0 > " + zp.getZapas().getKurz0() + " " + zp.profit);
            }
            if (zp.getResult() == 1) {
                System.out.println("1 > " + zp.getZapas().getKurz1() + " " + zp.profit);
            }
            if (zp.getResult() == 2) {
                System.out.println("2 > " + zp.getZapas().getKurz2() + " " + zp.profit);
            }
        }
    }

    private static float coutProbability(ArrayList<Point> points, int result) {
        float returnValue;
        float totalDelta = 0;
        float totalScore = 0;
        for (Point p : points) {
            totalDelta += 1 / (1 + p.getDelta());
            if (p.getResult() == result) {
                totalScore += 1 / (1 + p.getDelta());
            }
        }
        returnValue = totalScore / totalDelta;
        return returnValue;
    }

    private static void replaceMax(ArrayList<Point> points, Float delta, Integer result) {
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
                        "zapas = '" + v.getZapas() + "'" +
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
                            "'" + v.getLiga() + "', " +
                            "'" + v.getSport() + "', " +
                            "'" + v.getDatum() + "', " +
                            "'" + v.getZapas() + "', " +
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
        private Integer result;

        public Point(Float delta, Integer result) {
            this.delta = delta;
            this.result = result;
        }

        public Float getDelta() {
            return delta;
        }

        public void setDelta(Float delta) {
            this.delta = delta;
        }

        public Integer getResult() {
            return result;
        }

        public void setResult(Integer result) {
            this.result = result;
        }
    }

    public static class ZapasProfit implements Comparable {
        private Zapas zapas;
        private float profit;
        private int result;

        public int getResult() {
            return result;
        }

        public void setResult(int result) {
            this.result = result;
        }

        public ZapasProfit(Zapas zapas, float profit, int result) {
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

            if (zp.getResult() == 0) {
                zpKurz = zp.getZapas().getKurz0();
            }
            if (zp.getResult() == 1) {
                zpKurz = zp.getZapas().getKurz1();
            }
            if (zp.getResult() == 2) {
                zpKurz = zp.getZapas().getKurz2();
            }

            if (this.getResult() == 0) {
                myKurz = this.getZapas().getKurz0();
            }
            if (this.getResult() == 1) {
                myKurz = this.getZapas().getKurz1();
            }
            if (this.getResult() == 2) {
                myKurz = this.getZapas().getKurz2();
            }

            int returnValue = 0;
            if (myKurz > zpKurz) {
                returnValue = 1;
            }
            if (myKurz < zpKurz) {
                returnValue = -1;
            }

            return returnValue;
        }
    }
}
