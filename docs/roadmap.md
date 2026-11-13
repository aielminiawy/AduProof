# Product roadmap — proposals, not completed features

## Next priority: independent proof verification

- Extract verification into a client that accepts a signed envelope and an explicit trusted issuer public key, without relying on the demo registry.
- Document how keys are distributed and pinned. Add adversarial key-substitution tests.
- Design a domain-control challenge. State clearly that DNS/website control is not legal identity verification.

## Publisher security

- Move signing to publisher-controlled tooling or protected keys; remove central server private-key custody.
- Design authenticated, audited revocation and expiration checks that remain available during outages.
- Model key rotation, compromise and recovery.
- Consider a signed transparency feed and reproducible verification policies.

## Client integrations

- Browser extension that checks selected text/URL, without reading all browser content by default.
- Telegram bot or Discord message command using official APIs and user-initiated requests.
- Android share target that accepts explicitly shared links and text from messaging apps.
- No claim of automatic or privileged access to private WhatsApp chats.

## Quality and user studies

- Add automated browser accessibility and visual-regression tests.
- Conduct user testing with sample (non-sensitive) student notices, and record both successes and failures.
- Add threat modeling for lookalike domains, compromised issuers and replayed posts.
- Obtain independent review before any public deployment.
