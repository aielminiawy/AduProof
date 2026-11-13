# Contributing to AduProof

Thanks for improving a small but security-sensitive learning project.

1. Start with the [product scope](docs/01-product-and-threat-model.md) and [security boundaries](SECURITY.md). Do not turn the demo into a public trust authority through a small change.
2. Open an issue describing the problem, the expected behavior and acceptance criteria.
3. Keep pull requests small. If possible, put one behavior change in each PR and include a regression test.
4. Run `bash scripts/test.sh` (or `.\\scripts\\test.ps1` on Windows) locally.
5. Avoid unrelated formatting changes, extra packages, hidden remote dependencies, and new features that cannot be demonstrated.
6. Never commit signing keys, `data/`, tokens, personal details, or real suspicious links.
7. Explain security implications and update docs when the trust model changes.
8. Disclose substantial AI assistance in PRs, and review all submitted code and dependency licenses yourself.

## Suggested first contributions

- Add more malformed-input and Unicode edge-case tests.
- Improve accessible messaging for `UNKNOWN` and `MISMATCH` states.
- Separate generic HTTP parsing into small testable functions.
- Add robust automated browser tests.
- Build a publisher-controlled signing CLI with no private key on the verifier server.

Security reports should be handled privately; see [SECURITY.md](SECURITY.md).
