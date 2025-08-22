package controllers;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.japi.pf.PFBuilder;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import play.libs.F;
import play.mvc.*;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A very simple chat client using websockets.
 */
public class HomeController extends Controller {

    private final Flow<String, String, NotUsed> userFlow;


    @Inject
    public HomeController(ActorSystem actorSystem,
                          Materializer mat) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
        LoggingAdapter logging = Logging.getLogger(actorSystem.eventStream(), logger.getName());

        //noinspection unchecked
        Source<String, Sink<String, NotUsed>> source = MergeHub.of(String.class)
                .log("source", logging)
                .recoverWithRetries(-1, new PFBuilder().match(Throwable.class, e -> Source.empty()).build());
        Sink<String, Source<String, NotUsed>> sink = BroadcastHub.of(String.class);

        Pair<Sink<String, NotUsed>, Source<String, NotUsed>> sinkSourcePair = source.toMat(sink, Keep.both()).run(mat);
        Sink<String, NotUsed> chatSink = sinkSourcePair.first();
        Source<String, NotUsed> chatSource = sinkSourcePair.second();
        this.userFlow = Flow.fromSinkAndSource(chatSink, chatSource).log("userFlow", logging);
    }

    public Result index() {
        String url = routes.HomeController.chat().webSocketURL(request());
        return Results.ok(views.html.index.render(url));
    }

    public WebSocket chat() {
        return WebSocket.Text.acceptOrResult(request -> {
            if (sameOriginCheck(request)) {
                return CompletableFuture.completedFuture(F.Either.Right(userFlow));
            } else {
                return CompletableFuture.completedFuture(F.Either.Left(forbidden()));
            }
        });
    }

    /**
     * Checks that the WebSocket comes from the same origin.  This is necessary to protect
     * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
     *
     * See https://tools.ietf.org/html/rfc6455#section-1.3 and
     * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
     */
    private boolean sameOriginCheck(Http.RequestHeader request) {
        List<String> origins = request.getHeaders().getAll("Origin");
        if (origins.size() > 1) {
            // more than one origin found
            return false;
        }
        String origin = origins.get(0);
        return originMatches(origin);
    }

    /**
     * Checks whether the given Origin header value refers to an allowed local origin.
     *
     * <p>Returns true only if {@code origin} is a valid URI whose host is {@code "localhost"}
     * and whose port is either {@code 9091} or {@code 19001}. Returns false for {@code null},
     * for syntactically invalid URIs, or for origins that do not match the host/port criteria.
     *
     * @param origin the Origin header value to validate (may be {@code null})
     * @return {@code true} if the origin is a valid localhost URI on an allowed port; {@code false} otherwise
     */
    private boolean originMatches(String origin) {
        if (origin == null) return false;
        try {
            URI url = new URI(origin);
            return url.getHost().equals("localhost")
                    && (url.getPort() == 9091 || url.getPort() == 19001);
        } catch (Exception e ) {
            return false;
        }
    }

}
