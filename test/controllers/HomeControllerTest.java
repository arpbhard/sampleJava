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
// Additional tests generated to improve coverage for HomeController WebSocket and origin checks.
// Framework: JUnit with Play test helpers (Java), Akka ActorSystem/Materializer via Guice application.
package controllers;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import play.mvc.WebSocket;

import akka.actor.ActorSystem;
import akka.stream.Materializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.*;
import static play.test.Helpers.*;

// We define a separate test class to avoid interfering with any existing tests/class definitions.
public class HomeControllerWebSocketTest {

    private Application app;
    private HomeController controller;

    @Before
    public void setUp() {
        app = new GuiceApplicationBuilder().build();
        Helpers.start(app);
        controller = app.injector().instanceOf(HomeController.class);
        assertNotNull("Controller should be injected", controller);
    }

    @After
    public void tearDown() {
        if (app != null) {
            Helpers.stop(app);
        }
    }

    @Test
    public void index_shouldReturnOk_andContainWebSocketUrl() {
        Result result = controller.index();
        assertEquals(OK, result.status());
        String body = Helpers.contentAsString(result, mat());
        // Expect the rendered index page to contain the chat websocket endpoint path or ws:// URL fragment
        assertTrue("Index should contain /chat", body.contains("/chat"));
    }

    @Test
    public void chat_shouldAccept_whenSingleAllowedOrigin_localhost9000() throws Exception {
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://localhost:9000");

        WebSocket ws = controller.chat();

        CompletionStage<play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>>> stage =
                ws.f(requestFactory(rb));

        play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>> either =
                stage.toCompletableFuture().get();

        assertTrue("Expected Right (accepted) when origin allowed", either.right.isDefined());
    }

    @Test
    public void chat_shouldAccept_whenSingleAllowedOrigin_localhost19001() throws Exception {
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://localhost:19001");

        WebSocket ws = controller.chat();

        CompletionStage<play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>>> stage =
                ws.f(requestFactory(rb));

        play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>> either =
                stage.toCompletableFuture().get();

        assertTrue("Expected Right (accepted) when origin allowed", either.right.isDefined());
    }

    @Test
    public void chat_shouldReject_whenMultipleOriginHeadersPresent() throws Exception {
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat");
        // Simulate multiple Origin headers by setting raw headers on the RequestHeader
        rb = rb.headers(new Http.Headers(
                Arrays.asList(
                        new Http.Header("Origin", Arrays.asList("http://localhost:9000", "http://evil.example:80"))
                )
        ));

        WebSocket ws = controller.chat();

        CompletionStage<play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>>> stage =
                ws.f(requestFactory(rb));

        play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>> either =
                stage.toCompletableFuture().get();

        assertTrue("Expected Left (rejected) when multiple origins present", either.left.isDefined());
        assertEquals(FORBIDDEN, either.left.get().status());
    }

    @Test
    public void chat_shouldReject_whenNullOrigin() throws Exception {
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat");
        // No Origin header -> internal sameOriginCheck reads list; if empty, List.get(0) would throw.
        // Construct a header with a single null entry to cover originMatches(null) path:
        rb = rb.headers(new Http.Headers(
                Collections.singletonList(new Http.Header("Origin", Collections.singletonList(null)))
        ));

        WebSocket ws = controller.chat();
        CompletionStage<play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>>> stage =
                ws.f(requestFactory(rb));
        play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>> either =
                stage.toCompletableFuture().get();

        assertTrue("Expected Left (rejected) when origin is null", either.left.isDefined());
        assertEquals(FORBIDDEN, either.left.get().status());
    }

    @Test
    public void chat_shouldReject_whenMalformedOrigin() throws Exception {
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", ":// bad uri ^^^");

        WebSocket ws = controller.chat();
        CompletionStage<play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>>> stage =
                ws.f(requestFactory(rb));
        play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>> either =
                stage.toCompletableFuture().get();

        assertTrue("Expected Left (rejected) for malformed origin", either.left.isDefined());
        assertEquals(FORBIDDEN, either.left.get().status());
    }

    @Test
    public void chat_shouldReject_whenWrongHost() throws Exception {
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://example.com:9000");

        WebSocket ws = controller.chat();
        CompletionStage<play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>>> stage =
                ws.f(requestFactory(rb));
        play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>> either =
                stage.toCompletableFuture().get();

        assertTrue("Expected Left (rejected) for wrong host", either.left.isDefined());
        assertEquals(FORBIDDEN, either.left.get().status());
    }

    @Test
    public void chat_shouldReject_whenWrongPort() throws Exception {
        Http.RequestBuilder rb = new Http.RequestBuilder()
                .method("GET")
                .uri("/chat")
                .header("Origin", "http://localhost:1234");

        WebSocket ws = controller.chat();
        CompletionStage<play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>>> stage =
                ws.f(requestFactory(rb));
        play.libs.F.Either<Result, akka.stream.javadsl.Flow<String,String,akka.NotUsed>> either =
                stage.toCompletableFuture().get();

        assertTrue("Expected Left (rejected) for wrong port", either.left.isDefined());
        assertEquals(FORBIDDEN, either.left.get().status());
    }

    // Utility to build a RequestHeader compatible with WebSocket.acceptOrResult callback
    private Http.RequestHeader requestFactory(Http.RequestBuilder rb) {
        return rb.build();
    }

    // Materializer helper for contentAsString
    private Materializer mat() {
        return app.injector().instanceOf(Materializer.class);
    }
}