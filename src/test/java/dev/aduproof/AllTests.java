package dev.aduproof;

import dev.aduproof.core.*;
import dev.aduproof.storage.LocalRepository;
import dev.aduproof.service.*;
import dev.aduproof.web.*;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;

/** Dependency-free test runner. Fails the build on the first unexpected result. */
public final class AllTests {
    private static int passed;
    private static final Instant TIME = Instant.parse("2026-11-13T12:00:00Z");
    private static final String TITLE = "Student Scholarship 2027";
    private static final String BODY = "Apply for the scholarship using our official website.";
    private static final String URL = "https://university.example/apply";

    private AllTests() {}

    public static void main(String[] args) throws Exception {
        run("01 Ed25519 signature and canonical round trip", AllTests::signatureRoundTrip);
        run("02 changing signed URL, title, and body breaks signature", AllTests::tampering);
        run("03 malformed or oversized proof fails closed", AllTests::malformedProof);
        run("04 untrusted publisher key cannot be accepted", AllTests::untrustedPublisher);
        run("05 exact match versus changed fields", AllTests::matches);
        run("06 revoked original is never reported valid", AllTests::revocation);
        run("07 expiration boundary and later time", AllTests::expiry);
        run("08 notices, keypair, and revocation survive restart", AllTests::persistence);
        run("09 invalid inputs are rejected", AllTests::inputValidation);
        run("10 browser response escapes unsafe text", AllTests::jsonSafety);
        run("11 HTTP publish, tamper, revoke, and policy headers", AllTests::httpFlow);
        run("12 HTTP missing token, bad origin, oversized data", AllTests::httpRejects);
        run("13 tampered on-disk signature is rejected", AllTests::storedTamper);
        run("14 incomplete or inconsistent keypair fails closed", AllTests::keyPairCorruption);
        run("15 UTF-8 Arabic and escaping round trip", AllTests::unicodeRoundTrip);
        System.out.println("\nPASS: " + passed + " test groups");
    }

    private static void run(String name, Checked test) throws Exception {
        test.run();
        ++passed;
        System.out.println("PASS " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void invalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private static SignedNotice signed() {
        KeyPair keys = Ed25519Signer.newKeyPair();
        NoticePayload payload = payload(keys.getPublic());
        return new SignedNotice(payload, Ed25519Signer.sign(payload, keys.getPrivate()));
    }

    private static NoticePayload payload(PublicKey key) {
        return new NoticePayload(UUID.randomUUID().toString(), "Local demo", TITLE, BODY, URL,
                TIME.getEpochSecond(), TIME.plusSeconds(86400).getEpochSecond(),
                Ed25519Signer.publicKey(key));
    }

    private static void signatureRoundTrip() {
        SignedNotice n = signed();
        check(Ed25519Signer.valid(n), "new signature invalid");
        SignedNotice loaded = NoticeCodec.decode(NoticeCodec.encode(n));
        check(loaded.equals(n), "record changed in codec");
        check(Ed25519Signer.valid(loaded), "signature invalid after round trip");
    }

    private static void tampering() {
        SignedNotice s = signed();
        NoticePayload p = s.payload();
        check(!Ed25519Signer.valid(new SignedNotice(new NoticePayload(p.id(), p.issuer(), p.title(), p.body(),
                "https://modified.example/apply", p.issuedAt(), p.expiresAt(), p.issuerPublicKey()), s.signature())), "URL tamper accepted");
        check(!Ed25519Signer.valid(new SignedNotice(new NoticePayload(p.id(), p.issuer(), "Changed title", p.body(),
                p.destination(), p.issuedAt(), p.expiresAt(), p.issuerPublicKey()), s.signature())), "title tamper accepted");
        check(!Ed25519Signer.valid(new SignedNotice(new NoticePayload(p.id(), p.issuer(), p.title(), "Changed body content",
                p.destination(), p.issuedAt(), p.expiresAt(), p.issuerPublicKey()), s.signature())), "body tamper accepted");
    }

    private static void malformedProof() {
        invalid(() -> NoticeCodec.decode("AA"));
        invalid(() -> NoticeCodec.decode("not-base64***"));
        invalid(() -> NoticeCodec.decode("a".repeat(24_001)));
        String encoded = NoticeCodec.encode(signed());
        byte[] changed = Base64.getUrlDecoder().decode(encoded);
        changed[0] ^= 1;
        invalid(() -> NoticeCodec.decode(Base64.getUrlEncoder().withoutPadding().encodeToString(changed)));
    }

    private static void untrustedPublisher() throws IOException {
        LocalRepository repo = new LocalRepository(Files.createTempDirectory("aduproof-key-"));
        SignedNotice outsider = signed();
        invalid(() -> {
            try { repo.add(outsider); }
            catch (IOException ex) { throw new IllegalStateException(ex); }
        });
    }

    private static ProofService newService(Path dir, Clock clock) throws IOException {
        return new ProofService(new LocalRepository(dir), clock);
    }

    private static void matches() throws IOException {
        ProofService service = newService(Files.createTempDirectory("aduproof-match-"), Clock.fixed(TIME, ZoneOffset.UTC));
        SignedNotice n = service.publish(TITLE, BODY, URL, 5);
        check(service.verify(n.payload().id(), TITLE, BODY, URL).status() == VerificationStatus.MATCH, "expected MATCH");
        VerificationResult other = service.verify(n.payload().id(), TITLE, BODY, "https://other.example/apply");
        check(other.status() == VerificationStatus.MISMATCH && !other.destinationMatches() && other.bodyMatches(), "link mismatch not isolated");
        check(service.verify(n.payload().id(), "Changed", BODY, URL).status() == VerificationStatus.MISMATCH, "title mismatch failed");
        check(service.verify(n.payload().id(), TITLE, "A completely different message", URL).status() == VerificationStatus.MISMATCH, "body mismatch failed");
        check(service.verify(UUID.randomUUID().toString(), TITLE, BODY, URL).status() == VerificationStatus.NOT_FOUND, "unknown must remain unknown");
    }

    private static void revocation() throws IOException {
        ProofService service = newService(Files.createTempDirectory("aduproof-revoke-"), Clock.fixed(TIME, ZoneOffset.UTC));
        SignedNotice n = service.publish(TITLE, BODY, URL, 5);
        check(service.revoke(n.payload().id()), "revoke failed");
        check(service.verify(n.payload().id(), TITLE, BODY, URL).status() == VerificationStatus.REVOKED, "revocation ignored");
        check(service.verify(n.payload().id(), TITLE, BODY, "https://modified.example/apply").status() == VerificationStatus.REVOKED, "revocation priority changed");
        check(!service.revoke(UUID.randomUUID().toString()), "unknown revocation accepted");
    }

    private static void expiry() throws IOException {
        Path dir = Files.createTempDirectory("aduproof-expire-");
        ProofService early = newService(dir, Clock.fixed(TIME, ZoneOffset.UTC));
        SignedNotice n = early.publish(TITLE, BODY, URL, 1);
        ProofService atBoundary = newService(dir, Clock.fixed(TIME.plusSeconds(86400), ZoneOffset.UTC));
        check(atBoundary.verify(n.payload().id(), TITLE, BODY, URL).status() == VerificationStatus.EXPIRED, "boundary expiration ignored");
        ProofService after = newService(dir, Clock.fixed(TIME.plusSeconds(86401), ZoneOffset.UTC));
        check(after.verify(n.payload().id(), TITLE, BODY, URL).status() == VerificationStatus.EXPIRED, "expired notice matched");
    }

    private static void persistence() throws IOException {
        Path dir = Files.createTempDirectory("aduproof-persist-");
        ProofService one = newService(dir, Clock.fixed(TIME, ZoneOffset.UTC));
        SignedNotice n = one.publish(TITLE, BODY, URL, 5);
        String firstProof = NoticeCodec.encode(n);
        ProofService two = newService(dir, Clock.fixed(TIME, ZoneOffset.UTC));
        check(two.verify(n.payload().id(), TITLE, BODY, URL).status() == VerificationStatus.MATCH, "notice missing after restart");
        check(firstProof.equals(NoticeCodec.encode(two.find(n.payload().id()))), "key or record changed on restart");
        two.revoke(n.payload().id());
        ProofService three = newService(dir, Clock.fixed(TIME, ZoneOffset.UTC));
        check(three.verify(n.payload().id(), TITLE, BODY, URL).status() == VerificationStatus.REVOKED, "revocation missing after restart");
    }

    private static void inputValidation() throws IOException {
        ProofService svc = newService(Files.createTempDirectory("aduproof-validate-"), Clock.fixed(TIME, ZoneOffset.UTC));
        invalid(() -> { try { svc.publish(TITLE, BODY, "http://example.com", 4); } catch (IOException ex) { throw new IllegalStateException(ex); } });
        invalid(() -> { try { svc.publish(TITLE, BODY, "https://name:secret@example.com/", 4); } catch (IOException ex) { throw new IllegalStateException(ex); } });
        invalid(() -> { try { svc.publish("a", BODY, URL, 4); } catch (IOException ex) { throw new IllegalStateException(ex); } });
        invalid(() -> { try { svc.publish(TITLE, BODY, URL, 0); } catch (IOException ex) { throw new IllegalStateException(ex); } });
        invalid(() -> { try { svc.publish(TITLE, BODY, URL, 91); } catch (IOException ex) { throw new IllegalStateException(ex); } });
        invalid(() -> { try { svc.publish(TITLE, "bad\u0000body", URL, 4); } catch (IOException ex) { throw new IllegalStateException(ex); } });
    }

    private static void jsonSafety() {
        String result = Json.quote("<script>alert('x')</script> \" \\ \n");
        check(result.contains("<script>") && result.contains("\\\"") && result.contains("\\n"), "incorrect JSON escaping");
        check(!result.contains("\n"), "unescaped newline in JSON");
    }

    private static String form(String... keyValue) {
        StringJoiner fields = new StringJoiner("&");
        for (int i = 0; i < keyValue.length; i += 2) fields.add(URLEncoder.encode(keyValue[i], StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(keyValue[i+1], StandardCharsets.UTF_8));
        return fields.toString();
    }

    private static HttpResponse<String> send(HttpClient client, int port, String path, String method,
                                             String body, String token, String origin, String type) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (method.equals("POST")) {
            req.POST(HttpRequest.BodyPublishers.ofString(body));
            if (origin != null) req.header("Origin", origin);
            if (token != null) req.header("X-AduProof-Demo-Token", token);
            req.header("Content-Type", type);
        } else req.GET();
        return client.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String tokenFromJson(String json) {
        // Token is a controlled base64url string generated by the server; no generic JSON parser is needed in tests.
        String marker = "\"token\":\"";
        int a = json.indexOf(marker), b = json.indexOf('"', a + marker.length());
        check(a >= 0 && b > a, "token missing");
        return json.substring(a + marker.length(), b);
    }

    private static String idFromJson(String json) {
        String marker = "\"id\":\"";
        int a = json.indexOf(marker), b = json.indexOf('"', a + marker.length());
        check(a >= 0 && b > a, "id missing");
        return json.substring(a + marker.length(), b);
    }

    private static void httpFlow() throws Exception {
        ProofService svc = newService(Files.createTempDirectory("aduproof-http-"), Clock.fixed(TIME, ZoneOffset.UTC));
        try (DemoServer server = new DemoServer(svc, 0)) {
            server.start();
            int port = server.port();
            String origin = "http://127.0.0.1:" + port;
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> info = send(client, port, "/api/info", "GET", "", null, null, "");
            String token = tokenFromJson(info.body());
            check(info.statusCode() == 200 && info.headers().firstValue("Content-Security-Policy").isPresent(), "security headers absent");
            String submitted = form("title", TITLE, "body", BODY, "destination", URL, "days", "7");
            HttpResponse<String> published = send(client, port, "/api/publish", "POST", submitted, token, origin,
                    "application/x-www-form-urlencoded");
            check(published.statusCode() == 201 && published.body().contains("\"proof\""), "publication API failed: " + published.body());
            String id = idFromJson(published.body());
            String original = form("id", id, "title", TITLE, "body", BODY, "destination", URL);
            check(send(client, port, "/api/verify", "POST", original, token, origin,
                    "application/x-www-form-urlencoded").body().contains("\"MATCH\""), "HTTP original mismatched");
            String changed = form("id", id, "title", TITLE, "body", BODY, "destination", "https://modified.example/apply");
            check(send(client, port, "/api/verify", "POST", changed, token, origin,
                    "application/x-www-form-urlencoded").body().contains("\"MISMATCH\""), "HTTP link tamper not detected");
            check(send(client, port, "/api/revoke", "POST", form("id", id), token, origin,
                    "application/x-www-form-urlencoded").statusCode() == 200, "HTTP revoke failed");
            check(send(client, port, "/api/verify", "POST", original, token, origin,
                    "application/x-www-form-urlencoded").body().contains("\"REVOKED\""), "HTTP revocation ignored");
            check(send(client, port, "/", "GET", "", null, null, "").body().contains("Know what was"), "web page missing");
            check(send(client, port, "/app.js", "GET", "", null, null, "").statusCode() == 200, "web JS missing");
        }
    }

    private static void httpRejects() throws Exception {
        ProofService svc = newService(Files.createTempDirectory("aduproof-reject-"), Clock.fixed(TIME, ZoneOffset.UTC));
        try (DemoServer server = new DemoServer(svc, 0)) {
            server.start();
            int port = server.port();
            String origin = "http://127.0.0.1:" + port;
            HttpClient client = HttpClient.newHttpClient();
            String token = tokenFromJson(send(client, port, "/api/info", "GET", "", null, null, "").body());
            String body = form("title", TITLE, "body", BODY, "destination", URL, "days", "7");
            check(send(client, port, "/api/publish", "POST", body, null, origin,
                    "application/x-www-form-urlencoded").statusCode() == 403, "missing token allowed");
            check(send(client, port, "/api/publish", "POST", body, token, "https://attacker.example",
                    "application/x-www-form-urlencoded").statusCode() == 403, "cross-site origin allowed");
            check(send(client, port, "/api/publish", "POST", body, token, origin,
                    "text/plain").statusCode() == 400, "bad content type allowed");
            check(send(client, port, "/api/publish", "POST", body + "&title=duplicate", token, origin,
                    "application/x-www-form-urlencoded").statusCode() == 400, "duplicate field allowed");
            check(send(client, port, "/api/publish", "POST", "x=" + "a".repeat(16_500), token, origin,
                    "application/x-www-form-urlencoded").statusCode() == 413, "oversized body allowed");
        }
    }

    private static void storedTamper() throws IOException {
        Path dir = Files.createTempDirectory("aduproof-corrupted-");
        ProofService service = newService(dir, Clock.fixed(TIME, ZoneOffset.UTC));
        SignedNotice n = service.publish(TITLE, BODY, URL, 5);
        NoticePayload p = n.payload();
        NoticePayload modified = new NoticePayload(p.id(), p.issuer(), p.title(), p.body(),
                "https://bad-link.example/apply", p.issuedAt(), p.expiresAt(), p.issuerPublicKey());
        Files.writeString(dir.resolve("notices.txt"), NoticeCodec.encode(new SignedNotice(modified, n.signature())) + "\n");
        ProofService restarted = newService(dir, Clock.fixed(TIME, ZoneOffset.UTC));
        check(restarted.verify(p.id(), p.title(), p.body(), modified.destination()).status()
                == VerificationStatus.INVALID_PROOF, "signed database was tampered but accepted");
    }

    private static void keyPairCorruption() throws IOException {
        Path dir = Files.createTempDirectory("aduproof-key-corrupted-");
        new LocalRepository(dir);
        Files.delete(dir.resolve("issuer-public.x509"));
        try { new LocalRepository(dir); throw new AssertionError("missing key half silently regenerated"); }
        catch (IOException expected) { /* fail closed */ }
        Path another = Files.createTempDirectory("aduproof-key-mismatch-");
        new LocalRepository(another);
        byte[] foreignPublic = Files.readAllBytes(another.resolve("issuer-public.x509"));
        Files.write(dir.resolve("issuer-public.x509"), foreignPublic);
        try { new LocalRepository(dir); throw new AssertionError("mismatched pair silently accepted"); }
        catch (IOException expected) { /* fail closed */ }
    }

    private static void unicodeRoundTrip() {
        KeyPair pair = Ed25519Signer.newKeyPair();
        NoticePayload p = new NoticePayload(UUID.randomUUID().toString(), "AduProof demo", "منحة طلابية",
                "التقديم مفتوح — Apply now ✨", URL, TIME.getEpochSecond(),
                TIME.plusSeconds(3600).getEpochSecond(), Ed25519Signer.publicKey(pair.getPublic()));
        SignedNotice n = new SignedNotice(p, Ed25519Signer.sign(p, pair.getPrivate()));
        check(NoticeCodec.decode(NoticeCodec.encode(n)).equals(n), "Unicode text failed round trip");
        check(Ed25519Signer.valid(n), "Unicode signature failed validation");
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
}
