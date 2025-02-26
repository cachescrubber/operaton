package org.operaton.bpm;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MediaType;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.openqa.selenium.chrome.ChromeDriverService;

import org.operaton.bpm.util.TestUtil;
import org.junit.After;
import org.junit.Before;

/**
 * Abstract integration test class for web interactions using Apache HttpComponents 5.
 *
 * @author Daniel Meyer
 * @author Roman Smirnov
 */
public abstract class AbstractWebIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(AbstractWebIntegrationTest.class.getName());

  protected static final String TASKLIST_PATH = "app/tasklist/default/";

  protected static final String COOKIE_HEADER = "Cookie";
  protected static final String X_XSRF_TOKEN_HEADER = "X-XSRF-TOKEN";

  protected static final String JSESSIONID_IDENTIFIER = "JSESSIONID=";
  protected static final String XSRF_TOKEN_IDENTIFIER = "XSRF-TOKEN=";

  protected static final String HOST_NAME = "localhost";

  protected String appBasePath;
  protected String appUrl;
  protected TestUtil testUtil;
  protected TestProperties testProperties;

  protected static ChromeDriverService service;
  protected CloseableHttpClient client;

  protected String csrfToken;
  protected String sessionId;

  @Before
  public void before() throws IOException {
    testProperties = new TestProperties(48080);
    testUtil = new TestUtil(testProperties);
    client = HttpClients.createDefault();
  }

  @After
  public void destroyClient() throws IOException {
    if (client != null) {
      client.close();
    }
  }

  public void createClient(String ctxPath) throws IOException {
    testProperties = new TestProperties();
    appBasePath = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application " + appBasePath);
  }

  protected void getTokens() throws IOException {
    // First request to retrieve initial cookies
    HttpGet initialRequest = new HttpGet(appBasePath + TASKLIST_PATH);
    try (CloseableHttpResponse response = client.execute(initialRequest)) {
      List<String> cookieValues = getCookieHeaders(response);
      csrfToken = getCookie(cookieValues, XSRF_TOKEN_IDENTIFIER);
      sessionId = getCookie(cookieValues, JSESSIONID_IDENTIFIER);
    }

    // Login request
    HttpPost loginRequest = new HttpPost(appBasePath + "api/admin/auth/user/default/login/cockpit");
    loginRequest.setHeader(COOKIE_HEADER, createCookieHeader(csrfToken, sessionId));
    loginRequest.setHeader(X_XSRF_TOKEN_HEADER, csrfToken);
    loginRequest.setEntity(new StringEntity("username=demo&password=demo", ContentType.APPLICATION_FORM_URLENCODED));

    try (CloseableHttpResponse response = client.execute(loginRequest)) {
      List<String> cookieValues = getCookieHeaders(response);
      sessionId = getCookie(cookieValues, JSESSIONID_IDENTIFIER);
    }
  }

  protected List<String> getCookieHeaders(CloseableHttpResponse response) {
    return Stream.of(response.getHeaders("Set-Cookie")).map(NameValuePair::getValue).toList();
  }

  protected String getCookie(List<String> cookieValues, String cookieName) {
    return cookieValues.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst().orElse("").substring(cookieName.length());
  }

  protected String createCookieHeader() {
    return createCookieHeader(csrfToken, sessionId);
  }

  protected String createCookieHeader(String csrf, String session) {
    return XSRF_TOKEN_IDENTIFIER + csrf + "; " + JSESSIONID_IDENTIFIER + session;
  }

  protected void preventRaceConditions() throws InterruptedException {
    // Wait to avoid race conditions with Wildfly / Cargo
    Thread.sleep(5 * 1000L);
  }
}
