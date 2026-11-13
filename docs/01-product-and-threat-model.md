# 1 — Product scope, acceptance criteria and threat model

## Why AduProof exists

A friend showed the project founder a suspicious link connected to a Telegram bot and described privacy-invasive things it might do. The claim itself was **not independently verified**. The incident raised a specific question: when a scholarship notice or public announcement is copied into a group chat, how can a reader compare the text and destination with what its publisher actually approved?

AI was used extensively as a research, design and implementation assistant; the founder defined the underlying problem, challenged proposed approaches and directed the product. Contributions should be acknowledged honestly in the hackathon story.

## The problem we actually solve

Given a publisher-approved signed announcement and the copy a reader received, verify that the title, text and destination are identical, that the cryptographic signature is valid under a **locally trusted demo issuer key**, and that the approval is current.

We do **not** inspect suspicious links, access messaging accounts, assess whether a URL is malicious, prove the legal identity of a domain owner, or prove that an announcement is true.

## Audience / demo story

- **Publisher**: creates a scholarship notice with an application URL.
- **Recipient**: receives a copy via any channel, checks the notice ID, title, body and URL.
- **Adversarial demonstration**: changes just one character in the destination and sees `MISMATCH`.
- **Publisher**: withdraws the notice; the original now shows `REVOKED`.

## Acceptance criteria

1. A local demo publisher obtains a persistent Ed25519 keypair and signs an announcement.
2. The cryptographic record is versioned and encoding is unambiguous.
3. An unmodified copy shows `MATCH`, a changed title/body/URL shows `MISMATCH`.
4. A revoked or expired notice never appears active.
5. A broken signature, unexpected signing key, or corrupted record cannot appear valid.
6. The web interface accepts user input safely and avoids rendered HTML from that input.
7. Tests cover signing, tampering, unknown IDs, expiry, revocation and persistence.
8. Local demo data and private keys are never included in the open-source repository.
9. The demo binds to loopback only. No claim of production readiness.
10. Clear human-readable limitations and the AI contribution statement are included.

## Trust boundaries

**Issuer:** locally generated private key, held by the demo server; never written into repository files. In a future production system keys belong to publishers and must be protected by a dedicated key management system.

**Reader:** untrusted form fields; exact field comparisons only. A browser UI is a convenience, not the security authority.

**Signature:** confirms the bytes were signed by a particular key. It does not prove domain ownership or organizational identity. The demo pins its own issuer public key. Production requires domain verification, independent identity claims and an auditable revocation model.

**Registry:** local persistent records with integrity checked on loading. Revocation entries are stored locally and have no independent authenticity against an attacker with file-system access.

**User:** always distinguish `MATCH`, `MISMATCH`, `REVOKED`, `EXPIRED`, `NOT_FOUND`, `INVALID_PROOF`. Never show a generic green "safe" badge.

## Non-goals for this release

- Official WhatsApp, Telegram and Discord integrations.
- Automatic scam detection or camera/network access.
- Public deployment, user registration, payment, rate limiting across the internet.
- Authenticating university or government identities.
- Preventing malicious publishers from signing malicious content.

## Open questions

- How should domain ownership be established and periodically revalidated?
- What trust store should a reader use when scanning outside this demo?
- How would authenticated publishers revoke a record under key compromise?
- How could a copy be verified when the reference has been stripped?
- Which client platforms allow a consent-based share-to-verify flow?
