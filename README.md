# AduProof

**What did the publisher actually publish?**

AduProof is an open-source **Java 21 local prototype** for signing announcements and checking whether a copy's title, text, and destination link still match the publisher's signed record.

An authentic-looking scholarship announcement can be copied with a different application link. The current demo illustrates how signed records can make that change visible. It is **not** an AI scam detector, a link safety scanner, or an official institution-verification service.

![AduProof brand banner](docs/brand-banner.jpg)

> **Read before using:** This is a localhost educational prototype. Its signing key belongs to an explicitly labeled **demo publisher**, not a validated university, company, or person. A valid signature does not mean the announcement is safe or true. Never publish this server publicly as-is.

## What already works

- Generate and persist a local Ed25519 signing keypair.
- Issue a digitally signed announcement with an HTTPS destination and expiry.
- Return an encoded, versioned proof envelope and a shareable notice ID.
- Verify exact received title, body, and destination against the signed record.
- Detect a changed link or any other changed signed field.
- Report revoked, expired, missing, and invalid records distinctly.
- Withdraw approval and keep that state after a server restart.
- Run a responsive browser interface and dependency-free Java test suite.

## Quick start

Requirements: **JDK 21**, with `java` and `javac` available on the command line. No Maven, npm, cloud account, database, or paid API is required.

### Run the prebuilt JAR (requires Java 21 runtime)

```bash
java -jar dist/aduproof-local-demo.jar
```

### Windows (PowerShell)

```powershell
.\scripts\run.ps1
```

To run tests:

```powershell
.\scripts\test.ps1
```

### Linux / macOS

```bash
bash scripts/run.sh
```

Tests:

```bash
bash scripts/test.sh
```

Open **http://127.0.0.1:8787** in a browser. Stop with `Ctrl+C`.

**What to click:**

1. Keep or edit the sample scholarship notice, then click **Issue signed notice**.
2. Click **Check this copy**. It should report an exact match.
3. Click **Change the link**, then **Check this copy**. It should show `MISMATCH` and the link field as different.
4. Click **Use original**, then **Withdraw this notice**, and check again. It should show `REVOKED`.

The application stores local demo data in `data/` and excludes it from version control. To reset this demo only, stop the server and delete the local `data/` directory; this deletes all demo notices and the signing key.

## How the parts fit together

```text
     Browser (HTML / CSS / JavaScript)
                      |
          Loopback-only HTTP adapter
                      |
              ProofService (Java)
                 /          \
          NoticeCodec     LocalRepository
                 |          /     \
          Ed25519Signer  Records  Demo issuer keys
```

- `core/`: signed records, canonical serialization, Ed25519 signatures and input validation.
- `service/`: publisher and verification business rules and statuses.
- `storage/`: persistent demo keypair, notice registry, revocations.
- `web/`: localhost-only HTTP routes; static front-end assets in `src/main/resources/web/`.
- `src/test/`: dependency-free test runner with cryptographic, logical and HTTP tests.

## Five incremental development sections

The implementation was broken into five sections, with each new section reviewed against the previous ones:

1. [Product scope and threat model](docs/01-product-and-threat-model.md)
2. [Digital signature protocol](docs/02-crypto-protocol.md)
3. [Service and local persistence](docs/03-local-service.md)
4. [Browser demonstration](docs/04-browser-demo.md)
5. [Testing and review](docs/05-testing-and-review.md)

For the rationale behind major choices, see [the engineering decision log](docs/decision-log.md).

## Status meanings

| Status | Meaning |
| --- | --- |
| `MATCH` | A signed notice exists under the locally pinned demo key; title, body and destination match; it is active. |
| `MISMATCH` | At least one of those fields differs from the signed record. |
| `REVOKED` | The demo publisher withdrew approval. |
| `EXPIRED` | Its expiry has passed. |
| `INVALID_PROOF` | The stored record's signature is broken or is not from the trusted demo key. |
| `NOT_FOUND` | There is no such notice ID in this local demo. |

**An unsigned post is not automatically malicious. An Ed25519 signature does not prove legal identity, good intentions, or truth.**

## The story behind the idea

The project started after a friend showed its founder a suspicious link connected to a Telegram bot. The friend described invasive capabilities, but those claims were not independently verified. The incident raised a broader concern: students routinely forward scholarship opportunities, files, and registration links through group chats, while copied content can be changed without clearly exposing the modification.

The founder proposed shifting the question from “Does this link look suspicious?” to “Does this copy match what its publisher signed?” The current release focuses on that narrow, testable claim.

AI was used substantially to research technical approaches, critique design decisions, help draft code and tests, and refine the visual identity. The founder defined the original problem, directed the concept, challenged alternatives, and is responsible for reviewing what the project claims. See [project story and AI disclosure](docs/project-story.md).

## Security and technical limits

- **Not internet-facing.** All endpoints bind to `127.0.0.1`. Demo CSRF protections are not a substitute for a real authorization system.
- **No identity proof.** The signing key is controlled by this local installation; no official domain or institutional identity has been verified.
- **No real chat integrations.** Telegram, Discord, WhatsApp and browser extensions are roadmap ideas, not current features.
- **Local storage only.** Do not use real student data, passwords, secrets, or sensitive notices.
- **Exact-match comparison.** Different whitespace or formatting may return a mismatch. This is deliberate for the first version.
- **Not a malware scanner.** The software never opens the destination URL or rates the site as safe.
- **No transparency ledger.** Revocation state is stored locally and can be changed by someone who controls the machine.
- **No independent security audit.** The test suite is useful evidence, not a security certification.

See [SECURITY.md](SECURITY.md) and [planned work](docs/roadmap.md).

## Contributing

Suggestions, bug reports and tests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before contributing and include a reproducible failing test for behavior changes whenever possible.

## License

MIT. See [LICENSE](LICENSE).

## Publish to GitHub

See [GitHub setup instructions](docs/GITHUB_SETUP.md). The source code is maintained at [AduProof on GitHub](https://github.com/aielminiawy/AduProof).
