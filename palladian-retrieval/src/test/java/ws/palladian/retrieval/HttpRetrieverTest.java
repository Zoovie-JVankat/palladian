package ws.palladian.retrieval;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.CollectionHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("restriction")
public class HttpRetrieverTest {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRetrieverTest.class);

    private HttpServer httpServer;

    private int port;

    @Before
    public void setUp() {
        port = pickPort();
        InetSocketAddress address = new InetSocketAddress(port);
        try {
            httpServer = HttpServer.create(address, 0);
        } catch (IOException e) {
            LOGGER.warn("Could not start HTTP test server: " + e.getMessage() + ", skipping test");
            Assume.assumeNoException(e);
        }
        httpServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
            }
        });
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();
    }

    private int pickPort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @After
    public void tearDown() {
        httpServer.stop(0);
    }

    @Test(timeout = 5000, expected = HttpException.class)
    public void testGetTimeout() throws HttpException {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        httpRetriever.setConnectionTimeout(1000);
        httpRetriever.setSocketTimeout(1000);
        httpRetriever.setNumRetries(0);
        httpRetriever.httpGet("http://localhost:" + port + "/");
    }

    /** See : https://bitbucket.org/palladian/palladian/issue/133/redirecting-throws-unexpected-error */
    @Test
    @Ignore
    public void testRedirects_issue133() throws Exception {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        String url = "http://feedproxy.google.com/~r/pwcooks/~3/iqsvvvykjpo/";
        List<String> redirectUrls = httpRetriever.getRedirectUrls(url);
        CollectionHelper.print(redirectUrls);
    }

    /** See: https://dev.twitter.com/docs/tco-redirection-behavior */
    @Test
    @Ignore
    public void testRedirects_tco() throws Exception {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        List<String> urls = httpRetriever.getRedirectUrls("http://t.co/RGJg5LjK");
        CollectionHelper.print(urls);
    }

    /** See: https://bitbucket.org/palladian/palladian/issue/286/possibility-to-accept-cookies-in */
    @Test
    @Ignore
    public void testCookies_issue286() throws HttpException {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        String url = "http://arc.aiaa.org/action/showPublications?pubType=meetingProc";
        HttpResult httpResult = httpRetriever.httpGet(url);
        System.out.println(httpResult.getStatusCode()); // is 200 :/
        System.out.println(httpResult.getStringContent()); // gives a description, that we need to enable cookies
    }

    @Test
    @Ignore
    public void testCookies() throws HttpException {
        // TODO write an actual test using a locally running HTTP server
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        DefaultCookieStore cookieStore = new DefaultCookieStore();
        httpRetriever.setCookieStore(cookieStore);

        // get the cookie
        HttpRequest2Builder builder = new HttpRequest2Builder(ws.palladian.retrieval.HttpMethod.GET, "https://bitbucket.org/account/signin/?next=/");
        httpRetriever.execute(builder.create());
        String csrftoken = CollectionHelper.getFirst(cookieStore.getCookies()).getValue();

        builder = new HttpRequest2Builder(ws.palladian.retrieval.HttpMethod.POST, "https://bitbucket.org/account/signin/?next=/");
        builder.addHeader("Origin", "https://bitbucket.org");
        builder.addHeader("Referer", "https://bitbucket.org/account/signin/?next=/account/signin");
        FormEncodedHttpEntity.Builder entityBuilder = new FormEncodedHttpEntity.Builder();
        entityBuilder.addData("username", "thats_me");
        entityBuilder.addData("password", "dont_wanna_tell");
        entityBuilder.addData("next", "/account/team_check/?next=/");
        entityBuilder.addData("csrfmiddlewaretoken", csrftoken);
        builder.setEntity(entityBuilder.create());
        HttpResult result = httpRetriever.execute(builder.create());
        // System.out.println(result.getStatusCode());
        // System.out.println(result.getStringContent());
        // CollectionHelper.print(result.getHeaders());
        assertEquals(302, result.getStatusCode());
    }

    @Test
    @Ignore
    public void testGetHttpRequest2() throws HttpException {
        HttpRequest2 request = new HttpRequest2Builder(ws.palladian.retrieval.HttpMethod.GET, "http://example.com").create();
        HttpResult result = HttpRetrieverFactory.getHttpRetriever().execute(request);
        System.out.println(result.getStringContent());
    }

    @Test
    public void testCookieWarning() throws HttpException {
        HttpRetrieverFactory.getHttpRetriever().httpGet("http://www.home24.de/arte-m/gallery-kommode-buche-2");
    }

    @Test
    public void testSelfSignedCertificate() throws HttpException {
        try (HttpRetrieverFactory factory = new HttpRetrieverFactory(true)) {
            HttpRetriever retriever = factory.create();
            HttpResult result = retriever.httpGet("https://jira.contasa.net");
            System.out.println(result);
        }
    }

    @Test
    public void testGetExample() throws HttpException {
        HttpResult result = HttpRetrieverFactory.getHttpRetriever().httpGet("http://example.com");
        System.out.println(result);
    }

    @Test
    @Ignore // TODO
    public void testGetCircularRedirects() throws HttpException {
        HttpResult result;

        result = HttpRetrieverFactory.getHttpRetriever().httpGet("https://www.westpac.co.nz/rednews/get-on-top-of-your-finances-by-plugging-your-money-leaks/");
        assertEquals(result.getStatusCode(), 200);

        result = HttpRetrieverFactory.getHttpRetriever().httpGet("https://www.journaldequebec.com/2023/10/05/les-geants-de-lepicerie-promettent-des-rabais");
        assertEquals(result.getStatusCode(), 200);

        result = HttpRetrieverFactory.getHttpRetriever().httpGet("https://www.washingtonpost.com/business/2023/10/02/corporate-diversity-inclusion-affirmative-action-ruling/");
        assertEquals(result.getStatusCode(), 200);
    }

    @Test
    public void testUnknownCertificate() throws HttpException {
        try (HttpRetrieverFactory factory = new HttpRetrieverFactory(true)) {
            HttpRetriever retriever = factory.create();
            HttpResult result = retriever.httpGet("https://dashingdish.com/recipe/1-minute-cookie-dough-greek-yogurt/");
            System.out.println(result);
        }
    }

    @Test
    @Ignore
    public void testSslSni() throws HttpException {
        try (HttpRetrieverFactory factory = new HttpRetrieverFactory(true)) {
            HttpRetriever retriever = factory.create();
            HttpResult result = retriever.httpGet("https://www.modernalchemyair.com/");
            System.out.println(result);
        }
    }

    @Test
    public void testSSLPeerUnverified() throws HttpException {
        try (HttpRetrieverFactory factory = new HttpRetrieverFactory(true)) {
            HttpRetriever retriever = factory.create();
            HttpResult result = retriever.httpGet("https://api.cilabs.net/v1/conferences/ws15/info/attendees");
            System.out.println(result);
        }
    }

    @Test
    public void testJsonCharset() throws HttpException {
        var result = HttpRetrieverFactory.getHttpRetriever().httpGet("https://httpbin.org/json");
        assertEquals(result.getCharsetOrDefault(), StandardCharsets.UTF_8);
    }
}
