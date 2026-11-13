# 4 — Browser user journey and safety

The front end is ordinary HTML, CSS and small JavaScript; the Java HTTP adapter is an explicit thin layer above the service. Both use the same notice rules. No external CDN, tracking script, social-platform integration or untrusted dependency is loaded.

1. Publisher enters title, body and an HTTPS URL, then signs it with the locally generated demo key.
2. The UI returns a notice ID and an encoded signed envelope. The received notice is pre-filled for testing.
3. User verifies the original, changes the destination using the shortcut, and verifies again.
4. Publisher can revoke; the verifier then reports `REVOKED`.

The user interface never claims that a matching record is safe, accurate or institution-verified. JSON response text is assigned with `textContent` and not injected using `innerHTML`.

Local publisher operations use a same-origin request header plus an unpredictable session token. This is **demo protection**, not proper user authentication. Requests have strict size limits, an allowlist of form keys, and specific content types. The server binds only to 127.0.0.1.
