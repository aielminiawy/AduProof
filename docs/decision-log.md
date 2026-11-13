# Engineering decisions

| Decision | Why | Trade-off |
| --- | --- | --- |
| Java 21 | Matches the author's learning goals and provides a standard Ed25519 provider | More verbose than a JavaScript-only demo |
| Standard library, no third-party runtime dependencies | Easy to audit and run without dependency download | Lightweight HTTP and encoding need careful tests |
| Ed25519 | Mature, standardized digital signatures | Authentication of the key owner remains a separate task |
| Exact byte-based comparison for notice fields | Easy to explain and avoids false assurances | Forwarding tools that change whitespace produce mismatches |
| Local demo issuer instead of pretending to verify organizations | Keeps claims honest | Must not be deployed as an institution-verification service |
| Loopback-only binding and CSRF token | Limits attack surface of publisher operations | Cannot verify from another device without engineering a secure public service |
| Explicit status categories | Avoids presenting "unverified" as "malicious" | More concepts for users to learn |
| Small bounded form API | No JSON dependencies; consistent input rules | API is not yet an integration-ready public JSON protocol |
