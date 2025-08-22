// Testing library/framework note:
// Using JUnit 4 with Play's test helpers (play.test.Helpers) for rendering Twirl views in Java tests.
// If your project is on JUnit 5, replace org.junit imports with org.junit.jupiter equivalents and adapt assertions accordingly.
package views;

import org.junit.Test;
import static org.junit.Assert.*;
import play.twirl.api.Content;
import static play.test.Helpers.*;

public class IndexViewTest {
    // Tests will be appended below.
}

    @Test
    public void index_rendersHtmlWithExpectedMetaAndTitle() {
        // Arrange
        final String wsUrl = "ws://localhost:9000/ws/chat";

        // Act
        Content html = views.html.index.render(wsUrl);

        // Assert
        assertEquals("text/html", contentType(html));
        assertEquals("utf-8", charset(html));

        String body = contentAsString(html);
        assertTrue("Should contain DOCTYPE", body.toLowerCase().contains("<!doctype html>"));
        assertTrue("Should contain language attribute", body.contains("<html lang=\"en\">"));

        // Title and H1 from the template
        assertTrue("Should include page title 'Chat Room'", body.contains("<title>Chat Room</title>"));
        assertTrue("Should include H1 heading 'Chat Room'", body.contains("<h1 class=\"\">Chat Room</h1>"));
    }

    @Test
    public void index_includesBootstrapCdnWithSingleAtSymbolAfterTwirlEscaping() {
        final String wsUrl = "ws://example.org/socket";
        Content html = views.html.index.render(wsUrl);
        String body = contentAsString(html);

        // The template uses '@@' to emit a literal '@'. We should see a single '@' in the final HTML.
        // Validate both bootstrap and bootstrap-theme CDN links are present with '@3.4.1'
        assertTrue("Bootstrap core CDN with @3.4.1 should be present",
            body.contains("https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css"));
        assertTrue("Bootstrap theme CDN with @3.4.1 should be present",
            body.contains("https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap-theme.min.css"));
    }

    @Test
    public void index_includesAssetLinksForMainCssAndFavicon() {
        final String wsUrl = "ws://localhost/ws";
        Content html = views.html.index.render(wsUrl);
        String body = contentAsString(html);

        // Play's default reverse route for assets is /assets/...
        assertTrue("Should include main.css via reverse routes",
            body.contains("<link rel=\"stylesheet\" media=\"screen\" href=\"/assets/stylesheets/main.css\">"));
        assertTrue("Should include favicon via reverse routes",
            body.contains("<link rel=\"shortcut icon\" type=\"image/png\" href=\"/assets/images/favicon.png\">"));
    }

    @Test
    public void index_includesCriticalDomElementsForChatUi() {
        final String wsUrl = "ws://localhost:9000/ws";
        Content html = views.html.index.render(wsUrl);
        String body = contentAsString(html);

        // Elements that the client-side JS depends on
        assertTrue("Should include messages list element", body.contains("<ul id=\"messages\" class=\"list-unstyled\">"));
        assertTrue("Should include input#message", body.contains("id=\"message\""));
        assertTrue("Should include button#send", body.contains("id=\"send\""));
        assertTrue("Send button should be configured as a block-level primary button",
            body.contains("class=\"btn btn-primary btn-lg btn-block\""));
    }

    @Test
    public void index_includesJqueryAndInitialDisablingOfSendButton() {
        final String wsUrl = "ws://host/ws";
        Content html = views.html.index.render(wsUrl);
        String body = contentAsString(html);

        // jQuery include
        assertTrue("Should include jQuery 1.11.1",
            body.contains("<script src=\"https://code.jquery.com/jquery-1.11.1.min.js\"></script>"));

        // JS snippet: $send.prop("disabled", true);
        assertTrue("Should disable send button initially via JS",
            body.contains("$send.prop(\"disabled\", true);"));
    }

    @Test
    public void index_injectsWebSocketUrlIntoScript() {
        final String wsUrl = "ws://chat.example.com:8080/room?user=joe";
        Content html = views.html.index.render(wsUrl);
        String body = contentAsString(html);

        // Must appear exactly as provided inside the WebSocket initialization
        String expectedLine = "connection = new WebSocket(\"" + wsUrl + "\");";
        assertTrue("Should inject provided WebSocket URL into the client script",
            body.contains(expectedLine));
    }

    @Test
    public void index_handlesEmptyWebSocketUrl_gracefullyAsRenderedString() {
        final String wsUrl = "";
        Content html = views.html.index.render(wsUrl);
        String body = contentAsString(html);

        // Even if empty, the template renders the string inside quotes (not executing JS here)
        assertTrue("Should render WebSocket call even with empty URL",
            body.contains("connection = new WebSocket(\"\");"));
    }

    @Test
    public void index_containsMessageHandlersAndKeyBindings() {
        final String wsUrl = "ws://x/ws";
        Content html = views.html.index.render(wsUrl);
        String body = contentAsString(html);

        // Verify presence of key JS behaviors to catch regressions
        assertTrue("Should register onopen handler", body.contains("connection.onopen = function () {"));
        assertTrue("Should enable send button on open", body.contains("$send.prop(\"disabled\", false);"));
        assertTrue("Should prepend 'Connected' message", body.contains("Connected</li>"));
        assertTrue("Should attach click handler to #send", body.contains("$send.on('click', send);"));
        assertTrue("Should handle Enter key in #message", body.contains("$message.keypress(function(event){"));
        assertTrue("Should handle websocket error", body.contains("connection.onerror = function (error) {"));
        assertTrue("Should append incoming messages", body.contains("connection.onmessage = function (event) {"));
    }
}