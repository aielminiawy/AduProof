package dev.aduproof.service;

public record VerificationResult(VerificationStatus status, boolean titleMatches,
                                 boolean bodyMatches, boolean destinationMatches,
                                 String explanation) {
}
