package model;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Crawler {

    public void getPageResponse(){

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String content = null;

        try {

            //網址
            String url = "https://www.imdb.com/chart/top/?ref_=nv_mv_250";

            long startTime = new Date().getTime();

            //使用 Http 拿取資料
            HttpGet get = new HttpGet(url);

            response = httpclient.execute(get);

            long getTime = new Date().getTime();

            HttpEntity entity = response.getEntity();

            content = EntityUtils.toString(entity);

            //使用 jsoup 做頁面的處理
            Document doc = Jsoup.parse(content);
            Elements tr = doc.select("tr");

            long endTime = new Date().getTime();

            System.out.println(this.processDate(new Date(getTime - startTime)) + ","
                                + this.processDate(new Date(endTime - getTime)) + ","
                                + this.processDate(new Date(endTime - startTime)) + ","
                                + this.parserDate(new Date(startTime)));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String processDate(Date date){

        SimpleDateFormat format = new SimpleDateFormat("ss.SSS");

        return format.format(date);
    }

    public String parserDate(Date date){

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return format.format(date);
    }

}
