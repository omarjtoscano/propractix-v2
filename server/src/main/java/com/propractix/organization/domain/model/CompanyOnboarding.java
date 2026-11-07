package com.propractix.organization.domain.model;

import com.propractix.organization.domain.service.CompanyEmailAdmissionPolicy;
import com.propractix.organization.domain.service.CompanyRegistrationPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** State/invariants only: no provisioning, delivery, scheduling or account activation. */
public final class CompanyOnboarding {
    // READY is reserved for the verified H04 ceremony; E1 exposes no transition to it.
    public enum Status {
        SUBMITTED, IDENTITY_PENDING, EMAIL_PENDING, READY, FAILED, EXPIRED
    }
    public enum InitialRole {
        COMPANY_OWNER
    }

    private final OnboardingId id;
    private final CompanyProfile profile;
    private final EmailAddress administratorEmail;
    private final PolicyReference admissionPolicy;
    private final LocaleCode initialLocale;
    private final Instant submittedAt;
    private final Instant expiresAt;
    private Status status = Status.SUBMITTED;
    private long version;
    private Instant lastChangedAt;
    private Optional<TenantId> tenantId = Optional.empty();
    private Optional<EmailDomainRoutingResult> routingResult = Optional.empty();

    private CompanyOnboarding(OnboardingId id, CompanyProfile profile, EmailAddress email,
            PolicyReference admissionPolicy, LocaleCode initialLocale, Instant submittedAt, Instant expiresAt) {
        this.id = id;
        this.profile = profile;
        this.administratorEmail = email;
        this.admissionPolicy = admissionPolicy;
        this.initialLocale = initialLocale;
        this.submittedAt = submittedAt;
        this.expiresAt = expiresAt;
        this.lastChangedAt = submittedAt;
    }

    public static CompanyOnboarding submit(OnboardingId id, CompanyProfile profile, EmailAddress email,
            CompanyEmailAdmissionPolicy admission, CompanyRegistrationPolicy registration,
            Instant at, Duration validity) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(admission, "admission policy must be supplied");
        Objects.requireNonNull(registration, "registration policy must be supplied");
        Objects.requireNonNull(at, "at must not be null");
        Objects.requireNonNull(validity, "validity must not be null");
        if (validity.isNegative() || validity.isZero()) {
            throw new IllegalArgumentException("validity must be positive");
        }
        if (admission.evaluate(email) != CompanyEmailAdmissionPolicy.Decision.NOT_LISTED_AS_PUBLIC_PROVIDER) {
            throw new IllegalArgumentException("company_email_domain_not_allowed");
        }
        return new CompanyOnboarding(id, profile, email, admission.reference(),
                registration.initialLocale(profile), at, at.plus(validity));
    }

    public void awaitIdentity(Instant at) {
        requireLive(at);
        requireStatus(Status.SUBMITTED);
        status = Status.IDENTITY_PENDING;
        lastChangedAt = at;
        version++;
    }

    /** Called only after the owning application has established eligibility to create this company. */
    public void awaitEmailVerification(Company company, Instant at) {
        requireLive(at);
        requireStatus(Status.IDENTITY_PENDING);
        Objects.requireNonNull(company, "company must not be null");
        if (!profile.equals(company.profile()) || !initialLocale.equals(company.initialLocale())) {
            throw new IllegalArgumentException("company must match the admitted candidate profile");
        }
        tenantId = Optional.of(company.id());
        status = Status.EMAIL_PENDING;
        lastChangedAt = at;
        version++;
    }

    public void recordRoutingResult(EmailDomainRoutingResult result, Instant at) {
        requireLive(at);
        Objects.requireNonNull(result, "result must not be null");
        routingResult = Optional.of(result);
        lastChangedAt = at;
        version++;
    }

    public void fail(Instant at) {
        requireLive(at);
        status = Status.FAILED;
        lastChangedAt = at;
        version++;
    }

    public void expire(Instant at) {
        requireChronology(at);
        requirePending();
        if (at.isBefore(expiresAt)) {
            throw new IllegalStateException("onboarding has not expired");
        }
        status = Status.EXPIRED;
        lastChangedAt = at;
        version++;
    }

    private void requireLive(Instant at) {
        requireChronology(at);
        requirePending();
        if (!at.isBefore(expiresAt)) {
            throw new IllegalStateException("onboarding has expired");
        }
    }

    private void requireChronology(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        if (at.isBefore(lastChangedAt)) {
            throw new IllegalArgumentException("time must not move backwards");
        }
    }

    private void requirePending() {
        if (status != Status.SUBMITTED && status != Status.IDENTITY_PENDING && status != Status.EMAIL_PENDING) {
            throw new IllegalStateException("onboarding is terminal");
        }
    }

    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new IllegalStateException("illegal onboarding transition");
        }
    }

    public OnboardingId id() {
        return id;
    }

    public CompanyProfile profile() {
        return profile;
    }

    public EmailAddress administratorEmail() {
        return administratorEmail;
    }

    public PolicyReference admissionPolicy() {
        return admissionPolicy;
    }

    public LocaleCode initialLocale() {
        return initialLocale;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Status status() {
        return status;
    }

    public Instant lastChangedAt() {
        return lastChangedAt;
    }

    public Optional<TenantId> tenantId() {
        return tenantId;
    }

    public Optional<EmailDomainRoutingResult> routingResult() {
        return routingResult;
    }

    public long version() {
        return version;
    }

    public InitialRole initialRole() {
        return InitialRole.COMPANY_OWNER;
    }

}
