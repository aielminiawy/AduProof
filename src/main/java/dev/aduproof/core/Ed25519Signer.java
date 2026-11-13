package dev.aduproof.core;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class Ed25519Signer {
    private Ed25519Signer() {}

    public static KeyPair newKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Ed25519 is unavailable in this JDK", ex);
        }
    }

    public static String publicKey(PublicKey key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getEncoded());
    }

    public static String sign(NoticePayload payload, PrivateKey key) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(key);
            signature.update(NoticeCodec.canonical(payload));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("unable to sign record", ex);
        }
    }

    public static boolean valid(SignedNotice notice) {
        try {
            byte[] publicKey = Base64.getUrlDecoder().decode(notice.payload().issuerPublicKey());
            byte[] signatureBytes = Base64.getUrlDecoder().decode(notice.signature());
            PublicKey key = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(publicKey));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(NoticeCodec.canonical(notice.payload()));
            return verifier.verify(signatureBytes);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            return false;
        }
    }
}
