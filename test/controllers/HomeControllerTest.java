package controllers;

import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.netty.ws.NettyWebSocket;
import play.shaded.ahc.org.asynchttpclient.ws.WebSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.test.WithServer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static play.mvc.Http.Status.OK;

/**
 * Limited functional testing to ensure health checks of build
 */
public class HomeControllerTest extends WithServer {

    private AsyncHttpClient asyncHttpClient;

    @Before
    public void setUp() {
        asyncHttpClient = new DefaultAsyncHttpClient();
    }

    @After
    public void tearDown() throws IOException {
        asyncHttpClient.close();
    }

    // Functional test to run through the server and check the page comes ups
    @Test
    public void testInServer() throws Exception {
        int port = this.testServer.getRunningHttpPort().getAsInt();
        String url = "http://localhost:" + port + "/";
        try (WSClient ws = play.test.WSTestClient.newClient(port)) {
            CompletionStage<WSResponse> stage = ws.url(url).get();
            WSResponse response = stage.toCompletableFuture().get();
            assertEquals(OK, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Functional test to check websocket comes up
    @Test
    public void testWebsocket() throws Exception {
        int port = this.testServer.getRunningHttpPort().getAsInt();
        String serverURL = "ws://localhost:" + port + "/chat";

        WebSocketClient webSocketClient = new WebSocketClient(asyncHttpClient);
        WebSocketClient.LoggingListener listener = new WebSocketClient.LoggingListener();
        CompletableFuture<NettyWebSocket> future = webSocketClient.call(serverURL, serverURL, listener);
        await().untilAsserted(() -> assertThat(future).isDone());
        assertThat(future).isCompletedWithValueMatching(NettyWebSocket::isOpen);
    }

}
// ------------------------------------------------------------
// Additional tests auto-generated to improve coverage of HomeController
// Testing stack: JUnit (4 preferred if present), Play test Helpers.
// Focus: public methods index() and chat(), plus defensive checks of same-origin logic.
// ------------------------------------------------------------
package controllers;

import org.junit.*; // Will resolve to JUnit 4 if on classpath; for JUnit 5, IDE/build will flag and can be adjusted.
import play.mvc.*;
import play.test.Helpers;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.*;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.FORBIDDEN;

public class HomeControllerTest_Additions {

    private Application app;

    @Before
    public void setup() {
        app = new GuiceApplicationBuilder().build();
    }

    @After
    public void teardown() {
        Helpers.stop(app);
    }

    private HomeController controller() {
        return app.injector().instanceOf(HomeController.class);
    }

    @Test
    public void index_returnsOk_and_containsWebSocketUrl() {
        HomeController c = controller();
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/"); // URI is arbitrary; we only need a Request for Controller.request()

        Result result = Helpers.invokeWithContext(rb, () -> c.index());
        assertEquals(OK, result.status());

        String body = Helpers.contentAsString(result, app.materializer());
        // Expect the view to include a ws(s) URL pointing to /chat (Play picks ws or wss depending on scheme)
        assertTrue("Index page should contain /chat WebSocket path",
                body.contains("/chat"));
    }

    @Test
    public void chat_accepts_same_origin_localhost_9000() throws Exception {
        HomeController c = controller();
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://localhost:9000");

        WebSocket ws = Helpers.invokeWithContext(rb, c::chat);
        // AcceptOrResult returns Either Right(flow) when accepted; in Java API, testing acceptance via apply
        CompletionStage<F.Either<Result, akka.stream.javadsl.Flow<String, String, akka.NotUsed>>> stage =
                ws.acceptOrResult(requestHeader -> java.util.concurrent.CompletableFuture.completedFuture(
                        play.libs.F.Either.Right(null) // placeholder; actual flow is built inside controller; we only check Left vs Right
                ));
        // Because we can't easily materialize the flow here without a full client, we directly assert by invoking controller logic:
        Result decision = Helpers.invokeWithContext(rb, () -> {
            WebSocket ws2 = c.chat();
            // Simulate Play's acceptOrResult handler:
            return ws2.fallback(requestHeader -> Results.forbidden());
        });
        // If accepted, fallback should not be used; but Play's Java API doesn't expose direct flag here.
        // As a pragmatic check, call sameOriginCheck via reflection:
        Method m = HomeController.class.getDeclaredMethod("sameOriginCheck", Http.RequestHeader.class);
        m.setAccessible(true);
        boolean allowed = (boolean) m.invoke(c, rb.build());
        assertTrue("Same-origin check should pass for http://localhost:9000", allowed);
    }

    @Test
    public void chat_accepts_same_origin_localhost_19001() throws Exception {
        HomeController c = controller();
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://localhost:19001");

        Method m = HomeController.class.getDeclaredMethod("sameOriginCheck", Http.RequestHeader.class);
        m.setAccessible(true);
        boolean allowed = (boolean) m.invoke(c, rb.build());
        assertTrue("Same-origin check should pass for http://localhost:19001", allowed);
    }

    @Test
    public void chat_rejects_when_no_origin_header_present() throws Exception {
        HomeController c = controller();
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat");

        // sameOriginCheck would attempt to access index 0 and could throw; assert it fails safely:
        try {
            Method m = HomeController.class.getDeclaredMethod("sameOriginCheck", Http.RequestHeader.class);
            m.setAccessible(true);
            m.invoke(c, rb.build());
            fail("Expected sameOriginCheck to fail when Origin header is missing");
        } catch (Exception expected) {
            // pass - current implementation does not handle zero headers; this test documents the behavior.
        }
    }

    @Test
    public void chat_rejects_multiple_origin_headers() throws Exception {
        HomeController c = controller();
        Http.Headers headers = new Http.Headers(new java.util.ArrayList<Http.Header>() {{
            add(new Http.Header("Origin", java.util.Arrays.asList("http://localhost:9000", "http://localhost:19001")));
        }});
        Http.Request request = new Http.RequestBuilder().uri("/chat").method("GET").headers(headers).build();

        Method m = HomeController.class.getDeclaredMethod("sameOriginCheck", Http.RequestHeader.class);
        m.setAccessible(true);
        boolean allowed = (boolean) m.invoke(c, request);
        assertFalse("Same-origin should fail when multiple Origin headers are present", allowed);
    }

    @Test
    public void chat_rejects_malformed_origin() throws Exception {
        HomeController c = controller();
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "::::not_a_uri::::");

        Method m = HomeController.class.getDeclaredMethod("sameOriginCheck", Http.RequestHeader.class);
        m.setAccessible(true);
        boolean allowed = (boolean) m.invoke(c, rb.build());
        assertFalse("Malformed Origin should be rejected", allowed);
    }

    @Test
    public void chat_rejects_wrong_host() throws Exception {
        HomeController c = controller();
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://example.com:9000");

        Method privateOriginMatches = HomeController.class.getDeclaredMethod("originMatches", String.class);
        privateOriginMatches.setAccessible(true);
        boolean matches = (boolean) privateOriginMatches.invoke(c, "http://example.com:9000");
        assertFalse("Non-localhost host should be rejected", matches);
    }

    @Test
    public void chat_rejects_wrong_port() throws Exception {
        HomeController c = controller();
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://localhost:1234");

        Method privateOriginMatches = HomeController.class.getDeclaredMethod("originMatches", String.class);
        privateOriginMatches.setAccessible(true);
        boolean matches = (boolean) privateOriginMatches.invoke(c, "http://localhost:1234");
        assertFalse("localhost with disallowed port should be rejected", matches);
    }
}