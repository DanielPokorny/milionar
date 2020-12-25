package com.pokesoft;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.File;
import java.io.IOException;


public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        // declaration and instantiation of objects/variables

        String path = "/home/daniel/IdeaProjects/milionar/lib/selenium/geckodriver";

        System.setProperty("webdriver.gecko.driver", path);

        WebDriver driver = new FirefoxDriver();
        //comment the above 2 lines and uncomment below 2 lines to use Chrome
        //System.setProperty("webdriver.chrome.driver","G:\\chromedriver.exe");
        //WebDriver driver = new ChromeDriver();

        String baseUrl = "http://demo.guru99.com/test/newtours/";
        String expectedTitle = "Welcome: Mercury Tours";
        String actualTitle = "";

        // launch Fire fox and direct it to the Base URL
        driver.get(baseUrl);

        // get the actual value of the title
        actualTitle = driver.getTitle();

        /*
         * compare the actual title of the page with the expected one and print
         * the result as "Passed" or "Failed"
         */
        if (actualTitle.contentEquals(expectedTitle)){
            System.out.println("Test Passed!");
        } else {
            System.out.println("Test Failed");
        }

        //close Fire fox
        driver.close();


/**
        WebClient wc = new WebClient(BrowserVersion.EDGE);
        wc.getOptions().setUseInsecureSSL(true);
        wc.getOptions().setCssEnabled(false);
        wc.getOptions().setJavaScriptEnabled(false);
        wc.getOptions().setThrowExceptionOnScriptError(false);
        wc.setJavaScriptTimeout(10000);
        wc.setAjaxController(new NicelyResynchronizingAjaxController());
        wc.getOptions().setTimeout(10000);
        HtmlPage page = wc.getPage("https://www.seznam.cz/");
        System.out.println(page.asText()); */

    }
}
