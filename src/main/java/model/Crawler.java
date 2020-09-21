package model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import util.HttpClientUtil;

import java.io.IOException;
import java.net.URISyntaxException;

public class Crawler {

    HttpClientUtil httpClientUtil = new HttpClientUtil();

    public void getPageResponse(String url){

        httpClientUtil.create();

        try {
            Document doc = Jsoup.parse(httpClientUtil.get(url, httpClientUtil.DATA_JSON_HEADERS).getHttpResponse());

            System.out.println(doc);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

}
