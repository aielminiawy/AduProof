package dev.aduproof.core;

/** A complete signed announcement, suitable for local storage or export. */
public record SignedNotice(NoticePayload payload, String signature) {
}
