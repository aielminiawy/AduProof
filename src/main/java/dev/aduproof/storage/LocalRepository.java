package dev.aduproof.storage;

import dev.aduproof.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/** Small, single-process, local-only data store. Not for internet-facing deployment. */
public final class LocalRepository {
    private final Path directory;
    private final KeyPair keys;
    private final Map<String, SignedNotice> notices = new LinkedHashMap<>();
    private final Set<String> revoked = new HashSet<>();

    public LocalRepository(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
        this.keys = loadOrCreateKeys();
        loadRecords();
    }

    private KeyPair loadOrCreateKeys() throws IOException {
        Path privatePath = directory.resolve("issuer-private.pk8");
        Path publicPath = directory.resolve("issuer-public.x509");
        boolean priv = Files.exists(privatePath), pub = Files.exists(publicPath);
        if (priv != pub) throw new IOException("issuer keypair is incomplete; refusing to regenerate it");
        try {
            if (priv) {
                byte[] privateBytes = Files.readAllBytes(privatePath);
                byte[] publicBytes = Files.readAllBytes(publicPath);
                KeyFactory factory = KeyFactory.getInstance("Ed25519");
                KeyPair loaded = new KeyPair(factory.generatePublic(new X509EncodedKeySpec(publicBytes)),
                        factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes)));
                // Check that both halves belong to the same keypair before accepting local state.
                byte[] challenge = "AduProof local issuer key check".getBytes(StandardCharsets.UTF_8);
                Signature signer = Signature.getInstance("Ed25519");
                signer.initSign(loaded.getPrivate());
                signer.update(challenge);
                byte[] signature = signer.sign();
                Signature verifier = Signature.getInstance("Ed25519");
                verifier.initVerify(loaded.getPublic());
                verifier.update(challenge);
                if (!verifier.verify(signature)) throw new IOException("issuer keys do not match");
                return loaded;
            }
            KeyPair pair = Ed25519Signer.newKeyPair();
            // A private key is created using restricted permissions on POSIX platforms where supported.
            Files.createFile(privatePath);
            restrictToOwner(privatePath);
            Files.write(privatePath, pair.getPrivate().getEncoded(), StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(publicPath, pair.getPublic().getEncoded(), StandardOpenOption.CREATE_NEW);
            return pair;
        } catch (GeneralSecurityException ex) {
            throw new IOException("invalid issuer keypair", ex);
        }
    }

    private static void restrictToOwner(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Windows permissions are managed by the local account/ACL configuration.
        }
    }

    private void loadRecords() throws IOException {
        Path file = directory.resolve("notices.txt");
        if (Files.exists(file)) {
            for (String encoded : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (encoded.isBlank()) continue;
                SignedNotice notice;
                try { notice = NoticeCodec.decode(encoded); }
                catch (IllegalArgumentException ex) { throw new IOException("corrupt local notice database", ex); }
                if (notices.putIfAbsent(notice.payload().id(), notice) != null) {
                    throw new IOException("duplicate notice ID in database");
                }
            }
        }
        Path revokedFile = directory.resolve("revoked.txt");
        if (Files.exists(revokedFile)) {
            for (String id : Files.readAllLines(revokedFile, StandardCharsets.UTF_8)) {
                if (!id.isBlank()) {
                    if (!notices.containsKey(id)) throw new IOException("unknown revocation in local database");
                    revoked.add(id);
                }
            }
        }
    }

    public KeyPair keys() { return keys; }

    public synchronized SignedNotice get(String id) { return notices.get(id); }

    public synchronized void add(SignedNotice notice) throws IOException {
        String id = notice.payload().id();
        if (!Ed25519Signer.valid(notice) || !Ed25519Signer.publicKey(keys.getPublic())
                .equals(notice.payload().issuerPublicKey())) {
            throw new IllegalArgumentException("invalid or foreign issuer signature");
        }
        if (notices.containsKey(id)) throw new IllegalArgumentException("duplicate notice ID");
        notices.put(id, notice);
        try { writeRecords(); }
        catch (IOException ex) { notices.remove(id); throw ex; }
    }

    public synchronized boolean isRevoked(String id) { return revoked.contains(id); }

    public synchronized boolean revoke(String id) throws IOException {
        if (!notices.containsKey(id)) return false;
        if (!revoked.add(id)) return true;
        try { writeRevocations(); }
        catch (IOException ex) { revoked.remove(id); throw ex; }
        return true;
    }

    private void writeRecords() throws IOException {
        List<String> lines = notices.values().stream().map(NoticeCodec::encode).toList();
        writeAtomically(directory.resolve("notices.txt"), lines);
    }

    private void writeRevocations() throws IOException {
        List<String> lines = revoked.stream().sorted().toList();
        writeAtomically(directory.resolve("revoked.txt"), lines);
    }

    private void writeAtomically(Path file, List<String> lines) throws IOException {
        Path temp = Files.createTempFile(directory, "aduproof-", ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
