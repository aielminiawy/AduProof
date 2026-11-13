package dev.aduproof.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;

public final class NoticeValidator {
    private NoticeValidator() {}

    public static String requiredText(String value, String field, int min, int max) {
        if (value == null || value.length() < min || value.length() > max
                || value.codePoints().anyMatch(cp -> Character.isISOControl(cp) && cp != '\n' && cp != '\t')) {
            throw new IllegalArgumentException(field + " must contain " + min + "–" + max + " characters without control codes");
        }
        return value;
    }

    public static String destination(String value) {
        requiredText(value, "destination", 12, 500);
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || uri.getHost().length() > 253) {
                throw new IllegalArgumentException("destination must be an absolute HTTPS URL without credentials or fragment");
            }
            return value;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("destination must be a valid HTTPS URL", exception);
        }
    }

    public static NoticePayload payload(NoticePayload p) {
        Objects.requireNonNull(p, "payload");
        try { UUID.fromString(p.id()); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("invalid notice ID", ex); }
        requiredText(p.issuer(), "issuer", 2, 140);
        requiredText(p.title(), "title", 2, 120);
        requiredText(p.body(), "body", 5, 2000);
        destination(p.destination());
        if (p.issuedAt() <= 0 || p.expiresAt() <= p.issuedAt()
                || p.expiresAt() - p.issuedAt() > 90L * 86400) {
            throw new IllegalArgumentException("invalid publication date range");
        }
        requiredText(p.issuerPublicKey(), "public key", 20, 256);
        return p;
    }
}
