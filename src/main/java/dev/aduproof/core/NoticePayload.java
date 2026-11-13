package dev.aduproof.core;

/** Data covered by the publisher's Ed25519 signature. */
public record NoticePayload(
        String id,
        String issuer,
        String title,
        String body,
        String destination,
        long issuedAt,
        long expiresAt,
        String issuerPublicKey) {
}
