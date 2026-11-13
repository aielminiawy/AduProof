# 5 — Testing, AI-code review and release checklist

## Why this review is necessary

GitHub's guidance on reviewing AI-generated code recommends compiling and executing tests, checking context and intent, looking for security issues, and reviewing maintainability rather than accepting AI suggestions at face value. A Reddit r/ExperiencedDevs discussion highlighted generated changes that were unnecessarily broad, incorrect, and difficult for colleagues to maintain. We used those concerns as review targets, not as proof that any specific coding assistant always makes those mistakes.

Sources:
- https://docs.github.com/en/copilot/tutorials/review-ai-generated-code
- https://www.reddit.com/r/ExperiencedDevs/comments/1jfhqye/
- https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html
- https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html

## Review checklist and status

| Risk | Mitigation and evidence |
| --- | --- |
| Non-compiling/imaginary Java APIs | Compile on a real JDK 21 with `javac -Xlint:all`; run all test groups |
| Unchecked AI-generated business logic | Test positive and negative scenarios, expiry boundary and revoke precedence |
| Fake digital signature | Use the standard JDK Ed25519 provider; test modified signed bytes and foreign keys |
| Ambiguous string concatenation | Versioned, length-prefixed UTF-8 signed payload |
| Trusting a self-signed stranger | Pin the single local demo issuer key; explicitly avoid institutional identity claims |
| UI XSS | Set text with `textContent`, not `innerHTML`; return escaped JSON; use CSP |
| Cross-site form submits to local server | Loopback binding, strict `Host`/`Origin`, unguessable POST request token |
| Oversized/malformed form inputs | Bound POST body (HTTP 413 on overflow), fixed field allowlist, duplicate rejection, server-side validation |
| Private keys in source | Generate keys in ignored `data/`; POSIX file owner permissions where supported |
| Storage corruption | Fail closed on broken envelopes; detect changed signatures after reload |
| Over-engineering | No external runtime dependencies, small layers, no fake integrations |
| Missing documentation | README, instructions, story, security policy, decision log, roadmap and open-source contribution guide |
| Tests working only once | Persistence, restart tests and CI test command |

## Executable test groups

`bash scripts/test.sh` or `.\\scripts\\test.ps1` tests:

1. Ed25519 signature generation and codec round trip.
2. Tampered title, body and destination cause signature validation failure.
3. Malformed or oversized proofs fail closed.
4. Untrusted issuer keys are rejected by the repository.
5. Exact match versus a changed title, body or destination.
6. Revoked notices never appear active.
7. Expiration at the exact boundary and afterward.
8. Record and revocation persistence across restarts.
9. Bad URL protocols, account credentials in URLs, invalid field lengths and control codes.
10. Escaping of user-controlled characters in JSON.
11. End-to-end HTTP publish, verification, tamper and revocation.
12. Missing token, cross-origin requests, wrong content type, duplicate form keys and oversize payload.
13. On-disk record modification cannot produce a valid signature.
14. Missing or mismatched issuer keypair fails closed.
15. Arabic and Unicode text survives encoding and remains verifiable.

## Additional automated checks

- `node --check src/main/resources/web/app.js` for JavaScript syntax (when Node is available).
- **Browser smoke test attempted but not completed:** the hosted Chromium browser returned `ERR_BLOCKED_BY_ADMINISTRATOR` when opening the local service. This is an environment limitation; it does not establish a product bug or prove the browser experience works. A maintainer should run a real browser end-to-end test locally before public release.
- Review of static HTML IDs and controls, static asset loading and security response headers.

## Important remaining risks

- No independent external audit; test coverage does not establish that the application is vulnerability-free.
- No production-grade organization verification, authorization, key storage, or distributed revocation.
- Client UI workflows currently require exact text input and a valid signed reference.
- No cloud deployment or official messaging-platform integrations in this version.
- A compromised demo machine can tamper with its local files and trust anchor.
- Browser accessibility and visual quality should be checked on actual user devices.

These are explicitly future-work items; they are **not** secretly implemented or claimed as complete.
