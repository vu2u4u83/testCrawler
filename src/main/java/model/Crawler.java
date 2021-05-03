package model;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

public class Crawler {

    public void getPageResponse(){

        CloseableHttpResponse response = null;
        String content = null;

        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        try {

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { tm }, null);

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf)
                                                                    .setMaxConnTotal(50)
                                                                    .setMaxConnPerRoute(50)
                                                                    .setDefaultRequestConfig(RequestConfig.custom()
                                                                    .setConnectionRequestTimeout(60000)
                                                                    .setConnectTimeout(60000)
                                                                    .setSocketTimeout(60000).build()).build();

            //網址
            String url = "https://www.ettoday.net/news/news-list.htm";

            long startTime = new Date().getTime();

            //使用 Http 拿取資料
            HttpGet get = new HttpGet(url);

            response = httpclient.execute(get);

            long getTime = new Date().getTime();

            HttpEntity entity = response.getEntity();

            content = EntityUtils.toString(entity);

            //使用 jsoup 做頁面的處理
            Document doc = Jsoup.parse(content);
            Elements tr = doc.select(".part_list_2");

            long endTime = new Date().getTime();

            System.out.println(this.processDate(new Date(getTime - startTime)) + ","
                                + this.processDate(new Date(endTime - getTime)) + ","
                                + this.processDate(new Date(endTime - startTime)) + ","
                                + this.parserDate(new Date(startTime)));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
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
