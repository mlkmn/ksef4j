package io.github.mlkmn.ksef4j.internal.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** In-process KSeF stand-in: stub responses per path (single or sequential), capture requests. */
public final class FakeKsef implements AutoCloseable {

    public record RecordedRequest(String method, String path, Map<String, String> headers, String body) {
    }

    public record Stub(int status, byte[] body, String contentType) {
        public static Stub json(int status, String json) {
            return new Stub(status, json.getBytes(StandardCharsets.UTF_8), "application/json");
        }

        public static Stub bytes(int status, byte[] body, String contentType) {
            return new Stub(status, body, contentType);
        }
    }

    private final HttpServer server;
    public final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private final Map<String, List<Stub>> stubs = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
    private final Map<String, Function<List<RecordedRequest>, Stub>> dynamicStubs = new ConcurrentHashMap<>();

    public FakeKsef() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    public void stubJson(String path, int status, String json) {
        stubSequence(path, Stub.json(status, json));
    }

    public void stubBytes(String path, int status, byte[] body, String contentType) {
        stubSequence(path, Stub.bytes(status, body, contentType));
    }

    /** Serve {@code path} with a response computed from the requests recorded so far. */
    public void stubDynamic(String path, Function<List<RecordedRequest>, Stub> responder) {
        dynamicStubs.put(path, responder);
    }

    /** Successive requests to {@code path} return successive responses; the last one is sticky. */
    public void stubSequence(String path, Stub... responses) {
        stubs.put(path, List.of(responses));
        calls.put(path, new AtomicInteger());
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] reqBody = exchange.getRequestBody().readAllBytes();
        Map<String, String> headers = new HashMap<>();
        exchange.getRequestHeaders().forEach((k, v) -> headers.put(k, String.join(",", v)));
        String path = exchange.getRequestURI().getPath();
        requests.add(new RecordedRequest(
                exchange.getRequestMethod(), path, headers,
                new String(reqBody, StandardCharsets.UTF_8)));

        Stub stub = resolve(path);
        exchange.getResponseHeaders().add("Content-Type", stub.contentType());
        byte[] out = stub.body();
        exchange.sendResponseHeaders(stub.status(), out.length == 0 ? -1 : out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    private Stub resolve(String path) {
        Function<List<RecordedRequest>, Stub> dynamic = dynamicStubs.get(path);
        if (dynamic != null) {
            return dynamic.apply(requests);
        }
        List<Stub> seq = stubs.get(path);
        if (seq == null || seq.isEmpty()) {
            return new Stub(404, "{}".getBytes(StandardCharsets.UTF_8), "application/json");
        }
        int idx = calls.get(path).getAndIncrement();
        return seq.get(Math.min(idx, seq.size() - 1));
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
