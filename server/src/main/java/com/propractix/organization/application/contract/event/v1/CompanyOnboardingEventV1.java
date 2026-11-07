package com.propractix.organization.application.contract.event.v1;

import com.propractix.organization.domain.model.OnboardingId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** ID-only durable messages; recipient and personal data are resolved transiently by the owner. */
public record CompanyOnboardingEventV1(UUID eventId, Type type, OnboardingId onboardingId,
        long aggregateVersion, Instant occurredAt, Instant availableAt, UUID correlationId) {
    public enum Type {
        COMPANY_ONBOARDING_SUBMITTED,
        ADMINISTRATOR_PROVISIONING_REQUESTED,
        EMAIL_DOMAIN_ROUTING_RECHECK_REQUESTED
    }

    public CompanyOnboardingEventV1 {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(onboardingId, "onboardingId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(availableAt, "availableAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        if (eventId.version() != 4 || eventId.variant() != 2 || aggregateVersion < 0
                || availableAt.isBefore(occurredAt)) {
            throw new IllegalArgumentException("invalid event metadata");
        }
    }

    public int eventVersion() { return 1; }
    public String producerContext() { return "organization"; }
    public String aggregateType() { return "CompanyOnboarding"; }
}
