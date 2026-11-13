# Security policy

AduProof v0.1 is a **localhost-only educational prototype**, not an audited identity-verification service. Do not deploy it to a public server, place real private information in it, or represent its demo issuer as a verified organization.

To report a security issue, privately contact the repository maintainer after the repository is published. Avoid posting exploit details or private data in public issues.

## Known limitations

- Publisher private key resides on the local filesystem and is generated for the demo. It is not hardware-backed or password-protected.
- Signed announcements are integrity-protected, but the local revocation database and local trust anchor are not tamper-resistant against filesystem attackers.
- Domain control and organization identity are **not verified**.
- No distributed trust registry, third-party key discovery, production login, or cloud infrastructure is provided.
- No malware scanning, phishing classifier, URL reputation check, or link preview fetching is performed.
- Validation requires an exact match for title, body, and URL; copied content may change whitespace.
- No guarantees against social engineering or compromised publishers.
