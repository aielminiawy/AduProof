# 2 — Signed notice protocol

The MVP uses the standard Java 21 `Ed25519` signature algorithm. A record is signed over a fixed version identifier and length-prefixed UTF-8 fields: UUID, issuer label, title, body, HTTPS destination, issued-at epoch seconds, expiry epoch seconds, and issuer public key. **All of these fields are signed.** The signature is then attached to an envelope with its own version header and exported in Base64URL form.

No handwritten hashing, encryption or private-key algorithms are used. A signed record cannot silently change the destination link without invalidating its signature. A reader must separately check that it trusts the issuer public key, and that this record is neither expired nor revoked.

A reader **must not** treat a valid cryptographic signature as proof that an institution is genuine. The MVP issuer is a specifically labeled demo key generated locally. Domain identity verification is intentionally out of scope.

The MVP uses **exact** UTF-8 field comparison, not semantic similarity. If a messaging platform changes whitespace or formatting, exact matching may fail. Do not automatically normalize URL paths, remove parameters, or drop characters to force a match.

### Status meanings

- `MATCH`: signature valid, demo key matches the pinned issuer, unexpired, not revoked, received text and destination identical.
- `MISMATCH`: the published reference exists and is authenticated, but the received fields differ.
- `REVOKED`: issuer withdrew approval for this known record.
- `EXPIRED`: notice is past its stated expiration.
- `INVALID_PROOF`: data is malformed, tampered, or not signed by the trusted demo key.
- `NOT_FOUND`: no such notice exists in the local registry.

This protocol is intentionally local and experimental; no portability guarantee across future versions exists.
