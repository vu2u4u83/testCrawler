package application;

import model.Crawler;

public class Main {

    public static void main(String[] args) {

        Crawler crawler = new Crawler();
        crawler.getPageResponse("http://bf9a285c03b490c9.api.sstream365.net/?lng=en");

    }

}
