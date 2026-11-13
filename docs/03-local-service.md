# 3 — Publisher, trust and local persistence

`ProofService` handles business rules; `LocalRepository` handles key generation and file persistence. These layers cannot depend on HTTP or browser code. The publisher private key is stored only in an ignored `data/` directory on the developer's machine. The demo pins that key's corresponding public key as its only trust anchor.

Notices are append-only at the service level. Revocations are separate. Each update is written via a temporary file and a replace/rename operation. Loading fails closed on malformed stored records rather than pretending the database is empty.

This is an honest prototype compromise. It is not an externally governed, independently audited revocation log, and it is not safe to host as a public publishing API. Private-key protection under Windows depends on Windows filesystem ACLs.

The `ProofService.verify()` return type contains machine-readable status plus separate matches for title, body and destination. Even after a match, the user sees a warning that the publisher identity is not legally verified.
