package dev.aduproof.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Versioned, length-prefixed binary envelope; avoids ambiguous string concatenation. */
public final class NoticeCodec {
    private static final int SIGNABLE_MAGIC = 0x41555031; // AUP1
    private static final int ENVELOPE_MAGIC = 0x41555045; // AUPE
    private static final int MAX_ENVELOPE_BYTES = 16_384;

    private NoticeCodec() {}

    public static byte[] canonical(NoticePayload payload) {
        NoticeValidator.payload(payload);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(SIGNABLE_MAGIC);
            writeString(out, payload.id());
            writeString(out, payload.issuer());
            writeString(out, payload.title());
            writeString(out, payload.body());
            writeString(out, payload.destination());
            out.writeLong(payload.issuedAt());
            out.writeLong(payload.expiresAt());
            writeString(out, payload.issuerPublicKey());
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("unexpected in-memory encoding failure", ex);
        }
    }

    public static String encode(SignedNotice notice) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(ENVELOPE_MAGIC);
            byte[] signed = canonical(notice.payload());
            out.writeInt(signed.length);
            out.write(signed);
            writeString(out, notice.signature());
            out.flush();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("unexpected in-memory encoding failure", ex);
        }
    }

    public static SignedNotice decode(String encoded) {
        if (encoded == null || encoded.isEmpty() || encoded.length() > 24_000) {
            throw new IllegalArgumentException("invalid proof length");
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            if (decoded.length > MAX_ENVELOPE_BYTES) throw new IllegalArgumentException("proof is too large");
            DataInputStream envelope = new DataInputStream(new ByteArrayInputStream(decoded));
            if (envelope.readInt() != ENVELOPE_MAGIC) throw new IllegalArgumentException("unknown proof version");
            int length = envelope.readInt();
            if (length < 0 || length > MAX_ENVELOPE_BYTES || length > envelope.available()) {
                throw new IllegalArgumentException("invalid proof payload size");
            }
            byte[] signed = envelope.readNBytes(length);
            String signature = readString(envelope, 256);
            if (envelope.available() != 0) throw new IllegalArgumentException("unexpected trailing bytes");
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(signed));
            if (in.readInt() != SIGNABLE_MAGIC) throw new IllegalArgumentException("unknown notice version");
            NoticePayload payload = new NoticePayload(
                    readString(in, 100), readString(in, 560), readString(in, 480),
                    readString(in, 8_000), readString(in, 2_000), in.readLong(), in.readLong(),
                    readString(in, 256));
            if (in.available() != 0) throw new IllegalArgumentException("unexpected notice payload bytes");
            NoticeValidator.payload(payload);
            return new SignedNotice(payload, signature);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid proof envelope", ex);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in, int maxBytes) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > maxBytes || n > in.available()) throw new IOException("invalid field size");
        byte[] raw = in.readNBytes(n);
        // Verify UTF-8 round trip; replacement characters must not silently change signed content.
        String result = new String(raw, StandardCharsets.UTF_8);
        if (!java.util.Arrays.equals(raw, result.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException("invalid UTF-8");
        }
        return result;
    }
}
