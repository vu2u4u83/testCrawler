package util;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.*;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.LineParser;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class HttpClientUtil {

    private final int MAX_CONNECT_TIMEOUT = 30_000; // 最大與伺服器連線時間
    private final int MAX_SO_TIMEOUT = 20_000; // 最大等待回應時間
    private final int DEFAULT_MAX_TOTAL = 200; // 連線池最大同時請求數量
    private final int DEFAULT_MAX_PRE_TOTAL = 100; // 單一伺服器最大同時請求數量

    private CloseableHttpClient httpclient;
    private RequestConfig defaultRequestConfig;
    private CookieStore cookieStore;
    private CredentialsProvider credentialsProvider;
    public static long endTXTime;
    public static long startTXTime;

    public void close()
    {
        try {
            if (httpclient != null)
            {
                httpclient.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void create() {
        // Use custom message parser / writer to customize the way HTTP
        // messages are parsed from and written out to the data stream.
        HttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {

            @Override
            public HttpMessageParser<HttpResponse> create(
                    SessionInputBuffer buffer, MessageConstraints constraints) {
                LineParser lineParser = new BasicLineParser() {

                    @Override
                    public Header parseHeader(final CharArrayBuffer buffer) {
                        try {
//                        	System.out.println(buffer.toString());
                            return super.parseHeader(buffer);
                        } catch (ParseException ex) {
                            return new BasicHeader(buffer.toString(), null);
                        }
                    }

                };
                return new DefaultHttpResponseParser(
                        buffer, lineParser, DefaultHttpResponseFactory.INSTANCE, constraints) {

                    @Override
                    protected boolean reject(final CharArrayBuffer line, int count) {
                        // try to ignore all garbage preceding a status line infinitely
                        return false;
                    }

                };
            }

        };

        HttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();

        // Use a custom connection factory to customize the process of
        // initialization of outgoing HTTP connections. Beside standard connection
        // configuration parameters HTTP connection factory can define message
        // parser / writer routines to be employed by individual connections.
        HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(requestWriterFactory, responseParserFactory);

        // Client HTTP connection objects when fully initialized can be bound to
        // an arbitrary network socket. The process of network socket initialization,
        // its connection to a remote address and binding to a local one is controlled
        // by a connection socket factory.

        // SSL context for secure connections can be created either based on
        // system or application specific properties.
        SSLContext sslcontext = SSLContexts.createSystemDefault();

        // Create a registry of custom connection socket factories for supported
        // protocol schemes.
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(sslcontext))
                .build();

        // Use custom DNS resolver to override the system DNS resolution.
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {

            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                if (host.equalsIgnoreCase("myhost")) {
                    return new InetAddress[] { InetAddress.getByAddress(new byte[] {127, 0, 0, 1}) };
                } else {
                    return super.resolve(host);
                }
            }

        };

        // Create a connection manager with custom configuration.
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, dnsResolver);

        // Create socket configuration
        SocketConfig socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .setSoTimeout(MAX_SO_TIMEOUT)
                .build();

        // Configure the connection manager to use socket configuration either
        // by default or for a specific host.
        connManager.setDefaultSocketConfig(socketConfig);
        connManager.setSocketConfig(new HttpHost("somehost", 80), socketConfig);

        // Validate connections after 1 sec of inactivity
        connManager.setValidateAfterInactivity(1000);

        // Create message constraints
        MessageConstraints messageConstraints = MessageConstraints.custom()
                .setMaxHeaderCount(200)
                .setMaxLineLength(5000)
                .build();

        // Create connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE)
                .setCharset(Consts.UTF_8)
                .setMessageConstraints(messageConstraints)
                .build();

        // Configure the connection manager to use connection configuration either
        // by default or for a specific host.
        connManager.setDefaultConnectionConfig(connectionConfig);
        connManager.setConnectionConfig(new HttpHost("somehost", 80), ConnectionConfig.DEFAULT);

        // Configure total max or per route limits for persistent connections
        // that can be kept in the pool or leased by the connection manager.
        connManager.setMaxTotal(DEFAULT_MAX_TOTAL);
        connManager.setDefaultMaxPerRoute(DEFAULT_MAX_PRE_TOTAL);
//        connManager.setMaxPerRoute(new HttpRoute(new HttpHost("somehost", 80)), 20);

        // Use custom cookie store if necessary.
        cookieStore = new BasicCookieStore();
        // Use custom credentials provider if necessary.
        credentialsProvider = new BasicCredentialsProvider();
        // Create global request configuration
        defaultRequestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setExpectContinueEnabled(true)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
                .build();

        // 请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception,
                                        int executionCount, HttpContext context) {
                if (executionCount >= 5) { // 如果已经重试了5次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// SSL握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();

                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };


        // Create an HttpClient with the given custom dependencies and configuration.
        httpclient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultCookieStore(cookieStore)
                .setDefaultCredentialsProvider(credentialsProvider)
//            .setProxy(new HttpHost("", 8080))
                .setDefaultRequestConfig(defaultRequestConfig)
                .setRetryHandler(httpRequestRetryHandler)
                .build();
    }


    public ResponseData get(String url, Header[] headers) throws ClientProtocolException, IOException, URISyntaxException {
        return get(url, null, headers);
    }

    public ResponseData get(String url, Map<String, String> params, Header[] headers) throws ClientProtocolException, IOException, URISyntaxException {
        startTXTime = System.currentTimeMillis();
        final HttpGet httpGet = new HttpGet(url);
        httpGet.setHeaders(headers);
        // 帶入參數
        httpGet.setURI(buildGetQuery(httpGet.getURI(), params));
        // Request configuration can be overridden at the request level.
        // They will take precedence over the one set at the client level.
        httpGet.setConfig(getBaseRequestConfig());
        return handleExecute(httpGet);
    }

    public ResponseData post(final String url, final Map<String, String> params, final Header[] headers) throws ParseException, IOException {
        final HttpPost httpPost = new HttpPost(url);
        final List <NameValuePair> nvps = new ArrayList<>();
        for (Entry<String, String> param : params.entrySet()) {
            nvps.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
        httpPost.setHeaders(headers);
        httpPost.setConfig(getBaseRequestConfig());
        return handleExecute(httpPost);
    }

    public ResponseData post(final String url, final Header[] headers) throws ParseException, IOException {
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        httpPost.setConfig(getBaseRequestConfig());
        return handleExecute(httpPost);
    }

    public ResponseData put(final String url, final Map<String, String> params, final Header[] headers) throws ParseException, IOException {
        final HttpPut httpPut = new HttpPut(url);
        final List <NameValuePair> nvps = new ArrayList<>();
        for (Entry<String, String> param : params.entrySet()) {
            nvps.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        httpPut.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
        httpPut.setHeaders(headers);
        httpPut.setConfig(getBaseRequestConfig());
        return handleExecute(httpPut);
    }

    public ResponseData putJson(final String url, final String json, final Header[] headers) throws ParseException, IOException {
        final HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity(json, Consts.UTF_8));
        httpPut.setHeaders(headers);
        httpPut.setConfig(getBaseRequestConfig());
        return handleExecute(httpPut);
    }

    public ResponseData postJson(final String url, final String json, final Header[] headers) throws ParseException, IOException {
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(json, Consts.UTF_8));
        httpPost.setHeaders(headers);
        httpPost.setConfig(getBaseRequestConfig());
        return handleExecute(httpPost);
    }

    private ResponseData handleExecute(final HttpRequestBase httpType) throws ClientProtocolException, IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss.SSS");

        // Execution context can be customized locally.
        final HttpClientContext context = HttpClientContext.create();
        // Contextual attributes set the local context level will take
        // precedence over those set at the client level.
        context.setCredentialsProvider(credentialsProvider);
        try (CloseableHttpResponse response = httpclient.execute(httpType, context)) {
            // Once the request has been executed the local context can
            // be used to examine updated state and various objects affected
            // by the request execution.
            endTXTime = System.currentTimeMillis();
            System.out.println(formatter.format(new Date()) + "executing request " + httpType.getURI() + ", code = " + response.getStatusLine());
            return new ResponseData(httpType.getURI().toString(), context, response.getStatusLine(), EntityUtils.toString(response.getEntity(), Consts.UTF_8));
        }
    }

    private RequestConfig getBaseRequestConfig() {
        // Request configuration can be overridden at the request level.
        // They will take precedence over the one set at the client level.
        return RequestConfig.copy(defaultRequestConfig)
                .setSocketTimeout(MAX_CONNECT_TIMEOUT) // Https超時
                .setConnectTimeout(MAX_CONNECT_TIMEOUT) // 連線超時
                .setConnectionRequestTimeout(MAX_SO_TIMEOUT) // 請求超時
                .build();
    }

    public static URI buildGetQuery(final URI uri, final Map<String, String> params) throws URISyntaxException {
        if (params == null) return uri;
        final URIBuilder builder = new URIBuilder(uri);
        for (Entry<String, String> param : params.entrySet()) {
            builder.addParameter(param.getKey(), param.getValue());
        }
        return builder.build();
    }

    public static String buildGetQuery(final Map<String, String> params) {
        final URIBuilder builder = new URIBuilder();
        for (Entry<String, String> param : params.entrySet()) {
            builder.addParameter(param.getKey(), param.getValue());
        }
        return builder.toString();
    }

    // 一般
    public static final Header[] DATA_POST_HEADERS = {
            new BasicHeader("Accept", "text/plain, */*; q=0.01")
            ,new BasicHeader("Accept-Encoding", "gzip, deflate")
            ,new BasicHeader("Accept-Language", "zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4")
            ,new BasicHeader("Connection", "keep-alive")
            ,new BasicHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            ,new BasicHeader("X-Requested-With", "XMLHttpRequest")
            ,new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
    };

    /**
     * 取得資料用 JSON
     */
    public static final Header[] DATA_JSON_HEADERS = {
            new BasicHeader("Accept", "text/plain, */*; q=0.01")
            ,new BasicHeader("Accept-Encoding", "gzip, deflate, sdch")
            ,new BasicHeader("Accept-Language", "zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4")
            ,new BasicHeader("Connection", "keep-alive")
            ,new BasicHeader("Content-Type", "application/json; charset=utf8")
            ,new BasicHeader("X-Requested-With", "XMLHttpRequest")
            ,new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
    };

}
