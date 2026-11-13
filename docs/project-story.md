# The project story

## Inspiration

A friend showed the founder a suspicious link connected to a Telegram bot, as a prank. He said it could collect camera photos, location and network information. These capabilities were **his claim**, not confirmed technical findings. He did not have programming experience.

The incident mattered because students often forward scholarship announcements and registration links through familiar group chats without checking whether the shared link matches the issuer's actual message. AduProof started with one question: can a publisher attach a checkable signature to the original announcement and link, so a reader can compare the copy they received?

## What we have built

A runnable **local demonstration** in Java 21 that issues Ed25519-signed sample notices, verifies exact content and approved destinations, and distinguishes modified, expired or withdrawn notices. The frontend has an interactive browser workflow and the backend has tests. It **does not verify real organizations**.

## Technical decisions

We deliberately avoided AI scam scoring and developed a narrow integrity check. We use Java's standard Ed25519 provider, a strict versioned format, specific result states, and a localhost-only demo publisher. The product does not claim a signature proves the truth of a message.

## AI collaboration

AI has played a substantial role in design exploration, implementation, test planning, writing and the visual identity. The founder identified the motivating incident and problem, selected the direction, rejected weaker approaches and guided the project's shape. Code produced with AI assistance must be reviewed and tested like any other contribution.

## Challenges and next steps

The hard part isn't just creating a signature. Verification needs a trustworthy link between a public key and a real publisher, protections against publisher key compromise, and authenticated revocation. Messaging platforms have different integration limits. This demo leaves those problems clearly identified instead of suggesting they are solved.

Next: independently verifiable proofs, verified publisher onboarding, and opt-in share-to-verify integrations. See [roadmap](roadmap.md).
