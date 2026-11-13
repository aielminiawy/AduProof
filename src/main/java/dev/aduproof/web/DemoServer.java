package dev.aduproof.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.aduproof.core.*;
import dev.aduproof.service.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** HTTP adapter for the local demonstration, deliberately bound to 127.0.0.1 only. */
public final class DemoServer implements AutoCloseable {
    private static final int MAX_POST_BYTES = 16_384;
    private final HttpServer server;
    private final ProofService service;
    private final ExecutorService executor;
    private final String csrfToken;

    public DemoServer(ProofService service, int port) throws IOException {
        this.service = service;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 16);
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        this.csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        this.executor = Executors.newFixedThreadPool(6);
        this.server.setExecutor(executor);
        server.createContext("/", this::dispatch);
    }

    public int port() { return server.getAddress().getPort(); }
    public void start() { server.start(); }

    @Override public void close() { server.stop(0); executor.shutdownNow(); }

    private void dispatch(HttpExchange exchange) throws IOException {
        try {
            setHeaders(exchange);
            if (!("127.0.0.1:" + port()).equals(exchange.getRequestHeaders().getFirst("Host"))) {
                throw new SecurityException("Invalid local host");
            }
            String route = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (method.equals("GET") && route.equals("/api/info")) {
                send(exchange, 200, "application/json", "{" + Json.field("token", csrfToken) + ","
                        + Json.field("issuer", ProofService.DEMO_ISSUER) + "}");
            } else if (method.equals("GET") && route.equals("/api/notice")) {
                Map<String, String> q = parseFields(exchange.getRequestURI().getRawQuery(), Set.of("id"));
                SignedNotice n = service.find(required(q, "id", 36));
                if (n == null) error(exchange, 404, "Notice was not found in the local demo registry.");
                else send(exchange, 200, "application/json", noticeJson(n));
            } else if (method.equals("POST") && route.startsWith("/api/")) {
                checkPost(exchange);
                Map<String, String> data;
                if (route.equals("/api/publish")) {
                    data = postFields(exchange, Set.of("title", "body", "destination", "days"));
                    int days;
                    try { days = Integer.parseInt(required(data, "days", 3)); }
                    catch (NumberFormatException ex) { throw new IllegalArgumentException("days must be an integer"); }
                    SignedNotice signed = service.publish(required(data, "title", 120),
                            required(data, "body", 2000), required(data, "destination", 500), days);
                    send(exchange, 201, "application/json", noticeJson(signed));
                } else if (route.equals("/api/verify")) {
                    data = postFields(exchange, Set.of("id", "title", "body", "destination"));
                    String id = required(data, "id", 36);
                    VerificationResult result = service.verify(id, required(data, "title", 120),
                            required(data, "body", 2000), required(data, "destination", 500));
                    send(exchange, 200, "application/json", "{" + Json.field("status", result.status().name()) + ","
                            + Json.flag("titleMatches", result.titleMatches()) + ","
                            + Json.flag("bodyMatches", result.bodyMatches()) + ","
                            + Json.flag("destinationMatches", result.destinationMatches()) + ","
                            + Json.field("explanation", result.explanation()) + "}");
                } else if (route.equals("/api/revoke")) {
                    data = postFields(exchange, Set.of("id"));
                    if (!service.revoke(required(data, "id", 36))) error(exchange, 404, "Notice was not found.");
                    else send(exchange, 200, "application/json", "{" + Json.field("status", "REVOKED") + "}");
                } else error(exchange, 404, "Unknown API route.");
            } else if (method.equals("GET") && route.equals("/")) {
                resource(exchange, "/web/index.html", "text/html; charset=utf-8");
            } else if (method.equals("GET") && route.equals("/app.css")) {
                resource(exchange, "/web/app.css", "text/css; charset=utf-8");
            } else if (method.equals("GET") && route.equals("/app.js")) {
                resource(exchange, "/web/app.js", "text/javascript; charset=utf-8");
            } else if (route.startsWith("/api/") || route.equals("/")) {
                error(exchange, 405, "Method is not supported for this endpoint.");
            } else error(exchange, 404, "Page not found.");
        } catch (PayloadTooLargeException ex) {
            error(exchange, 413, "Request body is too large.");
        } catch (IllegalArgumentException ex) {
            // Avoid leaking user-supplied input or stack traces into responses.
            error(exchange, 400, ex.getMessage());
        } catch (SecurityException ex) {
            error(exchange, 403, "This local demo requires a same-origin request and its anti-CSRF token.");
        } catch (Exception ex) {
            System.err.println("AduProof request error: " + ex.getClass().getSimpleName());
            error(exchange, 500, "Internal demo error. See server terminal.");
        } finally {
            exchange.close();
        }
    }

    private static final class PayloadTooLargeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static void setHeaders(HttpExchange e) {
        e.getResponseHeaders().set("Cache-Control", "no-store");
        e.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        e.getResponseHeaders().set("X-Frame-Options", "DENY");
        e.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        e.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
    }

    private void checkPost(HttpExchange e) {
        String expectedOrigin = "http://127.0.0.1:" + port();
        if (!expectedOrigin.equals(e.getRequestHeaders().getFirst("Origin"))
                || !csrfToken.equals(e.getRequestHeaders().getFirst("X-AduProof-Demo-Token"))) {
            throw new SecurityException("Cross-site or missing demo token");
        }
        String contentType = e.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].trim()
                .equalsIgnoreCase("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Content-Type must be application/x-www-form-urlencoded");
        }
    }

    private static Map<String, String> postFields(HttpExchange e, Set<String> allowed) throws IOException {
        byte[] data = e.getRequestBody().readNBytes(MAX_POST_BYTES + 1);
        if (data.length > MAX_POST_BYTES) throw new PayloadTooLargeException();
        return parseFields(new String(data, StandardCharsets.UTF_8), allowed);
    }

    private static Map<String, String> parseFields(String encoded, Set<String> allowed) {
        if (encoded == null || encoded.length() > 24_000) throw new IllegalArgumentException("Missing or oversized form data");
        Map<String, String> data = new HashMap<>();
        for (String pair : encoded.split("&", -1)) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid encoded field");
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            if (!allowed.contains(key) || data.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Unexpected or duplicate form field");
            }
        }
        return data;
    }

    private static String required(Map<String, String> data, String key, int max) {
        String value = data.get(key);
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(key + " is required and must be at most " + max + " characters");
        }
        return value;
    }

    private static String noticeJson(SignedNotice n) {
        NoticePayload p = n.payload();
        return "{" + Json.field("id", p.id()) + "," + Json.field("issuer", p.issuer()) + ","
                + Json.field("title", p.title()) + "," + Json.field("body", p.body()) + ","
                + Json.field("destination", p.destination()) + ","
                + Json.field("issuedAt", Instant.ofEpochSecond(p.issuedAt()).toString()) + ","
                + Json.field("expiresAt", Instant.ofEpochSecond(p.expiresAt()).toString()) + ","
                + Json.field("proof", NoticeCodec.encode(n)) + "}";
    }

    private static void resource(HttpExchange e, String name, String type) throws IOException {
        try (var stream = DemoServer.class.getResourceAsStream(name)) {
            if (stream == null) { error(e, 404, "Resource not found."); return; }
            byte[] bytes = stream.readAllBytes();
            e.getResponseHeaders().set("Content-Type", type);
            e.sendResponseHeaders(200, bytes.length);
            e.getResponseBody().write(bytes);
        }
    }

    private static void error(HttpExchange e, int status, String reason) throws IOException {
        send(e, status, "application/json", "{" + Json.field("error", reason) + "}");
    }

    private static void send(HttpExchange e, int status, String mime, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", mime + (mime.equals("application/json") ? "; charset=utf-8" : ""));
        e.sendResponseHeaders(status, bytes.length);
        e.getResponseBody().write(bytes);
    }
}
