package dev.aduproof.service;

import dev.aduproof.core.*;
import dev.aduproof.storage.LocalRepository;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Product rules separated from HTTP and storage details. */
public final class ProofService {
    public static final String DEMO_ISSUER = "AduProof local demo publisher — identity not verified";
    private final LocalRepository repository;
    private final Clock clock;

    public ProofService(LocalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public SignedNotice publish(String title, String body, String destination, int validDays) throws IOException {
        NoticeValidator.requiredText(title, "title", 2, 120);
        NoticeValidator.requiredText(body, "body", 5, 2000);
        NoticeValidator.destination(destination);
        if (validDays < 1 || validDays > 90) throw new IllegalArgumentException("valid days must be between 1 and 90");
        long now = Instant.now(clock).getEpochSecond();
        NoticePayload payload = NoticeValidator.payload(new NoticePayload(UUID.randomUUID().toString(), DEMO_ISSUER,
                title, body, destination, now, now + validDays * 86400L,
                Ed25519Signer.publicKey(repository.keys().getPublic())));
        SignedNotice signed = new SignedNotice(payload, Ed25519Signer.sign(payload, repository.keys().getPrivate()));
        repository.add(signed);
        return signed;
    }

    public SignedNotice find(String id) { return repository.get(id); }

    public boolean revoke(String id) throws IOException { return repository.revoke(id); }

    public VerificationResult verify(String id, String title, String body, String destination) {
        SignedNotice record = repository.get(id);
        if (record == null) return new VerificationResult(VerificationStatus.NOT_FOUND,
                false, false, false, "No notice with this ID is in the local demo registry.");
        if (!Ed25519Signer.valid(record) || !Ed25519Signer.publicKey(repository.keys().getPublic())
                .equals(record.payload().issuerPublicKey())) {
            return new VerificationResult(VerificationStatus.INVALID_PROOF,
                    false, false, false, "The stored record is not signed by the trusted local demo key.");
        }
        boolean titleMatch = record.payload().title().equals(title);
        boolean bodyMatch = record.payload().body().equals(body);
        boolean destinationMatch = record.payload().destination().equals(destination);
        if (repository.isRevoked(id)) return new VerificationResult(VerificationStatus.REVOKED,
                titleMatch, bodyMatch, destinationMatch, "The demo publisher withdrew this notice.");
        if (Instant.now(clock).getEpochSecond() >= record.payload().expiresAt()) {
            return new VerificationResult(VerificationStatus.EXPIRED,
                    titleMatch, bodyMatch, destinationMatch, "This notice is past its stated expiration date.");
        }
        if (!titleMatch || !bodyMatch || !destinationMatch) {
            return new VerificationResult(VerificationStatus.MISMATCH,
                    titleMatch, bodyMatch, destinationMatch, "The received copy differs from the signed announcement.");
        }
        return new VerificationResult(VerificationStatus.MATCH, true, true, true,
                "The received text and destination match this locally trusted demo publisher's signed notice. This does not establish identity, truth, or safety.");
    }
}
