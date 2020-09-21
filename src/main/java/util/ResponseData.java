package util;

import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthState;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;

public class ResponseData {

    private final String requestUri;
    private final StatusLine statusLine;
    private final HttpRequest httpRequest;
    private final RouteInfo httpRoute;
    private final AuthState targetAuthState;
    private final AuthState proxyAuthState;
    private final CookieOrigin cookieOrigin;
    private final CookieSpec cookieSpec;
    private final Object usetToken;
    private final String httpResponse;

    public ResponseData(String requestUri, HttpClientContext context, StatusLine statusLine, String response) {
        this.requestUri = requestUri;
        this.httpRequest = context.getRequest();
        this.httpRoute = context.getHttpRoute();
        this.targetAuthState = context.getTargetAuthState();
        this.proxyAuthState = context.getProxyAuthState();
        this.cookieOrigin = context.getCookieOrigin();
        this.cookieSpec = context.getCookieSpec();
        this.usetToken = context.getUserToken();
        this.statusLine = statusLine;
        this.httpResponse = response;
    }

    public String getRequestUri() {
        return requestUri;
    }

    /**
     * http request status
     * @return
     */
    public StatusLine getStatusLine() {
        return statusLine;
    }

    /**
     * Last executed request
     * @return
     */
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Execution route
     * @return
     */
    public RouteInfo getHttpRoute() {
        return httpRoute;
    }

    /**
     * Target auth state
     * @return
     */
    public AuthState getTargetAuthState() {
        return targetAuthState;
    }

    /**
     * Proxy auth state
     * @return
     */
    public AuthState getProxyAuthState() {
        return proxyAuthState;
    }

    /**
     * Cookie origin
     * @return
     */
    public CookieOrigin getCookieOrigin() {
        return cookieOrigin;
    }

    /**
     * Cookie spec used
     * @return
     */
    public CookieSpec getCookieSpec() {
        return cookieSpec;
    }

    /**
     * User security token
     * @return
     */
    public Object getUsetToken() {
        return usetToken;
    }

    /**
     * response
     * @return
     */
    public String getHttpResponse() {
        return httpResponse;
    }

}
