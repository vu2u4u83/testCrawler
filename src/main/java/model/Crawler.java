package model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.HttpClientUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Crawler {

    HttpClientUtil httpClientUtil = new HttpClientUtil();

    public void getPageResponse(String url){

        httpClientUtil.create();

        try {
            Document doc = Jsoup.parse(httpClientUtil.get(url, httpClientUtil.DATA_JSON_HEADERS).getHttpResponse());

            Elements tr = doc.select("tr");

            List<String> txt = new ArrayList();

            for (Element element : tr){
                txt.add(element.text());
            }

            for (String aa : txt){
                System.out.println(aa);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

}
