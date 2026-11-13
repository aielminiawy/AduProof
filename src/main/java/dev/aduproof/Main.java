package dev.aduproof;

import dev.aduproof.service.ProofService;
import dev.aduproof.storage.LocalRepository;
import dev.aduproof.web.DemoServer;
import java.nio.file.Path;
import java.time.Clock;

/** Run with Java 21; stops on Ctrl+C. */
public final class Main {
    private Main() {}
    public static void main(String[] args) throws Exception {
        Path data = Path.of(System.getenv().getOrDefault("ADUPROOF_DATA_DIR", "data"));
        int port = Integer.parseInt(System.getenv().getOrDefault("ADUPROOF_PORT", "8787"));
        DemoServer server = new DemoServer(new ProofService(new LocalRepository(data), Clock.systemUTC()), port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.println("AduProof LOCAL DEMO: http://127.0.0.1:" + server.port());
        System.out.println("Demo publisher only; organization identity is NOT verified. Press Ctrl+C to stop.");
    }
}
