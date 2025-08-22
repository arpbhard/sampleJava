package views;

import org.junit.Test;
import static org.junit.Assert.*;

import play.twirl.api.Content;

// Note: Adjust the import below if your template package/name differs.
// By convention, index.scala.html compiles to views.html.index for Java access.
public class IndexViewTest {

    private static String renderToString(String url) {
        // Render the Twirl template and return its string content.
        // If your template is not named 'index.scala.html', update the reference.
        Content html = views.html.index.render(url);
        // In Play, Content.body() returns the full rendered HTML string.
        return html.body();
    }

    @Test
    public void rendersHappyPathWithWebSocketUrl() {
        String wsUrl = "ws://localhost:9000/ws/chat";
        String html = renderToString(wsUrl);

        // Basic sanity
        assertNotNull("Rendered HTML should not be null", html);
        assertTrue("Should contain HTML5 doctype", html.contains("<!DOCTYPE html>"));

        // Title and header
        assertTrue("Should include page title", html.contains("<title>Chat Room</title>"));
        assertTrue("Should include H1 heading", html.contains("<h1 class=\"\">Chat Room</h1>"));

        // Critical elements by id
        assertTrue("Messages list should be present", html.contains("id=\"messages\""));
        assertTrue("Input field should be present", html.contains("id=\"message\""));
        assertTrue("Send button should be present", html.contains("id=\"send\""));

        // JS initializes WebSocket with provided url
        assertTrue("WebSocket URL should be interpolated into JS",
                html.contains("new WebSocket(\"" + wsUrl + "\");"));

        // Verify jQuery includes (the exact version may vary; check presence of the script tag)
        assertTrue("Should include jQuery script", html.contains("code.jquery.com/jquery-1.11.1.min.js"));
    }

    @Test
    public void rendersLiteralAtSymbolInCdnUrls() {
        String html = renderToString("ws://example.com/socket");

        // Twirl requires '@@' in source to render a single '@' in output.
        // The template includes bootstrap@@3.4.1; confirm it renders with a single '@'.
        assertTrue("Bootstrap CDN should include single '@' version segment",
                html.contains("bootstrap@3.4.1/dist/css/bootstrap.min.css"));
        assertTrue("Bootstrap theme CDN should include single '@' version segment",
                html.contains("bootstrap@3.4.1/dist/css/bootstrap-theme.min.css"));

        // Ensure we did not leak double '@@' tokens in the output
        assertFalse("Output must not contain raw '@@' Twirl escape",
                html.contains("@@3.4.1"));
    }

    @Test
    public void includesCriticalAttributesAndClasses() {
        String html = renderToString("ws://example.org/ws");

        // Input attributes
        assertTrue("Input should have placeholder",
                html.contains("placeholder=\"Type Here\""));
        assertTrue("Input should disable autocomplete",
                html.contains("autocomplete=\"off\""));
        assertTrue("Input should disable spellcheck",
                html.contains("spellcheck=\"false\""));
        assertTrue("Input should disable autocorrect",
                html.contains("autocorrect=\"off\""));
        assertTrue("Input should be large form-control",
                html.contains("class=\"form-control input-lg\""));

        // Button classes
        assertTrue("Send button should have bootstrap classes",
                html.contains("class=\"btn btn-primary btn-lg btn-block\""));

        // Messages list should be unstyled
        assertTrue("Messages list should be unstyled",
                html.contains("id=\"messages\" class=\"list-unstyled\""));
    }

    @Test
    public void handlesEmptyUrlByRenderingVerbatim() {
        String html = renderToString("");
        // When empty, it still should interpolate as empty string in the JS constructor
        assertTrue("WebSocket constructor should still be present",
                html.contains("new WebSocket(\"\");"));
    }

    @Test
    public void handlesNullUrlByRenderingStringNull() {
        // Twirl renders null parameters via String.valueOf which yields "null" in output.
        String html = renderToString(null);
        assertTrue("Null URL should render as literal \"null\" in JavaScript",
                html.contains("new WebSocket(\"null\");"));
    }

    @Test
    public void includesAssetLinks() {
        String html = renderToString("ws://localhost:9000/ws");
        // We expect the compiled reverse routes to generate paths under /assets/...
        // Exact digests or prefixes may vary; we check for key segments.
        assertTrue("Should link main stylesheet via Assets route",
                html.contains("stylesheets/main.css"));
        assertTrue("Should link favicon via Assets route",
                html.contains("images/favicon.png"));
    }

    @Test
    public void contentTypeIsHtml() {
        // Validate content type for sanity; not all Play versions expose ContentType easily,
        // but Content in Play exposes contentType() returning "text/html".
        play.twirl.api.Content html = views.html.index.render("ws://x/ws");
        assertEquals("text/html", html.contentType());
        assertNotNull("Body should not be null", html.body());
        assertFalse("Body should not be empty", html.body().isEmpty());
    }
}